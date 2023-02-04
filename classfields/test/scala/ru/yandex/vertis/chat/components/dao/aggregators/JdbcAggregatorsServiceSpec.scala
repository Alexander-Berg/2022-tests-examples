package ru.yandex.vertis.chat.components.dao.aggregators

import org.mockito.Mockito.reset
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.yandex.vertis.chat._
import ru.yandex.vertis.chat.common.aggregators.AggregatorsSupport
import ru.yandex.vertis.chat.components.clients.aggregators.{AggregatorClient, AggregatorsClientAware}
import ru.yandex.vertis.chat.components.dao.chat.storage.DatabaseStorage
import ru.yandex.vertis.chat.components.domains.DomainAware
import ru.yandex.vertis.chat.components.ComponentsSpecBase
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.model.ModelGenerators.dealerId
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.chat.service.exceptions.AggregatorException
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.language.postfixOps

class JdbcAggregatorsServiceSpec
  extends ComponentsSpecBase
  with ProducerProvider
  with MockitoSupport
  with BeforeAndAfter {

  implicit private val rc: RequestContext = RobotRequestContext.random("user", "name", CacheControl.Disallow)

  private val aggregatorClientMock = mock[AggregatorClient]
  private val databaseDefMock = mock[JdbcBackend.DatabaseDef]

  private val user: UserId = dealerId.next
  private val channelName: String = Gen.alphaNumStr.next

  val aggregatorsService: AggregatorsService =
    new JdbcAggregatorsService(DatabaseStorage(databaseDefMock, databaseDefMock))
      with DomainAware
      with AggregatorsClientAware {
      implicit override def domain: Domain = Domains.Auto

      override def aggregatorClient: DMap[AggregatorClient] =
        DMap.forAllDomains[AggregatorClient](aggregatorClientMock)
    }

  before {
    reset(aggregatorClientMock)
  }

  "AggregatorsService" when {
    "install external aggregator" when {
      val request = ApiModel.AggregatorInstallRequest
        .newBuilder()
        .addUsers(user)
        .setChannelName(channelName)
        .build()

      "JivoSite" when {
        "correct token" should {
          "return token" in {
            val authToken = Gen.listOfN(10, Gen.alphaChar).next.mkString

            val result = ApiModel.AggregatorInstallResult
              .newBuilder()
              .setAuthToken(authToken)

            when(aggregatorClientMock.install(eq(request), eq(AggregatorType.JivoSite))(?))
              .thenReturn(Future.successful(AggregatorsSupport.Response(authToken, "hook")))

            when(databaseDefMock.run[Int](?)).thenReturn(Future.successful[Int](1))

            aggregatorsService.install(AggregatorType.JivoSite, request).futureValue === result
          }
        }
        "incorrect token or error" should {
          "return error" in {
            val clientResult = Gen.listOfN(12, Gen.alphaChar).next.mkString

            when(aggregatorClientMock.install(eq(request), ?)(?))
              .thenReturn(
                Future.failed[AggregatorsSupport.Response](AggregatorException.installationFailed(clientResult))
              )

            aggregatorsService
              .install(AggregatorType.JivoSite, request)
              .failed
              .futureValue
              .isInstanceOf[AggregatorException]
          }
        }
      }

      "Bachata" when {
        "correct token" should {
          "return token" in {
            val authToken = Gen.listOfN(32, Gen.alphaChar).next.mkString

            val result = ApiModel.AggregatorInstallResult
              .newBuilder()
              .setAuthToken(authToken)

            when(aggregatorClientMock.install(eq(request), eq(AggregatorType.JivoSite))(?))
              .thenReturn(Future.successful(AggregatorsSupport.Response(authToken, "hook")))

            when(databaseDefMock.run[Int](?)).thenReturn(Future.successful[Int](1))

            aggregatorsService.install(AggregatorType.JivoSite, request).futureValue === result
          }
        }
        "incorrect token or error" should {
          "return error" in {
            val clientResult: String = Gen.listOfN(34, Gen.alphaNumChar).next.mkString

            when(aggregatorClientMock.install(eq(request), ?)(?))
              .thenReturn(
                Future.failed[AggregatorsSupport.Response](AggregatorException.installationFailed(clientResult))
              )

            aggregatorsService
              .install(AggregatorType.JivoSite, request)
              .failed
              .futureValue
              .isInstanceOf[AggregatorException]
          }
        }
      }
    }
  }
}
