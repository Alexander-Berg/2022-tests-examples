package ru.yandex.vertis.moderation.client.impl.ning

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.client.{ModerationClientFactory, SpecBase}
import ru.yandex.vertis.moderation.proto.Model.{Service, Visibility}
import ru.yandex.vertis.moderation.proto.{ModelFactory, RealtyLightFactory}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author potseluev
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class ManualNingRealtyUsersModerationClientSpec
  extends SpecBase {

  val moderationClientFactory: ModerationClientFactory =
    NingModerationClientFactory(new DefaultAsyncHttpClientConfig.Builder().build())

  val moderationClient = moderationClientFactory.client(
    "moderation-push-api-01-sas.test.vertis.yandex.net",
    37158,
    Service.USERS_REALTY
  )

  "Moderation client" should {
    "push correctly" in {
      val instanceSource =
        ModelFactory.newInstanceSourceBuilder()
          .setContext(ModelFactory.newContextSourceBuilder().setVisibility(Visibility.VISIBLE).build())
          .setExternalId(
            ModelFactory.newExternalIdBuilder()
              .setObjectId("yandex_uid_000")
              .setUser(ModelFactory.newUserBuilder().setYandexUser("000")))
          .setEssentials(ModelFactory.newEssentialsBuilder()
            .setUserRealty(RealtyLightFactory.newUserRealtyEssentialsBuilder().build()))
          .build()
      moderationClient.push(Seq(instanceSource), useBatch = false).futureValue
    }
  }

}
