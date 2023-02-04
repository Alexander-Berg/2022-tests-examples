package ru.yandex.vertis.feedprocessor.autoru.scheduler.util

import java.io.Closeable
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vertis.feedprocessor.http.{ApacheHttpClient, DisableSSL, HttpClient, HttpClientConfig}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author pnaydenov
  */
trait HttpClientSuite extends Matchers with BeforeAndAfterAll with ScalaFutures with IntegrationPatience {
  this: Suite =>

  protected def config: HttpClientConfig

  private val httpClient =
    HttpAsyncClients
      .custom()
      .setSSLContext(DisableSSL.context)
      .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .build()
  httpClient.start()

  protected val http: HttpClient with Closeable = new ApacheHttpClient(httpClient, config)

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true

  override protected def afterAll(): Unit = {
    super.afterAll()
    http.close()
  }
}
