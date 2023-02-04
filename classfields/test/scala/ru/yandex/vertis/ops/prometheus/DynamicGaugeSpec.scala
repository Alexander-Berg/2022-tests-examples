package ru.yandex.vertis.ops.prometheus

import java.lang.{Double => JDouble}
import java.util.function.Supplier

import com.google.common.util.concurrent.AtomicDouble
import io.prometheus.client.Collector
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * @author roose
  */
@RunWith(classOf[JUnitRunner])
class DynamicGaugeSpec extends WordSpec with Matchers {

  private val timeSupplier = new Supplier[JDouble] {
    override def get(): JDouble = System.currentTimeMillis().toDouble
  }

  "DynamicGauge" should {

    "not fail on proper creation" in {
      DynamicGauge
        .build("gauge_name", "help")
        .labelNames("label_name")
        .addSupplier(timeSupplier, "label_value")
        .create
    }

    "fail on no name" in intercept[IllegalStateException] {
      DynamicGauge
        .build().help("help")
        .labelNames("label_name")
        .addSupplier(timeSupplier, "label_value")
        .create
    }

    "fail on no help" in intercept[IllegalStateException] {
      DynamicGauge
        .build().name("gauge_name")
        .labelNames("label_name")
        .addSupplier(timeSupplier, "label_value")
        .create
    }

    "fail on no suppliers" in intercept[IllegalStateException] {
      DynamicGauge
        .build().name("gauge_name")
        .labelNames("label_name")
        .create
    }

    "fail on invalid name" in intercept[IllegalArgumentException] {
      DynamicGauge
        .build("gauge name", "help") // spaces are prohibited
        .labelNames("label_name")
        .addSupplier(timeSupplier, "label_value")
        .create
    }

    "fail on wrong number of label values" in intercept[IllegalArgumentException] {
      DynamicGauge
        .build().name("gauge name")
        .labelNames("l1", "l2", "l3")
        .addSupplier(timeSupplier, "v1", "v2")
        .create
    }

    "fail on bad label names change" in intercept[IllegalArgumentException] {
      DynamicGauge
        .build().name("gauge name")
        .labelNames("l1", "l2", "l3")
        .addSupplier(timeSupplier, "v1", "v2", "v3")
        .labelNames("l1", "l2") // failing because we already have a supplier with another number of labels
        .create
    }

    "describe properly" in {
      val g = DynamicGauge
        .build("gauge_name", "help")
        .labelNames("l1", "l2")
        .addSupplier(timeSupplier, "v1", "v2")
      .create()
      val describeResult = g.describe()
      describeResult.size() shouldBe 1
      val familySamples = describeResult.iterator().next()
      familySamples.name shouldBe "gauge_name"
      familySamples.help shouldBe "help"
      familySamples.`type` shouldBe Collector.Type.GAUGE
    }

    "collect properly" in {
      val d = new AtomicDouble(1d)
      val g = DynamicGauge
        .build("gauge_name", "help")
        .labelNames("l1", "l2")
        .addSupplier(new Supplier[JDouble] {
          override def get(): JDouble = d.get
        }, "v1", "v2")
        .create()
      val collectResult = g.collect()
      collectResult.size shouldBe 1
      val familySamples = collectResult.iterator().next()
      familySamples.name shouldBe "gauge_name"
      familySamples.help shouldBe "help"
      familySamples.`type` shouldBe Collector.Type.GAUGE
      val samples = familySamples.samples
      samples.size shouldBe 1
      val sample = samples.iterator().next()
      sample.labelNames should contain theSameElementsInOrderAs List("l1", "l2")
      sample.labelValues should contain theSameElementsInOrderAs List("v1", "v2")
      sample.value shouldBe 1d

      d.set(2d)
      val newCollectedValue = g.collect().iterator().next().samples.iterator().next().value
      newCollectedValue shouldBe 2d
    }
  }
}
