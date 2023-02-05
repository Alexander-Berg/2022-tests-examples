package ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatusTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackActionVo
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionState
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfoTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance

class GrowingCashbackActonFormatterTest {

    private val activeVo = mock<GrowingCashbackActionVo.GrowingCashbackActionStateVo>()
    private val completeVo = mock<GrowingCashbackActionVo.GrowingCashbackInfoVo>()
    private val endVo = mock<GrowingCashbackActionVo.GrowingCashbackInfoVo>()
    private val errorVo = mock<GrowingCashbackActionVo.GrowingCashbackInfoVo>()

    private val actionStateFormatter = mock<GrowingCashbackActionStateFormatter> {
        on { format(any()) } doReturn activeVo
    }
    private val infoFormatter = mock<GrowingCashbackInfoFormatter> {
        on { formatComplete(any(), any()) } doReturn completeVo
        on { formatEnd() } doReturn endVo
        on { formatError(any()) } doReturn errorVo
    }

    private val formatter = GrowingCashbackActonFormatter(
        actionStateFormatter,
        infoFormatter
    )

    @Test
    fun `format active action`() {
        val actual = formatter.format(
            growingCashbackActionInfoTestInstance(
                actionState = growingCashbackActionStateTestInstance(
                    state = GrowingCashbackActionState.State.ACTIVE
                )
            ),
            referralProgramStatusTestInstance()
        )
        assertThat(actual).isEqualTo(activeVo)
    }

    @Test
    fun `format completed action`() {
        val actual = formatter.format(
            growingCashbackActionInfoTestInstance(
                actionState = growingCashbackActionStateTestInstance(
                    state = GrowingCashbackActionState.State.COMPLETE
                )
            ),
            referralProgramStatusTestInstance()
        )
        assertThat(actual).isEqualTo(completeVo)
    }

    @Test
    fun `format ended action`() {
        val actual = formatter.format(
            growingCashbackActionInfoTestInstance(
                actionState = growingCashbackActionStateTestInstance(
                    state = GrowingCashbackActionState.State.END
                )
            ),
            referralProgramStatusTestInstance()
        )
        assertThat(actual).isEqualTo(endVo)
    }

    @Test
    fun `format terms violation action`() {
        val actual = formatter.format(
            growingCashbackActionInfoTestInstance(
                actionState = growingCashbackActionStateTestInstance(
                    state = GrowingCashbackActionState.State.TERMS_VIOLATION
                )
            ),
            referralProgramStatusTestInstance()
        )
        assertThat(actual).isEqualTo(endVo)
    }

    @Test
    fun `format error`() {
        val actual = formatter.formatError(Error())
        assertThat(actual).isEqualTo(errorVo)
    }
}