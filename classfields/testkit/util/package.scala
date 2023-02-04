package ru.yandex.vertis.shark

import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.{Arbitraries, AutoruCreditApplication, _}

package object util {
    import org.scalacheck.magnolia._

    def sampleAutoruCreditApplciation(): AutoruCreditApplication =
        Arbitraries.generate[AutoruCreditApplication].sample.get

    def sampleOffer(): AutoruCreditApplication.Offer =
        Arbitraries.generate[AutoruCreditApplication.Offer].sample.get

    def sampleClaim(): CreditApplication.AutoruClaim =
        Arbitraries.generate[CreditApplication.AutoruClaim].sample.get
}
