package ru.yandex.market.clean.presentation.feature.lavket.formatter

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfo
import ru.yandex.market.clean.domain.model.lavka2.cart.LavkaCart
import ru.yandex.market.clean.presentation.feature.cart.formatter.LavkaServiceInfoFormatter
import ru.yandex.market.clean.presentation.feature.lavka.view.DeliveryInformationBarVo
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.AVAILABLE_DATE
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.CLOSED_LAVKA_DELIVERY_INFORMATION_VO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.CLOSED_LAVKA_SERVICE_INFO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.CLOSED_UNTIL_MORNING_LAVKA_DELIVERY_INFORMATION_VO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.CLOSED_UNTIL_MORNING_LAVKA_SERVICE_INFO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.FIRST_DATE
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.HYPERLOCAL_ADDRESS_EXIST
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.HYPERLOCAL_ADDRESS_NOT_EXIST
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.LAVKA_CART
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.LAVKA_JURIDICAL_INFO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.LAVKA_JURIDICAL_INFO_VO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.OPEN_LAVKA_DELIVERY_INFORMATION_VO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.OPEN_LAVKA_SERVICE_INFO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.SECOND_DATE
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.UNKNOWN_LAVKA_DELIVERY_INFORMATION_VO
import ru.yandex.market.clean.presentation.feature.lavket.formatter.LavkaServiceInfoFormatterTestEntity.UNKNOWN_LAVKA_SERVICE_INFO
import ru.yandex.market.clean.presentation.formatter.LavkaJuridicalInfoFormatter
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.optional.Optional

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LavkaServiceInfoFormatterTest(
    private val lavkaServiceInfoOptional: Optional<LavkaServiceInfo>,
    private val isAddressExists: Boolean,
    private val exceptedResult: Optional<DeliveryInformationBarVo>,
    private val lavkaCart: Optional<LavkaCart>
) {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resourcesDataStore = ResourcesManagerImpl(context.resources)

    private val timeFormatter = mock<TimeFormatter> {
        on { formatShort(AVAILABLE_DATE) } doReturn "15:26"
        on { formatShort(FIRST_DATE) } doReturn "03:00"
        on { formatShort(SECOND_DATE) } doReturn "03:00"
    }

    private val juridicalInfoMapper = mock<LavkaJuridicalInfoFormatter> {
        on { map(LAVKA_JURIDICAL_INFO) } doReturn LAVKA_JURIDICAL_INFO_VO
    }

    private val formatter = LavkaServiceInfoFormatter(
        resourcesManager = resourcesDataStore,
        timeFormatter = timeFormatter,
        juridicalInfoMapper = juridicalInfoMapper
    )

    @Test
    fun `Check lavka service info formatter`() {
        assertThat(
            formatter.formatDeliveryInfo(
                lavkaServiceInfoOptional = lavkaServiceInfoOptional,
                isAddressExists = isAddressExists,
                lavkaCart = lavkaCart.valueOrNull(),
                isCartInSurgeEnabled = false,
            )
        ).isEqualTo(exceptedResult)
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun parameters() = listOf(

            // 0
            arrayOf(
                Optional.of(OPEN_LAVKA_SERVICE_INFO),
                HYPERLOCAL_ADDRESS_EXIST,
                Optional.of(OPEN_LAVKA_DELIVERY_INFORMATION_VO),
                Optional.of(LAVKA_CART),
            ),

            // 1
            arrayOf(
                Optional.of(OPEN_LAVKA_SERVICE_INFO),
                HYPERLOCAL_ADDRESS_NOT_EXIST,
                Optional.ofNullable(null),
                Optional.ofNullable(null),
            ),

            // 2
            arrayOf(
                Optional.ofNullable(null),
                HYPERLOCAL_ADDRESS_EXIST,
                Optional.ofNullable(null),
                Optional.ofNullable(null),
            ),

            // 3
            arrayOf(
                Optional.of(CLOSED_LAVKA_SERVICE_INFO),
                HYPERLOCAL_ADDRESS_EXIST,
                Optional.of(CLOSED_LAVKA_DELIVERY_INFORMATION_VO),
                Optional.of(LAVKA_CART),
            ),

            // 4
            arrayOf(
                Optional.of(CLOSED_UNTIL_MORNING_LAVKA_SERVICE_INFO),
                HYPERLOCAL_ADDRESS_EXIST,
                Optional.of(CLOSED_UNTIL_MORNING_LAVKA_DELIVERY_INFORMATION_VO),
                Optional.of(LAVKA_CART),
            ),

            // 5
            arrayOf(
                Optional.of(UNKNOWN_LAVKA_SERVICE_INFO),
                HYPERLOCAL_ADDRESS_EXIST,
                Optional.of(UNKNOWN_LAVKA_DELIVERY_INFORMATION_VO),
                Optional.of(LAVKA_CART),
            ),

            )
    }

}