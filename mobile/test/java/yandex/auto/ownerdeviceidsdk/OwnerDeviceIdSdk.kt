package yandex.auto.ownerdeviceidsdk

import android.content.Context

object OwnerDeviceIdSdk {

    private var ownerDeviceId: String? = null

    fun prepare(ownerDeviceId: String?) {
        OwnerDeviceIdSdk.ownerDeviceId = ownerDeviceId
    }

    @JvmStatic
    fun getOwnerDeviceId(context: Context): String? = ownerDeviceId
}
