package ru.yandex.vertis.telepony.properties

import com.typesafe.config.ConfigFactory
import org.scalatest.time.{Milliseconds, Seconds, Span}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.component.impl.CuratorComponentTest
import ru.yandex.vertis.telepony.factory.CuratorFactory
import ru.yandex.vertis.telepony.model.{Operator, Operators, TypedDomains}
import ru.yandex.vertis.telepony.properties.DynamicProperties._
import ru.yandex.vertis.telepony.settings.CuratorSettings

import scala.concurrent.duration._

/**
  * @author neron
  */
class ZkDynamicPropertiesIntSpec extends SpecBase with MockitoSupport with CuratorComponentTest {

  implicit val DefaultPatienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(3, Seconds), interval = Span(50, Milliseconds))
  //private val curatorSettings = CuratorSettings(connectString, "namespace", None, None)
  //private val curator = CuratorFactory.newClient(curatorSettings)

  "ZkDynamicProperties" should {
    "set value" in {
      val zk = new ZkDynamicProperties(curator, List(TypedDomains.autotest), syncPeriod = 1.second)
      val prop = UnhealthyOperatorsProperty
      zk.setValue(TypedDomains.autotest, prop, prop.default)
      eventually {
        zk.getValue(TypedDomains.autotest, prop) shouldEqual prop.default
      }
      val customValue = Set(Operators.Mtt, Operators.Mts)
      zk.setValue(TypedDomains.autotest, prop, customValue)
      eventually {
        zk.getValue(TypedDomains.autotest, prop) shouldEqual customValue
      }
    }

    "return default when fail to parse" in {
      val zk = new ZkDynamicProperties(curator, List(TypedDomains.autotest), syncPeriod = 1.second)
      val serDe = mock[PropertySerDe[Set[Operator]]]
      when(serDe.deserialize(eq("Mtt,Mts"))).thenThrow(new RuntimeException("fail to parse"))
      when(serDe.serialize(?)).thenReturn("Mtt,Mts")
      val prop = new Property[Set[Operator]]("name", null, serDe)
      zk.setValue(TypedDomains.autotest, prop, null)
      eventually {
        zk.getValue(TypedDomains.autotest, prop) shouldEqual prop.default
      }
    }
  }

}
