using Microsoft.AspNetCore.Mvc;
using System.Text.Json;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddAntiforgery();
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAndroidApp",
        policy =>
        {
            policy.AllowAnyOrigin()
                  .AllowAnyHeader()
                  .AllowAnyMethod();
        });
});

var app = builder.Build();

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors("AllowAndroidApp");
app.UseStaticFiles();

// Переконайтесь, що папка для зображень існує
var uploadsPath = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", "uploads");
if (!Directory.Exists(uploadsPath))
{
    Directory.CreateDirectory(uploadsPath);
}

// Папка для аватарів користувачів
var avatarsPath = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", "avatars");
if (!Directory.Exists(avatarsPath))
{
    Directory.CreateDirectory(avatarsPath);
}

// In-memory data storage (replace with database in production)
var users = new List<User>();
int nextUserId = 1;

var tasks = new List<TodoTask>
{
    new(1, "Купити продукти", "Молоко, хліб, яйця", false, DateTime.UtcNow, null),
    new(2, "Зробити домашнє завдання", "Математика та фізика", false, DateTime.UtcNow.AddHours(-2), null),
    new(3, "Прибрати квартиру", "Пропилососити та помити підлогу", true, DateTime.UtcNow.AddDays(-1), null),
    new(4, "Зателефонувати мамі", "", false, DateTime.UtcNow.AddMinutes(-30), null),
    new(5, "Піти в спортзал", "Тренування о 18:00", false, DateTime.UtcNow, null)
};

int nextTaskId = 6;

// Endpoints

// Health check
app.MapGet("/api/health", () => Results.Ok(new { status = "healthy", timestamp = DateTime.UtcNow }))
    .WithName("HealthCheck");

// ==================== AUTH ENDPOINTS ====================

// Реєстрація користувача
app.MapPost("/api/auth/register", async (HttpRequest request) =>
{
    var form = await request.ReadFormAsync();
    var email = form["email"].ToString().ToLower().Trim();
    var password = form["password"].ToString();
    var name = form["name"].ToString();
    
    // Валідація
    if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(password))
    {
        return Results.BadRequest(new { error = "Email та пароль обов'язкові" });
    }
    
    if (password.Length < 6)
    {
        return Results.BadRequest(new { error = "Пароль має бути мінімум 6 символів" });
    }
    
    // Перевірка чи email вже існує
    if (users.Any(u => u.Email == email))
    {
        return Results.BadRequest(new { error = "Користувач з таким email вже існує" });
    }
    
    // Обробка аватара
    string? avatarUrl = null;
    var avatarFile = form.Files.GetFile("avatar");
    if (avatarFile != null && avatarFile.Length > 0)
    {
        var fileName = $"{Guid.NewGuid()}{Path.GetExtension(avatarFile.FileName)}";
        var filePath = Path.Combine(avatarsPath, fileName);
        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await avatarFile.CopyToAsync(stream);
        }
        avatarUrl = $"/avatars/{fileName}";
    }
    
    // Хешування пароля (спрощена версія, в продакшні використовуйте BCrypt)
    var passwordHash = Convert.ToBase64String(
        System.Security.Cryptography.SHA256.HashData(
            System.Text.Encoding.UTF8.GetBytes(password + "salt_key_123")
        )
    );
    
    var newUser = new User(
        nextUserId++,
        email,
        passwordHash,
        string.IsNullOrWhiteSpace(name) ? email.Split('@')[0] : name,
        avatarUrl,
        DateTime.UtcNow
    );
    
    users.Add(newUser);
    
    return Results.Created($"/api/users/{newUser.Id}", new UserResponse(
        newUser.Id,
        newUser.Email,
        newUser.Name,
        newUser.AvatarUrl,
        newUser.CreatedAt
    ));
})
    .WithName("Register")
    .DisableAntiforgery();

// Логін
app.MapPost("/api/auth/login", ([FromBody] LoginRequest request) =>
{
    var email = request.Email.ToLower().Trim();
    var passwordHash = Convert.ToBase64String(
        System.Security.Cryptography.SHA256.HashData(
            System.Text.Encoding.UTF8.GetBytes(request.Password + "salt_key_123")
        )
    );
    
    var user = users.FirstOrDefault(u => u.Email == email && u.PasswordHash == passwordHash);
    
    if (user == null)
    {
        return Results.Unauthorized();
    }
    
    return Results.Ok(new UserResponse(
        user.Id,
        user.Email,
        user.Name,
        user.AvatarUrl,
        user.CreatedAt
    ));
})
    .WithName("Login");

// Отримати профіль користувача
app.MapGet("/api/users/{id}", (int id) =>
{
    var user = users.FirstOrDefault(u => u.Id == id);
    if (user == null) return Results.NotFound();
    
    return Results.Ok(new UserResponse(
        user.Id,
        user.Email,
        user.Name,
        user.AvatarUrl,
        user.CreatedAt
    ));
})
    .WithName("GetUserById");

