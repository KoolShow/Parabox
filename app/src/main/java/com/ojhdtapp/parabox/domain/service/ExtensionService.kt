package com.ojhdtapp.parabox.domain.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExtensionService : LifecycleService() {
    val extensionManager by lazy {
        ExtensionManager(context = this)
    }
    private var bridge: ExtensionServiceBridge? = null

    fun setBridge(mBridge: ExtensionServiceBridge){
        bridge = mBridge
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return ExtensionServiceBinder()
    }

    interface ExtensionServiceBridge

    inner class ExtensionServiceBinder : Binder() {
        fun getService(): ExtensionService {
            return this@ExtensionService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        extensionManager.loadExtensions()
        extensionManager.installedExtensionsFlow.collect{
            it.forEach {
                lifecycle.addObserver(it.ext)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

}

