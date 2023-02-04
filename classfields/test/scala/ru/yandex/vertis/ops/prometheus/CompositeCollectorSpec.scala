package ru.yandex.vertis.ops.prometheus

import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.{Collector, Counter}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * Specs on [[CompositeCollector]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class CompositeCollectorSpec
  extends WordSpec
    with Matchers {

  "CompositeCollector" should {

    "register different counters" in {
      val collector = new CompositeCollector

      val counter1 = Counter.build("name1", "help").create()
      collector.register(counter1) should be theSameInstanceAs counter1
      val counter2 = Counter.build("name2", "help").create()
      collector.register(counter2) should be theSameInstanceAs counter2
    }

    "not register duplicate counter" in {
      val collector = new CompositeCollector

      val initialCounter = Counter.build("name", "help").create()
      collector.register(initialCounter) should be theSameInstanceAs initialCounter

      val anotherCounter = Counter.build("name", "help").create()
      collector.register(anotherCounter) should be theSameInstanceAs initialCounter

      val anotherCounterWithLables = Counter.build("name", "help")
        .labelNames("foo", "bar")
        .create()
      collector.register(anotherCounterWithLables) should be theSameInstanceAs initialCounter
    }

    "register different wrapped counter" in {
      val collector = new CompositeCollector

      val Env = "testing"
      val Dc = "SAS"
      val Instance = "localhost:1234"
      val Prefix = "test_"

      val counter1 = Counter.build("name1", "help").create()
      val wrappedCollector1 = new OperationalAwareCollector(counter1, Env, Dc, Instance, Prefix)
      collector.register(wrappedCollector1)

      val counter2 = Counter.build("name2", "help").create()
      val wrappedCollector2 = new OperationalAwareCollector(counter2, Env, Dc, Instance, Prefix)

      collector.register(wrappedCollector2) should be theSameInstanceAs wrappedCollector2
    }

    "not register duplicate wrapped counter" in {
      val collector = new CompositeCollector

      val Env = "testing"
      val Dc = "SAS"
      val Instance = "localhost:1234"
      val Prefix = "test_"

      val counter = Counter.build("name", "help").create()

      val wrappedCollector1 = new OperationalAwareCollector(counter, Env, Dc, Instance, Prefix)
      collector.register(wrappedCollector1)

      val wrappedCollector2 = new OperationalAwareCollector(counter, Env, Dc, Instance, Prefix)

      collector.register(wrappedCollector2) should be theSameInstanceAs wrappedCollector1
    }

    "unregister collector" in {
      val collector = new CompositeCollector
      val counter1 = Counter.build("name1", "help").create()
      collector.register(counter1) should be theSameInstanceAs counter1
      val counter2 = Counter.build("name2", "help").create()
      collector.register(counter2) should be theSameInstanceAs counter2

      counter1.inc()
      counter2.inc()
      collector.collect().size() shouldBe 2

      collector.unregister(counter2)
      collector.collect().size() shouldBe 1
      collector.collect().get(0).name shouldBe "name1"
    }

    class NameTransformingCollector(source: Collector,
                                    prefixName: String) extends TransformingCollector(source) {
      override protected def enrich(name: String): String = prefixName + name

      override protected def enrich(sample: MetricFamilySamples.Sample): MetricFamilySamples.Sample = sample
    }

    "register collectors with same name and different scala types" in {
      val registry = new CompositeCollector

      val counter2 = Counter.build("name", "help").create()
      registry.register(counter2) should be theSameInstanceAs counter2

      val counter1 = Counter.build("name", "help").create()
      val emptyTransformingCounter1 = new NameTransformingCollector(counter1, "")
      // java.lang.ClassCastException: Counter cannot be cast to NameTransformingCollector
      val actualRegistered = registry.register(emptyTransformingCounter1)
      actualRegistered should be theSameInstanceAs emptyTransformingCounter1
    }

    "register wrapped collectors with different names" in {
      val registry = new CompositeCollector

      val counter1 = Counter.build("name", "help").create()
      val emptyTransformingCounter1 = new NameTransformingCollector(counter1, "prefix_")
      registry.register(emptyTransformingCounter1) should be theSameInstanceAs emptyTransformingCounter1

      val counter2 = Counter.build("name", "help").create()
      registry.register(counter2) should be theSameInstanceAs counter2

    }
  }

}
