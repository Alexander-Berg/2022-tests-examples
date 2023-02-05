package com.yandex.mail.react

import android.annotation.SuppressLint
import android.os.Looper
import android.text.util.Rfc822Token
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.yandex.mail.FeaturesConfig
import com.yandex.mail.LoadCallbacks
import com.yandex.mail.R
import com.yandex.mail.TestRuntimeException
import com.yandex.mail.asserts.IntentConditions.activity
import com.yandex.mail.asserts.ViewConditions
import com.yandex.mail.calendar.CalendarWebviewActivity
import com.yandex.mail.calendar_offline.OfflineCalendarActivity
import com.yandex.mail.conforms
import com.yandex.mail.metrica.MetricaConstns
import com.yandex.mail.provider.Constants.PRESET_QUERY_EXTRA
import com.yandex.mail.react.entity.ReactMessage
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.search.SearchQuery
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.FragmentController
import com.yandex.mail.tools.TestContext
import com.yandex.mail.tools.TestFragmentActivity
import com.yandex.mail.util.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.Shadows.shadowOf
import org.robolectric.fakes.RoboMenu

@RunWith(IntegrationTestRunner::class)
class ReactMailViewFragmentTest : BaseIntegrationTest() {

    lateinit var controller: FragmentController<ReactMailViewFragment>

    @Before
    fun `before each test`() {
        init(Accounts.testLoginData)

        controller = FragmentController.of(ReactMailViewFragment(), ReactMailViewTestActivity::class.java)

        controller.create().start().resume()
    }

    @Test
    fun `resetViewState should stop load webView resources if dom is ready`() {
        val reactMailViewFragment = controller.get()
        reactMailViewFragment.reactWebView = mock { on { isDomReady } doReturn true }

        reactMailViewFragment.resetViewState()
        verify(reactMailViewFragment.reactWebView).stopLoading()
    }

    @Test
    fun `resetViewState should not stop load webView resources if dom is not ready`() {
        val reactMailViewFragment = controller.get()
        reactMailViewFragment.reactWebView = mock { on { isDomReady } doReturn false }

        reactMailViewFragment.resetViewState()
        verify(reactMailViewFragment.reactWebView, never()).stopLoading()
    }

    @Test
    fun `onCanNotLoadThread shows error state`() {
        val reactMailViewFragment = controller.get()

        reactMailViewFragment.onCanNotLoadThread(TestRuntimeException())

        val errorView = controller.get().activity!!.findViewById<ViewGroup>(R.id.react_error)
        val loadingView = controller.get().activity!!.findViewById<ViewGroup>(R.id.react_loading)
        assertThat(errorView.visibility).isEqualTo(View.VISIBLE)
        assertThat(loadingView.visibility).isEqualTo(View.INVISIBLE)
        assertThat(reactMailViewFragment.reactWebView.visibility).isEqualTo(View.INVISIBLE)
    }

    @Test
    fun `onCanNotLoadMessageBodies shows snack`() {
        val reactMailViewFragment = controller.get()

        reactMailViewFragment.onCanNotLoadMessageBodies()

        val root = controller.get().activity!!.findViewById<ViewGroup>(android.R.id.content)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(controller.get().view).has(ViewConditions.snackbarWithText(root, R.string.connection_error))
    }

    @Test
    fun `cleanup menu works properly`() {
        val reactMailViewFragment = controller.get()
        reactMailViewFragment.positionInList = PositionInList.FIRST

        val menu = RoboMenu(app)
        val inflater = MenuInflater(app)

        reactMailViewFragment.onCreateOptionsMenu(menu, inflater)
        reactMailViewFragment.onPrepareOptionsMenu(menu)

        val deleteItem = menu.findMenuItem(app.getString(R.string.delete))
        assertThat(deleteItem.isVisible).isTrue
        assertThat(deleteItem.isEnabled).isTrue

        val upItem = menu.findMenuItem(app.getString(R.string.navigate_next))
        assertThat(upItem.isVisible).isTrue
        assertThat(upItem.isEnabled).isFalse

        val downItem = menu.findMenuItem(app.getString(R.string.navigate_previous))
        assertThat(downItem.isVisible).isTrue
        assertThat(downItem.isEnabled).isTrue

        val archiveItem = menu.findMenuItem(app.getString(R.string.archive))
        assertThat(archiveItem.isEnabled).isTrue
        assertThat(archiveItem.isVisible).isFalse
    }

    @Test
    fun `applyNewMessages does nothing with empty messages`() {
        val reactMailViewFragment = controller.get()
        reactMailViewFragment.reactWebView = mock()

        reactMailViewFragment.applyNewMessages(emptyList(), emptyList(), emptyList())

        verify(reactMailViewFragment.reactWebView, never()).evaluateJsFunction(anyString())
    }

