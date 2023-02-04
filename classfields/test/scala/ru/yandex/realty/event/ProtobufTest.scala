package ru.yandex.realty.event

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by Sergey Kozlov <slider5@yandex-team.ru> on 03.05.2018
  */
@RunWith(classOf[JUnitRunner])
class ProtobufTest extends WordSpec with Matchers {

  import ModelGen._
  import Protobuf._

  "VertisRequestContext" should {
    "correctly converts" in {
      check(forAll(VertisRequestContextGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "Offer" should {
    "correctly converts" in {
      check(forAll(OfferGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "CreateOfferEvent" should {
    "correctly converts" in {
      check(forAll(CreateOfferEventGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "UpdateOfferEvent" should {
    "correctly converts" in {
      check(forAll(UpdateOfferEventGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "DeleteOfferEvent" should {
    "correctly converts" in {
      check(forAll(DeleteOfferEventGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "CreateUserEvent" should {
    "correctly converts" in {
      check(forAll(CreateUserEventGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "UpdateUserEvent" should {
    "correctly converts" in {
      check(forAll(UpdateUserEventGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "AuthorisationUserEvent" should {
    "correctly converts" in {
      check(forAll(AuthorisationUserEventGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }
}
