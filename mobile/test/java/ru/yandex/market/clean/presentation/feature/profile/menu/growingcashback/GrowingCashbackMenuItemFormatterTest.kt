package ru.yandex.market.clean.presentation.feature.profile.menu.growingcashback

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.profile.menu.GrowingCashbackProfileMenuVo
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionState
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class GrowingCashbackMenuItemFormatterTest(
    private val input: GrowingCashbackActionState?,
    private val expected: GrowingCashbackProfileMenuVo?
) {

    private val formatter = GrowingCashbackMenuItemFormatter(
        ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    )

    @Test
    fun testFormat() {
        val actual = formatter.format(input)
        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index} : {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                null as GrowingCashbackActionState?,
                null as GrowingCashbackProfileMenuVo?
            ),
            //1
            arrayOf(
                growingCashbackActionStateTestInstance(
                    maxReward = 100,
                    state = GrowingCashbackActionState.State.ACTIVE
                ),
                GrowingCashbackProfileMenuVo(
                    title = "Получите до 100 баллов",
                    hasCloseButton = false
                )
            ),
            //2
            arrayOf(
                growingCashbackActionStateTestInstance(
                    maxReward = 200,
                    state = GrowingCashbackActionState.State.COMPLETE
                ),
                GrowingCashbackProfileMenuVo(
                    title = "Получите до 200 баллов",
                    hasCloseButton = true
                )
            ),
            //3
            arrayOf(
                growingCashbackActionStateTestInstance(
                    maxReward = 300,
                    state = GrowingCashbackActionState.State.TERMS_VIOLATION
                ),
                GrowingCashbackProfileMenuVo(
                    title = "Получите до 300 баллов",
                    hasCloseButton = true
                )
            ),
            //4
            arrayOf(
                growingCashbackActionStateTestInstance(
                    state = GrowingCashbackActionState.State.END
                ),
                null as GrowingCashbackProfileMenuVo?
            ),
        )
    }
}