    @Test
    fun `applyNewMessages passes messages to webView`() {
        val reactMailViewFragment = controller.get()
        reactMailViewFragment.reactWebView = mock()
        reactMailViewFragment.uid = user.uid

        val messagesToAdd: List<ReactMessage> = listOf(
            ReactMessage.Builder().messageId(1L).folderId(1L).timestamp(3L).read(true).typeMask(4096).build()
        )
        val messagesToUpdate: List<ReactMessage> = listOf(
            ReactMessage.Builder().messageId(2L).folderId(1L).timestamp(4L).read(true).typeMask(4096).build()
        )
        val messagesToRemove: List<ReactMessage> = listOf(
            ReactMessage.Builder().messageId(3L).folderId(1L).timestamp(5L).read(true).typeMask(4096).build()
        )

        reactMailViewFragment.applyNewMessages(messagesToAdd, messagesToUpdate, messagesToRemove)

        verify(reactMailViewFragment.reactWebView).evaluateJsFunction(eq(ReactWebView.JsCommand.ADD_MESSAGES), any())
        verify(reactMailViewFragment.reactWebView).evaluateJsFunction(eq(ReactWebView.JsCommand.REMOVE_MESSAGES), any())
        verify(reactMailViewFragment.reactWebView).evaluateJsFunction(eq(ReactWebView.JsCommand.UPDATE_MESSAGES), any())
    }

    @Test
    fun `test metrica for longTap yable action`() {
        val fragment = controller.get()
        fragment.onEmailDialogClickListener.onCopyEmailClicked("email")
        metrica.assertLastEvent(MetricaConstns.YableMetrics.YABLE_COPY_TAP)
        fragment.onEmailDialogClickListener.onNewEmailClicked("email")
        metrica.assertLastEvent(MetricaConstns.YableMetrics.YABLE_NEW_EMAIL_TAP)
        fragment.onEmailDialogClickListener.onShowCorrespondence("email")
        metrica.assertLastEvent(MetricaConstns.YableMetrics.YABLE_CORRESPONDENCE_TAP)
    }

    @Test
    fun `tap on show correspondence call SearchActivity`() {
        val email = "test@yandex.ru"
        val fragment = controller.get()

        fragment.onEmailDialogClickListener.onShowCorrespondence(email)

        val searchIntent = shadowOf(fragment.requireActivity()).nextStartedActivity
        val searchQuery = searchIntent.getParcelableExtra<SearchQuery>(PRESET_QUERY_EXTRA)
        assertThat(searchQuery).isNotNull
        assertEquals(email, searchQuery!!.query)
    }

    @Test
    fun `smart darken checks`() {
        val fragment = controller.get()
        fragment.uid = user.uid
        fragment.reactWebView = mock()

        fragment.setForcedLightThemeMid(1, false)
        fragment.setForcedLightThemeMid(1, true)
        fragment.setForcedLightThemeMid(3, true)
        fragment.setForcedLightThemeMid(4, false)

        assertThat(fragment.forcedLightTheme).containsExactly(4)

        val state = fragment.parentFragmentManager.saveFragmentInstanceState(fragment)
        controller.pause().stop().destroy()

        controller = FragmentController.of(ReactMailViewFragment(), ReactMailViewTestActivity::class.java)
        val restoredFragment = controller.get()
        restoredFragment.setInitialSavedState(state)
        controller.create().start().resume()

        assertThat(restoredFragment.forcedLightTheme).containsExactly(4)

        restoredFragment.reactWebView = mock()
        restoredFragment.setForcedLightThemeMids()
        val jsonVal = arrayOf<String>(gson.toJson("4"), gson.toJson(false))
        verify(restoredFragment.reactWebView).evaluateJsFunction(eq(ReactWebView.JsCommand.SET_DARK_MESSAGE), eq(jsonVal))
    }

    @Test
    fun `openLink opens calendar link in webview activity`() {
        val link = "https://calendar.yandex.ru/event/?uid=1&event_id=1&show_date=2020-05-30&view_type=week"

        val fragment = controller.get()
        fragment.uid = user.uid
        fragment.reactWebView = mock()

        fragment.openLink(link)
        val nextStartedActivity = TestContext.shapp.nextStartedActivity
        OfflineCalendarActivity
        val nextStartedActivityExpected =
            if (FeaturesConfig.FORCE_EXPERIMENTS) OfflineCalendarActivity::class.java
            else CalendarWebviewActivity::class.java

        assertThat(nextStartedActivity).conforms(activity(nextStartedActivityExpected))
    }

    @SuppressLint("Registered") // Required for test purpose only.
    class ReactMailViewTestActivity :
        TestFragmentActivity(),
        ReactMailViewFragment.Callbacks,
        LoadCallbacks {

        override fun showEmailDialog(token: Rfc822Token) {
        }

        override fun onMailViewFragmentEmpty() {
        }

        override fun onNavigateUpOrDown(uid: Long, itemId: Long, positionDelta: Int) {
        }

        override fun showLinkDialog(url: String) {
        }

        override fun dataLoaded(size: Int) {
        }

        override fun initialContentShown() {
        }
    }
}
