# Android Todo List Application

Modern Android application built with Jetpack Compose for managing tasks.

## Features

- 📱 Modern UI with Material Design 3
- 🔄 Real-time data synchronization with backend API
- ✅ Task completion tracking
- ➕ Add, edit, and delete tasks
- 🔄 Pull to refresh
- 🌐 Network error handling
- 📊 Loading states

## Technology Stack

- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - UI components
- **Retrofit** - HTTP client
- **Coroutines** - Asynchronous programming
- **Gson** - JSON serialization

## Prerequisites

- Android Studio (latest version)
- Android SDK 35 or higher
- JDK 11
- Backend API running (see [AndroidAPI](../AndroidAPI/README.md))

## Setup

1. **Open Project**
   ```bash
   # Open AndroidApplication folder in Android Studio
   ```

2. **Configure Backend URL**
   
   Edit `app/src/main/java/com/example/myapplication/ApiService.kt`:
   
   - For emulator: `http://10.0.2.2:5000/`
   - For physical device: `http://YOUR_COMPUTER_IP:5000/`

3. **Sync Gradle**
   - Wait for Gradle sync to complete
   - All dependencies will be downloaded automatically

4. **Run**
   - Click Run button or press `Shift + F10`
   - Select emulator or connected device

## Project Structure

```
app/src/main/
├── java/com/example/myapplication/
│   ├── MainActivity.kt          # Main activity and UI
│   ├── ApiService.kt           # API client configuration
│   └── ui/theme/              # Theme configuration
├── res/
│   ├── values/                # Strings, colors, themes
│   └── drawable/              # Icons and images
└── AndroidManifest.xml        # App configuration
```

## API Integration

The app communicates with the backend using Retrofit:

```kotlin
// Get all tasks
suspend fun getTasks(): List<TodoTaskResponse>

// Create task
suspend fun createTask(@Body request: CreateTaskRequest): TodoTaskResponse

// Toggle task completion
suspend fun toggleTask(@Path("id") id: Int): TodoTaskResponse

// Delete task
suspend fun deleteTask(@Path("id") id: Int)
```

## UI Components

### TaskListScreen
Main screen displaying the list of tasks with:
- Top app bar with refresh button
- Lazy column for task list
- Floating action button to add tasks
- Loading indicator
- Error messages

### TaskItem
Individual task card with:
- Checkbox for completion status
- Task title and description
- Delete button
- Strike-through text for completed tasks

### AddTaskDialog
Dialog for creating new tasks:
- Title input field
- Description input field
- Confirm/Cancel buttons

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## Troubleshooting

### Cannot connect to backend
- Verify backend is running at the correct port
- Check BASE_URL in ApiService.kt
- For emulator, use `10.0.2.2` instead of `localhost`
- For physical device, ensure same Wi-Fi network

### Gradle sync issues
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Dependencies not resolving
- Check internet connection
- Clear Gradle cache: File → Invalidate Caches → Invalidate and Restart

## License

MIT
