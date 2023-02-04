package ru.yandex.vos2.autoru.api.v1.moderation

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{`Content-Type`, Accept, RawHeader}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.auto.api.CommonModel.GeoPoint
import ru.auto.api.ModerationFieldsModel.ModerationFields.Fields
import ru.auto.api.ModerationUpdateModel.ModerationUpdate.{BadgesUpdate, BlockedPhotoHashUpdate}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Editor
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.{Photo, PhotoMetadata}
import ru.yandex.vos2.autoru.model.AutoruOfferID
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.autoru.utils.testforms.{ModerationUpdateTestUtils, TestFormParams}
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.proto.ProtoMacro.opt
import ru.yandex.vos2.services.mds.AutoruAllNamespaceSettings
import ru.yandex.vos2.util.{Protobuf, RandomUtil}

import scala.jdk.CollectionConverters._
import ru.auto.api.ModerationUpdateModel.ModerationUpdate
import ru.auto.api.ResponseModel
import ru.auto.api.ResponseModel.ModerationBlockedPhotoHashesResponse
import ru.auto.api.vos.BlockedPhotoHashModel.BlockedPhotoHash
import ru.yandex.vos2.autoru.dao.blockedphotohashes.BlockedPhotoHashesDao

@RunWith(classOf[JUnitRunner])
class ModerationHandlerTest
  extends AnyFunSuite
  with Vos2ApiSuite
  with BeforeAndAfterAll
  with ScalaCheckDrivenPropertyChecks {
  private val now = new DateTime()
  implicit val trace = Traced.empty

  override protected def beforeAll(): Unit = {
    components.skypper.transaction("truncate-blocked-photo-hashes") { executor =>
      executor.update("truncate-blocked-photo-hashes")("delete from blocked_photo_hashes;")
    }
    Get("/api/v1/offer/cars/blabla") ~> route ~> check {}
  }

  for {
    isDealer <- Seq(false, true)
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"moderation update (isDealer = $isDealer, category = $category)") {
      // создаем объявление, сохраняем
      val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
      val extUserId: String = formInfo.form.getUserRef
      val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))

      val offerId = checkSuccessRequest(req)
      // генерируем обновленную форму
      val formInfo2 = testFormGenerator.updateForm(formInfo, TestFormParams(now = now))

      // создаем обновление
      val update = ModerationUpdateTestUtils.generateModerationUpdate(formInfo.form, formInfo2.form).build()
      // сохраняем
      val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
      val offerId2 = checkSuccessRequest(req2)

      // читаем из базы, проверяем что update применился
      val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId2"))

      assert(ModerationUpdateTestUtils.checkModerationUpdateCorrect(readForm, update))

      // читаем из базы, проверяем что update применился
      val readFormProto = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId2/proto"))

      assert(readFormProto.getOfferAutoru.getEditor.getName == update.getEditor.getName)
    }
  }

  test("add or update notification on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)

    // генерируем обновленную форму
    val formBuilder = formInfo.form.toBuilder
    val newCoordinates = GeoPoint.newBuilder().setLatitude(55.734807).setLongitude(37.642765).build()
    formBuilder.getStateBuilder.setMileage((formInfo.form.getState.getMileage + 5000) % 1000000)
    formBuilder.getSellerBuilder.getLocationBuilder.setCoord(newCoordinates)
    val update = ModerationUpdateTestUtils.generateModerationUpdate(formInfo.form, formBuilder.build()).build()
    checkSuccessRequest(
      Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    )

    // читаем из базы, проверяем нотификации
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))

    // создается одна нотификация moderation update
    val extraArgs = readOffer.getOfferAutoru.getNotifications(0).getExtraArgsList.asScala
    assert(readOffer.getOfferAutoru.getNotificationsCount == 1)
    assert(extraArgs.size == 3)
    assert(extraArgs.contains("mileage"))
    assert(extraArgs.contains("geo"))

    // генерируем обновленную форму
    formBuilder.setDescription("description")
    val update2 = ModerationUpdateTestUtils.generateModerationUpdate(readForm, formBuilder.build()).build()
    checkSuccessRequest(
      Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update2)))
    )

    // первая нотификация должна быть отменена, и вместо нее создана вторая - со всеми полями
    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    val readOffer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    val extraArgs2 = readOffer2.getOfferAutoru.getNotifications(1).getExtraArgsList.asScala
    assert(readOffer2.getOfferAutoru.getNotificationsCount == 2)
    assert(readOffer2.getOfferAutoru.getNotifications(0).hasTimestampCancel)
    assert(extraArgs2.size == 4)
    assert(extraArgs2.contains("description"))
    assert(extraArgs2.contains("mileage"))
    assert(extraArgs2.contains("geo"))

    // вторая нотификация как бы послана
    components
      .getOfferDao()
      .useOfferID(AutoruOfferID.parse(offerId)) { offer =>
        val b = offer.toBuilder
        b.getOfferAutoruBuilder.getNotificationsBuilder(1).setTimestampSent(System.currentTimeMillis())
        OfferUpdate.visitNow(b.build())
      }(Traced.empty)

    formBuilder.getPriceInfoBuilder.setPrice(100000)
    val update3 = ModerationUpdateTestUtils.generateModerationUpdate(readForm2, formBuilder.build()).build()
    checkSuccessRequest(
      Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update3)))
    )

    // должна быть создана третья новая нотификация
    val readOffer3 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readOffer3.getOfferAutoru.getNotificationsCount == 3)
    assert(readOffer3.getOfferAutoru.getNotifications(2).getExtraArgsCount == 2)
    assert(readOffer3.getOfferAutoru.getNotifications(2).getExtraArgs(0) == "price_info")

    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
  }

  test("clear video on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    val origYoutubeVideoId = "test_youtube_id_0"

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder
    formInfoBuilder.getStateBuilder.getVideoBuilder.setYoutubeId(origYoutubeVideoId)

    val extUserId: String = formInfo.form.getUserRef
    var req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что YoutubeVideoId верный
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getState.getVideo.getYoutubeId == origYoutubeVideoId)

    // генерируем обновленную форму
    val formBuilder = formInfo.form.toBuilder
    // удаляем видео из формы
    formBuilder.getStateBuilder.clearVideo()
    val update = ModerationUpdateTestUtils.generateModerationUpdate(formInfo.form, formBuilder.build()).build()

    // сохраняем
    req = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req)

    // читаем из базы, проверяем что update применился
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(!readForm.getState.hasVideo)
  }

  test("change video on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    val origYoutubeVideoId = "test_youtube_id_0"
    val updatedYoutubeVideoId = "test_youtube_id_1"

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder
    formInfoBuilder.getStateBuilder.getVideoBuilder.setYoutubeId(origYoutubeVideoId)

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что YoutubeVideoId верный
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getState.getVideo.getYoutubeId == origYoutubeVideoId)

    // генерируем обновленную форму
    val formInfo2 = testFormGenerator.updateForm(formInfo, TestFormParams(now = now))
    val formInfoBuilder2 = formInfo.form.toBuilder
    formInfoBuilder2.getStateBuilder.getVideoBuilder.setYoutubeId(updatedYoutubeVideoId)
    val update = ModerationUpdateTestUtils.generateModerationUpdate(formInfo.form, formInfoBuilder2.build()).build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что update применился
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getState.hasVideo)
    assert(readForm.getState.getVideo.getYoutubeId != origYoutubeVideoId)
    assert(readForm.getState.getVideo.getYoutubeId == updatedYoutubeVideoId)
  }

  test("skip video on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    val origYoutubeVideoId = "test_youtube_id_0"

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder
    formInfoBuilder.getStateBuilder.getVideoBuilder.setYoutubeId(origYoutubeVideoId)

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что YoutubeVideoId верный
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getState.getVideo.getYoutubeId == origYoutubeVideoId)

    // генерируем обновленную форму с пустым полем VideoUpdate
    val formBuilder = formInfo.form.toBuilder
    val update =
      ModerationUpdateTestUtils.generateModerationUpdate(formInfo.form, formBuilder.build()).clearVideoUpdate().build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что update применился
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getState.hasVideo)
    assert(readForm.getState.getVideo.getYoutubeId == origYoutubeVideoId)
  }

  test("add badges on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)

    val category = "cars"
    val isDealer = false
    val addedBadges = BadgesUpdate
      .newBuilder()
      .addAllBadges(List("Badge1", "Badge2").asJava)
      .build();

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что badges отсутствуют
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getBadgesList.isEmpty)

    // генерируем обновленную форму с обновленным полем BadgesUpdate
    val formBuilder = formInfo.form.toBuilder
    val update =
      ModerationUpdateTestUtils
        .generateModerationUpdate(formInfo.form, formBuilder.build())
        .setBadgesUpdate(addedBadges)
        .build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что badges добавились
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(!readForm.getBadgesList.isEmpty)
    assert(readForm.getBadgesList == addedBadges.getBadgesList)
  }

  test("update badges on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    val origBadges = List("Badge1", "Badge2").asJava
    val updatedBadges = BadgesUpdate
      .newBuilder()
      .addAllBadges(List("Updated_badge1", "Updated_badge2").asJava)
      .build();

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder
    formInfoBuilder.addAllBadges(origBadges)

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что badges верные
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getBadgesList.containsAll(origBadges))

    // генерируем обновленную форму с обновленным полем BadgesUpdate
    val formBuilder = formInfo.form.toBuilder
    val update =
      ModerationUpdateTestUtils
        .generateModerationUpdate(formInfo.form, formBuilder.build())
        .setBadgesUpdate(updatedBadges)
        .build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что badges обновились
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(!readForm.getBadgesList.isEmpty)
    assert(readForm.getBadgesList == updatedBadges.getBadgesList)
  }

  test("remove badges on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    val origBadges = List("Badge1", "Badge2").asJava
    val updatedBadges = BadgesUpdate.newBuilder().build()

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder
    formInfoBuilder.addAllBadges(origBadges)

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что badges верные
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getBadgesList.containsAll(origBadges))

    // генерируем обновленную форму с пустым полем BadgesUpdate
    val formBuilder = formInfo.form.toBuilder
    val update =
      ModerationUpdateTestUtils
        .generateModerationUpdate(formInfo.form, formBuilder.build())
        .setBadgesUpdate(updatedBadges)
        .build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что badges удалены
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getBadgesList.isEmpty)
  }

  test("add moderation protection on fields") {
    val category = "cars"
    val isDealer = false

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))

    val offerId = checkSuccessRequest(req)

    val modProtectionReq = Put(s"/api/v1/moderation/protection/$category/$extUserId/$offerId?fields=MILEAGE")
      .withHeaders(`Content-Type`(ContentTypes.`application/json`))
    checkSimpleSuccessRequest(modProtectionReq)

    // читаем из базы, проверяем что установлена защита
    val readForm = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readForm.getOfferAutoru.getModerationProtectedFieldsCount == 1)
    assert(readForm.getOfferAutoru.getModerationProtectedFields(0) == Fields.MILEAGE)
  }

  test("add moderation protection on fields and set editor") {
    val category = "cars"
    val isDealer = false

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))

    val offerId = checkSuccessRequest(req)

    val editorName = RandomUtil.nextHexString(5)
    val modProtectionReq = Put(s"/api/v1/moderation/protection/$category/$extUserId/$offerId?fields=MILEAGE")
      .withEntity(
        HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(Editor.newBuilder().setName(editorName).build()))
      )
    checkSimpleSuccessRequest(modProtectionReq)

    // читаем из базы, проверяем что установлена защита и сохранен редактор
    val readForm = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readForm.getOfferAutoru.getModerationProtectedFieldsCount == 1)
    assert(readForm.getOfferAutoru.getModerationProtectedFields(0) == Fields.MILEAGE)
    assert(readForm.getOfferAutoru.getEditor.getName == editorName)
  }

  test("remove moderation protection from fields") {
    val category = "cars"
    val isDealer = false

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))

    val offerId = checkSuccessRequest(req)

    // ставим защиту на пробег
    val modProtectionReq = Put(s"/api/v1/moderation/protection/$category/$extUserId/$offerId?fields=MILEAGE")
      .withHeaders(`Content-Type`(ContentTypes.`application/json`))
    checkSimpleSuccessRequest(modProtectionReq)

    // читаем из базы, проверяем что установлена защита
    val readForm = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readForm.getOfferAutoru.getModerationProtectedFieldsCount == 1)
    assert(readForm.getOfferAutoru.getModerationProtectedFields(0) == Fields.MILEAGE)

    // удаляем защиту с пробега
    val modProtectionRemoveReq = Delete(s"/api/v1/moderation/protection/$category/$extUserId/$offerId?fields=MILEAGE")
      .withHeaders(`Content-Type`(ContentTypes.`application/json`))
    checkSimpleSuccessRequest(modProtectionRemoveReq)

    // читаем из базы, проверяем что защита удалена
    val readForm2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readForm2.getOfferAutoru.getModerationProtectedFieldsCount == 0)
  }

  test("remove moderation protection from fields with editor save") {
    val category = "cars"
    val isDealer = false

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))

    val offerId = checkSuccessRequest(req)

    // ставим защиту на пробег
    val modProtectionReq = Put(s"/api/v1/moderation/protection/$category/$extUserId/$offerId?fields=MILEAGE")
      .withHeaders(`Content-Type`(ContentTypes.`application/json`))
    checkSimpleSuccessRequest(modProtectionReq)

    // читаем из базы, проверяем что установлена защита
    val readForm = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readForm.getOfferAutoru.getModerationProtectedFieldsCount == 1)
    assert(readForm.getOfferAutoru.getModerationProtectedFields(0) == Fields.MILEAGE)

    // удаляем защиту с пробега
    val editorName = RandomUtil.nextHexString(5)
    val modProtectionRemoveReq = Delete(s"/api/v1/moderation/protection/$category/$extUserId/$offerId?fields=MILEAGE")
      .withEntity(
        HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(Editor.newBuilder().setName(editorName).build()))
      )
    checkSimpleSuccessRequest(modProtectionRemoveReq)

    // читаем из базы, проверяем что защита удалена
    val readForm2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(readForm2.getOfferAutoru.getModerationProtectedFieldsCount == 0)
    assert(readForm2.getOfferAutoru.getEditor.getName == editorName)
  }

  test("don't change badges on moderation update") {
    components.featureRegistry.updateFeature(components.featuresManager.NotificationModerationUpdate.name, true)
    val category = "cars"
    val isDealer = false
    val origBadges = List("Badge1", "Badge2").asJava

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder
    formInfoBuilder.addAllBadges(origBadges)

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    // читаем оригинальную запись из базы, проверяем что badges верные
    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getBadgesList.containsAll(origBadges))

    // генерируем обновленную форму без поля BadgesUpdate
    val formBuilder = formInfo.form.toBuilder
    val update =
      ModerationUpdateTestUtils
        .generateModerationUpdate(formInfo.form, formBuilder.build())
        .build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что badges остались прежними
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(!readForm.getBadgesList.isEmpty)
    assert(readForm.getBadgesList == origBadges)
  }

  test("remove photos on moderation update with blockedHash") {
    val blockedHash = "blockedHash"
    val category = "cars"
    val isDealer = false

    // создаем объявление, сохраняем
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
    val formInfoBuilder = formInfo.form.toBuilder

    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
    var offerId = checkSuccessRequest(req)

    var readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    components
      .getOfferDao()
      .useOfferID(AutoruOfferID.parse(offerId)) { offer =>
        val builder = offer.toBuilder
        builder.getOfferAutoruBuilder.clearPhoto()
        builder.getOfferAutoruBuilder.addPhoto(
          Photo
            .newBuilder()
            .setName("autoru-all:101404-1e190a2f94f4f29a8eb7dc720d75ec51")
            .setNamespace(AutoruAllNamespaceSettings.namespace)
            .setIsMain(true)
            .setOrder(0)
            .setCreated(System.nanoTime())
            .setMeta(
              PhotoMetadata
                .newBuilder()
                .setCvHash(blockedHash)
                .setVersion(1)
                .setIsFinished(true)
            )
        )
        OfferUpdate.visitNow(builder.build())
      }(Traced.empty)

    val formBuilder = formInfo.form.toBuilder
    val update =
      ModerationUpdateTestUtils
        .generateModerationUpdate(formInfo.form, formBuilder.build())
        .setBlockedPhotoHashUpdate(BlockedPhotoHashUpdate.newBuilder().addBlockedPhotoHash(blockedHash))
        .build()

    // сохраняем
    val req2 = Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(update)))
    offerId = checkSuccessRequest(req2)

    // читаем из базы, проверяем что photo удалены
    readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getState.getImageUrlsCount == 0)
  }

  test("properly update autoru_exclusive_manual_verified") {
    forAll { (beforeOpt: Option[Boolean], updateOptOpt: Option[Option[Boolean]]) =>
      val expected = updateOptOpt.getOrElse(beforeOpt)

      val category = "cars"

      // создаем объявление, сохраняем
      val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true, now = now))
      val formInfoBuilder = formInfo.form.toBuilder

      val extUserId: String = formInfo.form.getUserRef
      val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfoBuilder.build())))
      val offerId = checkSuccessRequest(req)

      // ставим флаг в выбранное начальное положение
      val formBuilder = formInfo.form.toBuilder
      beforeOpt.foreach { before =>
        val moderationUpdate1 = ModerationUpdateTestUtils
          .generateModerationUpdate(formInfo.form, formBuilder.build())
          .setAutoruExclusiveVerifiedUpdate(
            ModerationUpdate.AutoruExclusiveVerifiedUpdate.newBuilder().setAutoruExclusiveVerified(before)
          )
        checkSuccessRequest(
          Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
            .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(moderationUpdate1.build)))
        )
      }

      // производим обновление
      val moderationUpdate2 = ModerationUpdateTestUtils
        .generateModerationUpdate(formInfo.form, formBuilder.build())
        .clearAutoruExclusiveVerifiedUpdate()

      updateOptOpt.foreach { updateOpt =>
        val exclusiveUpdate = moderationUpdate2.getAutoruExclusiveVerifiedUpdateBuilder()
        updateOpt.foreach(exclusiveUpdate.setAutoruExclusiveVerified)
      }
      checkSuccessRequest(
        Put(s"/api/v1/moderation/$category/$extUserId/$offerId?accept_create_date=0")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(moderationUpdate2.build())))
      )

      // читаем из базы, проверяем результат
      val offerFromDb =
        components
          .getOfferDao()
          .findById(offerId)
          .getOrElse(sys.error("Updated offer not found"))
      val result = opt(offerFromDb.getOfferAutoru().getAutoruExclusiveManualVerified())

      result shouldBe expected
    }
  }

  test("get blocked photo hashes") {

    (1 to 9).foreach(i => {
      components.blockedPhotoHashesDao.upsertHash(
        s"AAA$i",
        s"offer$i",
        BlockedPhotoHashesDao.Update(
          userId = Some(s"user$i"),
          scope = Some(BlockedPhotoHash.Scope.OFFER),
          status = Some(BlockedPhotoHash.Status.ACTIVE),
          photoUrl = None,
          operator = Some(s"operator$i"),
          comment = None
        )
      )
    })

    Get(s"/api/v1/moderation/blocked-photo-hashes?after=AAA5limit=10").withHeaders(
      Accept(MediaRange.One(ru.yandex.vertis.util.akka.http.protobuf.Protobuf.mediaType, 1.0f))
    ) ~> route ~> check {
      status shouldBe StatusCodes.OK
      val response = Protobuf.fromBytes[ModerationBlockedPhotoHashesResponse](responseAs[Array[Byte]])
      response.getStatus shouldBe ResponseModel.ResponseStatus.SUCCESS
      val hashes = response.getBlockedPhotoHashesList.asScala
      hashes.size shouldBe 4
      hashes.head.getValue shouldBe "AAA6"
    }

    Get(s"/api/v1/moderation/blocked-photo-hashes?offer=offer1").withHeaders(
      Accept(MediaRange.One(ru.yandex.vertis.util.akka.http.protobuf.Protobuf.mediaType, 1.0f))
    ) ~> route ~> check {
      status shouldBe StatusCodes.OK
      val response = Protobuf.fromBytes[ModerationBlockedPhotoHashesResponse](responseAs[Array[Byte]])
      response.getStatus shouldBe ResponseModel.ResponseStatus.SUCCESS
      val hashes = response.getBlockedPhotoHashesList.asScala
      hashes.size shouldBe 1
      hashes.head.getValue shouldBe "AAA1"
    }

  }

  test("disable blocked photo hash") {

    components.blockedPhotoHashesDao.upsertHash(
      s"AAA",
      s"offer",
      BlockedPhotoHashesDao.Update(
        userId = Some(s"user"),
        scope = Some(BlockedPhotoHash.Scope.OFFER),
        status = Some(BlockedPhotoHash.Status.ACTIVE),
        photoUrl = None,
        operator = Some(s"operator1"),
        comment = None
      )
    )

    checkSuccessStringRequest(
      Delete(s"/api/v1/moderation/blocked-photo-hashes/offer/AAA?comment=test&operator=operator2")
    )

    val updatedHash = components.blockedPhotoHashesDao.getHashes(Seq("AAA")).head
    updatedHash.getStatus shouldBe BlockedPhotoHash.Status.DISABLED
    updatedHash.getHistory(1).getComment shouldBe "test"
    updatedHash.getHistory(1).getOperator shouldBe "operator2"

  }
}
