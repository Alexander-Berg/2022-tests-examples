package ru.yandex.yandexmaps.tools.testpalm.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class TestCase(
    val id: Int,
    val projectId: String,
    val name: String,
    val preconditions: String?,
    val steps: List<Step>
) {
    @Serializable
    data class Step(
        @SerialName("step") val description: String,
        val expect: String? = null
    )

    val codeName: String
        get() = projectId.toCamelCase() + id.toString()

    val fullName: String
        get() = "$projectId-$id"
}

enum class TestCaseStatus {
    UP_TO_DATE, // exists locally and up-to-date with testpalm counterpart
    OUTDATED, // exists locally and doesnt' match to testpalm counterpart
    REMOTE // doesnt' exists locally
}

fun String.toCamelCase(): String = split("_", "-").map { it.replaceFirstChar { it.uppercaseChar() } }.joinToString("")
