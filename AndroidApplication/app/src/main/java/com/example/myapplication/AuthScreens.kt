package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Модель для збереження стану користувача
data class User(
    val id: Int,
    val email: String,
    val name: String,
    val avatarUrl: String?
)

// Екран логіну
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вхід") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Іконка
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Помилка
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Пароль
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Сховати пароль" else "Показати пароль"
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопка входу
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = RetrofitInstance.api.login(LoginRequest(email, password))
                            onLoginSuccess(User(response.id, response.email, response.name, response.avatarUrl))
                        } catch (e: Exception) {
                            errorMessage = "Невірний email або пароль"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Увійти")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Перехід на реєстрацію
            TextButton(onClick = onNavigateToRegister) {
                Text("Немає акаунту? Зареєструватися")
            }
        }
    }
}

// Екран реєстрації з редагуванням фото
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (User) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Стан для фото
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var avatarFile by remember { mutableStateOf<File?>(null) }
    var showImageEditor by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Лаунчер для вибору зображення
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showImageEditor = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Реєстрація") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Помилка
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Вибір аватара
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (editedBitmap != null) {
                    Image(
                        bitmap = editedBitmap!!.asImageBitmap(),
                        contentDescription = "Аватар",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Додати фото",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Додати фото",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (editedBitmap != null) {
                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Змінити фото")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Ім'я
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ім'я") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Пароль
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                isError = password.isNotEmpty() && password.length < 6,
                supportingText = {
                    if (password.isNotEmpty() && password.length < 6) {
                        Text("Мінімум 6 символів")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Підтвердження паролю
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Підтвердіть пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text("Паролі не співпадають")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопка реєстрації
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val emailPart = createTextRequestBody(email)
                            val passwordPart = createTextRequestBody(password)
                            val namePart = createTextRequestBody(name)
                            val avatarPart = createAvatarPart(avatarFile)
                            
                            val response = RetrofitInstance.api.register(
                                emailPart, passwordPart, namePart, avatarPart
                            )
                            onRegisterSuccess(User(response.id, response.email, response.name, response.avatarUrl))
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Помилка реєстрації"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && 
                         password.length >= 6 && 
                         password == confirmPassword && 
                         !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Зареєструватися")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Перехід на логін
            TextButton(onClick = onNavigateToLogin) {
                Text("Вже є акаунт? Увійти")
            }
        }
        
        // Діалог редагування зображення
        if (showImageEditor && selectedImageUri != null) {
            ImageEditorDialog(
                imageUri = selectedImageUri!!,
                context = context,
                onDismiss = { 
                    showImageEditor = false
                    selectedImageUri = null
                },
                onConfirm = { bitmap ->
                    editedBitmap = bitmap
                    avatarFile = saveBitmapToFile(context, bitmap)
                    showImageEditor = false
                }
            )
        }
    }
}

// Діалог редагування зображення
@Composable
fun ImageEditorDialog(
    imageUri: Uri,
    context: Context,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var isLoading by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()
    
    // Завантаження зображення
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати фото") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    bitmap?.let { bmp ->
                        // Прев'ю зображення
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val transformedBitmap = remember(bmp, rotation, scale) {
                                transformBitmap(bmp, rotation, scale)
                            }
                            Image(
                                bitmap = transformedBitmap.asImageBitmap(),
                                contentDescription = "Прев'ю",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Кнопки редагування
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Поворот вліво
                            IconButton(onClick = { rotation -= 90f }) {
                                Icon(Icons.Default.RotateLeft, contentDescription = "Поворот вліво")
                            }
                            
                            // Поворот вправо
                            IconButton(onClick = { rotation += 90f }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Поворот вправо")
                            }
                            
                            // Зменшити
                            IconButton(
                                onClick = { if (scale > 0.5f) scale -= 0.1f }
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Зменшити")
                            }
                            
                            // Збільшити
                            IconButton(
                                onClick = { if (scale < 2f) scale += 0.1f }
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Збільшити")
                            }
                        }
                        
                        // Слайдер масштабу
                        Text(
                            "Масштаб: ${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.5f..2f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    bitmap?.let { bmp ->
                        val finalBitmap = transformBitmap(bmp, rotation, scale)
                        onConfirm(finalBitmap)
                    }
                },
                enabled = bitmap != null
            ) {
                Text("Застосувати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

// Трансформація зображення (поворот та масштаб)
fun transformBitmap(source: Bitmap, rotation: Float, scale: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotation)
    matrix.postScale(scale, scale)
    
    val rotated = Bitmap.createBitmap(
        source, 0, 0, source.width, source.height, matrix, true
    )
    
    // Обрізати до квадрата
    val size = minOf(rotated.width, rotated.height)
    val x = (rotated.width - size) / 2
    val y = (rotated.height - size) / 2
    
    return Bitmap.createBitmap(rotated, x, y, size, size)
}

// Збереження Bitmap у файл
fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
    val fileName = "avatar_${System.currentTimeMillis()}.jpg"
    val file = File(context.cacheDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file
}
