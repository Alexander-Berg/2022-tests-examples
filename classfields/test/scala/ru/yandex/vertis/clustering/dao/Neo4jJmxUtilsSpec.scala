package ru.yandex.vertis.clustering.dao

import org.junit.runner.RunWith
import org.neo4j.jmx.JmxUtils
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec

/**
  * Specs for neo4j [[JmxUtils]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class Neo4jJmxUtilsSpec extends BaseSpec {

  "JmxUtils" should {

    "provide kernel attributes" in {
      val graph = asGraphInstance("generation.data")
      // https://neo4j.com/docs/java-reference/current/jmx-metrics/#jmx-kernel
      val kernel = JmxUtils.getObjectName(graph.service.get, "Kernel")
      JmxUtils.getAttribute(kernel, "KernelVersion").toString should
        be("neo4j-kernel, version: 3.3.3,13ce0dc2e5eb1d72cc4e8df41612e4ec136c83e3")
      JmxUtils.getAttribute(kernel, "DatabaseName").toString should
        be("graph.db")
    }
  }

}
