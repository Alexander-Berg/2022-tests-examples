package ru.yandex.vertis.billing.integration.test.environment

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import ru.yandex.vertis.billing.BillingEvent.CampaignActiveStatusChangeEvent
import ru.yandex.vertis.billing.OperationComponent
import ru.yandex.vertis.billing.backend.{IndexerEnvironment, PushClientProvider}
import ru.yandex.vertis.billing.dao.impl.mds.S3CampaignStorage
import ru.yandex.vertis.billing.indexer.BindingsCleaner
import ru.yandex.vertis.billing.integration.test.mocks.TestS3CampaignStorage
import ru.yandex.vertis.billing.kafka.KafkaProtoProducer
import ru.yandex.vertis.billing.push.AsyncPushClient
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService
import ru.yandex.vertis.billing.settings.{ServiceSettings, Settings}

import scala.concurrent.ExecutionContext

object TestIndexerEnvironment {

  def create(
      config: Config,
      messageDeliveryService: MessageDeliveryService,
      campaignActiveStatusProducer: KafkaProtoProducer[CampaignActiveStatusChangeEvent],
      s3CampaignStorage: S3CampaignStorage) = {
    val system = ActorSystem("indexer")
    val settings = Settings(config)
    val serviceSettings = ServiceSettings("autoru", config)

    val pushClientProvider = new StubPushClientProvider(system.dispatcher)

    new IndexerEnvironment(
      settings,
      serviceSettings,
      messageDeliveryService,
      system,
      pushClientProvider,
      s3CampaignStorage,
      campaignActiveStatusProducer
    )
  }
}

class StubPushClientProvider(val ec: ExecutionContext) extends PushClientProvider with OperationComponent {
  override protected def clazz: Class[_] = classOf[PushClientProvider]
  def serviceName: String = "service"

  override def provide(
      config: Config,
      cleaner: BindingsCleaner
    )(implicit mat: Materializer,
      ec: ExecutionContext): Option[AsyncPushClient] = None
}
