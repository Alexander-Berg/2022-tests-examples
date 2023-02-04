package ru.auto.ara.core.rules.di

import org.junit.rules.ExternalResource
import ru.auto.ara.TestObjectsProvider
import ru.auto.ara.ui.fragment.picker.DialogListener
import ru.auto.feature.payment.context.PaymentStatusContext

object TestObjectsHolder : TestObjectsProvider {
    override var paymentStatusListener: DialogListener<PaymentStatusContext>? = null
}

/**
 * Used to push any objects into the application during tests (e.g. any listeners).
 *
 * In case you need to push your object to the app:
 * 1. add it as a nullable field into [TestObjectsProvider];
 * 2. override that field in [TestObjectsHolder];
 * 3. configure [TestObjectsHolder] using this rule;
 * 4. add [TestObjectsProvider] as a dependency into the object receiver like: `ru.auto.data.util.Optional<TestObjectProvider>`;
 * 5. access the object like: `testObjectsProvider.value()?.yourObject`.
 */
class TestObjectsRule(private val config: (TestObjectsHolder) -> Unit) : ExternalResource() {
    override fun before() {
        TestObjectsHolder.apply(config)
    }
}
