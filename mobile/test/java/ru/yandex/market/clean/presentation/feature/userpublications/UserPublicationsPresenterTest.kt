package ru.yandex.market.clean.presentation.feature.userpublications

import android.os.Build
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atMost
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.presentationSchedulersMock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class UserPublicationsPresenterTest : TestCase() {

    private val schedulers = presentationSchedulersMock()
    private val router = mock<Router>()
    private val view = mock<UserPublicationsView>()
    private val presenter = UserPublicationsPresenter(schedulers, router).apply {
        attachView(view)
    }

    @Test
    fun `navigate back`() {
        presenter.navigateBack()
        verify(router).back()
    }

    @Test
    fun `check initial tab is reviews`() {
        presenter.onTabSelected(UserPublicationTab.REVIEWS)
        verify(view, atMost(1)).selectTab(UserPublicationTab.REVIEWS)
    }

    @Test
    fun `check tab changed`() {
        presenter.onTabSelected(UserPublicationTab.ANSWERS)
        verify(view).selectTab(UserPublicationTab.ANSWERS)
    }

}