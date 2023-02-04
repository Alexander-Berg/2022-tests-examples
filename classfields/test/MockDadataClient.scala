package ru.yandex.vertis.shark.client.bank

import baker.common.client.dadata.DadataClient
import baker.common.client.dadata.DadataClient.DadataClient
import baker.common.client.dadata.model.Responses.SuccessResponse
import baker.common.client.dadata.model._
import cats.implicits._
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.zio_baker.model.{FiasId, Inn}
import zio.{Task, ULayer, ZLayer}

class MockDadataClient extends DadataClient.Service with StaticSamples {

  override def bankSuggestions(request: OrganizationRequest): Task[SuccessResponse[BankData]] =
    Task.fail(new IllegalStateException("Unusable method call."))

  override def organizationSuggestions(request: OrganizationRequest): Task[SuccessResponse[Organization]] =
    Task.succeed(sampleDadataOrganization)

  override def organizationByInn(inn: Inn): Task[Option[DadataOrganization]] = {
    val request = OrganizationRequest(
      query = inn.toString.some,
      count = None,
      kpp = None,
      branchType = None,
      `type` = None
    )
    organizationSuggestions(request).map(_.suggestions.headOption)
  }

  override def individualByInn(inn: Inn): Task[Option[DadataOrganization]] = organizationByInn(inn)

  override def addressSuggestionsByFiasId(fiasId: FiasId): Task[Responses.SuccessResponse[Address]] = {
    val request = OrganizationRequest(
      query = fiasId.some,
      count = None,
      kpp = None,
      branchType = None,
      `type` = None
    )
    for {
      organizations <- organizationSuggestions(request)
      addresses <- Task.succeed(organizations.suggestions.mapFilter(_.data).mapFilter(_.address))
    } yield Responses.SuccessResponse(addresses)
  }

  override def addressSuggestionsByString(address: String): Task[SuccessResponse[Address]] =
    for {
      organizations <- Task.succeed(sampleDadataOrganization)
      addresses <- Task.succeed(organizations.suggestions.mapFilter(_.data).mapFilter(_.address))
    } yield Responses.SuccessResponse(addresses)

  override def fioSuggestions(name: String): Task[SuccessResponse[Fio]] = Task.succeed {
    SuccessResponse(
      Seq(
        Suggestion(
          "",
          "",
          Fio(
            name = None,
            surname = None,
            patronymic = None,
            gender = Fio.Gender.Male.some,
            source = None,
            qc = None
          ).some
        )
      )
    )
  }
}

object MockDadataClient {

  val live: ULayer[DadataClient] = ZLayer.succeed(new MockDadataClient)
}
