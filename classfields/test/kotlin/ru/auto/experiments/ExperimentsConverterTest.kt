package ru.auto.experiments

import com.google.gson.Gson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import ru.auto.data.util.IBase64Converter
import java.nio.charset.Charset
import java.util.*

class ExperimentsConverterTest : FreeSpec({
    "describe experiments deserialization" - {
        "should convert int experiment" {
            @Language("JSON")
            val exp = withExperimentBody("""{ "android_stat_id": "1" }""")

            val result = experimentsConverter().fromNetwork(exp)

            result.value shouldBe serverExperimentsOf("android_stat_id" to ExperimentValue.IntValue(1))
        }
        "should convert boolean experiment" {
            @Language("JSON")
            val exp = withExperimentBody("""{ "android_stat_id": false }""")

            val result = experimentsConverter().fromNetwork(exp)

            result.value shouldBe serverExperimentsOf("android_stat_id" to ExperimentValue.BooleanValue(false))
        }
        "should convert string experiment" {
            @Language("JSON")
            val exp = withExperimentBody("""{ "android_stat_id": "abc" }""")

            val result = experimentsConverter().fromNetwork(exp)

            result.value shouldBe serverExperimentsOf("android_stat_id" to ExperimentValue.StringValue("abc"))
        }
        "should convert JSON object experiment" {
            @Language("JSON")
            val exp = withExperimentBody("""{ "android_stat_id": { "stat_id" :  "2" } }""")

            val result = experimentsConverter().fromNetwork(exp)

            result.value shouldBe serverExperimentsOf("android_stat_id" to ExperimentValue.StringValue("""{"stat_id":"2"}"""))
        }
    }
}) {
    companion object {
        private fun String.encodeBase64() = Base64.getEncoder().encode(this.toByteArray()).toString(Charset.forName("UTF-8"))


        @Language("JSON")
        private fun withExperimentBody(vararg bodies: String) = ExperimentsConfig(
            bodies.map { body ->
                """
                [
                  {
                    "HANDLER": "AUTORU_APP",
                    "CONTEXT": {
                      "MAIN": {
                        "AUTORU_APP": $body
                      }
                    },
                    "TESTID": [
                      "257130"
                    ]
                  }
                ]
            """.trimIndent().encodeBase64()
            }, null
        )

        private fun serverExperimentsOf(vararg experiments: Pair<String, ExperimentValue>) = ServerExperiments(
            experiments = experiments.toMap(),
            testIds = setOf("257130"),
            boxes = null
        )


        private fun experimentsConverter() = ExperimentsConverter(
            object : IBase64Converter {
                override fun decode(base64String: String): String {
                    val bytes = Base64.getDecoder().decode(base64String.toByteArray())
                    return String(bytes, Charsets.UTF_8)
                }
            }, Gson()
        )
    }
}
