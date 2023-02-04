package ru.yandex.vertis.punisher.services

import java.io.IOException
import java.net.URL
import org.apache.kafka.common.TopicPartition
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.clustering.proto.Model.ClusteringFormula
import ru.yandex.vertis.punisher.{BaseSpec, Globals}
import ru.yandex.vertis.punisher.config.UserClusteringApiConfig
import ru.yandex.vertis.punisher.model.KafkaOffset
import ru.yandex.vertis.punisher.services.impl.UserClusteringServiceImpl
import ru.yandex.vertis.quality.cats_utils.Awaitable._

/**
  * @author devreggs
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class UserIdClusteringServiceMannuallySpec extends BaseSpec {

  private val config: UserClusteringApiConfig =
    UserClusteringApiConfig(
      url = new URL("http://user-clustering-api-int.vrts-slb.test.vertis.yandex.net"),
      queueSize = 1000,
      throttleRps = 10,
      throttleMaxBurst = 5
    )

  private val userId = "51051504"
  private val topicPartition = new TopicPartition(Globals.VertisEventLogTopic, 1)
  private val nonExistentTopicPartition = new TopicPartition(Globals.VertisEventLogTopic, 999)
  private val oldKafkaOffset = 999L
  private val newKafkaOffset = 999999999L

  private val userClusteringService: UserClusteringService[F] =
    new UserClusteringServiceImpl(config, Domain.DOMAIN_AUTO)

  "UserClusteringService" should {
    "retrieve cluster members" in {
      val response = userClusteringService.getCluster(ClusteringFormula.L1_STRICT)(userId).await
      response.members.size should be > 1
    }

    "retrieve cluster members if kafka offset is specified" in {
      val response =
        userClusteringService
          .getCluster(ClusteringFormula.L1_STRICT)(
            userId,
            kafkaOffset = Some(KafkaOffset(topicPartition, oldKafkaOffset))
          )
          .await
      response.members.size should be > 1
    }

    "fail on retrieve cluster members if specified kafka offset is bigger than the committed one" in {
      val request =
        userClusteringService
          .getCluster(ClusteringFormula.L1_STRICT)(
            userId,
            kafkaOffset = Some(KafkaOffset(topicPartition, newKafkaOffset))
          )

      getException(request).isInstanceOf[IOException] shouldBe true
    }

    "fail on retrieve cluster members if specified topic partition doesn't exist" in {
      val request =
        userClusteringService
          .getCluster(ClusteringFormula.L1_STRICT)(
            userId,
            kafkaOffset = Some(KafkaOffset(nonExistentTopicPartition, newKafkaOffset))
          )

      getException(request).isInstanceOf[IOException] shouldBe true
    }
  }

  def getException(f: F[_]): Throwable = f.attempt.await.left.getOrElse(throw new IllegalArgumentException)
}
