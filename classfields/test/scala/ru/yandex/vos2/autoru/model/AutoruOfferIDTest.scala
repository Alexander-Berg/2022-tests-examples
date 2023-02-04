package ru.yandex.vos2.autoru.model

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatest.OptionValues._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.model.UserRef

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 10.12.16
  */
@RunWith(classOf[JUnitRunner])
class AutoruOfferIDTest extends AnyFunSuite {

  private val userRef = UserRef.refAid(123)
  private val expectedVBucket = VBuckets.get(userRef)

  test("generate hash") {
    (1 to 10).foreach { _ =>
      val hash = AutoruOfferID.generateHash(userRef)
      (hash should have).length(8)
    }
  }

  test("save vBucket") {
    (1 to 10).foreach { id =>
      val hash = AutoruOfferID.generateHash(userRef)
      val offerID = new AutoruOfferID(id, Some(hash))
      offerID.vBucket.value shouldBe expectedVBucket
    }
  }
}
