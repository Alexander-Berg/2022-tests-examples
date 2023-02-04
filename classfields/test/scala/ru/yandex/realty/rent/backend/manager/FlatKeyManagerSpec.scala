package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.errors.InvalidParamsApiException
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.dao._
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatKeyManagerSpec extends SpecBase with AsyncSpecBase with RequestAware {

  "FlatKeyManager" should {

    "generate two codes" in new Wiring with Data {
      (flatKeyCodeDao
        .generate(_: Long, _: Int)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(List(1, 2)))

      val result = flatKeyManager.getNewKeyCodes(userUid, 2).futureValue
      result shouldEqual Seq(("000001" -> "A00000000001"), ("000002" -> "A00000000002"))
    }

    "try generate too much codes" in new Wiring with Data {
      val count = FlatKeyManager.maximumKeyCodesAtOnce + 1
      val result = flatKeyManager.getNewKeyCodes(userUid, count).failed.futureValue
      result shouldBe a[InvalidParamsApiException]
    }

  }

  trait Wiring {

    val flatKeyCodeDao: FlatKeyCodeDao = mock[FlatKeyCodeDao]

    val flatKeyManager: FlatKeyManager = new FlatKeyManager(flatKeyCodeDao)

  }

  trait Data {
    this: Wiring =>

    val userUid = 101L

  }

}
