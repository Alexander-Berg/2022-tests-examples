package ru.auto.ara.core.mocks_and_stubbs.trust

import android.app.Activity
import android.content.Intent
import com.yandex.payment.sdk.ResultIntentKeys
import ru.auto.ara.core.TestEmptyActivityCommand
import ru.auto.ara.router.Navigator
import ru.auto.ara.util.payment.trust.ITrustPaymentController
import ru.auto.data.model.vas.PaymentMethod
import rx.Completable

class TestTrustPaymentController(
    private val router: Navigator,
    private val activityResultProvider: () -> TestEmptyActivityCommand.ActivityResult,
) : ITrustPaymentController {

    override fun showTrustPayment(paymentToken: String, paymentMethod: PaymentMethod): Completable =
        Completable.fromAction {
            router.perform(TestEmptyActivityCommand(activityResultProvider()))
        }

    companion object {
        const val LAUNCH_PAYMENT_ACTIVITY = 7673

        val TRUST_SUCCESS = TestEmptyActivityCommand.ActivityResult(
            requestCode = LAUNCH_PAYMENT_ACTIVITY,
            data = Intent(),
        )

        val TRUST_ERROR = TestEmptyActivityCommand.ActivityResult(
            requestCode = LAUNCH_PAYMENT_ACTIVITY,
            resultCode = Activity.RESULT_CANCELED,
            data = Intent().apply { putExtra(ResultIntentKeys.ERROR, false) },
        )
    }

}
