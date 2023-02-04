package ru.yandex.auto.actor.moto

import java.util.Date
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.app.OperationalSupport
import ru.yandex.auto.clone.moto.MotoUnifyProcessor
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.model.moto.UnifiedMotoInfo
import ru.yandex.auto.core.model.{MotoOffer, TypedAttribute}
import ru.yandex.auto.index.PartitionKey
import ru.yandex.auto.offers.InvalidOffersLogBroker

/**
  * Specification for [[ru.yandex.auto.actor.moto.Unifier]] actor
  */
@RunWith(classOf[JUnitRunner])
class MotoUnifierSpec(_system: ActorSystem)
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

  val infoProcessor = new MotoUnifyProcessor {

    override def processInfo(carInfo: UnifiedMotoInfo) {
      // Do nothing
    }
  }

  implicit val operationalSupport: OperationalSupport = new OperationalSupport()
  val offersLogBroker = new InvalidOffersLogBroker()
  val imageUnifierProbe = TestProbe()
  val unifier = system.actorOf(Props(new Unifier(imageUnifierProbe.ref, infoProcessor, offersLogBroker)))

  val offer = new MotoOffer
  offer.setId("1")
  offer.setDate(new Date())
  offer.setYear(2022)
  offer.setPrice(1000000)
  offer.setCurrencyType("RUR")
  offer.setRun(100)
  offer.setRunMetric("км")
  offer.setGeobaseId("213")
  offer.setMark(new TypedAttribute("mark", "ABM", "ABM", "ABM"))
  offer.setModel(new TypedAttribute("model", "ATV_90", "ATV_90", "ATV_90"))
  offer.setAutoruClientId("11111")
  offer.setStateKey(State.GOOD.name())
  offer.setDealerHidden(true)
  offer.setDealerShowcase(true)
  offer.setTrustedDealerCallsAccepted(true)

  "A Unifier" should {
    "successfully index a one offer partition" in {
      val feedOffers = List(offer)
      val feedPartition = PartitionKey(1)

      imageUnifierProbe.within(1000.second) {
        unifier ! MotoUnifyRequest(feedPartition, feedOffers)

        imageUnifierProbe.expectMsgClass(classOf[ModifyMotoInfoRequest]) match {
          case ModifyMotoInfoRequest(partition, offers) if partition.equals(feedPartition) => {
            val offer = offers.head
            assert(offer.getYear == 2022)
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
