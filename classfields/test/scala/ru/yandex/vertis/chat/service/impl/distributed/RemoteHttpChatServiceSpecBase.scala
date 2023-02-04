package ru.yandex.vertis.chat.service.impl.distributed

import java.util.concurrent.{CountDownLatch, TimeUnit}

import org.scalatest.prop.PropertyChecks._
import ru.yandex.vertis.chat.api.components.ChatApiComponents
import ru.yandex.vertis.chat.components.app.TestApplication
import ru.yandex.vertis.chat.components.app.config.LocalAppConfig
import ru.yandex.vertis.chat.components.dao.chat.locator.RemoteHttpChatService
import ru.yandex.vertis.chat.config.StaticLocatorConfig
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.service.ChatServiceSpecBase
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.util.http.{HttpClient, HttpClientConfig}
import ru.yandex.vertis.chat.util.logging.Logging
import ru.yandex.vertis.chat.{Domains, ServiceStarter}
import ru.yandex.vertis.generators.BasicGenerators._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Base Specs on [[RemoteHttpChatService]] running
  * along locally started HTTP API servers.
  *
  * @author dimas
  */
abstract class RemoteHttpChatServiceSpecBase extends ChatServiceSpecBase with Logging {

  /**
    * Specifies number of HTTP server instances
    */
  def numberOfInstances: Int

  private val staticLocatorConfig = new StaticLocatorConfig

  private val domain = Domains.Auto

  private val latch = new CountDownLatch(numberOfInstances)

  Iterator
    .continually {
      val config = new LocalAppConfig(staticLocatorConfig, domain)
      val app = new TestApplication(config)
      val components = new ChatApiComponents(app)
      app.start(Array())
      ServiceStarter
        .start(components)
        .onComplete { _ =>
          log.info(s"Server on port ${components.app.config.server.apiPort} started")
          latch.countDown()
        }
    }
    .take(numberOfInstances)
    .toList

  latch.await(1, TimeUnit.MINUTES)

  val hostPort = staticLocatorConfig.endpoints.head

  //noinspection TypeAnnotation
  val httpClient = HttpClient
    .createClientForTests(HttpClientConfig(hostPort.hostname, Some(hostPort)))

  val service = new RemoteHttpChatService(httpClient, domain, onlyInternalRequests = false)

  "RemoteHttpChatService" should {
    "successfully invalidate cache" in {
      forAll(userId, list(0, 10, cacheRecord).map(_.toSet)) { (user, records) =>
        service.invalidate(user, records) should be(())
      }
    }
  }
}
