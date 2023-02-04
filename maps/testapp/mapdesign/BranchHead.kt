package com.yandex.maps.testapp.mapdesign

import com.yandex.maps.testapp.mapdesign.Utils.requestContent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

class BranchHead(val hostConfig: HostConfig, json: String) {
    private companion object {
        val LOGGER = Logger.getLogger("yandex.maps.testapp.mapdesign")!!
    }

    private var jsonObject: JSONObject = JSONObject(json)
    private var onChanged: ((BranchHead) -> Unit)? = null

    val author: String get() = jsonObject.getString("author")
    val revisionId: Int get() =  jsonObject.getInt("revisionId")
    val branchId: Int get() = jsonObject.getInt("branchId")
    val message: String get() = jsonObject.getString("msg")
    val styleSetNames: Array<String> get() = jsonObject.getJSONArray("styleSets").run {
                (0 until length()).map { this.getJSONObject(it).getString("name") }
                        .toTypedArray()
                        .sortedArray()
    }
    val owner: String get() = jsonObject.getString("owner")
    val branchName: String get() = jsonObject.getString("branchName")

    val revisionDateString: String
        get() {
            val dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
            val dateFormat = SimpleDateFormat(dateFormatPattern, Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("MSK")

            val date = dateFormat.parse(jsonObject.getString("timestamp"))
            return dateStringFormat.format(date)
        }

    private var lastUpdateDate: Date = Date()
    private val dateStringFormat = SimpleDateFormat("MMM dd, HH:mm:ss Z", Locale.US)
    val lastUpdateDateString: String get() = dateStringFormat.format(lastUpdateDate)

    override fun toString(): String = branchName

    fun update() {
        val urlString = "https:/${hostConfig.styleRepoHost}/heads/$branchId?format=json"
        LOGGER.info("Run updating branch head (branchId: $branchId) using URL `$urlString`")

        var changed = false
        val content = requestContent(urlString)
        jsonObject = JSONObject(content).also { changed = it.getInt("revisionId") != revisionId }

        lastUpdateDate = Date()

        LOGGER.info("Branch head has been updated. Changed: $changed. Update timestamp: $lastUpdateDateString")

        if (changed)
            onChanged?.invoke(this)
    }

    fun setOnChanged(callback: (BranchHead) -> Unit) {
        onChanged = callback
    }
}