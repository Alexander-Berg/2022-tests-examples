package com.yandex.frankenstein.agent.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

private const val CLAZZ = "CommandClass"
private const val NAME = "commandName"

class CommandImplementationsTest {

    @Mock private lateinit var block: (CommandInput) -> Unit

    private val commandImplementations = CommandImplementations()

    init {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testGetImplementationAfterRegister() {
        commandImplementations.withCommand(CLAZZ, NAME, block)
        val actualBlock = commandImplementations.get(CLAZZ, NAME)

        assertThat(actualBlock).isEqualTo(block)
    }

    @Test
    fun testGetImplementationAfterRegisterWithWrongClass() {
        commandImplementations.withCommand(CLAZZ, NAME, block)
        val actualBlock = commandImplementations.get("UnknownClass", NAME)

        assertThat(actualBlock).isNotNull
    }

    @Test
    fun testGetImplementationAfterRegisterWithWrongName() {
        commandImplementations.withCommand(CLAZZ, NAME, block)
        val actualBlock = commandImplementations.get(CLAZZ, "unknownName")

        assertThat(actualBlock).isNotNull
    }

    @Test
    fun testGetImplementationAfterRegisterWithWrongClassAndName() {
        commandImplementations.withCommand(CLAZZ, NAME, block)
        val actualBlock = commandImplementations.get("UnknownClass", "unknownName")

        assertThat(actualBlock).isNotNull
    }

    @Test
    fun testGetImplementationWithoutRegister() {
        val actualBlock = commandImplementations.get(CLAZZ, NAME)

        assertThat(actualBlock).isNotNull
    }
}
