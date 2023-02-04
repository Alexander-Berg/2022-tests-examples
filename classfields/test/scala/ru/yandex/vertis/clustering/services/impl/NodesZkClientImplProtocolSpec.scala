package ru.yandex.vertis.clustering.services.impl

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.services.NodeDecider._
import ru.yandex.vertis.clustering.utils.DateTimeUtils

import scala.util.Success

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class NodesZkClientImplProtocolSpec extends BaseSpec {

  import NodesZkClientImplProtocol._

  private val nodeGen = for {
    nodeId <- Gen.alphaNumStr.filter(_.nonEmpty)
    lastFactEpochLateInMin <- Gen.option(Gen.choose(1, 360).map(_.toLong))
    lastRotatesLateInMin <- Gen.option(Gen.choose(1, 360).map(_.toLong))
  } yield {
    val lastFactEpoch = lastFactEpochLateInMin.map(DateTimeUtils.now.minusMinutes)
    val lastRotates = lastRotatesLateInMin.map(DateTimeUtils.now.minusMinutes)
    Node(nodeId, lastFactEpoch, lastRotates)
  }

  "NodesZkClientImplProtocol" should {
    (1 to 100).flatten(_ => nodeGen.sample).distinct.foreach { node =>
      val ser = NodeSerializer.serialize(node)
      s"$node should serialize/deserialize correctly" in {
        NodeSerializer.deserialize(ser) shouldBe Success(node)
      }
    }
  }

}
