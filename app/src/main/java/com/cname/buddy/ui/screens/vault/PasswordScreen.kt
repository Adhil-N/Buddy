package com.cname.buddy.ui.screens.vault

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cname.buddy.data.local.PasswordEntity

@Composable
fun PasswordsScreen(
    triggerAdd: Boolean = false,
    onAddConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: PasswordViewModel = viewModel(factory = PasswordViewModelFactory(context))
    val passwordList by viewModel.passwords.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PasswordEntity?>(null) }

    LaunchedEffect(triggerAdd) {
        if (triggerAdd) {
            itemToEdit = null
            showDialog = true
            onAddConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            items(passwordList) { passwordItem ->
                PasswordCard(
                    passwordItem = passwordItem,
                    onEditClick = {
                        itemToEdit = passwordItem
                        showDialog = true
                    },
                    onDeleteClick = { viewModel.delete(passwordItem) }
                )
            }
        }
    }

    if (showDialog) {
        PasswordInputDialog(
            initialData = itemToEdit,
            onDismiss = { showDialog = false },
            onSave = { newEntity ->
                if (itemToEdit == null) viewModel.insert(newEntity) else viewModel.update(newEntity.copy(id = itemToEdit!!.id))
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordCard(
    passwordItem: PasswordEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Grabs the phone's clipboard so we can copy the password
    val clipboardManager = LocalClipboardManager.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEditClick() })
                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDeleteClick() })
            }

            // Platform (e.g., Netflix)
            Text(text = passwordItem.platform, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            // Username/Email
            Text(text = passwordItem.username, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Password Row with Toggle and Copy Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // The actual password text (masked or unmasked)
                Text(
                    text = if (isPasswordVisible) passwordItem.password else "••••••••••••",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )

                // Toggle visibility button
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Password Visibility"
                    )
                }

                // Copy to clipboard button
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(passwordItem.password)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
                }
            }
        }
    }
}

@Composable
fun PasswordInputDialog(
    initialData: PasswordEntity?,
    onDismiss: () -> Unit,
    onSave: (PasswordEntity) -> Unit
) {
    var platform by remember { mutableStateOf(initialData?.platform ?: "") }
    var username by remember { mutableStateOf(initialData?.username ?: "") }
    var password by remember { mutableStateOf(initialData?.password ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "Add Password" else "Edit Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = platform, onValueChange = { platform = it },
                    label = { Text("Platform (e.g., Google)") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username / Email") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(PasswordEntity(platform = platform, username = username, password = password)) },
                enabled = platform.isNotBlank() && password.isNotBlank() // Prevent empty saves!
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}