package ru.auto.api.services

import java.io.Closeable

import com.google.common.util.concurrent.MoreExecutors
import org.apache.http.HttpHost
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import ru.auto.api.http.{ApacheHttpClient, DisableSSL, HttpClient, HttpClientConfig}
import ru.auto.api.managers.personalization.PersonalizationManager.BigBrotherProfile
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.ExecutionContext

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 10.02.17
  */
trait HttpClientSuite
  extends AnyFunSuite
  with Matchers
  with BeforeAndAfterAll
  with ScalaFutures
  with IntegrationPatience
  with CachingHttpClient {

  implicit protected val ec: ExecutionContext = HttpClientSuite.ec

  protected def config: HttpClientConfig

  protected val http: HttpClient with Closeable = HttpClientSuite.createClientForTests(config, cachingProxy)

  implicit val trace: Traced = Traced.empty
  implicit val bigBSearcher: BigBrotherProfile = BigBrotherProfile.Empty

  override protected def afterAll(): Unit = {
    super.afterAll()
    http.close()
  }

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true
}

object HttpClientSuite {

  private val ec: ExecutionContext =
    ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  def createClientForTests(config: HttpClientConfig, proxy: Option[HttpHost] = None): HttpClient with Closeable = {

    val client =
      HttpAsyncClients
        .custom()
        .setSSLContext(DisableSSL.context)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build()

    client.start()

    val res = config.copy(hostPort = config.hostPort.copy(proxyHost = proxy))
    new ApacheHttpClient(client, res, ec) with IntTestHttpClient
  }
}
