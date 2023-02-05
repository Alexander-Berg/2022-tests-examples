@file:Suppress("ObjectPropertyName")

package ru.yandex.yandexmaps.tools.analyticsgenerator.kotlin

val `complex name without parameters` = """
    fun mapAddBookmarkSubmit() {
        val params = LinkedHashMap<String, Any?>(0)
        eventTracker.trackEvent("map.add-bookmark.submit", params)
    }
""".trim('\n')

const val singleLineComment = "Пользователь нажал на кнопку"
const val multiLineComment = """first line
second line
third line"""

val `plain name without parameters with single line comment` = """
    /**
     * $singleLineComment
     */
    fun start() {
        val params = LinkedHashMap<String, Any?>(0)
        eventTracker.trackEvent("start", params)
    }
""".trim('\n')

val `plain name without parameters with multi line comment` = """
    /**
     * first line
     * second line
     * third line
     */
    fun start() {
        val params = LinkedHashMap<String, Any?>(0)
        eventTracker.trackEvent("start", params)
    }
""".trim('\n')

val `single bool parameter` = """
    fun mapAddBookmarkSubmit(authorized: Boolean?) {
        val params = LinkedHashMap<String, Any?>(1)
        params["authorized"] = authorized
        eventTracker.trackEvent("map.add-bookmark.submit", params)
    }
""".trim('\n')

val `single int parameter` = """
    fun event(parameter: Int?) {
        val params = LinkedHashMap<String, Any?>(1)
        params["parameter"] = parameter
        eventTracker.trackEvent("event", params)
    }
""".trim('\n')

val `single double parameter` = """
    fun event(parameter: Double?) {
        val params = LinkedHashMap<String, Any?>(1)
        params["parameter"] = parameter
        eventTracker.trackEvent("event", params)
    }
""".trim('\n')

val `single float parameter` = """
    fun event(parameter: Float?) {
        val params = LinkedHashMap<String, Any?>(1)
        params["parameter"] = parameter
        eventTracker.trackEvent("event", params)
    }
""".trim('\n')

val `single string parameter` = """
    fun event(parameter: String?) {
        val params = LinkedHashMap<String, Any?>(1)
        params["parameter"] = parameter
        eventTracker.trackEvent("event", params)
    }
""".trim('\n')

val `dictionary parameter` = """
    fun event(dictionary: Map<String, Any?>) {
        eventTracker.trackEvent("event", dictionary)
    }
""".trim('\n')

val `single enum parameter` = """
    fun event(applicationLayerType: EventApplicationLayerType?) {
        val params = LinkedHashMap<String, Any?>(1)
        params["application_layer_type"] = applicationLayerType?.originalValue
        eventTracker.trackEvent("event", params)
    }

    enum class EventApplicationLayerType(
        val originalValue: String
    ) {
        MAP("map"),

        SATELLITE("satellite"),

        HYBRID("hybrid");
    }
""".trim('\n')

val `many parameters` = """
    fun event(
        eventName: String?,
        applicationLayerType: EventApplicationLayerType?,
        amount: Int?
    ) {
        val params = LinkedHashMap<String, Any?>(3)
        params["event_name"] = eventName
        params["application_layer_type"] = applicationLayerType?.originalValue
        params["amount"] = amount
        eventTracker.trackEvent("event", params)
    }

    enum class EventApplicationLayerType(
        val originalValue: String
    ) {
        MAP("map"),

        SATELLITE("satellite"),

        HYBRID("hybrid");
    }
""".trim('\n')
