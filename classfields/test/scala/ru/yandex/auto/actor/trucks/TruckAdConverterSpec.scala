package ru.yandex.auto.actor.trucks

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.actor.trucks.TruckAdConverter.PartitionedClusters
import ru.yandex.auto.core.model.TypedAttribute
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.model.trucks.{TruckAd, UnifiedTruckInfo}
import ru.yandex.auto.index.PartitionKey
import ru.yandex.auto.message.CarAdSchema.DiscountOptions

/**
  * Specification for [[ru.yandex.auto.actor.trucks.TruckAdConverter]] actor
  *
  * @author incubos
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class TruckAdConverterSpec(_system: ActorSystem)
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

  val indexProbe = TestProbe()
  val holocronPusherProbe = TestProbe()
  val converter = system.actorOf(Props(new TruckAdConverter(indexProbe.ref)))

  val unifiedTruckInfo = new UnifiedTruckInfo
  unifiedTruckInfo.setId("1")
  unifiedTruckInfo.setYear(2005)
  unifiedTruckInfo.setPrice(980000)
  unifiedTruckInfo.setCurrencyType("RUR")
  unifiedTruckInfo.setRun(176000)
  unifiedTruckInfo.setRunMetric("км")
  unifiedTruckInfo.setGeobaseId("213")
  unifiedTruckInfo.setMark(new TypedAttribute("mark", "GAZ", "GAZ", "GAZ"))
  unifiedTruckInfo.setModel(new TypedAttribute("model", "SOBOL_2752", "SOBOL_2752", "SOBOL_2752"))
  unifiedTruckInfo.setAutoruClientId("20113")
  unifiedTruckInfo.setAutoCathegory("TRUCK_CAT_LCV")
  unifiedTruckInfo.setStateKey(State.GOOD.toString)
  unifiedTruckInfo.setCarLocation("Екатеринбург, ул.Маневровая, 45")
  unifiedTruckInfo.setDiscountOptions(DiscountOptions.newBuilder().setTradein(36).build())
  unifiedTruckInfo.setUrlHashId("65ftyg8gh87")
  unifiedTruckInfo.setDealerShowcase(true)
  unifiedTruckInfo.setDealerHidden(true)
  unifiedTruckInfo.setTrustedDealerCallsAccepted(true)

  "A Converter" should {
    "successfully transform a one offer partition" in {
      val feedUnifiedInfo = List(unifiedTruckInfo)
      val feedPartition = PartitionKey(1)

      holocronPusherProbe.within(10.second) {
        converter ! ModifyTruckInfoRequest(feedPartition, feedUnifiedInfo)

        holocronPusherProbe.expectMsgClass(classOf[PartitionedClusters]) match {
          case x: PartitionedClusters if x.partition.equals(feedPartition) => {
            assert(x.entities.head != null)
            val ad = x.entities.head.asInstanceOf[TruckAd]
            assert(ad.getDiscountOptions.getTradein == 36)
            assert(ad.isDealerHidden)
            assert(ad.isDealerShowcase)
            assert(ad.isTrustedDealerCallsAccepted)
            Some("OK")
          }
          case msg => fail("Wrong clusterization request: %s".format(msg))
        }
      }
    }
  }
}
