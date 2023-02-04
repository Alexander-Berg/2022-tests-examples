package ru.auto.ara.deeplink.parser.search

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.auto.ara.data.models.form.state.CallbackGroupState
import ru.auto.core_ui.util.Consts.FILTER_PARAM_MARK
import ru.auto.data.model.catalog.Vendor
import ru.auto.data.repository.IVendorRepository
import ru.auto.data.util.AUTO_ID
import rx.Single
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * @author themishkun on 02/07/2018.
 */
@RunWith(AllureParametrizedRunner::class)
class VendorToFieldStateParametrizedParserTest(vendorDeeplink: String, private val vendorId: String, private val vendorName: String, state: String) {

    private val uriPath = "/$AUTO_ID/$vendorDeeplink/$state/"
    val incorrectUriPath = "/$AUTO_ID/not-vendor/$state/"

    private val vendorRepository: IVendorRepository = mock {
        on { getVendors(any()) } doReturn (Single.just(vendors))
    }

    val parser = VendorToFieldStateParser(vendorRepository)

    @Test
    fun `given path containing vendor id it should create vendor fromstate`() {

        val parsedResult = parser(uriPath) as? CallbackGroupState

        assertNotNull(parsedResult)
        assertEquals(FILTER_PARAM_MARK, parsedResult?.fieldName)
        assertEquals(vendorId, parsedResult?.vendorId)
        assertEquals(vendorName, parsedResult?.name)
    }

    @Test
    fun `given path with incorrect vendor id it should return null`() {

        val parsedResult = parser(incorrectUriPath) as? CallbackGroupState

        assertNull(parsedResult)
    }

    companion object {
        private const val VENDOR_RUSSIAN = "VENDOR1"
        private const val VENDOR_FOREIGN = "2"
        private const val VENDOR_EUROPEAN = "VENDOR3"
        private const val VENDOR_JAPANESE = "VENDOR7"
        private const val VENDOR_AMERICAN = "VENDOR8"
        private const val VENDOR_KOREAN = "VENDOR9"
        private const val VENDOR_CHINESE = "VENDOR10"
        private const val VENDOR_CHINESE_EXCLUDE = "VENDOR15"

        private const val VENDOR_RUSSIAN_NAME = "RUSSIAN"
        private const val VENDOR_FOREIGN_NAME = "FOREIGN"
        private const val VENDOR_EUROPEAN_NAME = "EUROPEAN"
        private const val VENDOR_JAPANESE_NAME = "JAPANESE"
        private const val VENDOR_AMERICAN_NAME = "AMERICAN"
        private const val VENDOR_KOREAN_NAME = "KOREAN"
        private const val VENDOR_CHINESE_NAME = "CHINESE"
        private const val VENDOR_CHINESE_EXCLUDE_NAME = "CHINESE_EXCLUDE"

        private const val ALL = "all"
        private const val NEW = "new"
        private const val USED = "used"

        private fun Vendor(id: String, name: String) = Vendor(id, name, emptyList())

        private val vendors = listOf(Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_RUSSIAN, VendorToFieldStateParametrizedParserTest.VENDOR_RUSSIAN_NAME),
                Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_FOREIGN, VendorToFieldStateParametrizedParserTest.VENDOR_FOREIGN_NAME, listOf(
                        Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_EUROPEAN, VendorToFieldStateParametrizedParserTest.VENDOR_EUROPEAN_NAME),
                        Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_AMERICAN, VendorToFieldStateParametrizedParserTest.VENDOR_AMERICAN_NAME),
                        Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_JAPANESE, VendorToFieldStateParametrizedParserTest.VENDOR_JAPANESE_NAME),
                        Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_KOREAN, VendorToFieldStateParametrizedParserTest.VENDOR_KOREAN_NAME),
                        Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_CHINESE, VendorToFieldStateParametrizedParserTest.VENDOR_CHINESE_NAME),
                        Vendor(VendorToFieldStateParametrizedParserTest.VENDOR_CHINESE_EXCLUDE, VendorToFieldStateParametrizedParserTest.VENDOR_CHINESE_EXCLUDE_NAME)
                )))

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: extracted /cars/{0}/{3}/ expect Vendor(id = {1}, name = {2})")
        fun data(): Collection<Array<out Any>> {
            return listOf(
                    arrayOf("vendor1", VENDOR_RUSSIAN, VENDOR_RUSSIAN_NAME),
                    arrayOf("vendor2", VENDOR_FOREIGN, VENDOR_FOREIGN_NAME),
                    arrayOf("vendor3", VENDOR_EUROPEAN, VENDOR_EUROPEAN_NAME),
                    arrayOf("vendor7", VENDOR_JAPANESE, VENDOR_JAPANESE_NAME),
                    arrayOf("vendor8", VENDOR_AMERICAN, VENDOR_AMERICAN_NAME),
                    arrayOf("vendor9", VENDOR_KOREAN, VENDOR_KOREAN_NAME),
                    arrayOf("vendor10", VENDOR_CHINESE, VENDOR_CHINESE_NAME),
                    arrayOf("vendor15", VENDOR_CHINESE_EXCLUDE, VENDOR_CHINESE_EXCLUDE_NAME)
            ).flatMap {
                listOf(
                        it + ALL,
                        it + USED,
                        it + NEW
                )
            }
        }
    }
}
