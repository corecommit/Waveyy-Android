package com.waveyy.app.ui.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import timber.log.Timber

/** Receives PackageInstaller session status and logs it. */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Timber.tag("UpdateInstaller").d("install status=$status msg=$message")
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.waveyy.app.INSTALL_STATUS"
    }
}
