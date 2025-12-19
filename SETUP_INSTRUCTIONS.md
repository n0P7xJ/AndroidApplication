# Setup Instructions

## Backend (ASP.NET Core API)

1. Start the backend server:
```bash
cd AndroidAPI
dotnet run
```

Backend will be available at `http://0.0.0.0:5000`

## Frontend (Android App)

### Configuration

1. **For Android Emulator:** 
   - IP address in ApiService.kt is already configured to `10.0.2.2:5000`
   - `10.0.2.2` is a special address that points to `localhost` of the host machine from the emulator

2. **For Physical Device:**
   - Find your computer's IP address on the local network (e.g., `192.168.1.100`)
   - Open `ApiService.kt` and change:
     ```kotlin
     private const val BASE_URL = "http://YOUR_IP:5000/"
     ```
   - Make sure the device and computer are connected to the same Wi-Fi network

### Running

1. Open the AndroidApplication project in Android Studio
2. Wait for Gradle sync to complete
3. Run the app on emulator or physical device

## Features

- ✅ Task list loads from backend on startup
- ✅ Add new tasks
- ✅ Mark tasks as completed
- ✅ Delete tasks
- ✅ Refresh list with button
- ✅ Display network errors

## API Testing

You can test the API through browser or Postman:

- `GET http://localhost:5000/api/tasks` - get all tasks
- `GET http://localhost:5000/api/health` - check server status
- Swagger UI: `http://localhost:5000/swagger`

## Troubleshooting

1. **Connection error on emulator:**
   - Make sure backend is running
   - Use `10.0.2.2` instead of `localhost`

2. **Error on physical device:**
   - Check that both devices are on the same network
   - Check firewall settings on your computer
   - Make sure you're using the correct IP address

3. **SSL/HTTPS errors:**
   - Use HTTP instead of HTTPS for local development
   - Emulator may require additional configuration for HTTPS