// Оновити профіль користувача
app.MapPut("/api/users/{id}", async (int id, HttpRequest request) =>
{
    var index = users.FindIndex(u => u.Id == id);
    if (index == -1) return Results.NotFound();
    
    var form = await request.ReadFormAsync();
    var name = form["name"].ToString();
    var existingUser = users[index];
    
    // Обробка нового аватара
    string? avatarUrl = existingUser.AvatarUrl;
    var avatarFile = form.Files.GetFile("avatar");
    if (avatarFile != null && avatarFile.Length > 0)
    {
        // Видалити старий аватар
        if (!string.IsNullOrEmpty(existingUser.AvatarUrl))
        {
            var oldPath = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", existingUser.AvatarUrl.TrimStart('/'));
            if (File.Exists(oldPath)) File.Delete(oldPath);
        }
        
        var fileName = $"{Guid.NewGuid()}{Path.GetExtension(avatarFile.FileName)}";
        var filePath = Path.Combine(avatarsPath, fileName);
        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await avatarFile.CopyToAsync(stream);
        }
        avatarUrl = $"/avatars/{fileName}";
    }
    
    var updatedUser = existingUser with
    {
        Name = string.IsNullOrWhiteSpace(name) ? existingUser.Name : name,
        AvatarUrl = avatarUrl
    };
    
    users[index] = updatedUser;
    
    return Results.Ok(new UserResponse(
        updatedUser.Id,
        updatedUser.Email,
        updatedUser.Name,
        updatedUser.AvatarUrl,
        updatedUser.CreatedAt
    ));
})
    .WithName("UpdateUser")
    .DisableAntiforgery();

// ==================== TASKS ENDPOINTS ====================

// Tasks endpoints
app.MapGet("/api/tasks", () => Results.Ok(tasks.OrderByDescending(t => t.CreatedAt)))
    .WithName("GetAllTasks");

app.MapGet("/api/tasks/{id}", (int id) =>
{
    var task = tasks.FirstOrDefault(t => t.Id == id);
    return task is not null ? Results.Ok(task) : Results.NotFound();
})
    .WithName("GetTaskById");

app.MapPost("/api/tasks", async (HttpRequest request) =>
{
    var form = await request.ReadFormAsync();
    var title = form["title"].ToString();
    var description = form["description"].ToString();
    
    string? imageUrl = null;
    var imageFile = form.Files.GetFile("image");
    if (imageFile != null && imageFile.Length > 0)
    {
        var fileName = $"{Guid.NewGuid()}{Path.GetExtension(imageFile.FileName)}";
        var filePath = Path.Combine(uploadsPath, fileName);
        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await imageFile.CopyToAsync(stream);
        }
        imageUrl = $"/uploads/{fileName}";
    }
    
    var newTask = new TodoTask(
        nextTaskId++,
        title,
        description,
        false,
        DateTime.UtcNow,
        imageUrl
    );
    tasks.Add(newTask);
    return Results.Created($"/api/tasks/{newTask.Id}", newTask);
})
    .WithName("CreateTask")
    .DisableAntiforgery();

app.MapPut("/api/tasks/{id}", (int id, [FromBody] UpdateTaskRequest request) =>
{
    var index = tasks.FindIndex(t => t.Id == id);
    if (index == -1) return Results.NotFound();
    
    var existingTask = tasks[index];
    var updatedTask = new TodoTask(
        existingTask.Id,
        request.Title ?? existingTask.Title,
        request.Description ?? existingTask.Description,
        request.IsCompleted ?? existingTask.IsCompleted,
        existingTask.CreatedAt,
        existingTask.ImageUrl
    );
    tasks[index] = updatedTask;
    return Results.Ok(updatedTask);
})
    .WithName("UpdateTask");

app.MapPatch("/api/tasks/{id}/toggle", (int id) =>
{
    var index = tasks.FindIndex(t => t.Id == id);
    if (index == -1) return Results.NotFound();
    
    var existingTask = tasks[index];
    var updatedTask = existingTask with { IsCompleted = !existingTask.IsCompleted };
    tasks[index] = updatedTask;
    return Results.Ok(updatedTask);
})
    .WithName("ToggleTask");

// Endpoint для видалення зображення при видаленні задачі
app.MapDelete("/api/tasks/{id}", (int id) =>
{
    var task = tasks.FirstOrDefault(t => t.Id == id);
    if (task is null) return Results.NotFound();
    
    // Видаляємо файл зображення якщо він є
    if (!string.IsNullOrEmpty(task.ImageUrl))
    {
        var imagePath = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", task.ImageUrl.TrimStart('/'));
        if (File.Exists(imagePath))
        {
            File.Delete(imagePath);
        }
    }
    
    tasks.Remove(task);
    return Results.NoContent();
})
    .WithName("DeleteTask");

app.Run();

// Models
record TodoTask(int Id, string Title, string Description, bool IsCompleted, DateTime CreatedAt, string? ImageUrl);
record CreateTaskRequest(string Title, string? Description, string? ImageUrl);
record UpdateTaskRequest(string? Title, string? Description, bool? IsCompleted, string? ImageUrl);

// Auth Models
record User(int Id, string Email, string PasswordHash, string Name, string? AvatarUrl, DateTime CreatedAt);
record UserResponse(int Id, string Email, string Name, string? AvatarUrl, DateTime CreatedAt);
record LoginRequest(string Email, string Password);
record RegisterRequest(string Email, string Password, string? Name);
