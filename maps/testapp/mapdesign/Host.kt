package com.yandex.maps.testapp.mapdesign

import android.content.Context
import com.yandex.maps.testapp.Environment.hostConfig
import com.yandex.maps.testapp.mapdesign.Utils.requestContent
import org.json.JSONObject

class Host(val context: Context) {
    val config = hostConfig(context)
    fun requestServices(): Array<Service> {
        val urlString = "https://${config.styleRepoHost}/services/list?format=json"
        val content = requestContent(urlString)

        val services = JSONObject(content).getJSONArray("services")
        return (0 until(services.length()))
                .map { Service(config, services[it].toString()) }
                .toTypedArray()
    }
}

class HostConfig(val styleRepoHost: String, val rendererHost: String, val vec3RendererHost: String)
