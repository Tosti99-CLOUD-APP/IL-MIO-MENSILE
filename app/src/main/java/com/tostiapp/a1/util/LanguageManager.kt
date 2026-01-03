package com.tostiapp.a1.util

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

class LanguageManager(private val context: Context) {

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)

    fun downloadLanguage(languageCode: String, onDownloaded: () -> Unit) {
        val request = SplitInstallRequest.newBuilder()
            .addLanguage(java.util.Locale(languageCode))
            .build()

        val listener = object : SplitInstallStateUpdatedListener {
            override fun onStateUpdate(state: com.google.android.play.core.splitinstall.SplitInstallSessionState) {
                when (state.status()) {
                    SplitInstallSessionStatus.INSTALLED -> {
                        onDownloaded()
                        splitInstallManager.unregisterListener(this)
                    }
                    SplitInstallSessionStatus.FAILED, SplitInstallSessionStatus.CANCELED -> {
                        splitInstallManager.unregisterListener(this)
                    }
                    else -> {}
                }
            }
        }

        splitInstallManager.registerListener(listener)
        splitInstallManager.startInstall(request)
    }
}