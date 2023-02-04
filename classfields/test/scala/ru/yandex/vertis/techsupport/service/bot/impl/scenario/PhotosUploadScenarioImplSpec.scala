package ru.yandex.vertis.vsquality.techsupport.service.bot.impl.scenario

import java.time.Instant
import cats.Monad
import com.softwaremill.tagging._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.{FeatureType, FeatureTypes}
import ru.yandex.vertis.moderation.proto.model.HoboCheckType
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.feature_registry_utils.FeatureRegistryF
import ru.yandex.vertis.vsquality.techsupport.clients.ModerationClient
import ru.yandex.vertis.vsquality.techsupport.config.PhotosUploadScenarioConfig
import ru.yandex.vertis.vsquality.techsupport.dao.photo.InMemoryPhotosDao
import ru.yandex.vertis.vsquality.techsupport.dao.photo.PhotosDao.PhotoGroupKey
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.service.AvatarsService
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action.{CompleteConversation, Reply}
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.State
import ru.yandex.vertis.vsquality.techsupport.service.bot.CommonContext
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.scenario.PhotosUploadScenarioImpl.{
  JsonPhotoUploadScenarioTextFeatureType,
  PhotoUploadScenarioText
}
import ru.yandex.vertis.vsquality.techsupport.service.impl.PhotoServiceImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

class PhotosUploadScenarioImplSpec extends SpecBase {

  private val statePrefix = "upload"

  private val dao = new InMemoryPhotosDao[F]
  private val client = mock[ModerationClient[F]]
  private val avatarsService = mock[AvatarsService[F]]
  private val photoService = new PhotoServiceImpl[F](avatarsService, client, dao)

  implicit val featureRegistry: FeatureRegistryF[F] =
    new FeatureRegistryF[F](new InMemoryFeatureRegistry(new FeatureTypes {

      override def featureTypes: Iterable[FeatureType[_]] =
        Iterable(JsonPhotoUploadScenarioTextFeatureType) ++ BasicFeatureTypes.featureTypes
    }))

  when(client.appendSignal(any(), any(), any(), any())).thenReturn(Monad[F].unit)

  when(avatarsService.uploadWithSecureWatermark(any(), any(), any(), any(), any())).thenAnswer(x =>
    Monad[F].pure(x.getArgument(0))
  )

  private val scenarioId: ScenarioId.Internal =
    ScenarioId.Internal.ProvenOwnerUploadPhotos

  private val externalScenarioIdToReset = "".taggedWith[Tags.ExternalScenarioId]

  private val config =
    PhotosUploadScenarioConfig(
      nodePrefix = statePrefix,
      photosAmount = 3,
      checkType = HoboCheckType.PROVEN_OWNER
    )

  private val scenario =
    PhotosUploadScenarioImpl[F](
      scenarioId,
      externalScenarioIdToReset,
      config,
      photoService,
      dao
    ).await

  private val posText = Map(1 -> "text1", 2 -> "text2", 3 -> "text3", 4 -> "text4")
  private val terminal = "terminal"

  featureRegistry
    .updateFeature(
      s"${scenarioId.entryName}-text",
      PhotoUploadScenarioText(
        posText,
        terminal
      )
    )
    .await

  val userId: UserId.Client =
    UserId.Client.Autoru.PrivatePerson(123L.taggedWith[Tags.AutoruPrivatePersonId])

  implicit val ctx: CommonContext = new CommonContext {
    override val clientId: UserId.Client = userId

    override val appealCreateTime: Instant = Instant.now()

    override val messageProcessingTime: Instant = Instant.now()
  }

  def url(s: String): Url = s.taggedWith[Tags.Url]

  def imagePayload(imageNum: Int): Message.Payload =
    Message.Payload("", Seq(Image(url(s"https://pic-$imageNum.jpeg"))), Seq.empty, None)

  private val statePhoto0 = s"${statePrefix}_photo-0".taggedWith[Tags.BotStateId]
  private val statePhoto1 = s"${statePrefix}_photo-1".taggedWith[Tags.BotStateId]
  private val statePhoto2 = s"${statePrefix}_photo-2".taggedWith[Tags.BotStateId]
  private val statePhoto3 = s"${statePrefix}_photo-3".taggedWith[Tags.BotStateId]
  private val statePhoto4 = s"${statePrefix}_photo-4".taggedWith[Tags.BotStateId]
  private val terminalStateId = "upload_final_state".taggedWith[Tags.BotStateId]

  private def checkDao(expected: Seq[Image]) =
    dao.getPhotos(PhotoGroupKey(scenarioId, userId, ctx.appealCreateTime)).await.map(_.photo) shouldBe expected

  private val payload1 = imagePayload(1)
  private val payload2 = imagePayload(2)
  private val payload3 = imagePayload(3)

  private def state(nextState: BotStateId, pos: Int) = {
    State(nextState, Reply(posText(pos), Seq.empty), Seq("Назад".taggedWith[Tags.BotCommand]))
  }

  private val terminalState =
    State(terminalStateId, CompleteConversation(terminal, needFeedback = false), Seq.empty)

  "PhotosUploadScenarioImpl" should {
    "append hobo signal if all photos were uploaded" in {
      scenario.transit(statePhoto1, payload1).await shouldBe Some(state(statePhoto2, 2))
      checkDao(payload1.images)

      scenario.transit(statePhoto2, payload2).await shouldBe Some(state(statePhoto3, 3))
      checkDao(payload1.images ++ payload2.images)

      scenario.transit(statePhoto3, payload3).await shouldBe Some(terminalState)
      checkDao(Seq.empty)
    }

    "not accept 0-state" in {
      scenario.transit(statePhoto0, payload1).await shouldBe None
    }

    "not accept state that is more than photos amount" in {
      scenario.transit(statePhoto4, payload1).await shouldBe None
    }
  }
}
