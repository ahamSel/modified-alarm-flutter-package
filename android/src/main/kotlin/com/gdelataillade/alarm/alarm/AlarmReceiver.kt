package com.gdelataillade.alarm.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.database.sqlite.SQLiteDatabase
import io.flutter.Log

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ALARM_STOP = "com.gdelataillade.alarm.ACTION_STOP"
        const val ACTION_ALARM_TEST = "com.gdelataillade.alarm.ACTION_TEST"
        const val EXTRA_ALARM_ACTION = "EXTRA_ALARM_ACTION"
        private const val TAG = "AlarmReceiver"
    }

    private fun markReminderComplete(context: Context, reminderId: String) {
        try {
            val dbPath = context.getDatabasePath("reminders.db")
            val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            
            db.execSQL(
                "UPDATE reminders SET isCompleted = 1 WHERE id = ?",
                arrayOf(reminderId)
            )
            
            Log.d(TAG, "Successfully marked reminder $reminderId as complete")
            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking reminder as complete: ${e.message}")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        /// Stop alarm from notification stop button.
        if (action == ACTION_ALARM_STOP) {
            val id = intent.getIntExtra("id", 0) 
            Log.d(TAG, "Received stop alarm command from notification, id: $id")
            AlarmService.instance?.let {
                it.handleStopAlarmCommand(id)
                return
            }
        }

        /// Handle mark as done action
        if (action == ACTION_ALARM_TEST) {
            val id = intent.getIntExtra("id", 0)
            val reminderId = intent.getStringExtra("notification_title") ?: ""
            
            Log.d(TAG, "Mark as Done clicked - Alarm ID: $id, Reminder ID: $reminderId")
            
            // Mark the reminder as complete in the database
            markReminderComplete(context, reminderId)
            
            // Stop the alarm (this will also handle notification cleanup)
            AlarmService.instance?.let {
                it.handleStopAlarmCommand(id)
                return
            }
        }

        // Start Alarm Service
        val serviceIntent = Intent(context, AlarmService::class.java)
        serviceIntent.putExtras(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pendingIntent = PendingIntent.getForegroundService(
                context,
                1,
                serviceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            pendingIntent.send()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}