package ru.auto.ara

import ru.auto.ara.ui.fragment.picker.DialogListener
import ru.auto.feature.payment.context.PaymentStatusContext

interface TestObjectsProvider {
    val paymentStatusListener: DialogListener<PaymentStatusContext>?
}
