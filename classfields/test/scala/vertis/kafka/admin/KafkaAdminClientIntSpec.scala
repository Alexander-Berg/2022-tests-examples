package vertis.kafka.admin

import org.apache.kafka.common.errors.{InvalidTopicException, TopicExistsException, UnknownTopicOrPartitionException}
import org.scalatest.Assertion
import zio.Task

import scala.reflect.ClassTag

class KafkaAdminClientIntSpec extends KafkaTest {

  "KafkaClient" should {
    val topic = "kafka-client-spec-test-topic"

    "create new topic" in kafkaTest { kafkaClient =>
      for {
        _ <- kafkaClient.createTopic(topic)
        found <- kafkaClient.topicExists(topic)
        _ <- check(found shouldBe true)
      } yield ()
    }

    "fail to create duplicate topic" in kafkaTest { kafkaClient =>
      for {
        result <- kafkaClient.createTopic(topic).either
        _ <- expect[TopicExistsException](result)
      } yield ()
    }

    "throw InvalidTopicException when topic name is invalid" in kafkaTest { kafkaClient =>
      val badTopic = "no/slashes/please"
      for {
        result <- kafkaClient.createTopic(badTopic).either
        _ <- expect[InvalidTopicException](result)
      } yield ()
    }

    "delete topic" in kafkaTest { kafkaClient =>
      for {
        _ <- kafkaClient.deleteTopic(topic)
        found <- kafkaClient.topicExists(topic)
        _ <- check(found shouldBe false)
      } yield ()
    }

    "fail to delete nonexistent topic" in kafkaTest { kafkaClient =>
      for {
        result <- kafkaClient.deleteTopic(topic).either
        _ <- expect[UnknownTopicOrPartitionException](result)
      } yield ()
    }
  }

  private def expect[E](value: Either[KafkaError, _])(implicit tag: ClassTag[E]): Task[Assertion] = {
    val correctExceptionThrown =
      value.fold(_.getCause shouldBe a[E], _ => fail(s"Expected ${tag.toString} to be thrown"))
    check(correctExceptionThrown)
  }
}
