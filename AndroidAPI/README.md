# Android API Backend

ASP.NET Core Web API for Android Todo List application.

## Description

RESTful API providing endpoints for:
- Task management (CRUD operations)
- Task status toggling (completed/not completed)
- Health check

## Technologies

- .NET 10.0
- ASP.NET Core Minimal APIs
- Swagger/OpenAPI
- CORS support

## Getting Started

### Requirements
- .NET 10.0 SDK

### Run in Development Mode

```bash
cd AndroidAPI
dotnet restore
dotnet run
```

API will be available at:
- HTTP: http://localhost:5000
- HTTPS: https://localhost:5001
- Swagger UI: http://localhost:5000/swagger

### Run in Production Mode

```bash
dotnet run --configuration Release
```

## API Endpoints

### Health Check
- `GET /api/health` - Check API status

### Tasks
- `GET /api/tasks` - Get all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/{id}` - Update task
- `PATCH /api/tasks/{id}/toggle` - Toggle task status (completed/not completed)
- `DELETE /api/tasks/{id}` - Delete task

## Data Models

### TodoTask
```json
{
  "id": 1,
  "title": "Buy groceries",
  "description": "Milk, bread, eggs",
  "isCompleted": false,
  "createdAt": "2025-12-17T10:00:00Z"
}
```

### CreateTaskRequest
```json
{
  "title": "New task",
  "description": "Task description (optional)"
}
```

### UpdateTaskRequest
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "isCompleted": true
}
```

## Usage Examples

### cURL

```bash
# Health check
curl http://localhost:5000/api/health

# Get all tasks
curl http://localhost:5000/api/tasks

# Create task
curl -X POST http://localhost:5000/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"New task","description":"Description"}'

# Toggle task status
curl -X PATCH http://localhost:5000/api/tasks/1/toggle

# Update task
curl -X PUT http://localhost:5000/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated task","isCompleted":true}'

# Delete task
curl -X DELETE http://localhost:5000/api/tasks/1
```

## Android Configuration

In your Android app, use the following base URL:

- For emulator: `http://10.0.2.2:5000`
- For physical device: `http://{YOUR_IP}:5000`

Replace `{YOUR_IP}` with your computer's IP address on the local network.

## Development

### Adding New Endpoints

Add new endpoints to [Program.cs](Program.cs):

```csharp
app.MapGet("/api/yourEndpoint", () => Results.Ok(new { data = "your data" }))
    .WithName("YourEndpointName");
```

### CORS

CORS is configured to allow all origins in development mode. For production, configure specific origins in [Program.cs](Program.cs#L10-L18).

## License

MIT
