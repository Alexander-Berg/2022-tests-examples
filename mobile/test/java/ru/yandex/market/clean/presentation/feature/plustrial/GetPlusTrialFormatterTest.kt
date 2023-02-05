package ru.yandex.market.clean.presentation.feature.plustrial

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.model.plustrial.plusTrialInfoTestInstance
import ru.yandex.market.common.android.ResourcesManagerImpl
import java.util.GregorianCalendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class GetPlusTrialFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val plusTrialInfo = plusTrialInfoTestInstance(
        marketCashbackPercent = 10,
        trialEndDate = GregorianCalendar(2019, 1, 10, 0, 0, 0).time
    )

    private val formatter = GetPlusTrialFormatter(ResourcesManagerImpl(context.resources))

    @Test
    fun `format info for gift state`() {
        val actual = formatter.formatInfo(plusTrialInfo, GetPlusTrialArguments.State.GET_PRESENT)
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.GET_PLUS,
            title = "Вам подарок — бесплатная подписка на Яндекс Плюс!",
            subtitle = "Она даёт доступ к повышенному кешбэку баллами, купонам впечатлений и эксклюзиву во время Незабудней. Отключится 10 февраля.",
            primaryButton = GetPlusTrialVo.Button(
                title = "Забрать бесплатно",
                buttonAction = GetPlusTrialVo.ButtonAction.GET_PLUS,
                GetPlusTrialVo.ButtonStyle.PLUS_GRADIENT
            ),
            secondaryButton = GetPlusTrialVo.Button(
                title = "Что такое Яндекс Плюс",
                buttonAction = GetPlusTrialVo.ButtonAction.ABOUT_PLUS,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format info for get access state`() {
        val actual = formatter.formatInfo(plusTrialInfo, GetPlusTrialArguments.State.GET_TO_ACCESS)
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.GET_PLUS,
            title = "Для участия заберите бесплатный Яндекс Плюс",
            subtitle = "Он даёт доступ к повышенному кешбэку баллами, купонам впечатлений и эксклюзиву во время Незабудней. Отключится 10 февраля.",
            primaryButton = GetPlusTrialVo.Button(
                title = "Забрать бесплатно",
                buttonAction = GetPlusTrialVo.ButtonAction.GET_PLUS,
                buttonStyle = GetPlusTrialVo.ButtonStyle.PLUS_GRADIENT
            ),
            secondaryButton = GetPlusTrialVo.Button(
                title = "Что такое Яндекс Плюс",
                buttonAction = GetPlusTrialVo.ButtonAction.ABOUT_PLUS,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format plus received for gift state`() {
        val actual = formatter.formatPlusReceived(GetPlusTrialArguments.State.GET_PRESENT)
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.PLUS_SUCCESS,
            title = "Отлично, вы в Плюсе!",
            subtitle = "Посмотрите, какие скидки и впечатления мы для вас приготовили. А завтра возвращайтесь за новыми!",
            primaryButton = GetPlusTrialVo.Button(
                title = "Посмотреть",
                buttonAction = GetPlusTrialVo.ButtonAction.SEE_PROMO,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            ),
            secondaryButton = null
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format plus received for access state`() {
        val actual = formatter.formatPlusReceived(GetPlusTrialArguments.State.GET_TO_ACCESS)
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.PLUS_SUCCESS,
            title = "Отлично, вы в Плюсе!",
            subtitle = "Теперь вы можете покупать товары из коллекции Незабудни Эксклюзив",
            primaryButton = GetPlusTrialVo.Button(
                title = "К товару",
                buttonAction = GetPlusTrialVo.ButtonAction.CLOSE,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            ),
            secondaryButton = null
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format load info error`() {
        val actual = formatter.formatInfoError()
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.ERROR,
            title = "Что-то пошло не так",
            subtitle = "Проверьте подключение к интернету и попробуйте еще раз",
            primaryButton = GetPlusTrialVo.Button(
                title = "Попробовать ещё раз",
                buttonAction = GetPlusTrialVo.ButtonAction.RELOAD_INFO,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            ),
            secondaryButton = null
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format get trial error`() {
        val actual = formatter.formatPlusError(isNetworkError = false)
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.ERROR,
            title = "Не удаётся подключить Плюс",
            subtitle = "Попробуйте снова или зайдите позже",
            primaryButton = GetPlusTrialVo.Button(
                title = "Повторить",
                buttonAction = GetPlusTrialVo.ButtonAction.GET_PLUS,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            ),
            secondaryButton = null
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format get trial network error`() {
        val actual = formatter.formatPlusError(isNetworkError = true)
        val expected = GetPlusTrialVo(
            image = GetPlusTrialVo.Image.ERROR,
            title = "Нет интернета",
            subtitle = "",
            primaryButton = GetPlusTrialVo.Button(
                title = "Обновить",
                buttonAction = GetPlusTrialVo.ButtonAction.GET_PLUS,
                buttonStyle = GetPlusTrialVo.ButtonStyle.REGULAR
            ),
            secondaryButton = null
        )
        assertThat(actual).isEqualTo(expected)
    }
}