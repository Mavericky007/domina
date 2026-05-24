package com.domina.cycle.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Swaps between the real launcher alias and a discreet "Notes" alias. */
class DiscreetIconManager(private val context: Context) {
    private val default = ComponentName(context, "com.domina.cycle.LauncherDefault")
    private val discreet = ComponentName(context, "com.domina.cycle.LauncherDiscreet")

    fun setDiscreet(enabled: Boolean) {
        val pm = context.packageManager
        // Exactly one launcher alias is enabled at any time.
        pm.setComponentEnabledSetting(
            discreet,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
            default,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
