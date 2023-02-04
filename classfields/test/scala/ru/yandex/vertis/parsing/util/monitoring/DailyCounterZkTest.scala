package ru.yandex.vertis.parsing.util.monitoring

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.components.zookeeper.zkinterface.ZookeeperInterface
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.zookeeper.ZookeeperWrapperStub

/**
  * Tests for DailyCounterZk
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DailyCounterZkTest extends FunSuite with MockitoSupport {
  val zkClient: ZookeeperInterface = new ZookeeperWrapperStub

  test("inc") {
    val d = new DailyCounterZk("test", "test", zkClient)("l1", "l2")
    d.inc("a1", "a2")(5)
    d.inc("b1", "b2")(6)
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 5),
        GaugeData(Seq("b1", "b2"), 6)
      )
    )
    d.inc("b1", "b2")(6)
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 5),
        GaugeData(Seq("b1", "b2"), 12)
      )
    )
  }

  test("concurrent increment") {
    val t1 = new Thread(() => {
      val d = new DailyCounterZk("test2", "test", zkClient)("l1", "l2")
      (1 to 1000).foreach(_ => d.inc("a1", "a2")())
    }, "T1")

    val t2 = new Thread(() => {
      val d = new DailyCounterZk("test2", "test", zkClient)("l1", "l2")
      (1 to 1000).foreach(_ => d.inc("a1", "a2")())
    }, "T2")

    t1.start()
    t2.start()

    t1.join()
    t2.join()

    val d = new DailyCounterZk("test2", "test", zkClient)("l1", "l2")

    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 2000)
      )
    )
  }

  test("reset") {
    val date = DateUtils.currentDayStart
    val path = "test3"
    zkClient.create("/" + path)
    val data =
      DailyCounterData(date.minusDays(1), Seq("l1", "l2"), Map(Seq("a1", "a2") -> 45)).serialize.getBytes("UTF-8")
    zkClient.updateAndGet("/" + path)(_ => Some(data))
    val d = new DailyCounterZk(path, "test", zkClient)("l1", "l2")
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 0)
      )
    )
    d.inc("a1", "a2")()
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 1)
      )
    )
  }

  test("load from zk") {
    val date = DateUtils.currentDayStart
    val path = "test4"
    zkClient.create("/" + path)
    val data = DailyCounterData(date, Seq("l1", "l2"), Map(Seq("a1", "a2") -> 45)).serialize.getBytes("UTF-8")
    zkClient.updateAndGet("/" + path)(_ => Some(data))
    val d = new DailyCounterZk(path, "test", zkClient)("l1", "l2")
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 45)
      )
    )
  }
}
