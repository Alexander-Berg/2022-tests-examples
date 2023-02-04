package ru.yandex.vertis.clustering.dao

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.utils.JcmdMemoryMetrics

import scala.util.Try

/**
  * Specs for neo4j memory consming
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class Neo4jMemorySpec extends BaseSpec {

  "Neo4j" should {

    // -XX:NativeMemoryTracking=detail
    "consume memory if start and stop" in {

      def iteration(): Try[Int] = {
        println(s"*** A ${JcmdMemoryMetrics.jcmd.get}")
        val graph = asGraphInstance("generation.data")
        graph.service.get.execute("MATCH (u) RETURN u")
        graph.service.get.shutdown()
        Try(0)
      }

      (0 to 100).foreach(_ => iteration())
    }
  }
}
