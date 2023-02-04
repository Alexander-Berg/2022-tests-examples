package ru.yandex.vertis.moderation.client.impl.ning

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.client.{ModerationClientFactory, SpecBase}
import ru.yandex.vertis.moderation.proto.Model._
import ru.yandex.vertis.moderation.proto.{AutoruFactory, ModelFactory}

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * @author potseluev
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class ManualNingDealersAutoruModerationClientSpec
  extends SpecBase {

  val moderationClientFactory: ModerationClientFactory =
    NingModerationClientFactory(new DefaultAsyncHttpClientConfig.Builder().build())

  val moderationClient = moderationClientFactory.client(
    "moderation-push-api-01-sas.test.vertis.yandex.net",
    37158,
    Service.DEALERS_AUTORU
  )

  "Moderation client" should {
    "push correctly" in {
      val instanceSource =
        ModelFactory.newInstanceSourceBuilder()
          .setContext(ModelFactory.newContextSourceBuilder().setVisibility(Visibility.VISIBLE).build())
          .setExternalId(
            ModelFactory.newExternalIdBuilder()
              .setObjectId("dealer_000")
              .setUser(ModelFactory.newUserBuilder().setDealerUser("000")))
          .setEssentials(ModelFactory.newEssentialsBuilder()
            .setDealerAutoruEssentials(AutoruFactory.newDealerAutoruEssentialsBuilder().build()))
          .addSignals(
            ModelFactory.newSignalSourceBuilder()
              .setIndexErrorSignal(
                ModelFactory.newIndexErrorSignalSourceBuilder()
                  .setDomain(ModelFactory.newDomainBuilder().setDealersAutoru(Domain.DealersAutoru.DEFAULT_DEALERS_AUTORU))
                  .addReasons(Reason.NOT_VERIFIED)
                  .setSource(ModelFactory.newSourceBuilder().setAutomaticSource(ModelFactory.newAutomaticSourceBuilder().setApplication(AutomaticSource.Application.INDEXER)))
              ).build()
          ).build()
      moderationClient.push(Seq(instanceSource), useBatch = false).futureValue
    }
  }

}