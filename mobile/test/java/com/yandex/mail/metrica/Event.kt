package com.yandex.mail.metrica

data class Event(
    val name: String,
    val attributes: Map<String, Any>?,
    val throwable: Throwable?
) {
    companion object {

        @JvmStatic
        fun create(name: String): Event {
            return Event(name, null, null)
        }

        @JvmStatic
        fun create(name: String, throwable: Throwable?): Event {
            return Event(name, null, throwable)
        }

        @JvmStatic
        fun create(name: String, attributes: Map<String, Any>): Event {
            return Event(name, attributes, null)
        }
    }
}
