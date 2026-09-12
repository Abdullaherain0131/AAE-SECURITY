package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AAEAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("AAEAdminReceiver", "Device Admin Enabled. We are unkillable.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("AAEAdminReceiver", "Device Admin Disabled! Protection weakened.")
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
        Log.d("AAEAdminReceiver", "Password failed! Triggering Intruder Selfie...")
        // TODO: Implement CameraX selfie capture here or trigger a work request
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        Log.d("AAEAdminReceiver", "Password succeeded.")
    }
}
