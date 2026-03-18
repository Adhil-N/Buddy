package com.cname.buddy.utils // Keep your package name!

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cname.buddy.data.local.BuddyDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EmiReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Grab the database
        val dao = BuddyDatabase.getDatabase(context).financeDao()
        val finances = dao.getAllFinancesSync()

        // 2. Setup the Notification Channel (Required for Android 8.0+)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "emi_channel",
                "EMI Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Check today's date and tomorrow's date
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val todayStr = formatter.format(Calendar.getInstance().time)

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowStr = formatter.format(tomorrowCal.time)

        // 4. Loop through expenses and notify if needed
        finances.forEach { finance ->
            // Skip if it's already fully paid
            if (finance.paidAmount >= finance.totalAmount) return@forEach

            var alertMessage = ""
            if (finance.dueDate == todayStr) {
                alertMessage = "Your ${finance.title} EMI (₹${finance.dueAmount}) is due TODAY!"
            } else if (finance.dueDate == tomorrowStr) {
                alertMessage = "Your ${finance.title} EMI (₹${finance.dueAmount}) is due TOMORROW!"
            }

            if (alertMessage.isNotEmpty()) {
                val notification = NotificationCompat.Builder(context, "emi_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a default android icon
                    .setContentTitle("Buddy Reminder")
                    .setContentText(alertMessage)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                // Use the finance ID as the notification ID so they don't overwrite each other
                notificationManager.notify(finance.id, notification)
            }
        }

        return Result.success()
    }
}