package com.edadeal.android.model.webapp

import com.edadeal.android.data.edweb.EdWebJsonConverter
import com.edadeal.android.metrics.ErrorGroup
import com.edadeal.android.metrics.ErrorReporter
import com.edadeal.android.model.macros.PlaceHolder
import com.edadeal.android.model.macros.PlaceHolder.Adid
import com.edadeal.android.model.macros.PlaceHolder.Duid
import com.edadeal.android.model.macros.PlaceHolder.Uid
import com.edadeal.android.model.macros.PlaceholderResolver
import com.edadeal.android.model.webapp.handler.ResolveMacrosHandler
import com.edadeal.android.util.fromJsonSafely
import com.edadeal.platform.JsonValue
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ResolveMacrosHandlerTest(
    private val input: List<String>,
    private val output: List<String>,
    private val hasErrors: Boolean
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            params(
                listOf("{{YA_DEVICE_ID}}", "{{YA_UUID}}", "{{PUID}}", "{{ADID}}", "{{UNKNOWN}}", "{{}}"),
                listOf(
                    getValue(PlaceHolder.YaDeviceId),
                    getValue(PlaceHolder.YaUuid),
                    getValue(PlaceHolder.Puid),
                    getValue(Adid),
                    "{{UNKNOWN}}",
                    "{{}}"
                ),
                hasErrors = true
            ),
            params(
                input = listOf("duid={{DUID}}, uid={{UID}}, adid={{ADID}}"),
                output = listOf("duid=${getValue(Duid)}, uid=${getValue(Uid)}, adid=${getValue(Adid)}")
            ),
            params(
                input = listOf(""),
                output = listOf("")
            )
        )

        private fun getValue(placeHolder: PlaceHolder): String {
            return placeHolder.name
        }

        private fun params(
            input: List<String>,
            output: List<String>,
            hasErrors: Boolean = false
        ): Array<Any> {
            return arrayOf(input, output, hasErrors)
        }
    }

    private val moshi = Moshi.Builder().build()

    @Test
    fun `should return expected values`() {
        val errorReporter = mock<ErrorReporter>()
        val jsonConverter = EdWebJsonConverter(moshi)
        val handler = ResolveMacrosHandler(errorReporter, jsonConverter, Resolver)
        val params = JsonValue("{\"value\":${jsonConverter.toJson(input)}}")

        val tmp = handler.invoke("environment.resolveMacros", params).blockingGet()
        val actualOutput = moshi.fromJsonSafely<List<String>>(tmp.toJson())
        assertEquals(output, actualOutput)
        if (!hasErrors) {
            verifyZeroInteractions(errorReporter)
        } else {
            verify(errorReporter).reportError(eq(ErrorGroup.WEBAPP_MACROS), any())
        }
    }

    @Suppress("Detekt.NotImplementedDeclaration")
    private object Resolver : PlaceholderResolver {

        override fun getPlaceholderValue(placeholder: PlaceHolder): String {
            return getValue(placeholder)
        }

        override fun getPlaceholderValue(placeholderAlias: String): String {
            return PlaceHolder.findByAlias(placeholderAlias)
                ?.let { getPlaceholderValue(it) }
                ?: placeholderAlias
        }

        override fun replacePlaceholders(
            input: String,
            transformValue: (placeholderAlias: String, placeholderValue: String) -> String
        ): String = throw NotImplementedError()

        override fun getPlaceholderAliases(): Collection<String> = PlaceHolder.values().map { it.alias }

        override fun prepareUrl(inputUrl: String): String = throw NotImplementedError()
        override fun replacePlaceholders(input: String): String = throw NotImplementedError()
        override fun getDefaultArgsKeyValueMap(): Map<String, String> = throw NotImplementedError()
        override fun getFeedbackUrlDefaultArgs(): Map<String, String> = throw NotImplementedError()
    }
}
