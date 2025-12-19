package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

data class TodoTask(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TaskListScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen() {
    var tasks by remember { mutableStateOf<List<TodoTask>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
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
                title = { Text("Мої задачі") },
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
                onConfirm = { title, description ->
                    scope.launch {
                        try {
                            val request = CreateTaskRequest(title, description.ifBlank { null })
                            RetrofitInstance.api.createTask(request)
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
                createdAt = taskResponse.createdAt
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
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            
            TextButton(onClick = onDelete) {
                Text("Видалити", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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

@Preview(showBackground = true)
@Composable
fun TaskListPreview() {
    MyApplicationTheme {
        TaskListScreen()
    }
}