package com.cname.buddy

import android.Manifest
import android.os.Build
import android.os.Bundle
import com.cname.buddy.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.InspectableModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.navigation.compose.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.cname.buddy.data.local.UserPreferences
import com.cname.buddy.ui.screens.finances.FinancesScreen
import com.cname.buddy.ui.screens.home.HomeScreen
import com.cname.buddy.ui.screens.notes.NotesScreen
import com.cname.buddy.ui.screens.vault.PasswordsScreen
import com.cname.buddy.ui.theme.BuddyTheme
import com.cname.buddy.utils.DriveResult
import com.cname.buddy.utils.EmiReminderWorker
import com.cname.buddy.utils.GoogleDriveManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Call your main layout here!
                    BuddyMainScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    var isFabExpanded by remember { mutableStateOf(false) }
    var triggerAddFinance by remember { mutableStateOf(false) }
    var triggerAddPassword by remember { mutableStateOf(false) }
    var triggerAddNote by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) { isFabExpanded = false }

    // --- NEW DRAWER STATE ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) println("Notification permission granted!")
    }
    val drivePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // The user clicked "Allow"!
            // In a full production app, you would auto-retry the backup here.
            scope.launch { snackBarHostState.showSnackbar("Permission granted! Please click Backup again.") }
        } else {
            scope.launch { snackBarHostState.showSnackbar("Backup requires Drive permission.") }
        }
    }

    // Put EVERYTHING inside this LaunchedEffect so it only runs once!
    LaunchedEffect(Unit) {
        // 1. Request permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 2. Calculate time until 9:00 AM tomorrow
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9) // 9 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // If it's already past 9 AM today, set it for tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        // 3. Create the real 24-hour request with the exact delay
        val dailyWorkRequest = PeriodicWorkRequestBuilder<EmiReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        // 4. Schedule it!
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyEmiReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    val userPreferences = remember { UserPreferences(context) }

    val profilePicUrl by userPreferences.profilePicUrlFlow.collectAsState(initial = null)
    val userEmail by userPreferences.userEmailFlow.collectAsState(initial = null)

    val driveManager = remember { GoogleDriveManager(context) }

    fun launchGoogleSignIn(){
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(request = request, context = context)

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                    val profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    val userEmail = googleIdTokenCredential.id

                    userPreferences.saveUserData(userEmail, profilePicUrl)
                    println("Logged in successfully as: $userEmail")
                }
            } catch (e: Exception) {
                println("Error logging in: $e")
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Buddy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Text(
                    text = "Cloud Sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Backup to Cloud") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (userEmail == null) {
                            scope.launch { snackBarHostState.showSnackbar("Please sign in first!") }
                        } else {
                            scope.launch {
                                snackBarHostState.showSnackbar("Connecting to Google Drive...")
                                val result = driveManager.backupToDrive(userEmail!!) // Defaults to forceOverwrite = false

                                when (result) {
                                    is DriveResult.Success -> snackBarHostState.showSnackbar("Backup Complete!")
                                    is DriveResult.Failed -> snackBarHostState.showSnackbar("Backup Failed.")
                                    is DriveResult.NeedsPermission -> drivePermissionLauncher.launch(result.intent)
                                    is DriveResult.FileExists -> {
                                        // Trigger the warning popup!
                                        showOverwriteDialog = true
                                    }
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Restore from Cloud") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (userEmail.isNullOrBlank()) {
                            scope.launch { snackBarHostState.showSnackbar("Please sign in first!") }
                        } else {
                            scope.launch {
                                snackBarHostState.showSnackbar("Restoring data...")

                                // 1. MAKE SURE IT SAYS 'restoreFromDrive' HERE!
                                val result = driveManager.restoreFromDrive(userEmail!!)

                                when (result) {
                                    // 2. MAKE SURE THE SNACKBARS ARE CORRECT!
                                    is DriveResult.Success -> snackBarHostState.showSnackbar("Data successfully merged!")
                                    is DriveResult.Failed -> snackBarHostState.showSnackbar("No backup found or failed.")
                                    is DriveResult.NeedsPermission -> drivePermissionLauncher.launch(result.intent)
                                    is DriveResult.FileExists -> { /* Do nothing for restore */ }
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // --- GENERAL SECTION ---
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() /* TODO: Navigate to Settings */ } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Feedback") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() /* TODO: Open email intent */ } },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Share Buddy") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() /* Placeholder for now */ } },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // MAGIC TRICK: This Spacer pushes everything below it to the bottom of the screen!
                Spacer(modifier = Modifier.weight(1f))

                // --- LOG OUT BUTTON ---
                NavigationDrawerItem(
                    label = { Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            userPreferences.clearUserData() // Wipes the DataStore!
                            drawerState.close()
                        }
                    },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                )
            }
        }
    ) {

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackBarHostState)},

            topBar = {
                // Use our new custom app bar!
                BuddySearchBar(
                    onMenuClick = {
                        // Slide the drawer open when the hamburger is clicked
                        scope.launch { drawerState.open() }
                    },
                    onProfileClick = {
                        if (profilePicUrl == null){
                            launchGoogleSignIn()
                        } else {
                            println("Already logged in as: $userEmail")
                        }
                                     },
                    profileImageUrl = profilePicUrl
                )
            },
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple("home", "Home", Icons.Default.Home),
                        Triple("finances", "Finances", Icons.Default.AccountBalanceWallet),
                        Triple("passwords", "Passwords", Icons.Default.Lock),
                        Triple("notes", "Notes", Icons.Default.Edit)
                    )

                    items.forEach { (route, title, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title) },
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute != "home") {
                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedVisibility(visible = isFabExpanded) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                when (currentRoute) {
                                    "finances" -> {
                                        MiniFab("Create EMI", Icons.Default.CreditCard) {
                                            triggerAddFinance = true
                                            isFabExpanded = false
                                        }
                                        MiniFab("Add Expense", Icons.Default.ShoppingCart) { }
                                    }

                                    "passwords" -> MiniFab("New Password", Icons.Default.Key) {
                                        triggerAddPassword = true
                                        isFabExpanded = false
                                    }
                                    "notes" -> MiniFab("New Note", Icons.Default.NoteAdd) {
                                        triggerAddNote = true // <--- ADD THIS
                                        isFabExpanded = false
                                    }
                                }
                            }
                        }
                        FloatingActionButton(onClick = { isFabExpanded = !isFabExpanded }) {
                            Icon(
                                if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                                "Menu"
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, "home", Modifier.padding(innerPadding)) {
                composable("home") {
                    HomeScreen(userEmail = userEmail)
                }
                composable("finances") { FinancesScreen(
                    triggerAdd = triggerAddFinance,
                    onAddConsumed = { triggerAddFinance = false }
                ) }
                composable("passwords") {
                    PasswordsScreen(
                        triggerAdd = triggerAddPassword,
                        onAddConsumed = { triggerAddPassword = false }
                    )
                }
                composable("notes") {
                    NotesScreen(
                        triggerAdd = triggerAddNote,
                        onAddConsumed = { triggerAddNote = false },
                        snackbarHostState = snackBarHostState
                    )
                }
            }
        }

        if (showOverwriteDialog) {
            AlertDialog(
                onDismissRequest = { showOverwriteDialog = false },
                title = { Text("Backup Already Exists") },
                text = { Text("You already have a backup saved in Google Drive. Do you want to overwrite it with your current data? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showOverwriteDialog = false
                            // The user clicked yes, so we force the overwrite!
                            scope.launch {
                                snackBarHostState.showSnackbar("Overwriting backup...")
                                val result = driveManager.backupToDrive(userEmail!!, forceOverwrite = true)
                                if (result is DriveResult.Success) {
                                    snackBarHostState.showSnackbar("Backup Complete!")
                                } else {
                                    snackBarHostState.showSnackbar("Backup Failed.")
                                }
                            }
                        }
                    ) {
                        Text("Overwrite", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverwriteDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun Column(horizontalAlignment: InspectableModifier.End, content: @Composable () -> FloatingActionButton) {
    TODO("Not yet implemented")
}

@Composable
fun BuddySearchBar(
    profileImageUrl: String?,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Rounded.Menu, contentDescription = "Open Navigation Drawer")
        }

        Surface (
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .fillMaxHeight(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onProfileClick) {
            if (profileImageUrl != null){
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle, // Placeholder for their picture
                    contentDescription = "Profile",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun MiniFab(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 2.dp, modifier = Modifier.padding(end = 8.dp)) {
            Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        SmallFloatingActionButton(onClick = onClick) { Icon(icon, label) }
    }
}