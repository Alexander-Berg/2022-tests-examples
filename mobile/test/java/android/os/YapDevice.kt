package android.os

import android.content.Context

object YapDevice {

    private lateinit var headId: String

    @JvmStatic
    var vendor: String? = null
        private set
    @JvmStatic
    var model: String? = null
        private set
    @JvmStatic
    var type: String? = null
        private set
    @JvmStatic
    var mcu: String? = null
        private set
    @JvmStatic
    var firmwareBuildNumber: String? = null
        private set
    @JvmStatic
    var firmwareBuildDate: String? = null
        private set

    fun prepare(
        headId: String,
        vendor: String? = null,
        model: String? = null,
        type: String? = null,
        mcu: String? = null,
        firmwareBuildNumber: String? = null,
        firmwareBuildDate: String? = null
    ) {
        YapDevice.headId = headId
        YapDevice.vendor = vendor
        YapDevice.model = model
        YapDevice.type = type
        YapDevice.mcu = mcu
        YapDevice.firmwareBuildNumber = firmwareBuildNumber
        YapDevice.firmwareBuildDate = firmwareBuildDate
    }

    @JvmStatic
    fun getHeadId(context: Context): String = headId
}
