package com.yandex.frankenstein.agent.client

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

private const val ID = "commandId"
private const val CLAZZ = "CommandClass"
private const val NAME = "commandName"

class CommandTest {

    private val arguments = JSONObject().put("key", "value")

    private val command = Command(JSONObject().put("id", ID)
            .put("class", CLAZZ).put("name", NAME).put("arguments", arguments))

    @Test
    fun testId() {
        assertThat(command.id).isEqualTo(ID)
    }

    @Test
    fun testClass() {
        assertThat(command.clazz).isEqualTo(CLAZZ)
    }

    @Test
    fun testName() {
        assertThat(command.name).isEqualTo(NAME)
    }

    @Test
    fun testArguments() {
        assertThat(command.arguments).isEqualTo(arguments)
    }
}
