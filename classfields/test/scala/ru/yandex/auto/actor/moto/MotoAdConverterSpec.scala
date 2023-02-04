package ru.yandex.auto.actor.moto

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.actor.moto.MotoAdConverter.PartitionedClusters
import ru.yandex.auto.core.model.TypedAttribute
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.model.moto.{MotoAd, UnifiedMotoInfo}
import ru.yandex.auto.index.PartitionKey

import java.util.Date

/**
  * Specification for [[ru.yandex.auto.actor.moto.MotoAdConverter]] actor
  *
  * @author incubos
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class MotoAdConverterSpec(_system: ActorSystem)
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
  val converter = system.actorOf(Props(new MotoAdConverter(indexProbe.ref)))

  val unifiedMotoInfo = new UnifiedMotoInfo
  unifiedMotoInfo.setId("1")
  unifiedMotoInfo.setYear(2022)
  unifiedMotoInfo.setPrice(1000000)
  unifiedMotoInfo.setCreationDate(new Date().getTime)
  unifiedMotoInfo.setCurrencyType("RUR")
  unifiedMotoInfo.setRun(100)
  unifiedMotoInfo.setRunMetric("км")
  unifiedMotoInfo.setGeobaseId("213")
  unifiedMotoInfo.setMark(new TypedAttribute("mark", "ABM", "ABM", "ABM"))
  unifiedMotoInfo.setModel(new TypedAttribute("model", "ATV_90", "ATV_90", "ATV_90"))
  unifiedMotoInfo.setAutoruClientId("11111")
  unifiedMotoInfo.setStateKey(State.GOOD.name())
  unifiedMotoInfo.setDealerHidden(true)
  unifiedMotoInfo.setDealerShowcase(true)
  unifiedMotoInfo.setTrustedDealerCallsAccepted(true)

  "A Converter" should {
    "successfully transform a one offer partition" in {
      val feedUnifiedInfo = List(unifiedMotoInfo)
      val feedPartition = PartitionKey(1)

      holocronPusherProbe.within(10.second) {
        converter ! ModifyMotoInfoRequest(feedPartition, feedUnifiedInfo)

        holocronPusherProbe.expectMsgClass(classOf[PartitionedClusters]) match {
          case x: PartitionedClusters if x.partition.equals(feedPartition) => {
            assert(x.entities.head != null)
            val ad = x.entities.head.asInstanceOf[MotoAd]
            assert(ad.getRun == 100)
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
