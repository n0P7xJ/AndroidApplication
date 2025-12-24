package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class TodoTask(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: String,
    val imageUrl: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

// Навігація додатку
@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentScreen by remember { mutableStateOf("login") }
    
    when {
        currentUser != null -> {
            TaskListScreen(
                user = currentUser!!,
                onLogout = {
                    currentUser = null
                    currentScreen = "login"
                }
            )
        }
        currentScreen == "register" -> {
            RegisterScreen(
                onRegisterSuccess = { user ->
                    currentUser = user
                },
                onNavigateToLogin = {
                    currentScreen = "login"
                }
            )
        }
        else -> {
            LoginScreen(
                onLoginSuccess = { user ->
                    currentUser = user
                },
                onNavigateToRegister = {
                    currentScreen = "register"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    user: User? = null,
    onLogout: () -> Unit = {}
) {
    var tasks by remember { mutableStateOf<List<TodoTask>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TodoTask?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Завантаження задач при запуску
    LaunchedEffect(Unit) {
        loadTasks(
            onLoading = { isLoading = it },
            onSuccess = { tasks = it },
            onError = { errorMessage = it }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Мої задачі")
                        if (user != null) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            loadTasks(
                                onLoading = { isLoading = it },
                                onSuccess = { tasks = it },
                                onError = { errorMessage = it }
                            )
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Оновити")
                    }
                    if (user != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Вийти")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Додати задачу")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Показати помилку якщо є
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Показати індикатор завантаження
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Немає задач. Додайте нову!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = {
                                scope.launch {
                                    try {
                                        RetrofitInstance.api.toggleTask(task.id)
                                        loadTasks(
                                            onLoading = { isLoading = it },
                                            onSuccess = { tasks = it },
                                            onError = { errorMessage = it }
                                        )
                                    } catch (e: Exception) {
                                        errorMessage = "Помилка: ${e.message}"
                                    }
                                }
                            },
                            onEdit = {
                                taskToEdit = task
                                showEditDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        RetrofitInstance.api.deleteTask(task.id)
                                        loadTasks(
                                            onLoading = { isLoading = it },
                                            onSuccess = { tasks = it },
                                            onError = { errorMessage = it }
                                        )
                                    } catch (e: Exception) {
                                        errorMessage = "Помилка: ${e.message}"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AddTaskDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, description, imageFile ->
                    scope.launch {
                        try {
                            val titlePart = createTextRequestBody(title)
                            val descriptionPart = createTextRequestBody(description)
                            val imagePart = createImagePart(imageFile)
                            RetrofitInstance.api.createTask(titlePart, descriptionPart, imagePart)
                            loadTasks(
                                onLoading = { isLoading = it },
                                onSuccess = { tasks = it },
                                onError = { errorMessage = it }
                            )
                            showDialog = false
                        } catch (e: Exception) {
                            errorMessage = "Помилка створення задачі: ${e.message}"
                        }
                    }
                }
            )
        }
        
        // Діалог редагування задачі
        if (showEditDialog && taskToEdit != null) {
            EditTaskDialog(
                task = taskToEdit!!,
                onDismiss = { 
                    showEditDialog = false
                    taskToEdit = null
                },
                onConfirm = { title, description ->
                    scope.launch {
                        try {
                            val request = UpdateTaskRequest(
                                title = title,
                                description = description,
                                isCompleted = null
                            )
                            RetrofitInstance.api.updateTask(taskToEdit!!.id, request)
                            loadTasks(
                                onLoading = { isLoading = it },
                                onSuccess = { tasks = it },
                                onError = { errorMessage = it }
                            )
                            showEditDialog = false
                            taskToEdit = null
                        } catch (e: Exception) {
                            errorMessage = "Помилка редагування задачі: ${e.message}"
                        }
                    }
                }
            )
        }
    }
}

// Функція для завантаження задач з API
suspend fun loadTasks(
    onLoading: (Boolean) -> Unit,
    onSuccess: (List<TodoTask>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        onLoading(true)
        onError("")  // Очистити попередні помилки
        val response = RetrofitInstance.api.getTasks()
        val tasks = response.map { taskResponse ->
            TodoTask(
                id = taskResponse.id,
                title = taskResponse.title,
                description = taskResponse.description,
                isCompleted = taskResponse.isCompleted,
                createdAt = taskResponse.createdAt,
                imageUrl = taskResponse.imageUrl
            )
        }
        onSuccess(tasks)
    } catch (e: Exception) {
        onError("Не вдалося завантажити задачі: ${e.message}")
        onSuccess(emptyList())
    } finally {
        onLoading(false)
    }
}

@Composable
fun TaskItem(
    task: TodoTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Показати зображення якщо є
            task.imageUrl?.let { imageUrl ->
                val fullImageUrl = "${RetrofitInstance.BASE_URL}$imageUrl"
                Image(
                    painter = rememberAsyncImagePainter(fullImageUrl),
                    contentDescription = "Зображення задачі",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Кнопка редагування
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редагувати",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Кнопка видалення
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Видалити",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Діалог редагування задачі
@Composable
fun EditTaskDialog(
    task: TodoTask,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати задачу") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва задачі") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис (необов'язково)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title, description)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, File?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    
    // Лаунчер для вибору зображення з галереї
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageFile = uriToFile(context, it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нова задача") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва задачі") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис (необов'язково)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Секція вибору фото
                Text(
                    text = "Фото (необов'язково)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (selectedImageUri != null) {
                    // Показати прев'ю обраного зображення
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Обране зображення",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Кнопка для видалення зображення
                        IconButton(
                            onClick = { 
                                selectedImageUri = null
                                imageFile = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Видалити фото",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    // Кнопка для вибору фото
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Вибрати фото з галереї")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title, description, imageFile)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Додати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

// Допоміжна функція для конвертації URI в File
fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "upload_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun TaskListPreview() {
    MyApplicationTheme {
        TaskListScreen()
    }
}