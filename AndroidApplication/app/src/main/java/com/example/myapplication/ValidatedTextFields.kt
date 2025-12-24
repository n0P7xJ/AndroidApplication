package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

// ==================== ВАЛІДАТОРИ ====================

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

object Validators {
    // Email валідація
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult.Invalid("Email обов'язковий")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationResult.Invalid("Невірний формат email")
            else -> ValidationResult.Valid
        }
    }
    
    // Пароль валідація
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult.Invalid("Пароль обов'язковий")
            password.length < 6 -> ValidationResult.Invalid("Мінімум 6 символів")
            !password.any { it.isDigit() } -> ValidationResult.Invalid("Має містити цифру")
            !password.any { it.isLetter() } -> ValidationResult.Invalid("Має містити букву")
            else -> ValidationResult.Valid
        }
    }
    
    // Простий пароль (тільки мінімальна довжина)
    fun validateSimplePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult.Invalid("Пароль обов'язковий")
            password.length < 6 -> ValidationResult.Invalid("Мінімум 6 символів")
            else -> ValidationResult.Valid
        }
    }
    
    // Підтвердження паролю
    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isEmpty() -> ValidationResult.Invalid("Підтвердіть пароль")
            password != confirmPassword -> ValidationResult.Invalid("Паролі не співпадають")
            else -> ValidationResult.Valid
        }
    }
    
    // Ім'я
    fun validateName(name: String): ValidationResult {
        return when {
            name.isEmpty() -> ValidationResult.Invalid("Ім'я обов'язкове")
            name.length < 2 -> ValidationResult.Invalid("Мінімум 2 символи")
            name.length > 50 -> ValidationResult.Invalid("Максимум 50 символів")
            else -> ValidationResult.Valid
        }
    }
    
    // Заголовок задачі
    fun validateTaskTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Invalid("Назва обов'язкова")
            title.length > 100 -> ValidationResult.Invalid("Максимум 100 символів")
            else -> ValidationResult.Valid
        }
    }
}

// ==================== ПОКРАЩЕНІ TEXT FIELDS ====================

/**
 * Покращене текстове поле з валідацією та Material Design 3 стилем
 */
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    validator: ((String) -> ValidationResult)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    helperText: String? = null,
    maxLength: Int? = null,
    showCharacterCounter: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    var hasBeenFocused by remember { mutableStateOf(false) }
    
    val validationResult = remember(value, hasBeenFocused) {
        if (hasBeenFocused && validator != null) validator(value) else ValidationResult.Valid
    }
    
    val isError = validationResult is ValidationResult.Invalid
    val errorMessage = (validationResult as? ValidationResult.Invalid)?.message

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (maxLength == null || newValue.length <= maxLength) {
                    onValueChange(newValue)
                }
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        hasBeenFocused = true
                    }
                },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = when {
                            isError -> MaterialTheme.colorScheme.error
                            isFocused -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = placeholder?.let { { Text(it) } },
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Помилка або допоміжний текст
                    AnimatedVisibility(
                        visible = isError || helperText != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage ?: helperText ?: "",
                            color = if (isError) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Лічильник символів
                    if (showCharacterCounter && maxLength != null) {
                        Text(
                            text = "${value.length}/$maxLength",
                            color = if (value.length >= maxLength) 
                                MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

/**
 * Поле для введення email з автоматичною валідацією
 */
@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Email",
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Email,
        validator = Validators::validateEmail,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { 
                focusManager.moveFocus(FocusDirection.Down)
                onImeAction()
            },
            onDone = { 
                focusManager.clearFocus()
                onImeAction()
            }
        ),
        enabled = enabled,
        helperText = "example@email.com"
    )
}

/**
 * Поле для введення пароля з показом/приховуванням
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Пароль",
    enabled: Boolean = true,
    useStrongValidation: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Lock,
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Сховати пароль" else "Показати пароль"
                )
            }
        },
        validator = if (useStrongValidation) Validators::validatePassword else Validators::validateSimplePassword,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { 
                focusManager.moveFocus(FocusDirection.Down)
                onImeAction()
            },
            onDone = { 
                focusManager.clearFocus()
                onImeAction()
            }
        ),
        enabled = enabled,
        helperText = if (useStrongValidation) "Мін. 6 символів, букви та цифри" else "Мінімум 6 символів"
    )
}

/**
 * Поле підтвердження пароля
 */
@Composable
fun ConfirmPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    password: String,
    modifier: Modifier = Modifier,
    label: String = "Підтвердіть пароль",
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Lock,
        validator = { Validators.validateConfirmPassword(password, it) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { 
                focusManager.moveFocus(FocusDirection.Down)
                onImeAction()
            },
            onDone = { 
                focusManager.clearFocus()
                onImeAction()
            }
        ),
        enabled = enabled
    )
}

/**
 * Поле для введення імені
 */
@Composable
fun NameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ім'я",
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Person,
        validator = Validators::validateName,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { 
                focusManager.moveFocus(FocusDirection.Down)
                onImeAction()
            },
            onDone = { 
                focusManager.clearFocus()
                onImeAction()
            }
        ),
        enabled = enabled,
        maxLength = 50,
        showCharacterCounter = true
    )
}

/**
 * Поле для введення назви задачі
 */
@Composable
fun TaskTitleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Назва задачі",
    enabled: Boolean = true
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Title,
        validator = Validators::validateTaskTitle,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        enabled = enabled,
        maxLength = 100,
        showCharacterCounter = true
    )
}

/**
 * Поле для введення опису (багаторядкове)
 */
@Composable
fun DescriptionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Опис",
    placeholder: String = "Додайте опис...",
    enabled: Boolean = true,
    maxLength: Int = 500
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Description,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        ),
        enabled = enabled,
        singleLine = false,
        maxLines = 4,
        placeholder = placeholder,
        maxLength = maxLength,
        showCharacterCounter = true,
        helperText = "Необов'язково"
    )
}

// ==================== FILLED TEXT FIELD ВАРІАНТ ====================

/**
 * Filled текстове поле (альтернативний стиль Material Design)
 */
@Composable
fun FilledValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    validator: ((String) -> ValidationResult)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    helperText: String? = null
) {
    var hasBeenFocused by remember { mutableStateOf(false) }
    
    val validationResult = remember(value, hasBeenFocused) {
        if (hasBeenFocused && validator != null) validator(value) else ValidationResult.Valid
    }
    
    val isError = validationResult is ValidationResult.Invalid
    val errorMessage = (validationResult as? ValidationResult.Invalid)?.message

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) hasBeenFocused = true },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailingIcon,
        isError = isError,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        supportingText = {
            Text(
                text = errorMessage ?: helperText ?: "",
                color = if (isError) MaterialTheme.colorScheme.error 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
