package ru.yandex.vertis.moderation.feature

import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.TelegramClient.Message
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.TelegramChatNotificator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext

/**
  * @author Anton Tsyganov (jenkl)
  */
class TelegramNotifyingFeatureServiceSpec extends SpecBase {

  val testEnvironment: Environments.Value = Environments.Testing
  val testService: Service = Service.AUTORU
  val mockTelegramNotificator: TelegramChatNotificator = spy(new TelegramNotificatorStub)

  val featureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
  val featureService: FeatureServiceImpl with TelegramNotifyingFeatureService =
    new FeatureServiceImpl(featureRegistry) with TelegramNotifyingFeatureService {
      override def opsContext: ExecutionContext = global
      override def telegramNotificator: TelegramChatNotificator = mockTelegramNotificator
      override def environment: Environments.Value = testEnvironment
      override def service: Service = testService
    }

  val featureName = "feature"
  val nonExistFeature = "noSuchFeature"
  val featureOldValue = "oldValue"
  val featureNewValue = "newValue"
  val author = "Author"
  val description = "Description"

  featureRegistry.register(featureName, featureOldValue)

  "TelegramNotifyingFeatureRegistry" should {

    "not send telegram notification if new feature value is equals to old value with no author and description" in {
      featureService.updateFeature(featureName, featureOldValue, None, None).futureValue
      there.was(no(mockTelegramNotificator).sendNotification(anyString))
    }

    "not send telegram notification if new feature value is equals to old value with author and description" in {
      featureService.updateFeature(featureName, featureOldValue, Some(author), Some(description)).futureValue
      there.was(no(mockTelegramNotificator).sendNotification(anyString))
    }

    "not send telegram notification if no such feature with no author and description" in {
      featureService
        .updateFeature(nonExistFeature, featureOldValue, None, None)
        .shouldCompleteWithException[NoSuchElementException]
      there.was(no(mockTelegramNotificator).sendNotification(anyString))
    }

    "not send telegram notification if no such feature with author and description" in {
      featureService
        .updateFeature(nonExistFeature, featureOldValue, Some(author), Some(description))
        .shouldCompleteWithException[NoSuchElementException]
      there.was(no(mockTelegramNotificator).sendNotification(anyString))
    }

    "send telegram notification if new feature value and old one are different with no author and description" in {
      featureService.updateFeature(featureName, featureNewValue, None, None).futureValue
      there.was(
        one(mockTelegramNotificator)
          .sendNotification(
            s"[$testEnvironment]: $featureName@$testService = $featureOldValue -> $featureNewValue"
          )
      )
    }

    "send telegram notification if new feature value and old one are different with author and description" in {
      featureService.updateFeature(featureName, featureOldValue, Some(author), Some(description)).futureValue
      there.was(
        one(mockTelegramNotificator)
          .sendNotification(
            s"[$testEnvironment]: $featureName@$testService = $featureNewValue -> $featureOldValue\n" +
              s"Author: $author\n" +
              s"Description: $description"
          )
      )
    }
  }
}

private class TelegramNotificatorStub extends TelegramChatNotificator {

  override def sendNotification(msg: Message): Unit = ()
}
