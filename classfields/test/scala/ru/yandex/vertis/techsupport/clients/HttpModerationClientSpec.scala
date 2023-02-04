package ru.yandex.vertis.vsquality.techsupport.clients

import java.time.Instant
import com.softwaremill.tagging._
import org.scalatest.Ignore
import ru.yandex.vertis.moderation.proto.model.Reason.SERVICE_SUGGEST
import ru.yandex.vertis.moderation.proto.model.{HoboCheckType, Service}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpModerationClient
import ru.yandex.vertis.vsquality.techsupport.dao.photo.PhotosDao.{PhotoGroupKey, PhotoRecord}
import ru.yandex.vertis.vsquality.techsupport.model.Context.Visibility._
import ru.yandex.vertis.vsquality.techsupport.model.Opinion.Type
import ru.yandex.vertis.vsquality.techsupport.model.Opinion.Type._
import ru.yandex.vertis.vsquality.techsupport.model.{
  Context,
  Image,
  Instance,
  Opinion,
  QuotaMetadata,
  ScenarioId,
  Tags,
  UserId
}
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.scenario.PhotosUploadScenarioImpl.TaskView
import ru.yandex.vertis.vsquality.techsupport.util.{ModerationUtils, SpecBase}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.SttpBackend

@Ignore
class HttpModerationClientSpec extends SpecBase {
  implicit private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await
  private val client = new HttpModerationClient("http://moderation-api.vrts-slb.test.vertis.yandex.net:80/api/1.x")

  "HttpModerationClient" should {
    "appendSignal" in {

      val userId = UserId.Client.Autoru.PrivatePerson(63695962L.taggedWith[Tags.AutoruPrivatePersonId])
      val url = "http://avatars-int.mds.yandex.net/get-autoru-orig/3905782/7364b6f7d4669d893b78db7615f238dc/orig"
        .taggedWith[Tags.Url]
      val appealCreateTime = Instant.now()
      val groupKey =
        PhotoGroupKey(
          ScenarioId.Internal.ProvenOwnerUploadPhotos,
          userId,
          appealCreateTime
        )
      val photos =
        Seq(
          PhotoRecord(groupKey, 1, Image(url), Instant.now()),
          PhotoRecord(groupKey, 2, Image(url), Instant.now()),
          PhotoRecord(groupKey, 3, Image(url), Instant.now())
        )

      val taskViewPhotos =
        photos.map { record =>
          TaskView.Photo(
            record.photo.url,
            record.photoUploadTime.toEpochMilli,
            record.photo.isFromCamera
          )
        }

      client
        .appendSignal(
          Service.USERS_AUTORU,
          ModerationUtils.clientToString(userId),
          ModerationUtils.clientToString(userId),
          Seq(
            ModerationUtils.buildAppendHoboSignalRequest(
              ModerationUtils.UsersAutoruDefaultDomain,
              HoboCheckType.PROVEN_OWNER,
              TaskView(taskViewPhotos, ModerationUtils.clientToString(userId)),
              ModerationUtils.photosQualifier(photos)
            )
          )
        )
    }

    "getUserInstance auto" in {
      val userId = UserId.Client.Autoru.PrivatePerson(63695962L.taggedWith[Tags.AutoruPrivatePersonId])
      val expected =
        Some(Instance(Opinion(Opinion.Type.UNKNOWN, List()), Context(VISIBLE), QuotaMetadata(Some(false)), None))

      client.getUserInstance(userId, Service.USERS_AUTORU).await shouldBe expected

    }
    "get user instance realty" in {
      val userId = UserId.Client.Realty(4042245555L.taggedWith[Tags.RealtyUserId])
      val expected = Some(Instance(Opinion(Type.UNKNOWN, List()), Context(VISIBLE), QuotaMetadata(Some(true)), None))

      client.getUserInstance(userId, Service.USERS_REALTY).await shouldBe expected

    }

    "get blocked offer instances" in {
      val userId = UserId.Client.Realty(4002087869L.taggedWith[Tags.RealtyUserId])
      val expected = Seq(
        Instance(
          Opinion(FAILED, List(SERVICE_SUGGEST)),
          Context(INVALID),
          QuotaMetadata(None),
          None
        ),
        Instance(
          Opinion(FAILED, List(SERVICE_SUGGEST)),
          Context(INVALID),
          QuotaMetadata(None),
          None
        )
      )

      client.getBlockedOfferInstances(userId, Service.REALTY).await shouldBe expected

    }

  }
}
