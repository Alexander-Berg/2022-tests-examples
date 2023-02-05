import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.yandex.yandexmaps.tools.tanker.sync.impl.Language
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.ExportInfo
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.Info
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.IosLocalizableStringsParser
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.Keyset
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.LocalizableStringsTJsonConverter
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.Meta
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.Request
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.TJson
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.Translation
import ru.yandex.yandexmaps.tools.tanker.sync.impl.ios.ValueOfKey
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("ObjectPropertyName")
class LocalizableStringsTJsonConverterTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `parse tjson test`() {
        val tjsonFile = getResourceFile("translates.tjson")
        val tJson = json.decodeFromString<TJson>(tjsonFile.readText())
        assertEquals(`parse tjson expect`, tJson)
    }

    @Test
    fun `Localizable strings to tJson test`() {
        val localizableStringsFile = getResourceFile("Localizable.strings")
        val converter = LocalizableStringsTJsonConverter(keyset = "for_test", localizableStringParser = IosLocalizableStringsParser)
        val tJson = converter.convert(localizableStringsFile, projectId = "nmaps_ios", language = Language.RU)
        assertEquals(`localizable strings to tJson expect`, tJson)
    }

    @Test
    fun `TJson to Localizable strings`() {
        val tjsonFile = getResourceFile("translates.tjson")
        val tJson = json.decodeFromString<TJson>(tjsonFile.readText())
        val converter = LocalizableStringsTJsonConverter(keyset = "for_test", localizableStringParser = IosLocalizableStringsParser)
        val result = converter.convert(tJson, Language.RU)

        assertEquals(`expected localizable strings`, result.readText().trimEnd())
    }

    @Serializable
    data class ExampleJson(
        val example: String,
    )

    companion object {

        private val `expected localizable strings` = """
            /* Кнопка в секции филиалов в карточке организации */
            "Все %[many]d филиалов" = "Все %d филиалов";
            "Все %[none]d филиалов" = "Все %d филиалов";
            "Все %[one]d филиалов" = "%d филиал";
            "Все %[some]d филиалов" = "Все %d филиала";

            /* Единственное число \n тест */
            "Единственное число \n тест" = "Единственное число \n тест";
        """.trimIndent()

        private val `localizable strings to tJson expect`: TJson = TJson.build {
            project = "nmaps_ios"
            keysetId = "for_test"
            branch = "master"
            +pluralString(
                key = "Все %[plural]d филиалов",
                context = "Кнопка в секции филиалов в карточке организации",
                one = "Все %d филиалов"
            )
            +string(
                key = """Единственное число \n тест""",
                context = """И еще одно описание""",
                form = """Единственное число \n тест"""
            )
        }

        private val `parse tjson expect` = TJson(
            exportInfo = ExportInfo(
                request = Request(
                    keysetId = "for_test"
                ),
                branch = "master",
                name = "nmaps_ios"
            ),
            keysets = mapOf(
                "for_test" to Keyset(
                    keys = mapOf(
                        "Все %[plural]d филиалов" to ValueOfKey(
                            info = Info(
                                context = "Кнопка в секции филиалов в карточке организации",
                                isPlural = true
                            ),
                            translations = mapOf(
                                "ru" to Translation(
                                    form = null,
                                    one = "%d филиал",
                                    some = "Все %d филиала",
                                    many = "Все %d филиалов",
                                    none = "Все %d филиалов"
                                )
                            )
                        ),
                        """Единственное число \n тест""" to ValueOfKey(
                            info = Info(
                                context = """Единственное число \n тест""",
                                isPlural = false
                            ),
                            translations = mapOf(
                                "ru" to Translation(
                                    form = """Единственное число \n тест"""
                                )
                            )
                        )
                    ),
                    meta = Meta(
                        languages = listOf("ru", "en", "uz", "tr", "uk")
                    )
                )
            )
        )
    }
}
