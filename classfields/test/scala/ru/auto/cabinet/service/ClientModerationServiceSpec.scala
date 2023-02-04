package ru.auto.cabinet.service

import cats.data.NonEmptyList
import com.google.protobuf.{BoolValue, StringValue}
import org.scalacheck.Gen
import ru.auto.cabinet.ApiModel.Moderation
import ru.auto.cabinet.dao.entities.DealerSiteCheckFilter
import ru.auto.cabinet.dao.jdbc.{
  DealerSiteCheckDao,
  JdbcClientDao,
  ModerationDao,
  NotFoundError
}
import ru.auto.cabinet.environment._
import ru.auto.cabinet.model.{DealerSiteCheckRecord, DetailedClient}
import ru.auto.cabinet.test.BaseSpec
import ru.auto.cabinet.test.gens.ClientGens.clientGen
import ru.auto.cabinet.trace.Context

import java.time.{OffsetDateTime, ZoneOffset}

class ClientModerationServiceSpec extends BaseSpec {
  implicit private val rc = Context.unknown
  private val clientDao = mock[JdbcClientDao]
  private val moderationDao = mock[ModerationDao]
  private val dealerSiteCheckDao = mock[DealerSiteCheckDao]

  private val service =
    new ClientModerationService(clientDao, moderationDao, dealerSiteCheckDao)

  "ClientModerationService" should {
    "fail if no client with id" in {
      (clientDao
        .getDetailed(_: Long)(_: Context))
        .expects(1L, *)
        .throwingF(NotFoundError("no client"))

      service.getModeration(1L).failed.futureValue shouldBe a[NotFoundError]
    }

    "return moderation on any client id" in {

      val client = clientGen().sample.get
      val comment = Gen.option(Gen.alphaStr).sample.get
      val banReasons = Gen.listOf(Gen.alphaStr).sample.get
      val onModeration = Gen.oneOf(true, false).sample.get
      val siteCheckResolution = Gen.oneOf(true, false).sample.get

      (clientDao
        .getDetailed(_: Long)(_: Context))
        .expects(client.clientId, *)
        .returningF(
          DetailedClient(
            id = client.clientId,
            clientProperties = client.properties,
            isAgent = false,
            name = None,
            agencyId = None,
            agencyName = None,
            companyId = None,
            companyName = None))

      (clientDao
        .getClientComment(_: Long)(_: Context))
        .expects(client.clientId, *)
        .returningF(comment)

      (moderationDao
        .banReasons(_: Long)(_: Context))
        .expects(client.clientId, *)
        .returningF(banReasons)

      (moderationDao
        .onModeration(_: Long)(_: Context))
        .expects(client.clientId, *)
        .returningF(onModeration)

      val checkDate =
        OffsetDateTime.of(2020, 2, 10, 18, 50, 0, 0, ZoneOffset.UTC)
      (dealerSiteCheckDao
        .list(_: DealerSiteCheckFilter)(_: Context))
        .expects(
          DealerSiteCheckFilter(
            clientId = Some(NonEmptyList.of(client.clientId))),
          *)
        .returningF(
          List(
            DealerSiteCheckRecord(
              clientId = client.clientId,
              checkDate = checkDate,
              resolution = siteCheckResolution)))

      val b = Moderation
        .newBuilder()
        .setFirstModeration(BoolValue.of(client.properties.firstModerated))
        .setBanReasons(StringValue.of(banReasons.mkString("; ")))
        .setOnModeration(BoolValue.of(onModeration))
        .setSitecheck(BoolValue.of(siteCheckResolution))
        .setSitecheckDate(checkDate.asProtoTimestamp())

      comment.foreach(c => b.setModerationComment(StringValue.of(c)))

      val res = b.build()

      service.getModeration(client.clientId).futureValue shouldBe res
    }
  }
}
