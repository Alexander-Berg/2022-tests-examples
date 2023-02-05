package com.yandex.mail.pin

import android.os.Looper.getMainLooper
import android.view.View
import com.yandex.mail.R
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.settings.dialog.AlertDialogFragment
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.FragmentController
import com.yandex.mail.tools.TestFragmentActivity
import com.yandex.mail.util.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

@RunWith(IntegrationTestRunner::class)
class EnterPinFragmentTest : BaseIntegrationTest() {

    lateinit var controller: FragmentController<EnterPinFragment>

    lateinit var fragment: EnterPinFragment

    @Before
    fun beforeEachTest() {
        init(Accounts.testLoginData)

        controller = FragmentController.of(EnterPinFragment(), EnterPinTestActivity::class.java)
        fragment = controller.get()
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun `onPinVerificationError should clear pin`() {
        controller.create().start().resume().visible()

        fragment.onPinVerificationError(false)
        assertThat(fragment.viewBinding.pinView.pin).isEqualTo("")

        fragment.onPinVerificationError(true)
        assertThat(fragment.viewBinding.pinView.pin).isEqualTo("")
    }

    @Test
    fun `onPinVerificationError should set clearPin text if canEnter`() {
        controller.create().start().resume().visible()
        fragment.onPinVerificationError(true)

        assertThat(fragment.viewBinding.pinInfo.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.viewBinding.pinInfo.text).isEqualTo(IntegrationTestRunner.app().getString(R.string.enter_pin_forgot_title))
    }

    @Test
    fun `onPinVerificationError should not set clearPin text if not canEnter`() {
        controller.create().start().resume().visible()

        fragment.viewBinding.pinInfo.text = ""
        fragment.viewBinding.pinInfo.visibility = View.INVISIBLE

        fragment.onPinVerificationError(false)

        assertThat(fragment.viewBinding.pinInfo.visibility).isEqualTo(View.INVISIBLE)
        assertThat(fragment.viewBinding.pinInfo.text.toString()).isEqualTo("")
    }

    @Test
    fun `changeTimeTillCanEnterPin should hide pinInfo if zero`() {
        controller.create().start().resume().visible()
        fragment.changeTimeTillCanEnterPin(0)

        assertThat(fragment.viewBinding.pinInfo.visibility).isEqualTo(View.INVISIBLE)
    }

    @Test
    fun `changeTimeTillCanEnterPin should set text with time if non zero`() {
        controller.create().start().resume().visible()
        fragment.changeTimeTillCanEnterPin(10)

        val infoView = fragment.viewBinding.pinInfo

        assertThat(infoView.visibility).isEqualTo(View.VISIBLE)
        assertThat(infoView.text).isEqualTo(IntegrationTestRunner.app().getString(R.string.try_again_in, 10))
    }

    @Test
    fun `onFingerprintErrorNotRecognized should not fail after killing the fragment`() {
        controller.create().start().resume().visible()
        shadowOf(getMainLooper()).idle()
        fragment.onFingerprintErrorNotRecognized()
        shadowOf(getMainLooper()).idle()
        controller.pause().stop().destroy()
        shadowOf(getMainLooper()).idle()

        ShadowSystemClock.advanceBy(EnterPinFragment.ERROR_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    }

    class EnterPinTestActivity : TestFragmentActivity(), EnterPinFragment.EnterPinFragmentCallback {
        override fun onAccountsDropped() {}

        override fun onPinEntered() {}

        override fun showClearPinDialog(alertDialogFragment: AlertDialogFragment) {}

        override fun onFingerprintAuthenticationSucceed() {}
    }
}
