package ru.yandex.vertis.ops.prometheus

import io.prometheus.client.Counter
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

import scala.collection.JavaConverters._

/**
  * Specs on collector operational enriching.
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class OperationalAwareCollectorSpec
  extends WordSpec
    with Matchers {

  "OperationalAwareCollector" should {
    "enrich metrics with operational stuff" in {
      val counter = Counter.build("example", "example").create()

      val Env = "testing"
      val Dc = "SAS"
      val Instance = "localhost:1234"
      val Prefix = "test_"

      val collector = new OperationalAwareCollector(counter, Env, Dc, Instance, Prefix)

      val familySamples = collector.collect()
      familySamples.asScala.foreach {
        sample =>
          sample.name should startWith(Prefix)
          sample.samples.asScala.foreach {
            s =>
              val names = s.labelNames.asScala.toSet
              names should contain(OperationalAwareCollector.LABEL_ENV)
              names should contain(OperationalAwareCollector.LABEL_DC)
              names should contain(OperationalAwareCollector.LABEL_INSTANCE)

              val values = s.labelValues.asScala.toSet
              values should contain(Env)
              values should contain(Dc)
              values should contain(Instance)
          }
      }
    }
  }

}
