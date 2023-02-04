package ru.auto.cabinet.tasks.impl.client.properties

import akka.stream.scaladsl.Source
import cats.data.NonEmptyList
import com.amazonaws.services.s3.model.PutObjectResult
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.cabinet.ClientPropertiesOuterClass.ClientPropertiesData
import ru.auto.cabinet.ClientPropertiesOuterClass.ClientPropertiesData.EnabledMatchApplications
import ru.auto.cabinet.TestActorSystem
import ru.auto.cabinet.dao.jdbc.JdbcClientDao
import ru.auto.cabinet.model.{ClientFilter, ClientId, ClientStatuses}
import ru.auto.cabinet.service.s3.S3ClientWrapper
import ru.auto.cabinet.service.salesman.HttpSalesmanClient.ClientCampaigns
import ru.auto.cabinet.service.salesman.SalesmanClient
import ru.auto.cabinet.tasks.impl.client.properties.S3ClientPropertiesUploaderTestData.{
  clients,
  createCampaignHeader
}
import ru.auto.cabinet.test.BaseSpec
import ru.auto.cabinet.trace.Context

import scala.jdk.CollectionConverters._

class ClientsMatchApplicationsToS3UploadTaskSpec
    extends BaseSpec
    with TestActorSystem {
  implicit private val rc = Context.unknown

  private val clientDao = mock[JdbcClientDao]
  private val writer = mock[S3ClientWrapper]
  private val salesmanClient = mock[SalesmanClient]

  private val task = new ClientsMatchApplicationsToS3UploadTask(
    clientDao,
    writer,
    salesmanClient
  )

  "ClientsMatchApplicationsToS3UploadTask" should {
    "upload properties for clients with only active campaigns to s3" in {
      val filter = ClientFilter(status = Some(ClientStatuses.Active))
      (clientDao
        .query(_: ClientFilter)(_: Context))
        .expects(filter, *)
        .returningF(clients())

      // Клиенты с активной РК и один клиент только с неактивной
      (salesmanClient
        .clientsMatchApplicationCampaigns(_: List[ClientId])(_: Context))
        .expects(clients().map(_.clientId), *)
        .returning(
          Source[ClientCampaigns](
            clients().tail.map { client =>
              ClientCampaigns(
                client.clientId,
                Some(
                  NonEmptyList.of(
                    createCampaignHeader(isActive = true),
                    createCampaignHeader(isActive = false)
                  ))
              )
            } :+
              ClientCampaigns(
                clients().head.clientId,
                Some(
                  NonEmptyList.of(
                    createCampaignHeader(isActive = false)
                  ))
              )
          )
        )

      val expectedResult = ClientPropertiesData
        .newBuilder()
        .putAllClientProperties(
          clients().tail
            .map { client =>
              val builder = ClientPropertiesData.ClientProperties
                .newBuilder()
                .setId(client.clientId)
                .addMatchApplications(
                  EnabledMatchApplications
                    .newBuilder()
                    .setClientOfferCategory(Category.CARS)
                    .setClientOfferSection(Section.NEW)
                )

              long2Long(client.clientId) -> builder.build()
            }
            .toMap
            .asJava
        )
        .build()

      (writer
        .write(_: String, _: Array[Byte], _: String)(_: Context))
        .expects(
          *,
          argThat { bytes: Array[Byte] =>
            ClientPropertiesData.parseFrom(bytes).equals(expectedResult)
          },
          *,
          *)
        .returningF(new PutObjectResult())

      task.execute.futureValue(Timeout(Span(1, Seconds)))
    }
  }

}
