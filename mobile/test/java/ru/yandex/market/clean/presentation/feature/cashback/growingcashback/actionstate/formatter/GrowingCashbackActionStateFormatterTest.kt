package ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.formatter

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackActionVo
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackButtonsVo
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackListHeaderVo
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionInfo
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfoTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfo_OrderRewardTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance
import ru.yandex.market.utils.createDate
import java.math.BigDecimal

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class GrowingCashbackActionStateFormatterTest(
    val input: GrowingCashbackActionInfo,
    val expected: GrowingCashbackActionVo.GrowingCashbackActionStateVo
) {

    private val formatter = GrowingCashbackActionStateFormatter(
        ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    )

    @Test
    fun testFormat() {
        val actual = formatter.format(input)
        assertThat(actual).isEqualTo(expected)
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                growingCashbackActionInfoTestInstance(
                    actionState = growingCashbackActionStateTestInstance(
                        orderThreshold = BigDecimal(3500),
                        maxOrdersCount = 3,
                        actionEndDate = createDate(2022, 11, 16, 0, 0, 0, 0),
                        aboutPageLink = "aboutPageLink",
                        maxReward = 600
                    ),
                    ordersReward = listOf(
                        growingCashbackActionInfo_OrderRewardTestInstance(
                            orderLabel = "???????????? ??????????",
                            isOrderPurchased = true,
                            isOrderDelivered = false,
                            reward = 300
                        ),
                        growingCashbackActionInfo_OrderRewardTestInstance(
                            orderLabel = "???????????? ??????????",
                            isOrderPurchased = true,
                            isOrderDelivered = true,
                            reward = 500
                        ),
                        growingCashbackActionInfo_OrderRewardTestInstance(
                            orderLabel = "???????????? ??????????",
                            isOrderPurchased = false,
                            isOrderDelivered = false,
                            reward = 700
                        ),
                    ),
                ),
                GrowingCashbackActionVo.GrowingCashbackActionStateVo(
                    title = "???????????????? ???? 600 ????????????????????????",
                    header = GrowingCashbackListHeaderVo(
                        title = "???????????????? 3 ????????????,\n???????????? ???? 3500?????",
                        endDate = "???? 16????????????????"
                    ),
                    items = listOf(
                        GrowingCashbackActionVo.GrowingCashbackItem(
                            title = "???????????? ??????????",
                            subtitle = "?????????? ???????????? ???????????? ?? ??????????????",
                            value = 300,
                            hasDoneIcon = false
                        ),
                        GrowingCashbackActionVo.GrowingCashbackItem(
                            title = "???????????? ??????????",
                            subtitle = null,
                            value = 500,
                            hasDoneIcon = true
                        ),
                        GrowingCashbackActionVo.GrowingCashbackItem(
                            title = "???????????? ??????????",
                            subtitle = null,
                            value = 700,
                            hasDoneIcon = false
                        ),
                    ),
                    footer = GrowingCashbackButtonsVo(
                        primaryButton = GrowingCashbackButtonsVo.Button(
                            title = "???? ??????????????????",
                            action = GrowingCashbackButtonsVo.ButtonAction.Home
                        ),
                        secondaryButton = GrowingCashbackButtonsVo.Button(
                            title = "??????????????????",
                            action = GrowingCashbackButtonsVo.ButtonAction.About("aboutPageLink")
                        )
                    )
                ),
            ),
            //1
            arrayOf(
                growingCashbackActionInfoTestInstance(
                    actionState = growingCashbackActionStateTestInstance(
                        orderThreshold = BigDecimal(500),
                        maxOrdersCount = 1,
                        actionEndDate = createDate(2022, 8, 8, 0, 0, 0, 0),
                        aboutPageLink = "aboutPageLink1",
                        maxReward = 600
                    ),
                    ordersReward = emptyList(),
                ),
                GrowingCashbackActionVo.GrowingCashbackActionStateVo(
                    title = "???????????????? ???? 600 ????????????????????????",
                    header = GrowingCashbackListHeaderVo(
                        title = "???????????????? 1 ??????????,\n???????????? ???? 500?????",
                        endDate = "???? 8??????????????????"
                    ),
                    items = emptyList(),
                    footer = GrowingCashbackButtonsVo(
                        primaryButton = GrowingCashbackButtonsVo.Button(
                            title = "???? ??????????????????",
                            action = GrowingCashbackButtonsVo.ButtonAction.Home
                        ),
                        secondaryButton = GrowingCashbackButtonsVo.Button(
                            title = "??????????????????",
                            action = GrowingCashbackButtonsVo.ButtonAction.About("aboutPageLink1")
                        )
                    )
                ),
            ),
            //2
            arrayOf(
                growingCashbackActionInfoTestInstance(
                    actionState = growingCashbackActionStateTestInstance(
                        orderThreshold = BigDecimal(9999),
                        maxOrdersCount = 5,
                        actionEndDate = createDate(2040, 1, 10, 0, 0, 0, 0),
                        aboutPageLink = "aboutPageLink2",
                        maxReward = 600
                    ),
                    ordersReward = emptyList(),
                ),
                GrowingCashbackActionVo.GrowingCashbackActionStateVo(
                    title = "???????????????? ???? 600 ????????????????????????",
                    header = GrowingCashbackListHeaderVo(
                        title = "???????????????? 5 ??????????????,\n???????????? ???? 9999?????",
                        endDate = "???? 10????????????????"
                    ),
                    items = emptyList(),
                    footer = GrowingCashbackButtonsVo(
                        primaryButton = GrowingCashbackButtonsVo.Button(
                            title = "???? ??????????????????",
                            action = GrowingCashbackButtonsVo.ButtonAction.Home
                        ),
                        secondaryButton = GrowingCashbackButtonsVo.Button(
                            title = "??????????????????",
                            action = GrowingCashbackButtonsVo.ButtonAction.About("aboutPageLink2")
                        )
                    )
                ),
            ),
        )
    }
}
