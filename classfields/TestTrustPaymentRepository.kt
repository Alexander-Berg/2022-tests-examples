package ru.auto.ara.core.mocks_and_stubbs.trust

import ru.auto.data.repository.ITrustPaymentRepository
import rx.Single

object TestTrustPaymentRepository : ITrustPaymentRepository {

    val paymentMethodsIdsSource = mutableSetOf(
        "card-x027e2be14c177b417dfb2507",
        "card-xdc22d1a6802cf04d1d6b4c04",
    )

    override fun getPaymentMethodsIds(): Single<Set<String>> = Single.just(paymentMethodsIdsSource)

}
