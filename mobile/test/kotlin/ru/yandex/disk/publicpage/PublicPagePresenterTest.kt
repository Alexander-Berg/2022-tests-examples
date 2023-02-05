package ru.yandex.disk.publicpage

import android.net.Uri
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import ru.yandex.disk.publicpage.PublicPageAuthorizer.AuthStatus
import ru.yandex.disk.test.AndroidTestCase2
import rx.Single

class PublicPagePresenterTest : AndroidTestCase2() {

    private val mockView = mock<PublicPageView>()
    private val mockToolbarView = mock<PublicPageToolbarContract>()
    private val mockJsApi = mock<PublicJsApi>()
    private val mockNativeApi = mock<PublicNativeApi>()
    private val presenter = PublicPagePresenter(mockJsApi, mockNativeApi, mock(), mock()).apply {
        setInitialUrl(TEST_LINK)
        attachView(mockView, mockToolbarView)
    }

    @Test
    fun testHostApiSetTitle() {
        presenter.setTitle("TestTitle")
        ShadowLooper.runUiThreadTasks()
        verify(mockToolbarView).setTitle("TestTitle")
    }

    @Test
    fun testHostApiGotoDir() {
        presenter.gotoDir("dir", "file")
        ShadowLooper.runUiThreadTasks()
        verify(mockNativeApi).gotoDir("dir", "file")
    }

    @Test
    fun testRenderActionBarMainIdle() {
        val params = "{\"view\" : \"main\" , \"state\" : \"IDLE\" }"
        verifyDarkThemeCommon(params, true, true)
        verify(mockToolbarView, never()).setTitle(ArgumentMatchers.nullable(String::class.java))
    }

    @Test
    fun testRenderActionBarError() {
        val params = "{\"view\" : \"error\"}"
        verifyDarkThemeCommon(params, true, false)
        verify(mockToolbarView, never()).setTitle(ArgumentMatchers.nullable(String::class.java))
    }

    @Test
    fun testRenderActionBarSlider() {
        val params = "{\"view\" : \"slider\"}"
        verifyDarkThemeCommon(params, true, true)
        verify(mockToolbarView).setTitle("")
    }

    @Test
    fun testRenderActionBarFolder() {
        val params = "{\"view\" : \"folder\"}"
        verifyDarkThemeCommon(params, false, true)
        verify(mockToolbarView, never()).setTitle(ArgumentMatchers.nullable(String::class.java))
    }

    private fun verifyDarkThemeCommon(params: String, expectedHomeAsClose: Boolean,
        hasMenu: Boolean) {
        presenter.renderActionBar(params)
        ShadowLooper.runUiThreadTasks()
        verify(mockToolbarView).setDarkTheme()
        verify(mockToolbarView).setProgressEnabled(false)
        verify(mockToolbarView).setDisplayHomeAsClose(expectedHomeAsClose, true)
        verify(mockView).invalidateOptionsMenu()
        assertEquals(hasMenu, presenter.hasMenu())
    }

    @Test
    fun testRenderActionBarInProgress() {
        val params = "{\"view\" : \"main\" , \"state\" : \"IN_PROGRESS\" }"
        presenter.renderActionBar(params)
        ShadowLooper.runUiThreadTasks()
        verify(mockToolbarView).setProgressEnabled(true)
        assertTrue(presenter.isInProgressState)
    }

    @Test
    fun testRenderActionBarIdle() {
        val params = "{\"view\" : \"slider\" , \"state\" : \"IDLE\" }"
        presenter.renderActionBar(params)
        ShadowLooper.runUiThreadTasks()
        verify(mockToolbarView).setProgressEnabled(false)
        assertFalse(presenter.isInProgressState)
    }

    @Test
    fun testRenderActionBarSetTitle() {
        val params = "{\"view\" : \"main\" , \"state\" : \"IN_PROGRESS\", \"title\" : \"New Title\" }"
        presenter.renderActionBar(params)
        ShadowLooper.runUiThreadTasks()
        verify(mockToolbarView).setTitle("New Title")
        assertTrue(presenter.isInProgressState)
    }

    @Test
    fun testRenderActionBarComments() {
        whenever(mockNativeApi.commentsTitle).thenReturn("Comments")
        val params = "{\"view\" : \"comments\" , \"state\" : \"IDLE\" }"
        presenter.renderActionBar(params)
        ShadowLooper.runUiThreadTasks()
        verify(mockToolbarView).setLightTheme()
        verify(mockToolbarView).setTitle("Comments")
        verify(mockToolbarView).setProgressEnabled(false)
        verify(mockView).invalidateOptionsMenu()
        verify(mockToolbarView).setDisplayHomeAsClose(true, false)
        assertFalse(presenter.hasMenu())
        assertFalse(presenter.isInProgressState)
    }

