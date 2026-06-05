package com.example.data.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconChanger {

    private val aliases = listOf(
        "com.example.AliasVaultCam",
        "com.example.AliasCalculator",
        "com.example.AliasNotes",
        "com.example.AliasCompass"
    )

    fun applyIconAlias(context: Context, targetAlias: String) {
        val packageManager = context.packageManager
        
        for (aliasName in aliases) {
            val componentName = ComponentName(context, aliasName)
            val state = if (aliasName == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            try {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
