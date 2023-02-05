package com.yandex.mail.metrica

data class StatboxEvent @JvmOverloads constructor(
    val name: String,
    val value: String? = null,
    val attributes: Map<String, Any>? = null
)
