package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File


object CacheUtil {
    fun getChatFilesSizeBeforeTimestamp(context: Context, timestamp: Long) : Long{
        val chatFilesDir = context.getExternalFilesDir("chat")
        return if (chatFilesDir?.exists() == true){
            var size: Long = 0
            chatFilesDir.listFiles()?.forEach {
                if (it.lastModified() < timestamp){
                    size += it.length()
                }
            }
            size
        } else {
            0
        }
    }

    fun deleteChatFilesBeforeTimestamp(context: Context, timestamp: Long){
        val chatFilesDir = context.getExternalFilesDir("chat")
        if (chatFilesDir?.exists() == true){
            chatFilesDir.listFiles()?.forEach {
                if (it.lastModified() < timestamp){
                    it.delete()
                }
            }
        }
    }

    fun getCacheSize(context: Context): Long {
        var size: Long = 0
        val cacheDir = context.cacheDir
        if (cacheDir != null && cacheDir.isDirectory) {
            size += getDirSize(cacheDir)
        }
        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir != null && externalCacheDir.isDirectory) {
            size += getDirSize(externalCacheDir)
        }
        return size
    }
    private fun getDirSize(dir: File?): Long {
        var size: Long = 0
        if (dir != null && dir.isDirectory) {
            val children = dir.listFiles()
            for (file in children!!) {
                size += if (file.isDirectory) {
                    getDirSize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
    fun clearCache(context: Context) {
        deleteDir(context.cacheDir)
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            deleteDir(context.externalCacheDir)
        }
    }
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.listFiles()
            for (file in children!!) {
                val success = if (file.isDirectory) {
                    deleteDir(file)
                } else {
                    file.delete()
                }
                if (!success) {
                    return false
                }
            }
        }
        return dir?.delete() ?: false
    }
}