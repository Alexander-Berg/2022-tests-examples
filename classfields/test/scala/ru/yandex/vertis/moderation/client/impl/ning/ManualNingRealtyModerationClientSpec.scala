package ru.yandex.vertis.moderation.client.impl.ning

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.Span
import ru.yandex.vertis.moderation.client.ModerationClient.ClientException
import ru.yandex.vertis.moderation.client.{ModerationClientFactory, ModerationClientSpecBase, SpecBase}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.{Service, Source}
import ru.yandex.vertis.moderation.proto.{Model, ModelFactory}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.JavaConverters._

/**
  * Impl of [[ModerationClientSpecBase]] for realty to run it manually
  *
  * @author alesavin
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class ManualNingRealtyModerationClientSpec
  extends SpecBase {

  implicit val p = PatienceConfig(
    timeout = Span.convertDurationToSpan(1.seconds),
    interval = Span.convertDurationToSpan(50.millis)
  )

  val moderationClientFactory: ModerationClientFactory =
    NingModerationClientFactory(new DefaultAsyncHttpClientConfig.Builder().build())
  val moderationClient = moderationClientFactory.client(
    "moderation-push-api-01-sas.test.vertis.yandex.net",
    37158,
    Service.REALTY
  )

  val ExternalId =
    ModelFactory.newExternalIdBuilder.
      setUser(ModelFactory.newUserBuilder().
        setYandexUser("557207357").
        build()).
      setObjectId("6968698581650205185").
      build

  val UnknownExternalId =
    ModelFactory.newExternalIdBuilder.
      setUser(ModelFactory.newUserBuilder().
        setYandexUser("4002386897").
        build()).
      setObjectId("9053211304042960385").
      build

  "opinions" should {
    "correctly work for one unknown external id" in {
      val externalId = ExternalId
      val opinions = moderationClient.opinions(Seq(externalId)).futureValue
      info(opinions.toList.toString)
      val op = moderationClient.domainOpinion(externalId).futureValue
      info(op.toString)
    }
  }

  "get by external id" should {
    "works correctly" in {
      val externalId = ExternalId
      moderationClient.getCurrent(externalId).futureValue
    }

    "fail for unknown external id" in {
      val externalId = UnknownExternalId
      val e = the[ClientException] thrownBy Await.result(moderationClient.getCurrent(externalId), 1.second)
      e.code shouldBe 404
    }
  }

  private val signalKey = "automatic_COMPLAINTS_warn_NO_ANSWER"

  "append signals" should {
    "works correctly" in {
      val source: Source = ModelFactory.newSourceBuilder.
        setAutomaticSource(ModelFactory.newAutomaticSourceBuilder.setApplication(Application.COMPLAINTS))
        .build
      val signalSource = ModelFactory.newSignalSourceBuilder.setWarnSignal(
        ModelFactory.newWarnSignalSourceBuilder
          .setSource(source)
          .setReason(Model.Reason.NO_ANSWER)
          .setWeight(1.0)
          .build
      ).build
      val appendSignalsRequest = ModelFactory.newAppendSignals()
        .setExternalId(ExternalId)
        .addAllSignalSources(List(signalSource).asJava)
        .build
      moderationClient.appendSignals(appendSignalsRequest).futureValue
    }
  }

  "add switch-off" should {
    "works correctly" in {

      val source = ModelFactory
        .newSignalSwitchOffSource()
        .setSignalKey(signalKey)
        .build()

      val addSwitchOffsRequest = ModelFactory.newAddSwitchOffs()
        .setExternalId(ExternalId)
        .addSignalSwitchOffSources(source)
        .build()

      moderationClient.addSwitchOffs(addSwitchOffsRequest).futureValue
    }
  }

  "delete switch-off" should {
    "works correctly" in {

      val deleteSwitchOffsRequest = ModelFactory.newDeleteSwitchOffs()
        .setExternalId(ExternalId)
        .addSignalKeys(signalKey)
        .build()

      moderationClient.deleteSwitchOffs(deleteSwitchOffsRequest).futureValue
    }
  }

  "remove signals" should {
    "works correctly" in {
      val removeSignalsRequest = ModelFactory.newRemoveSignals()
        .setExternalId(ExternalId)
        .addAllSignalKeys(List(signalKey).asJava)
        .build
      moderationClient.removeSignals(removeSignalsRequest).futureValue
    }
  }

}
