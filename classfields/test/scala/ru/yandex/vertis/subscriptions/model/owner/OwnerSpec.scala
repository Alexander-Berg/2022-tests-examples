package ru.yandex.vertis.subscriptions.model.owner

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.subscriptions.SpecBase

/**
  * Specs on [[Owner]] model objects.
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class OwnerSpec extends SpecBase with PropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)

  "Owner plain conversion" should {
    "round trip owner" in {
      forAll(OwnerGenerators.owner) { owner =>
        roundTripPlain(owner)
        roundTripModel(owner)
      }
    }

    "round trip owner with overriden toPlain()" in {
      roundTripPlain(AuthorizedAutoruUser("123"))
      roundTripPlain(UnauthorizedAutoruUser("123.123.foo.123"))
    }
  }

  private def roundTripPlain(owner: Owner) = {
    Owner.parse(owner.toPlain).get should be(owner)
  }

  private def roundTripModel(owner: Owner) = {
    Owner.fromLegacyUser(owner.asLegacyUser) should be(owner)
  }
}
