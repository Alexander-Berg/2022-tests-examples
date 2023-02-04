package ru.yandex.auto.actor.trucks

import java.util.Date

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.app.OperationalSupport
import ru.yandex.auto.clone.trucks.TrucksUnifyProcessor
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.model.{TruckOffer, TypedAttribute}
import ru.yandex.auto.core.model.trucks.UnifiedTruckInfo
import ru.yandex.auto.index.PartitionKey
import ru.yandex.auto.message.CarAdSchema.DiscountOptions
import ru.yandex.auto.offers.InvalidOffersLogBroker

/**
  * Specification for [[ru.yandex.auto.actor.trucks.Unifier]] actor
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

  val infoProcessor = new TrucksUnifyProcessor {

    override def processInfo(carInfo: UnifiedTruckInfo) {
      // Do nothing
    }
  }

  implicit val operationalSupport: OperationalSupport = new OperationalSupport()
  val offersLogBroker = new InvalidOffersLogBroker()
  val imageUnifierProbe = TestProbe()
  val unifier = system.actorOf(Props(new Unifier(imageUnifierProbe.ref, infoProcessor, offersLogBroker)))

  val offer = new TruckOffer
  offer.setId("1")
  offer.setDate(new Date())
  offer.setYear(2005)
  offer.setPrice(980000)
  offer.setCurrencyType("RUR")
  offer.setRun(176000)
  offer.setRunMetric("км")
  offer.setGeobaseId("213")
  offer.setMark(new TypedAttribute("mark", "GAZ", "GAZ", "GAZ"))
  offer.setModel(new TypedAttribute("model", "SOBOL_2752", "SOBOL_2752", "SOBOL_2752"))
  offer.setAutoruClientId("20113")
  offer.setAutoCathegory("TRUCK_CAT_LCV")
  offer.setStateKey(State.GOOD.toString)
  offer.setCarLocation("Екатеринбург, ул.Маневровая, 45")
  offer.setDiscountOptions(DiscountOptions.newBuilder().setTradein(46).build())
  offer.setDealerHidden(true)
  offer.setDealerShowcase(true)
  offer.setTrustedDealerCallsAccepted(true)

  "A Unifier" should {
    "successfully index a one offer partition" in {
      val feedOffers = List(offer)
      val feedPartition = PartitionKey(1)

      imageUnifierProbe.within(1000.second) {
        unifier ! TruckUnifyRequest(feedPartition, feedOffers)

        imageUnifierProbe.expectMsgClass(classOf[ModifyTruckInfoRequest]) match {
          case ModifyTruckInfoRequest(partition, offers) if partition.equals(feedPartition) => {
            val offer = offers.head
            assert(offer.getDiscountOptions != null)
            assert(offer.getDiscountOptions.getTradein == 46)
            assert(offer.isDealerHidden)
            assert(offer.isDealerShowcase)
            assert(offer.isTrustedDealerCallsAccepted)
            Some("OK")
          }
          case msg => fail("Wrong clusterization request: %s".format(msg))
        }
      }
    }
  }
}
