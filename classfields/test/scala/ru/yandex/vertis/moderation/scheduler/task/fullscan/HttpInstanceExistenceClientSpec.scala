package ru.yandex.vertis.moderation.scheduler.task.fullscan

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.instanceGen
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.instance.ExternalId
import ru.yandex.vertis.moderation.model.instance.User.Partner

import scala.concurrent.ExecutionContext

/**
  * Specs on [[HttpInstanceExistenceClient]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpInstanceExistenceClientSpec extends SpecBase {

  lazy val client: InstanceExistenceClient =
    HttpInstanceExistenceClient(
      "http://realty3-indexer-01-myt.test.vertis.yandex.net:36600/rawOffer",
      new DefaultAsyncHttpClientConfig.Builder().build
    )(ExecutionContext.Implicits.global)

  lazy val brokenClient: InstanceExistenceClient =
    HttpInstanceExistenceClient(
      "http://realty3-indexer-01-myt.test.vertis.yandex.net:20/rawOffer",
      new DefaultAsyncHttpClientConfig.Builder().build
    )(ExecutionContext.Implicits.global)

  "MarkAsDeleted.HttpClient" should {
    "return instance non-exist" in {
      val instance1 = instanceGen(ExternalId(Partner("1"), "1")).next
      client.exist(instance1).futureValue should be(false)

      val instance2 = instanceGen(ExternalId(Partner("1"), "-1ffff")).next
      client.exist(instance2).futureValue should be(false)

      val instance3 = instanceGen(ExternalId(Partner("1"), "")).next
      client.exist(instance3).futureValue should be(false)
    }
    "return instance exist" in {
      val instance = instanceGen(ExternalId(Partner("1"), "2838997269184142129")).next
      client.exist(instance).futureValue should be(true)
    }
    "throw error if use broken client" in {

      val instance = instanceGen(ExternalId(Partner("1"), "2838997269184142129")).next
      intercept[Exception] {
        brokenClient.exist(instance).futureValue
      }
    }
  }
}
