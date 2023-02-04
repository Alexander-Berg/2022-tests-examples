package ru.yandex.vos2.api.managers.util

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vos2.model.{OfferRef, UserRef}
import ru.yandex.vos2.realty.dao.offers.RealtyOfferDao

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Success

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class EmergencyRescheduleManagerSpec extends WordSpec with Matchers with PropertyChecks with MockFactory {

  private val offerDao: RealtyOfferDao = mock[RealtyOfferDao]

  implicit val ex: ExecutionContext = Threads.SameThreadEc

  "EmergencyRescheduleManager" should {
    "successfully reschedule offers" in {

      val offers = Seq(RescheduleOffer("100000273", "281475848178432"))
      val offerRefs = offers.map(o => OfferRef(o.userRef.toPlain, s"i_${o.offerId}"))

      (offerDao
        .resheduleOffers(_: Seq[OfferRef])(_: Traced))
        .expects(offerRefs, *)
        .returning(1)
      (offerDao
        .resolveExternalIds(_: UserRef, _: Iterable[String])(_: Traced))
        .expects(*, *, *)
        .returning(Success(Map.empty))
      (offerDao.normalizeId _)
        .expects(*)
        .onCall { id: String =>
          s"i_$id"
        }

      val manager = new EmergencyRescheduleManagerImpl(offerDao)
      Await.result(manager.reschedule(offers)(Traced.empty), 5.seconds) should be(())
    }
  }
}
