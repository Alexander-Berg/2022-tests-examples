package ru.auto.ara.core.mocks_and_stubbs.trust

import ru.auto.ara.core.TestEmptyActivityCommand
import ru.auto.ara.router.Navigator
import ru.auto.ara.util.payment.trust.ITrustPaymentController

object TestTrustPaymentControllerFactory : ITrustPaymentController.Factory {

    private val defaultActivityResultProvider: () -> TestEmptyActivityCommand.ActivityResult = {
        TestEmptyActivityCommand.ActivityResult()
    }

    var activityResultProvider: () -> TestEmptyActivityCommand.ActivityResult = defaultActivityResultProvider

    override fun create(router: Navigator): ITrustPaymentController =
        TestTrustPaymentController(router, activityResultProvider)

    fun restoreDefaultActivityResultProvider() {
        activityResultProvider = defaultActivityResultProvider
    }

}
