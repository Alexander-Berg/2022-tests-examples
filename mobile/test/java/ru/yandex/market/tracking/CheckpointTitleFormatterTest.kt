package ru.yandex.market.tracking

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
import ru.yandex.market.data.order.OrderSubstatus
import ru.yandex.market.di.TestScope
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.tracking.model.domain.CanceledCheckpoint
import ru.yandex.market.tracking.model.domain.Checkpoint
import ru.yandex.market.tracking.model.domain.CompletedCheckpoint
import ru.yandex.market.tracking.model.domain.CreationCheckpoint
import ru.yandex.market.tracking.model.domain.ErrorCheckpoint
import ru.yandex.market.tracking.model.domain.MoneyRefusedCheckpoint
import ru.yandex.market.tracking.model.domain.PassedCheckpoint
import ru.yandex.market.tracking.model.domain.PickupCheckpoint
import ru.yandex.market.tracking.model.domain.RefusedCheckpoint
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CheckpointTitleFormatterTest(
    private val checkpoint: Checkpoint,
    private val expectedTitle: String
) {
    @Inject
    lateinit var formatter: CheckpointTitleFormatter

    @Before
    fun setUp() {
        DaggerCheckpointTitleFormatterTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)
    }

    @Test
    fun `Properly formats checkpoint title`() {
        val title = formatter.format(checkpoint)

        assertThat(title).isEqualTo(expectedTitle)
    }

    companion object {

        private const val CHECKPOINT_TITLE = ""

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(PassedCheckpoint(CHECKPOINT_TITLE, 0L), CHECKPOINT_TITLE),
            arrayOf(CreationCheckpoint(0, 0), "?????????? ????????????"),
            arrayOf(PickupCheckpoint(CHECKPOINT_TITLE, 0, null), CHECKPOINT_TITLE),
            arrayOf(RefusedCheckpoint(0, emptyList()), "0 ?????????????? ????????????????????"),
            arrayOf(
                MoneyRefusedCheckpoint(Money(BigDecimal.ZERO, Currency.RUR)),
                "???????????? ????????????????????"
            ),
            arrayOf(CanceledCheckpoint(OrderSubstatus.SHOP_FAILED, 0), "?????????????? ??????????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.USER_NOT_PAID, 0), "?????????????? ????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.USER_CHANGED_MIND, 0), "?????????????? ????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.USER_UNREACHABLE, 0), "???? ?????????????? ??????????????????"),
            arrayOf(
                CanceledCheckpoint(OrderSubstatus.USER_REFUSED_DELIVERY, 0),
                "???? ?????????????? ??????????????????"
            ),
            arrayOf(CanceledCheckpoint(OrderSubstatus.USER_REFUSED_PRODUCT, 0), "??????????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.USER_REFUSED_QUALITY, 0), "??????????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.REPLACING_ORDER, 0), "??????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.PROCESSING_EXPIRED, 0), "??????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.PENDING_EXPIRED, 0), "??????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.SHOP_PENDING_CANCELLED, 0), "??????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.PENDING_CANCELLED, 0), "??????????????"),
            arrayOf(CanceledCheckpoint(OrderSubstatus.RESERVATION_EXPIRED, 0), "??????????????"),
            arrayOf(CompletedCheckpoint(0), "?????? ?? ??????"),
            arrayOf(ErrorCheckpoint(CHECKPOINT_TITLE, 0), CHECKPOINT_TITLE)
        )
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<CheckpointTitleFormatterTest>
}