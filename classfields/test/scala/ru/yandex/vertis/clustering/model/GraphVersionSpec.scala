package ru.yandex.vertis.clustering.model

import java.time.ZonedDateTime

import org.apache.kafka.common.TopicPartition
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import cats.syntax.semigroup._

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class GraphVersionSpec extends BaseSpec {

  case class CombineVersionTestCase(a: GraphVersion, b: GraphVersion, result: GraphVersion)

  private val zdt1 = ZonedDateTime.now
  private val topicPartition1 = new TopicPartition("topic1", 0)
  private val topicPartition2 = new TopicPartition("topic1", 1)
  private val topicPartition3 = new TopicPartition("topic2", 0)

  private val combineVersionCases = Seq(
    CombineVersionTestCase(
      a = GraphVersion.Empty,
      b = GraphVersion.Empty,
      result = GraphVersion.Empty
    ),
    CombineVersionTestCase(
      a = GraphVersion.Empty,
      b = GraphVersion(lastFactEpoch = Some(zdt1), kafkaOffsets = Some(Map(topicPartition1 -> 0))),
      result = GraphVersion(lastFactEpoch = Some(zdt1), kafkaOffsets = Some(Map(topicPartition1 -> 0)))
    ),
    CombineVersionTestCase(
      a = GraphVersion(lastFactEpoch = Some(zdt1), kafkaOffsets = Some(Map(topicPartition1 -> 0))),
      b = GraphVersion(lastFactEpoch = Some(zdt1), kafkaOffsets = Some(Map(topicPartition1 -> 10))),
      result = GraphVersion(lastFactEpoch = Some(zdt1), kafkaOffsets = Some(Map(topicPartition1 -> 10)))
    ),
    CombineVersionTestCase(
      a = GraphVersion(lastFactEpoch = Some(zdt1), kafkaOffsets = Some(Map(topicPartition1 -> 0))),
      b = GraphVersion(lastFactEpoch = Some(zdt1.plusHours(1)), kafkaOffsets = Some(Map(topicPartition1 -> 0))),
      result = GraphVersion(lastFactEpoch = Some(zdt1.plusHours(1)), kafkaOffsets = Some(Map(topicPartition1 -> 0)))
    ),
    CombineVersionTestCase(
      a = GraphVersion(
        lastFactEpoch = Some(zdt1.plusHours(1)),
        kafkaOffsets = Some(Map(topicPartition1 -> 0, topicPartition2 -> 10, topicPartition3 -> 20))
      ),
      b = GraphVersion(
        lastFactEpoch = Some(zdt1),
        kafkaOffsets = Some(Map(topicPartition1 -> 10, topicPartition2 -> 0, topicPartition3 -> 20))
      ),
      result = GraphVersion(
        lastFactEpoch = Some(zdt1.plusHours(1)),
        kafkaOffsets = Some(Map(topicPartition1 -> 10, topicPartition2 -> 10, topicPartition3 -> 20))
      )
    )
  )

  "GraphVersion" should {
    "be correctly combined with other graph version" in {
      combineVersionCases.foreach {
        case CombineVersionTestCase(a, b, result) =>
          val actualResult1 = a |+| b
          val actualResult2 = b |+| a
          actualResult1 shouldBe result
          actualResult2 shouldBe result
      }
    }
  }

}
