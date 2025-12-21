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

// In-memory data storage (replace with database in production)
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
        existingTask.CreatedAt
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
