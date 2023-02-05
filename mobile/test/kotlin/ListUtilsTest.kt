import ru.yandex.yandexmaps.multiplatformmodulecreator.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ListStringsTest {

    @Test
    fun `insert multiplatform module to gradle settings`() {
        var settings = listOf(
            "\n",
            "include ':multiplatform'\n",
            "include ':multiplatform:core'\n",
            "\n",
            "include ':multiplatform:licensing'\n",
            "\n",
            "include ':android'\n",
            "include ':android:yandexmaps'\n",
            "\n"
        )

        val lineForInsert = "include ':multiplatform:test-module'\n"
        val updatedSettings = settings.inserted(
            lineForInsert,
            InsertionPoint.Between("include ':multiplatform:.*'", InsertionPoint.emptyLine),
            ifNotContains = lineForInsert
        )

        assertEquals(
            listOf(
                "\n",
                "include ':multiplatform'\n",
                "include ':multiplatform:core'\n",
                "include ':multiplatform:test-module'\n",
                "\n",
                "include ':multiplatform:licensing'\n",
                "\n",
                "include ':android'\n",
                "include ':android:yandexmaps'\n",
                "\n"
            ),
            updatedSettings
        )
    }

    @Test
    fun `Repeated insert multiplatform module to gradle settings`() {
        var settings = listOf(
            "\n",
            "include ':multiplatform'\n",
            "include ':multiplatform:core'\n",
            "include ':multiplatform:test-module'\n",
            "\n",
            "include ':multiplatform:licensing'\n",
            "\n",
            "include ':android'\n",
            "include ':android:yandexmaps'\n",
            "\n"
        )

        val lineForInsert = "include ':multiplatform:test-module'\n"
        val updatedSettings = settings.inserted(
            lineForInsert,
            InsertionPoint.Between("include ':multiplatform:.*'", InsertionPoint.emptyLine),
            ifNotContains = lineForInsert
        )

        assertEquals(
            listOf(
                "\n",
                "include ':multiplatform'\n",
                "include ':multiplatform:core'\n",
                "include ':multiplatform:test-module'\n",
                "\n",
                "include ':multiplatform:licensing'\n",
                "\n",
                "include ':android'\n",
                "include ':android:yandexmaps'\n",
                "\n"
            ),
            updatedSettings
        )
    }
}
