package com.cname.buddy.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cname.buddy.ui.screens.finances.FinanceViewModel
import com.cname.buddy.ui.screens.finances.FinanceViewModelFactory
import com.cname.buddy.ui.screens.notes.NoteViewModel
import com.cname.buddy.ui.screens.notes.NoteViewModelFactory
import com.cname.buddy.ui.screens.vault.PasswordViewModel
import com.cname.buddy.ui.screens.vault.PasswordViewModelFactory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(userEmail: String?) {
    val context = LocalContext.current

    // 1. Grab all three ViewModels!
    val financeViewModel: FinanceViewModel = viewModel(factory = FinanceViewModelFactory(context))
    val passwordViewModel: PasswordViewModel = viewModel(factory = PasswordViewModelFactory(context))
    val noteViewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(context))

    // 2. Collect the live data
    val finances by financeViewModel.finances.collectAsState()
    val passwords by passwordViewModel.passwords.collectAsState()
    val notes by noteViewModel.notes.collectAsState()

    // 3. Calculate some quick stats
    val totalRemainingDebt = finances.sumOf { (it.totalAmount - it.paidAmount).toDouble() }.toFloat()
    val formattedDebt = NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalRemainingDebt)

    // Create a friendly display name from their email (e.g., "john.doe@gmail.com" -> "John.doe")
    val displayName = userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // --- HEADER ---
        item {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- AT A GLANCE STATS ---
        item {
            Text("At a Glance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Passwords Stat
                StatCard(
                    title = "Secured",
                    value = "${passwords.size} Logins",
                    icon = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )
                // Notes Stat
                StatCard(
                    title = "Notes",
                    value = "${notes.size} Saved",
                    icon = Icons.Default.Note,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Finances Stat (Full width)
            StatCard(
                title = "Total Pending EMIs/Loans",
                value = "₹$formattedDebt",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.fillMaxWidth(),
                isHighlight = true
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- RECENT NOTES PREVIEW ---
        if (notes.isNotEmpty()) {
            item {
                Text("Recent Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Just show the 2 most recent notes
            items(notes.take(2)) { note ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(note.title, fontWeight = FontWeight.Bold)
                        Text(note.content, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// A reusable mini-card for the dashboard stats
@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, isHighlight: Boolean = false) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isHighlight) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}