    @Test
    fun testJsApi() {
        whenever(mockJsApi.currentUrl).thenReturn(TEST_LINK)
        presenter.back()
        Robolectric.flushForegroundThreadScheduler()
        verify(mockNativeApi).close()
        whenever(mockJsApi.currentUrl).thenReturn(TEST_LINK + "/path")
        presenter.back()
        Robolectric.flushForegroundThreadScheduler()
        verify(mockJsApi).back()
        presenter.save()
        Robolectric.flushForegroundThreadScheduler()
        verify(mockJsApi).save()
        presenter.showInfo()
        Robolectric.flushForegroundThreadScheduler()
        verify(mockJsApi).showInfo()
    }

    @Test
    fun testRequestAuthorizationWithAccount() {
        testRequestAuthorizationWithAccount(AuthStatus.OK)
        testRequestAuthorizationWithAccount(AuthStatus.ERROR)
        testRequestAuthorizationWithAccount(AuthStatus.CANCELED)
    }

    private fun testRequestAuthorizationWithAccount(status: String) {
        whenever(mockNativeApi.hasActiveAccount()).thenReturn(true)
        whenever(mockNativeApi.authorize(TEST_URL)).thenReturn(Single.just(status))
        presenter.requestAuthorization(TEST_URL, true)
        ShadowLooper.runUiThreadTasks()
        verify(mockJsApi).onAuthorizeResult(status)
    }

    @Test
    fun testRequestAuthorizationError() {
        whenever(mockNativeApi.hasActiveAccount()).thenReturn(true)
        whenever(mockNativeApi.authorize(TEST_URL)).thenReturn(Single.fromCallable { throw RuntimeException() })
        presenter.requestAuthorization(TEST_URL, true)
        ShadowLooper.runUiThreadTasks()
        verify(mockJsApi).onAuthorizeResult(AuthStatus.ERROR)
    }

    @Test
    fun testRequestAuthorizationNoAccount() {
        whenever(mockNativeApi.hasActiveAccount()).thenReturn(false)
        presenter.requestAuthorization(TEST_URL, true)
        ShadowLooper.runUiThreadTasks()
        verify(mockJsApi).onAuthorizeResult(AuthStatus.CANCELED)
        whenever(mockNativeApi.hasActiveAccount()).thenReturn(false)
        presenter.requestAuthorization(TEST_URL, false)
        ShadowLooper.runUiThreadTasks()
        verify(mockNativeApi).startLogin(PublicNativeApi.JS_LOGIN_REQUEST_CODE)
    }

    @Test
    fun testPublicLinkWithPath() {
        whenever(mockJsApi.currentUrl).thenReturn(TEST_LINK + "/path/path")
        presenter.back()
        whenever(mockJsApi.currentUrl).thenReturn(TEST_LINK + "/path/")
        presenter.back()
        verify(mockNativeApi, never()).close()
    }

    @Test
    fun testInitiaUrlNetworkError() {
        presenter.handleNetworkError(500, Uri.parse(TEST_LINK))
        ShadowLooper.runUiThreadTasks()
        verify(mockView).showNetworkError()
    }

    @Test
    fun testNetworkError() {
        presenter.handleNetworkError(500, Uri.parse(TEST_LINK + "/path"))
        ShadowLooper.runUiThreadTasks()
        verify(mockView, never()).showNetworkError()
    }

    @Test
    fun testMetricaError() {
        presenter.handleNetworkError(500, Uri.parse("https://mc.yandex.ru"))
        ShadowLooper.runUiThreadTasks()
        verify(mockView, never()).showNetworkError()
    }

    @Test
    fun testNotFoundError() {
        presenter.handleNetworkError(404, Uri.parse(TEST_LINK))
        ShadowLooper.runUiThreadTasks()
        verify(mockView, never()).showNetworkError()
    }

    @Test
    fun testSslError() {
        presenter.handleSslError(mock())
        ShadowLooper.runUiThreadTasks()
        verify(mockView).showSslError()
    }

    companion object {
        private const val TEST_LINK = "https://yadi.sk/i/kDQu743P3MG9hW"
        private const val TEST_URL = "https://yandex.ru"
    }
}
