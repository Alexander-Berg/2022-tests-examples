package ru.yandex.market.checkout.pickup.multiple

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.clean.domain.model.pickup.LegalInfo
import ru.yandex.market.clean.domain.model.pickup.legalInfoTestInstance
import ru.yandex.market.di.TestScope
import javax.inject.Inject

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LegalInfoFormatterTest(
    private val input: LegalInfo,
    private val expected: String
) {

    @Inject
    lateinit var formatter: LegalInfoFormatter

    @Before
    fun setUp() {
        DaggerLegalInfoFormatterTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<LegalInfoFormatterTest>

    @Test
    fun `Format juridical info  as expected`() {
        assertThat(formatter.format(input)).isEqualTo(expected)
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: expected result is \"{1}\"")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(

            // 0
            arrayOf(
                legalInfoTestInstance(),
                "type name, юр.адрес: juridicalAddress, ОГРН ogrn. Лицензия № licenceNumber от licenceStartDate"
            ),

            // 1
            arrayOf(
                legalInfoTestInstance(licenceNumber = null, licenceStartDate = null),
                "type name, юр.адрес: juridicalAddress, ОГРН ogrn."
            ),

            // 2
            arrayOf(
                legalInfoTestInstance(licenceNumber = "", licenceStartDate = ""),
                "type name, юр.адрес: juridicalAddress, ОГРН ogrn."
            ),

            // 3
            arrayOf(
                legalInfoTestInstance(ogrn = null, licenceNumber = "", licenceStartDate = ""),
                "type name, юр.адрес: juridicalAddress"
            )
        )
    }
}