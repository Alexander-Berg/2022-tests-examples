package ru.yandex.vertis.ops.prometheus

import io.prometheus.client.Counter
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class OperationalAwareRegistrySpec
  extends WordSpec
    with Matchers {

  "OperationalAwareRegistry" should {
    "return already registered collector" in {
      val Env = "testing"
      val Dc = "SAS"
      val Instance = "localhost:1234"
      val Prefix = "test_"
      val collector = new OperationalAwareRegistry(new CompositeCollector, Env, Dc, Instance, Prefix)

      val counter1 = Counter.build("name", "help").create()
      collector.register(counter1)

      val counter2 = Counter.build("name", "help").create()
      collector.register(counter2) should be theSameInstanceAs counter1
    }
  }

}
