import ru.yandex.yandexmaps.tools.tanker.sync.impl.Language
import ru.yandex.yandexmaps.tools.tanker.sync.impl.android.AndroidPlatformFormatConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidEmptyKeysRemoverTest {

    @Test
    fun `test remove empty keys`() {
        val inFile = getResourceFile("strings_with_empty_strings.xml")
        val expectFile = getResourceFile("strings_without_empty_strings.xml")

        val resultFile = AndroidPlatformFormatConverter().convert(inFile, Language.RU)

        assertEquals(expectFile.readText().trim(), resultFile.readText().trim())
        resultFile.delete()
    }
}
