package ru.yandex.auto.actor

import java.util.{Collections, Date}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.actor.cars.{Unifier, UnifyRequest}
import ru.yandex.auto.app.OperationalSupport
import ru.yandex.auto.clone.unifier.InfoProcessor
import ru.yandex.auto.core.model.expenses.OwnerExpenses
import ru.yandex.auto.core.model.{Offer, UnifiedCarInfo}
import ru.yandex.auto.index.PartitionKey
import ru.yandex.auto.offers.InvalidOffersLogBroker
import ru.yandex.auto.unifier.UnunifiedFieldManager

/**
  * Specification for [[ru.yandex.auto.actor.cars.Unifier]] actor
  *
  * @author incubos
  */
@RunWith(classOf[JUnitRunner])
class UnifierSpec(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with MockitoSugar {

  def this() =
    this(ActorSystem("testsystem", ConfigFactory.parseString("""
akka.event-handlers = ["akka.testkit.TestEventListener"]
                                 """)))

  import concurrent.duration._

  val infoProcessor = new InfoProcessor {

    def processInfo(carInfo: UnifiedCarInfo, ununifiedFieldManager: UnunifiedFieldManager) {
      // Do nothing
    }
  }

  implicit val operationalSupport: OperationalSupport = new OperationalSupport()
  val offersLogBroker = new InvalidOffersLogBroker()
  val imageUnifierProbe = TestProbe()
  val unifier = system.actorOf(Props(new Unifier(imageUnifierProbe.ref, infoProcessor, offersLogBroker)))

  val offer = new Offer
  offer.setId("1")
  offer.setUrl("http://www.test.ru/offer/1")
  offer.setDate(new Date())
  offer.setMark("Mercedes-Benz")
  offer.setModel("M-класс")
  offer.setYear(2005)
  offer.setSellerPhones(Collections.singletonList("9286623107"))
  offer.setPrice(980000)
  offer.setCurrencyType("RUR")
  offer.setRun(176000)
  offer.setRunMetric("км")
  offer.setColor("Бежевый металлик")
  offer.setBodyType("Внедорожник")
  offer.setTransmission("Автомат")
  offer.setSteeringWheel("Левый")
  offer.setRealDisplacement(3500)
  offer.setImagePaths(Collections.singletonList("http://www.test.ru/image/1"))
  offer.setResourceId(-1L)
  offer.setOwnerExpenses(new OwnerExpenses(null, null))
  offer.setGeobaseId("213")
  offer.setState("USED")
  offer.setDealerHidden(true)
  offer.setDealerShowcase(true)
  offer.setTrustedDealerCallsAccepted(true)

  "A Unifier" should {
    "successfully index a one offer partition" in {
      val feedOffers = List(offer)
      val feedPartition = PartitionKey(1)

      imageUnifierProbe.within(1000.second) {
        unifier ! UnifyRequest(feedPartition, feedOffers)

        imageUnifierProbe.expectMsgClass(classOf[ModifyCarInfoRequest]) match {
          case ModifyCarInfoRequest(partition, offers) if partition.equals(feedPartition) =>
            val offer = offers.head
            assert(offer.getYear == 2005)
            assert(offer.getPrice == 980000)
            assert(offer.isDealerHidden)
            assert(offer.isDealerShowcase)
            assert(offer.isTrustedDealerCallsAccepted)
            Some("OK")
          case msg => fail("Wrong clusterization request: %s".format(msg))
        }
      }
    }
  }
}
