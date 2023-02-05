import org.junit.Assert.assertArrayEquals
import org.w3c.dom.Document
import ru.yandex.yandexmaps.tools.tanker.sync.impl.android.XmlStringResourcesParser
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test

class AndroidXmlResourcesParserTest {

    private val parser = XmlStringResourcesParser.Common

    @Test
    fun `parse sample new-strings`() {
        val document = checkNotNull(getNewStringsDocument()) { "Unable to read new-strings.xml" }

        val keys = parser.parse(document)

        assertArrayEquals(arrayOf("test_string", "plurals_test_strings"), keys.toTypedArray())
    }

    private fun getNewStringsDocument(): Document? {
        val file = getResourceFile("new-strings.xml")
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    }
}
