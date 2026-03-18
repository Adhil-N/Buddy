package com.cname.buddy.ui.screens.finances

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cname.buddy.data.local.FinanceEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FinancesScreen(
    triggerAdd: Boolean = false,
    onAddConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    // 1. Hook up the ViewModel
    val viewModel: FinanceViewModel = viewModel(factory = FinanceViewModelFactory(context))
    val financeList by viewModel.finances.collectAsState()

    // 2. State for our Dialog
    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<FinanceEntity?>(null) }

    LaunchedEffect(triggerAdd){
        if (triggerAdd) {
            itemToEdit = null // Ensure it's empty for a new item
            showDialog = true
            onAddConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            items(financeList) { finance ->
                FinanceCard(
                    finance = finance,
                    onEditClick = {
                        itemToEdit = finance
                        showDialog = true
                    },
                    onDeleteClick = { viewModel.delete(finance) },
                    onMarkPaid = {
                        val nextMonthDate = calculateNextMonth(finance.dueDate)

                        // 2. Update BOTH the amount and the date!
                        val updated = finance.copy(
                            paidAmount = finance.paidAmount + finance.dueAmount,
                            dueDate = nextMonthDate
                        )
                        viewModel.update(updated)
                    }
                )
            }
        }
    }

    // 3. Show the Input Dialog when triggered
    if (showDialog) {
        FinanceInputDialog(
            initialData = itemToEdit,
            onDismiss = { showDialog = false },
            onSave = { newEntity ->
                if (itemToEdit == null) {
                    viewModel.insert(newEntity) // Add new
                } else {
                    viewModel.update(newEntity.copy(id = itemToEdit!!.id)) // Update existing
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinanceCard(
    finance: FinanceEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkPaid: () -> Unit
) {
    // State to control the long-press dropdown menu
    var showMenu by remember { mutableStateOf(false) }

    // Math calculations
    val remainingAmount = finance.totalAmount - finance.paidAmount
    val progress = if (finance.totalAmount > 0) finance.paidAmount / finance.totalAmount else 0f
    val isCurrentlyDue = isDue(finance.dueDate)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // THE MAGIC: combinedClickable detects long presses!
            .combinedClickable(
                onClick = { /* Do nothing on normal click for now */ },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Dropdown Menu that appears when you Hold the card
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    }
                )
            }

            // TOP ROW: Title & Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = finance.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Due: ${finance.dueDate}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MIDDLE ROW: Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountColumn("Total", finance.totalAmount)
                AmountColumn("Remaining", remainingAmount)
                AmountColumn("EMI Due", finance.dueAmount, isHighlight = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PROGRESS BAR
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round
            )

            // BOTTOM ROW: "Mark Paid" Button
            Spacer(modifier = Modifier.height(16.dp))
            if (remainingAmount > 0f) {
                Button(
                    onClick = onMarkPaid,
                    enabled = isCurrentlyDue,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        // Gives it a nice faded grey look when disabled
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(if (isCurrentlyDue) "Mark Paid" else "Not Due Yet")
                }
            } else {
                // Show a celebratory text when finished!
                Text(
                    text = "Fully Paid! 🎉",
                    color = Color(0xFF388E3C), // Green
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// Formats numbers neatly
@Composable
fun AmountColumn(label: String, amount: Float, isHighlight: Boolean = false) {
    val formattedAmount = NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "₹$formattedAmount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isHighlight) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
        )
    }
}

// THE MATERIAL DIALOG FOR INPUTTING DATA
@Composable
fun FinanceInputDialog(
    initialData: FinanceEntity?,
    onDismiss: () -> Unit,
    onSave: (FinanceEntity) -> Unit
) {
    // Keep track of what the user is typing
    var title by remember { mutableStateOf(initialData?.title ?: "") }
    var totalAmount by remember { mutableStateOf(initialData?.totalAmount?.toString() ?: "") }
    var paidAmount by remember { mutableStateOf(initialData?.paidAmount?.toString() ?: "") }
    var dueAmount by remember { mutableStateOf(initialData?.dueAmount?.toString() ?: "") }
    var dueDate by remember { mutableStateOf(initialData?.dueDate ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "Add New EMI/Loan" else "Edit EMI") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g., Car Loan)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = { Text("Total Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = paidAmount,
                    onValueChange = { paidAmount = it },
                    label = { Text("Already Paid Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = dueAmount,
                        onValueChange = { dueAmount = it },
                        label = { Text("EMI (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { },
                        readOnly = true, // Prevents typing!
                        label = { Text("Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Convert text fields safely to floats, default to 0 if empty
                    val entity = FinanceEntity(
                        title = title,
                        totalAmount = totalAmount.toFloatOrNull() ?: 0f,
                        paidAmount = paidAmount.toFloatOrNull() ?: 0f,
                        dueAmount = dueAmount.toFloatOrNull() ?: 0f,
                        dueDate = dueDate,
                        isExpense = true
                    )
                    onSave(entity)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Convert the selected milliseconds into a readable date!
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        dueDate = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun calculateNextMonth(dateString: String): String {
    return try {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = formatter.parse(dateString) ?: return dateString

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, 1) // Pushes it forward exactly 1 month

        formatter.format(calendar.time)
    } catch (e: Exception) {
        dateString // If something goes wrong, just keep the old date safely
    }
}

fun isDue(dateString: String): Boolean {
    return try {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dueDate = formatter.parse(dateString) ?: return false

        // Get exactly right now, but set the time to midnight so we only compare the days
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Returns true if the due date is today or has already passed
        !dueDate.after(today)
    } catch (e: Exception) {
        false // Failsafe
    }
}