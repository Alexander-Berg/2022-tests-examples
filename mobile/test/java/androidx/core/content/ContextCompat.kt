package androidx.core.content

import android.content.Context
import android.content.pm.PackageManager

object ContextCompat {

    const val INTERACT_ACROSS_USERS = "android.permission.INTERACT_ACROSS_USERS"
    const val MANAGE_USERS = "android.permission.MANAGE_USERS"

    var currentPermissionState = mutableMapOf(
        INTERACT_ACROSS_USERS to PackageManager.PERMISSION_GRANTED,
        MANAGE_USERS to PackageManager.PERMISSION_GRANTED
    )

    fun reset() {
        currentPermissionState[INTERACT_ACROSS_USERS] = PackageManager.PERMISSION_GRANTED
        currentPermissionState[MANAGE_USERS] = PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkSelfPermission(context: Context, permission: String): Int =
        currentPermissionState[permission] ?: PackageManager.PERMISSION_DENIED
}
