package ru.yandex.market.clean.presentation.feature.cms.item

import android.net.Uri
import android.os.Build
import com.annimon.stream.Optional
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.usecase.adfox.SendFenekBannerClickedUseCase
import ru.yandex.market.data.deeplinks.DeeplinkParser
import ru.yandex.market.data.deeplinks.links.MarketWebDeeplink
import ru.yandex.market.data.deeplinks.params.resolver.AdfoxRedirectResolver
import ru.yandex.market.data.deeplinks.params.resolver.MapUrlToDeeplinkUseCase
import ru.yandex.market.data.deeplinks.params.resolver.ResolveDeeplinkUseCase
import ru.yandex.market.deeplinks.DeeplinkSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MapUrlToDeeplinkUseCaseTest {

    private val deeplinkParser = mock<DeeplinkParser>()
    private val adfoxRedirectResolver = mock<AdfoxRedirectResolver>()
    private val sendFenekBannerClickedUseCase = mock<SendFenekBannerClickedUseCase>()
    private val resolveDeeplinkUseCase = mock<ResolveDeeplinkUseCase>()

    private val mapUrlToDeeplinkUseCase = MapUrlToDeeplinkUseCase(
        deeplinkParser,
        adfoxRedirectResolver,
        sendFenekBannerClickedUseCase,
        resolveDeeplinkUseCase
    )

    init {
        whenever(adfoxRedirectResolver.resolveAdfoxRedirect(any()))
            .doReturn(Optional.empty())
        whenever(adfoxRedirectResolver.resolveAdfoxRedirect(ADFOX_URL_PARAM))
            .doReturn(Optional.of(ADFOX_URL_PARAM))

        whenever(deeplinkParser.parse(any(), any()))
            .doReturn(null)
        whenever(deeplinkParser.parse(eq(Uri.parse(DEEPLINK_URL)), eq(DeeplinkSource.INTERNAL_DEEPLINK)))
            .doReturn(DEEPLINK_URL_DEEPLINK)
        whenever(deeplinkParser.parse(eq(Uri.parse(SEND_FENEK_ANSWER)), eq(DeeplinkSource.INTERNAL_DEEPLINK)))
            .doReturn(DEEPLINK_ADFOX_NO_PARAM)


        whenever(sendFenekBannerClickedUseCase.execute(any()))
            .doReturn(Single.error(IllegalStateException()))
        whenever(sendFenekBannerClickedUseCase.execute(ADFOX_URL_NO_PARAM))
            .doReturn(Single.just(SEND_FENEK_ANSWER))
    }

    @Test
    fun `When url is adfox link with deeplink in param usecase extracts deeplink and sends metrics`() {
        val deeplink = mapUrlToDeeplinkUseCase.execute(ADFOX_URL_PARAM)
            .test()
            .assertNoErrors()
            .assertComplete()
            .values().first()

        assertEquals(deeplink.uri, DEEPLINK_ADFOX_PARAM.uri)

        verify(adfoxRedirectResolver, times(1)).resolveAdfoxRedirect(any())
        verify(deeplinkParser, never()).parse(any(), any())
        verify(resolveDeeplinkUseCase, times(1)).resolve(any())
        //должен выполниться для отправки метрики
        verify(sendFenekBannerClickedUseCase, times(1)).execute(any())
    }

    @Test
    fun `When url is simple deeplink usecase extracts this deeplink and sends metrics`() {
        val deeplink = mapUrlToDeeplinkUseCase.execute(DEEPLINK_URL)
            .test()
            .assertValueCount(1)
            .assertNoErrors()
            .assertComplete()
            .values().first()

        assertEquals(deeplink.uri, DEEPLINK_URL_DEEPLINK.uri)

        verify(adfoxRedirectResolver, times(1)).resolveAdfoxRedirect(any())
        verify(deeplinkParser, times(1)).parse(any(), any())
        verify(resolveDeeplinkUseCase, times(1)).resolve(any())
        //должен выполниться для отправки метрики
        verify(sendFenekBannerClickedUseCase, times(1)).execute(any())
    }

    @Test
    fun `When url is adfox link without deeplink in param usecase tries to get deeplink from fenek service`() {
        val deeplink = mapUrlToDeeplinkUseCase.execute(ADFOX_URL_NO_PARAM)
            .test()
            .assertValueCount(1)
            .assertNoErrors()
            .assertComplete()
            .values().first()

        assertEquals(deeplink.uri, DEEPLINK_ADFOX_NO_PARAM.uri)

        verify(adfoxRedirectResolver, times(1)).resolveAdfoxRedirect(any())
        verify(deeplinkParser, times(2)).parse(any(), any())
        verify(resolveDeeplinkUseCase, times(1)).resolve(any())
        verify(sendFenekBannerClickedUseCase, times(1)).execute(any())
    }

    @Test
    fun `When url is none of the above return simple MarketWebDeeplink from the url`() {
        val deeplink = mapUrlToDeeplinkUseCase.execute(WEB_URL)
            .test()
            .assertValueCount(1)
            .assertNoErrors()
            .assertComplete()
            .values().first()

        assertEquals(deeplink.uri, DEEPLINK_WEB_URL.uri)

        verify(adfoxRedirectResolver, times(1)).resolveAdfoxRedirect(any())
        verify(deeplinkParser, times(1)).parse(any(), any())
        verify(resolveDeeplinkUseCase, times(1)).resolve(any())
        verify(sendFenekBannerClickedUseCase, times(1)).execute(any())
    }

    companion object {
        private const val DEEPLINK_URL = "https://beru.ru/1"
        private const val ADFOX_URL_PARAM = "https://beru.ru/2"
        private const val ADFOX_URL_NO_PARAM = "https://beru.ru/3"
        private const val SEND_FENEK_ANSWER = "${ADFOX_URL_NO_PARAM}_1"
        private const val WEB_URL = "https://beru.ru/4"

        private val DEEPLINK_URL_DEEPLINK = MarketWebDeeplink(Uri.parse(DEEPLINK_URL))
        private val DEEPLINK_ADFOX_PARAM = MarketWebDeeplink(Uri.parse(ADFOX_URL_PARAM))

        // нужно для проверки, что именно распарсился ответ от adfox, а не упал парсинг и дальше вернулся обычный
        // MarketWebDeeplink
        private val DEEPLINK_ADFOX_NO_PARAM = MarketWebDeeplink(Uri.parse(SEND_FENEK_ANSWER))
        private val DEEPLINK_WEB_URL = MarketWebDeeplink(Uri.parse(WEB_URL))
    }
}