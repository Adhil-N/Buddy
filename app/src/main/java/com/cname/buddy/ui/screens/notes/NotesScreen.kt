package com.cname.buddy.ui.screens.notes

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cname.buddy.data.local.BuddyDatabase
import com.cname.buddy.data.local.NoteDao
import com.cname.buddy.data.local.NoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- VIEW MODEL ---
class NoteViewModel(private val dao: NoteDao) : ViewModel() {
    val notes: StateFlow<List<NoteEntity>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun insert(note: NoteEntity) = viewModelScope.launch { dao.insertNote(note) }
    fun update(note: NoteEntity) = viewModelScope.launch { dao.updateNote(note) }
    fun delete(note: NoteEntity) = viewModelScope.launch { dao.deleteNote(note) }
}

class NoteViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NoteViewModel(BuddyDatabase.getDatabase(context).noteDao()) as T
    }
}

// --- UI SCREEN ---
@Composable
fun NotesScreen(triggerAdd: Boolean = false, onAddConsumed: () -> Unit = {}, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val viewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(context))
    val notesList by viewModel.notes.collectAsState()

    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<NoteEntity?>(null) }

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
            items(notesList) { note ->
                NoteCard(
                    note = note,
                    onEditClick = { itemToEdit = note; showDialog = true },
                    onDeleteClick = {
                        // 1. Delete it from the database
                        viewModel.delete(note)

                        // 2. Show the Undo Snackbar
                        scope.launch {
                            // Dismiss any current snackbar so this one pops up instantly
                            snackbarHostState.currentSnackbarData?.dismiss()

                            val result = snackbarHostState.showSnackbar(
                                message = "Note deleted",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short // Only stays for a split second!
                            )

                            // 3. If they click UNDO, shove it back into the database!
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.insert(note)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showDialog) {
        NoteInputDialog(
            initialData = itemToEdit,
            onDismiss = { showDialog = false },
            onSave = { newEntity ->
                if (itemToEdit == null){
                    viewModel.insert(newEntity)
                    scope.launch {
                        snackbarHostState.showSnackbar("Note added!")
                    }
                } else {
                    viewModel.update(newEntity.copy(id = itemToEdit!!.id))
                    scope.launch { snackbarHostState.showSnackbar("Note Updated!") }
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(note: NoteEntity, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(onClick = { onEditClick() }, onLongClick = { showMenu = true }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDeleteClick() })
            }
            Text(text = note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // Shows a preview of the note content (max 3 lines)
            Text(text = note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = note.dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun NoteInputDialog(initialData: NoteEntity?, onDismiss: () -> Unit, onSave: (NoteEntity) -> Unit) {
    var title by remember { mutableStateOf(initialData?.title ?: "") }
    var content by remember { mutableStateOf(initialData?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "New Note" else "Edit Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("Note") }, modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                    onSave(NoteEntity(title = title, content = content, dateString = date))
                },
                enabled = title.isNotBlank() || content.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}