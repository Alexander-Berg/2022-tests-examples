package com.yandex.maps.testapp.mapdesign

import com.yandex.maps.testapp.mapdesign.Utils.requestContent
import org.json.JSONObject

class Service(private val hostConfig: HostConfig, json: String) : JSONObject(json) {

    val description: String = getString("description")
    val id: Int = getInt("id")
    val name: String = getString("name")

    fun requestBranchHeads(): Array<BranchHead>  {
        val urlString = "https://${hostConfig.styleRepoHost}/heads?format=json&service_id=${id}"
        val content = requestContent(urlString)

        return JSONObject(content).getJSONArray("branches").run {
                (0 until length())
                        .map { BranchHead(hostConfig, getString(it)) }
                        .toTypedArray()
            }
    }

    override fun toString(): String = name
}