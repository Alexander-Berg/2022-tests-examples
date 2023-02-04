package ru.auto.ara.test.payment

import ru.auto.ara.ui.fragment.picker.DialogListener
import ru.auto.feature.payment.context.PaymentStatusContext
import java.io.Serializable

class PaymentStatusListener: DialogListener<PaymentStatusContext>, Serializable {
    private var states: MutableList<PaymentStatusContext> = mutableListOf()

    override fun onChosen(context: PaymentStatusContext?) {
        context ?: return
        states.add(context)
    }

    fun getStates() = states.toList()
}
