package com.cname.buddy.utils

import android.content.Context
import android.content.Intent
import com.cname.buddy.data.local.BuddyDatabase
import com.cname.buddy.data.local.FinanceEntity
import com.cname.buddy.data.local.NoteEntity
import com.cname.buddy.data.local.PasswordEntity
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class AppBackupData(
    val finances: List<FinanceEntity>,
    val passwords: List<PasswordEntity>,
    val notes: List<NoteEntity>
)

class GoogleDriveManager(private val context: Context) {
    private val db = BuddyDatabase.getDatabase(context)
    private val gson = Gson()

    // Authenticates the user silently for Google Drive
    private fun getDriveService(email: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = android.accounts.Account(email, "com.google")

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Buddy").build()
    }

    // --- 1. SILENT BACKUP TO HIDDEN FOLDER ---
    suspend fun backupToDrive(email: String, forceOverwrite: Boolean = false): DriveResult = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext DriveResult.Failed

        try {
            val driveService = getDriveService(email)

            // Gather all database info
            val backupData = AppBackupData(
                finances = db.financeDao().getAllFinances().first(),
                passwords = db.passwordDao().getAllPasswords().first(),
                notes = db.noteDao().getAllNotes().first()
            )
            val jsonString = gson.toJson(backupData)
            val fileContent = ByteArrayContent.fromString("application/json", jsonString)

            // Check if a backup file already exists in the hidden folder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='buddy_auto_backup.json'")
                .execute()

            val fileExists = fileList.files.isNotEmpty()

            if (fileExists && !forceOverwrite) {
                return@withContext DriveResult.FileExists
            }

            if (fileExists) {
                // They confirmed the overwrite, so update the existing file!
                val fileId = fileList.files[0].id
                driveService.files().update(fileId, null, fileContent).execute()
            } else {
                // Create a brand new hidden file
                val fileMetadata = File().apply {
                    name = "buddy_auto_backup.json"
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, fileContent).execute()
            }
            return@withContext DriveResult.Success
        } catch (e: UserRecoverableAuthIOException) {
            return@withContext DriveResult.NeedsPermission(e.intent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext DriveResult.Failed
        }
    }

    // --- 2. RESTORE & SMART MERGE ---
    suspend fun restoreFromDrive(email: String): DriveResult = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext DriveResult.Failed

        try {
            val driveService = getDriveService(email)

            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='buddy_auto_backup.json'")
                .execute()

            if (fileList.files.isNotEmpty()) {
                val fileId = fileList.files[0].id
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

                val jsonString = outputStream.toString("UTF-8")
                val backupData = gson.fromJson(jsonString, AppBackupData::class.java)

                // 1. Fetch current local data for comparison
                // Use .first() to get the current list from the Flow
                val localNotes = db.noteDao().getAllNotes().first()
                val localFinances = db.financeDao().getAllFinances().first()
                val localPasswords = db.passwordDao().getAllPasswords().first()

                // 2. Filter out duplicates and reset IDs to 0
                // Logic: Only keep backup items where NONE of the local items match the key content

                val uniqueNotes = backupData.notes.filter { backupNote ->
                    localNotes.none { it.title == backupNote.title && it.content == backupNote.content }
                }.map { it.copy(id = 0) }

                val uniqueFinances = backupData.finances.filter { backupFin ->
                    localFinances.none { it.title == backupFin.title }
                }.map { it.copy(id = 0) }

                val uniquePasswords = backupData.passwords.filter { backupPass ->
                    localPasswords.none { it.platform == backupPass.platform && it.username == backupPass.username }
                }.map { it.copy(id = 0) }

                // 3. Insert only the truly new items
                if (uniqueNotes.isNotEmpty()) db.noteDao().insertAllNotes(uniqueNotes)
                if (uniqueFinances.isNotEmpty()) db.financeDao().insertAllFinances(uniqueFinances)
                if (uniquePasswords.isNotEmpty()) db.passwordDao().insertAllPasswords(uniquePasswords)

                return@withContext DriveResult.Success
            } else {
                return@withContext DriveResult.Failed
            }
        } catch (e: UserRecoverableAuthIOException) {
            return@withContext DriveResult.NeedsPermission(e.intent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext DriveResult.Failed
        }
    }
}

sealed class DriveResult {
    object Success : DriveResult()
    object Failed : DriveResult()
    object FileExists : DriveResult()
    class NeedsPermission(val intent: Intent) : DriveResult() // Holds the permission popup!
}