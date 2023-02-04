package ru.yandex.vos2.autoru.api.v1.draft

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import org.joda.time.{DateTime => JodaDateTime}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuiteLike
import ru.auto.api.ApiOfferModel.AdditionalInfo.ProvenOwnerStatus
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.MotoModel.MotoInfo
import ru.auto.api.TrucksModel.TruckInfo
import ru.auto.api.{ApiOfferModel, CarsModel, TrucksModel}
import ru.yandex.auto.message.AutoExtDataSchema.DealersInfoMessage
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.{Platform, Source}
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.api.utils.{CommonDirectives, UserLocation}
import ru.yandex.vos2.autoru.model.{AutoruOfferID, AutoruUser}
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils._
import ru.yandex.vos2.autoru.utils.testforms._
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.model.ModelUtils.RichOffer
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.util.ExternalAutoruUserRef
import ru.yandex.vos2.{OfferID, OfferModel}

import java.net.URLEncoder
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by andrey on 12/27/16.
  */
@RunWith(classOf[JUnitRunner])
class DraftHandlerTest extends AnyFunSuiteLike with Vos2ApiSuite with OptionValues {

  implicit private val t = Traced.empty

  test("unknown category") {
    val req = createRequest(Post("/api/v1/draft/pew"))()
    checkErrorRequest(req, StatusCodes.NotFound, unknownHandler)
  }

  test("unknown draft id") {
    checkErrorRequest(
      Get(s"/api/v1/draft/cars/user:1/qwe"),
      StatusCodes.BadRequest,
      illegalArgumentError("Draft Id is Invalid: qwe")
    )
    checkErrorRequest(
      Get(s"/api/v1/draft/trucks/user:1/12345-qwe"),
      StatusCodes.BadRequest,
      illegalArgumentError("Draft Id is Invalid: 12345-qwe")
    )
    checkErrorRequest(
      Put(s"/api/v1/draft/moto/user:1/12345-qwe"),
      StatusCodes.BadRequest,
      illegalArgumentError("Draft Id is Invalid: 12345-qwe")
    )
    checkErrorRequest(
      Delete(s"/api/v1/draft/cars/user:1/12345-qwe"),
      StatusCodes.BadRequest,
      illegalArgumentError("Draft Id is Invalid: 12345-qwe")
    )
  }

  test("russian strings") {
    // передает данные, указав Content-type: application/json, без charset=UTF-8
    val req1 = createRequest(Post(s"/api/v1/draft/cars/user:1"), ContentTypes.`application/json`) {
      _.setDescription("тест тест")
    }
    val draftId1 = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/user:1/$draftId1"))
    assert(readForm.getDescription == "тест тест")
  }

  for {
    isDealer <- Seq(false, true)
    rawCategory <- Seq("cars", "trucks", "moto", "trucks_special")
  } {
    test(s"draft creation, update and publish (inVos = false, isDealer = $isDealer, category = $rawCategory)") {

      val (ownerId: Long, userRef: String, userRef0) = testFormGenerator.randomOwnerIds(isDealer)
      // сохранили минималистичный черновик
      val req1 = minimalDraftCreationRequest(rawCategory, userRef)
      val draft1 = checkSuccessRequestWithOffer(req1)

      val category = if (rawCategory == "trucks_special") "trucks" else rawCategory

      // читаем черновик и проверяем поля
      val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$userRef/${draft1.getId}"))
      minimalDraftCheck(readForm, rawCategory)
      assert(readForm.getStatus == OfferStatus.DRAFT)
      assert(readForm.getAdditionalInfo.getLastStatus == OfferStatus.STATUS_UNKNOWN)
      assert(readForm.getId == draft1.getId)
      assert(draft1 == readForm)

      // отредактировали черновик
      val req2 = minimalDraftUpdateRequest(draft1.getId, rawCategory, userRef)
      val draft2 = checkSuccessRequestWithOffer(req2)
      assert(draft1.getId == draft2.getId)

      // читаем черновик и проверяем поля
      val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$userRef/${draft1.getId}"))
      updatedMinimalDraftCheck(readForm2, rawCategory)
      assert(readForm2.getStatus == OfferStatus.DRAFT)
      assert(readForm2.getId == draft1.getId)
      assert(readForm2 == draft2)

      // пробуем опубликовать, проверяем ошибки

      // сохраняем заполненное объявление
      val now = JodaDateTime.now()
      val formInfo = testFormGenerator.createForm(
        rawCategory,
        TestFormParams(isDealer = isDealer, optOwnerId = Some(ownerId), now = now)
      )

      val req4 = createRequest(Put(s"/api/v1/draft/$category/$userRef/${draft1.getId}")) {
        _.mergeFrom(formInfo.form)
      }
      checkSuccessRequestWithOffer(req4)

      // читаем и проверяем все поля
      val readForm3 = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$userRef/${draft1.getId}"))
      val dealersInfo = formInfo.optSalon.flatMap(_.client.flatMap(c => components.dealersData.getDealerById(c.id)))
      checkForm(formInfo, readForm3, OfferStatus.DRAFT, dealersInfo)

      // not found for another user
      checkErrorRequest(
        Post(s"/api/v1/draft/$category/${draft1.getId}/publish/anon:100500"),
        StatusCodes.NotFound,
        DraftNotFoundError
      )

      // успешно публикуем
      val offer = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/${draft1.getId}/publish/$userRef"))
      assert(offer.getId != draft1.getId)
      // проверяем историю статусов у объявления
      val activeStatus: String = "CS_NEED_ACTIVATION"
      checkStatusHistory(
        category,
        offer.getId,
        ("CS_DRAFT", "Draft create from api"),
        (activeStatus, "Draft publish from api")
      )

      // повторная публикация возвращает этот же айдишник объявления
      val offer2 = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/${draft1.getId}/publish/$userRef"))
      assert(offer2.getId == offer.getId)
      /*offer.getDescriptorForType.getFields.asScala.foreach {
        field =>
          val left = offer.getField(field)
          val right = offer2.getField(field)
          assert(left == right, field.getName)
      }*/

      // черновик удален
      checkErrorRequest(
        Get(s"/api/v1/draft/$category/$userRef/${draft1.getId}"),
        StatusCodes.NotFound,
        DraftNotFoundError
      )

      // проверим историю статусов у черновика
      checkDraftStatusHistory(
        userRef0,
        category,
        draft1.getId,
        ("CS_DRAFT", "Draft create from api"),
        ("CS_REMOVED", "Draft published as offer")
      )

      // попытка вызова перемещения отдаст 404
      checkErrorRequest(
        Post(s"/api/v1/draft/$category/$userRef/${draft1.getId}/move/$userRef"),
        StatusCodes.NotFound,
        DraftNotFoundError
      )

      // читаем объявление и проверяем все поля
      val readForm4 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/${offer.getId}"))
      val needStatus: OfferStatus = OfferStatus.NEED_ACTIVATION
      checkForm(formInfo, readForm4, needStatus, dealersInfo)

      val draftOffer2 = checkSuccessRequestWithOffer(Post(s"/api/v1/offer/$category/$userRef/${offer.getId}/draft"))
      assert(draftOffer2.getAdditionalInfo.getLastStatus == OfferStatus.NEED_ACTIVATION)

      // попытка обновить полноценное объявление как черновик
      checkErrorRequest(
        createRequest(Put(s"/api/v1/draft/$category/$userRef/${offer.getId}")) { builder =>
          builder.clear().mergeFrom(readForm)
        },
        StatusCodes.NotFound,
        DraftNotFoundError
      )

      // Tried to publish non-draft
      checkErrorRequest(
        Post(s"/api/v1/draft/$category/${offer.getId}/publish/$userRef"),
        StatusCodes.NotFound,
        DraftNotFoundError
      )

      // Tried to move non-draft
      checkErrorRequest(
        Post(s"/api/v1/draft/$category/$userRef/${offer.getId}/move/$userRef"),
        StatusCodes.NotFound,
        DraftNotFoundError
      )
    }
  }

  test("visible mileage history") {

    val inVos = false
    val isDealer = false
    val category = "cars"
    val (ownerId: Long, extOwnerId: String, _) = testFormGenerator.randomOwnerIds(isDealer)

    // сохранили минималистичный черновик
    val req1 = minimalDraftCreationRequest(category, extOwnerId)
    val draft1 = checkSuccessRequestWithOffer(req1)

    // пробуем опубликовать, проверяем ошибки

    // сохраняем заполненное объявление
    val now = JodaDateTime.now()
    val formInfo = testFormGenerator.createForm(
      category,
      TestFormParams(isDealer = isDealer, optOwnerId = Some(ownerId), now = now)
    )

    val mileage = formInfo.form.getState.getMileage

    val req2 = createRequest(Put(s"/api/v1/draft/$category/$extOwnerId/${draft1.getId}")) { builder =>
      builder.mergeFrom(formInfo.form)
    }
    checkSuccessRequestWithOffer(req2)

    // успешно публикуем и проверяем наличие истории
    val offer = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/${draft1.getId}/publish/$extOwnerId"))
    assert(offer.getId != draft1.getId)

    assert(offer.getMileageHistoryCount == 1)
    assert(offer.getMileageHistory(0).getMileage == mileage)

    // повторная публикация возвращает этот же айдишник объявления
    val offer2 = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/${draft1.getId}/publish/$extOwnerId"))

    assert(offer2.getMileageHistoryCount == 1)
    assert(offer2.getMileageHistory(0).getMileage == mileage)

    val draft2 = checkSuccessRequestWithOffer(Post(s"/api/v1/offer/$category/$extOwnerId/${offer2.getId}/draft"))

    // у черновика созданного из оффера с историей должна так же быть история
    assert(draft2.getMileageHistoryCount == 1)
    assert(draft2.getMileageHistory(0).getMileage == mileage)

    val draft3 = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$extOwnerId/${draft2.getId}"))

    assert(draft3.getMileageHistoryCount == 1)
    assert(draft3.getMileageHistory(0).getMileage == mileage)

    //проверим запрос на оффер
    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/${offer2.getId}"))

    assert(readForm2.getMileageHistoryCount == 1)
    assert(readForm2.getMileageHistory(0).getMileage == mileage)
  }

  test("draft creation for anon user") {
    val extUserId: String = "anon:100600"
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId1 = checkSuccessRequest(req1)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId1"))
    assert(readForm.getCarInfo.getArmored)
    assert(readForm.getStatus == OfferStatus.DRAFT)
    assert(readForm.getId == draftId1)

    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$draftId1/publish/$extUserId"),
      StatusCodes.BadRequest,
      illegalArgumentError("Unallowed userRef")
    )
  }

  test("user ref too long") {
    pending
    val extUserId: String = "anon:1234567890123456789012345678"
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    checkErrorRequest(req1, StatusCodes.BadRequest, longUserRefError)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    publishWithIp <- Seq(false, true)
  } {
    test(s"ip save (category = $category, publishWithIp = $publishWithIp)") {
      ipSaveTests(category, publishWithIp)
    }
  }

  //scalastyle:off method.length
  private def ipSaveTests(category: String, publishWithIp: Boolean): Unit = {
    // сохраняем новый черновик, передав айпишник. Айпишник должен сохраниться.
    val user: AutoruUser = testFormGenerator.randomUser
    val userId: Long = user.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)
    val ip1: String = "8.8.8.8"
    val req1 = createRequest(
      Post(s"/api/v1/draft/$category/$extUserId")
        .withHeaders(RawHeader("X-UserIp", ip1))
    ) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId1 = checkSuccessRequest(req1)
    val draft1 = readDraft(user.userRef, category, draftId1)
    assert(draft1.hasFlag(OfferFlag.OF_DRAFT))
    assert(draft1.getOfferAutoru.getIp == ip1)

    // обновляем черновик и передаем другой айпишник. Айпишник НЕ должен поменяться.
    val req2 = createRequest(
      Put(s"/api/v1/draft/$category/$extUserId/$draftId1").withHeaders(RawHeader("X-UserIp", "9.9.9.9"))
    ) { builder =>
      builder
        .clearCarInfo()
        .setState(builder.getStateBuilder.setMileage(56000))
    }
    checkSuccessRequest(req2)
    val draft2 = readDraft(user.userRef, category, draftId1)
    assert(draft2.hasFlag(OfferFlag.OF_DRAFT))
    assert(draft2.getOfferAutoru.getIp == ip1)

    // сохраняем полноценное объявление. Айпишник не должен потеряться, в случае, если новый не передан при публикации
    // иначе сохраняем тот, что передан
    val fullForm =
      testFormGenerator.createForm(category, TestFormParams(isDealer = false, optOwnerId = Some(userId))).form
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/$category/user:$userId/$draftId1")) { builder =>
      builder.mergeFrom(fullForm)
    })
    // пишем его в старую базу и заодно оно пишется в vos
    val (offerId, needIp) = if (publishWithIp) {
      val publishIp: OfferID = "7.7.7.7"
      (
        checkSuccessRequest(
          Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId").withHeaders(RawHeader("X-UserIp", publishIp))
        ),
        publishIp
      )
    } else {
      (checkSuccessRequest(Post(s"/api/v1/draft/$category/$draftId1/publish/$extUserId")), ip1)
    }
    // при чтении из старой базы ip не увидим, потому что на чтение он не отдается в апи. Прочитаем его прям так
    assert(getIp(offerId, category).contains(needIp))
    // ip не потерялся при чтении из vos
    val offer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto?fromvos=1"))
    assert(offer2.getOfferAutoru.getIp == needIp)

    // обновляем объявление. Айпи не должен измениться при апдейте
    val draftId3 = checkSuccessRequest(Post(s"/api/v1/offer/$category/$extUserId/$offerId/draft"))
    val ip3 = "6.6.6.6"
    checkSuccessRequest(
      Post(s"/api/v1/draft/$category/$draftId3/publish/$extUserId")
        .withHeaders(RawHeader("X-UserIp", ip3))
    )
    val offer3 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto?fromvos=1"))
    assert(offer3.getOfferAutoru.getIp == needIp)
  }

  for {
    publishWithOtherSource <- Seq(false, true)
  } {
    test(s"source platform save (publishWithOtherSource = $publishWithOtherSource)") {
      // сохраняем новый черновик, передав источник. Источник должен сохраниться.
      val user: AutoruUser = testFormGenerator.randomUser
      val userId: Long = user.id
      val extUserId: String = ExternalAutoruUserRef.privateRef(userId)
      val p1 = "auto24"
      val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId?source=$p1")) { builder =>
        builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
      }
      val draftId1 = checkSuccessRequest(req1)
      val draft1 = readDraft(user.userRef, "cars", draftId1)
      assert(draft1.hasFlag(OfferFlag.OF_DRAFT))
      assert(draft1.getOfferAutoru.getSourceInfo.getPlatform == SourceInfo.Platform.PARTNER)
      assert(draft1.getOfferAutoru.getSourceInfo.getSource == SourceInfo.Source.AUTO24)

      // сохраняем полноценное объявление. Источник не должен потеряться, в случае, если новый не передан при публикации
      // иначе сохраняем тот, что передан
      val fullForm =
        testFormGenerator.createForm("cars", TestFormParams(isDealer = false, optOwnerId = Some(userId))).form
      checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId1")) { builder =>
        builder.mergeFrom(fullForm)
      })
      // пишем его в старую базу и заодно оно пишется в vos
      val (offerId, needSource) = if (publishWithOtherSource) {
        val publishSource = "hsd"
        (
          checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId1/publish/$extUserId?source=$publishSource")),
          publishSource
        )
      } else {
        (checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId1/publish/$extUserId")), p1)
      }
      // при чтении из старой базы источник не увидим, потому что на чтение он не отдается в dao. Прочитаем его прям так
      assert(getSourcePlatform(offerId).map(_.getSource.name().toLowerCase).contains(needSource))
      // источник не потерялся при чтении из vos
      val offer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto?fromvos=1"))
      assert(offer2.getOfferAutoru.getSourceInfo.getPlatform == SourceInfo.Platform.PARTNER)
      assert(offer2.getOfferAutoru.getSourceInfo.getSource.name().toLowerCase == needSource)
    }
  }

  test("location in draft") {
    // сохраняем новый черновик, передав источник и координаты
    val user: AutoruUser = testFormGenerator.randomUser
    val userId: Long = user.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)
    val draftId1 = checkSuccessRequest(createRequest(Post(s"/api/v1/draft/cars/$extUserId?source=ios")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }.withHeaders(RawHeader("X-User-Location", UserLocation(5, 6, 7).toString)))
    val draft1 = readDraft(user.userRef, "cars", draftId1)
    assert(draft1.hasFlag(OfferFlag.OF_DRAFT))
    assert(draft1.getOfferAutoru.getSourceInfo.getPlacement.getLatitude == 5)
    assert(draft1.getOfferAutoru.getSourceInfo.getPlacement.getLongitude == 6)
    assert(draft1.getOfferAutoru.getSourceInfo.getPlacement.getAccuracy == 7)

    val fullForm =
      testFormGenerator.createForm("cars", TestFormParams(isDealer = false, optOwnerId = Some(userId))).form
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$extUserId/$draftId1")) { builder =>
      builder.mergeFrom(fullForm)
    })

    // публикуем объявление без источника - источник и координаты возьмутся из черновика
    {
      val offerId = checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId1/publish/$extUserId?insert_new=1"))
      // читаем объявление. Источник и координаты из черновика не должны потеряться
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLatitude == 5)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLongitude == 6)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getAccuracy == 7)
    }

    // публикуем объявление c источником - координаты сохранятся новые
    {
      val draftId1 = checkSuccessRequest(createRequest(Post(s"/api/v1/draft/cars/$extUserId?source=ios")) { builder =>
        builder.mergeFrom(fullForm)
      }.withHeaders(RawHeader("X-User-Location", UserLocation(5, 6, 7).toString)))
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/draft/cars/$draftId1/publish/$extUserId?insert_new=1&source=ios")
          .withHeaders(RawHeader("X-User-Location", UserLocation(8, 9, 10).toString))
      )
      // читаем объявление. Источник и координаты из черновика не должны потеряться
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/cars/$offerId/proto"))
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLatitude == 8)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLongitude == 9)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getAccuracy == 10)
    }
  }

  test("draft deletion") {
    val userRef0 = UserRef.refAnon("1")
    val userRef = "anon:1"
    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$userRef")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)

    // черновик можно прочитать
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$userRef/$draftId"))
    assert(readForm.getCarInfo.getArmored)
    assert(readForm.getStatus == OfferStatus.DRAFT)
    assert(readForm.getId == draftId)

    // удаляем черновик
    val delReq = Delete(s"/api/v1/draft/cars/$userRef/$draftId")
    delReq ~> route ~> check {
      val response: String = entityAs[String]
      withClue(response) {
        status shouldBe StatusCodes.OK
        response shouldBe """{"status":"SUCCESS"}"""
      }
    }

    // прочитать его больше не получается
    checkErrorRequest(Get(s"/api/v1/draft/cars/$userRef/$draftId"), StatusCodes.NotFound, DraftNotFoundError)
    // история статусов
    checkDraftStatusHistory(
      userRef0,
      "cars",
      draftId,
      ("CS_DRAFT", "Draft create from api"),
      ("CS_REMOVED", "Draft deleted")
    )
  }

  test("draft move") {
    val anonRef0 = UserRef.refAnon("1")
    val anonRef = "anon:1"
    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$anonRef")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId1 = checkSuccessRequest(req1)

    // Unallowed UserRef
    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$anonRef/$draftId1/move/anon:100500"),
      StatusCodes.BadRequest,
      illegalArgumentError("Unallowed userRef")
    )

    // меняем владельца черновика
    val user: AutoruUser = testFormGenerator.randomUser
    val userId: Long = user.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    val draftId2 = checkSuccessRequest(Post(s"/api/v1/draft/cars/$anonRef/$draftId1/move/$extUserId"))
    assert(draftId2 != draftId1)

    // предыдущий вариант черновика удален
    checkErrorRequest(Get(s"/api/v1/draft/cars/$anonRef/$draftId1"), StatusCodes.NotFound, DraftNotFoundError)
    // его история статусов
    checkDraftStatusHistory(
      anonRef0,
      "cars",
      draftId1,
      ("CS_DRAFT", "Draft create from api"),
      ("CS_REMOVED", "Draft moved")
    )

    // новый черновик читается
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId2"))
    // владелец в нем такой, как мы хотели
    assert(readForm.getUserRef == extUserId)

    // его история статусов
    checkDraftStatusHistory(user.userRef, "cars", draftId2, ("CS_DRAFT", "Draft create from api"))

    // повторный вызов метода move с теми же параметрами возвращает тот же результат
    val draftId3 = checkSuccessRequest(Post(s"/api/v1/draft/cars/$anonRef/$draftId1/move/$extUserId"))
    assert(draftId3 == draftId2)

    // попытка опубликовать отдаст 404
    checkErrorRequest(
      Post(s"/api/v1/draft/cars/$draftId1/publish/$extUserId"),
      StatusCodes.NotFound,
      DraftNotFoundError
    )

    // старый черновик пробуем переместить к тому же юзеру - должен просто вернуть этот же черновик
    val draftId4 = checkSuccessRequest(Post(s"/api/v1/draft/cars/$anonRef/$draftId1/move/$extUserId"))
    assert(draftId4 == draftId2)
  }

  test("draft creation and update for moto") {
    val anonRef = "anon:1"
    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/moto/$anonRef")) { builder =>
      builder.setState(State.newBuilder().setMileage(100000))
    }
    val draftId1 = checkSuccessRequest(req1)
    val readForm1 = checkSuccessReadRequest(Get(s"/api/v1/draft/moto/$anonRef/$draftId1"))
    assert(readForm1.getCategory == ApiOfferModel.Category.MOTO)

    // обновляем черновик
    val req2 = createRequest(Put(s"/api/v1/draft/moto/$anonRef/$draftId1")) { builder =>
      builder.setState(builder.getStateBuilder.setMileage(56000))
    }
    checkSuccessRequest(req2)
    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/draft/moto/$anonRef/$draftId1"))
    assert(readForm2.getCategory == ApiOfferModel.Category.MOTO)
  }

  test("draft creation from unknown offer") {
    checkErrorRequest(Post(s"/api/v1/offer/trucks/user:1/100500-hash/draft"), StatusCodes.NotFound, DraftNotFoundError)
  }

  test("offer update from draft with unknown origin") {
    // сохраняем полноценное объявление
    val formInfo: FormInfo = testFormGenerator.createForm("trucks", TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/trucks/$extUserId")) { builder =>
      builder.mergeFrom(fullForm)
    })
    // делаем из него черновик
    val draftId = checkSuccessRequest(Post(s"/api/v1/offer/trucks/$extUserId/$offerId/draft"))

    // изменяем в черновике original_offer_id на несуществующий
    Await.result(
      components.draftDao
        .updateDraft(formInfo.userRef, Category.TRUCKS, draftId, "", includeRemoved = true) { draft =>
          val builder: OfferModel.Offer.Builder = draft.toBuilder
          builder.getOfferAutoruBuilder.setOriginalOfferId("100500-hash")
          Future.successful(OfferUpdate.visitSoon(builder.build()), ())
        }
        .value,
      Duration.Inf
    )

    // пытаемся обновить объявление из такого черновика
    checkErrorRequest(
      Post(s"/api/v1/draft/trucks/$draftId/publish/$extUserId"),
      StatusCodes.Forbidden,
      unknownDraftOriginError
    )
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"draft creation from offer and offer update from draft (category = $category)") {
    // сохраняем полноценное объявление
    val formInfo: FormInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val mileage = fullForm.getState.getMileage
    val newMileage = math.min(mileage + 100000, 999999)
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$extUserId?source=desktop")) {
      builder =>
        builder.mergeFrom(fullForm)
    })
    // делаем из него черновик
    val draftId = checkSuccessRequest(Post(s"/api/v1/offer/$category/$extUserId/$offerId/draft"))
    assert(draftId != offerId)
    // проверяем, что в черновике правильно сохранилось поле original_offer_id
    val draft = checkSuccessOfferRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId/proto"))
    assert(draft.getOfferAutoru.getOriginalOfferId == offerId)
    // проверяем корректность полей offerIRef, refLegacy, hashCode
    val autoruOfferId: AutoruOfferID = AutoruOfferID.parse(draft.getOfferID)
    assert(draft.getOfferIRef == autoruOfferId.id)
    assert(draft.getRefLegacy == draft.getOfferIRef)
    assert(autoruOfferId.hash.contains(draft.getOfferAutoru.getHashCode))
    // редактируем черновик
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/$category/$extUserId/$draftId")) { builder =>
      val draft2 = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId"))
      builder.mergeFrom(draft2).getStateBuilder.setMileage(newMileage)
    })
    // обновляем исходное объявление
    val offerId1 = checkSuccessRequest(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId"))
    assert(offerId1 == offerId)
    // проверим историю изменения статусов в обновленном объявлении. Должны быть только прежние статусы
    val activeStatus: String = "CS_NEED_ACTIVATION"
    checkStatusHistory(category, offerId, (activeStatus, "Offer create from api"))
    checkUserChangeActionHistory(category, offerId, List("CREATE"))

    // черновик удален
    checkErrorRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId"), StatusCodes.NotFound, DraftNotFoundError)
    // проверим статусы в нем
    checkDraftStatusHistory(
      formInfo.userRef,
      category,
      draftId,
      (activeStatus, "Offer create from api"),
      ("CS_DRAFT", "Offer edit"),
      ("CS_REMOVED", "Draft published as offer")
    )
    // читаем исходное объявление и проверяем изменение
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))

    assert(offer.getState.getMileage == newMileage)
    // повторная публикация возвращает тот же айдишник
    val offerId2 = checkSuccessRequest(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId"))
    assert(offerId2 == offerId)
  }

  test(s"blocked photo hashes check") {

    val category = "cars"
    val blockedHash = "blockedHash"
    // сохраняем полноценное объявление
    val formInfo: FormInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val fullForm = formInfo.form
    val mileage = fullForm.getState.getMileage
    val newMileage = math.min(mileage + 100000, 999999)
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$extUserId?source=desktop")) {
      builder =>
        builder.mergeFrom(fullForm)
    })

    components.getOfferDao().useOfferID(AutoruOfferID.parse(offerId), true, "") { offer =>
      val builder = offer.toBuilder
      builder.getOfferAutoruBuilder.addBlockedPhotoHash(blockedHash)
      OfferUpdate.visitNow(builder.build())
    }

    // делаем из него черновик
    val draftId = checkSuccessRequest(Post(s"/api/v1/offer/$category/$extUserId/$offerId/draft"))
    assert(draftId != offerId)
    // проверяем, что в черновике правильно сохранилось поле original_offer_id
    val draft = checkSuccessOfferRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId/proto"))
    assert(draft.getOfferAutoru.getOriginalOfferId == offerId)
    // проверяем корректность полей offerIRef, refLegacy, hashCode
    val autoruOfferId: AutoruOfferID = AutoruOfferID.parse(draft.getOfferID)
    assert(draft.getOfferIRef == autoruOfferId.id)
    assert(draft.getRefLegacy == draft.getOfferIRef)
    assert(autoruOfferId.hash.contains(draft.getOfferAutoru.getHashCode))
    // редактируем черновик
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/$category/$extUserId/$draftId")) { builder =>
      val draft2 = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId"))
      builder.mergeFrom(draft2).getStateBuilder.setMileage(newMileage)
    })
    // обновляем исходное объявление
    val offerId1 = checkSuccessRequest(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId"))
    assert(offerId1 == offerId)
    // проверим историю изменения статусов в обновленном объявлении. Должны быть только прежние статусы
    val activeStatus: String = "CS_NEED_ACTIVATION"
    checkStatusHistory(category, offerId, (activeStatus, "Offer create from api"))
    checkUserChangeActionHistory(category, offerId, List("CREATE"))

    // черновик удален
    checkErrorRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId"), StatusCodes.NotFound, DraftNotFoundError)
    // проверим статусы в нем
    checkDraftStatusHistory(
      formInfo.userRef,
      category,
      draftId,
      (activeStatus, "Offer create from api"),
      ("CS_DRAFT", "Offer edit"),
      ("CS_REMOVED", "Draft published as offer")
    )
    // читаем исходное объявление и проверяем изменение
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))

    val vosOffer = components.getOfferDao().findById(offerId).value

    assert(vosOffer.getOfferAutoru.getBlockedPhotoHashList.asScala.contains(blockedHash))
    assert(offer.getState.getMileage == newMileage)
    // повторная публикация возвращает тот же айдишник
    val offerId2 = checkSuccessRequest(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId"))
    assert(offerId2 == offerId)
  }

  test("save services") {
    // создаем черновик
    val req1 = createRequest(Post("/api/v1/draft/trucks/anon:1"))()
    val draftId1 = checkSuccessRequest(req1)
    val readForm1 = checkSuccessReadRequest(Get(s"/api/v1/draft/trucks/anon:1/$draftId1"))
    val userRef = readForm1.getUserRef

    val req2 = createRequest(Put(s"/api/v1/draft/trucks/$userRef/$draftId1")) { builder =>
      builder.mergeFrom(readForm1).addServices(PaidService.newBuilder().setService("all_sale_special"))
    }
    checkSuccessRequest(req2)
    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/draft/trucks/$userRef/$draftId1"))
    assert(readForm2.getServicesCount == 1)
    assert(readForm2.getServices(0).getService == "all_sale_special")
    assert(!readForm2.getServices(0).getIsActive)
  }

  /*test("noPhoneOwnerCheck") {
    // если передан параметр noPhoneOwnerCheck, то сохраняем без проверки, что телефон принадлежит пользователю
  }*/

  test("source and remote_id") {
    // сохраняем черновик с присланным remote_id. Публикуем черновик с присланным источником.
    // remote_id должен сохраниться, источник тоже
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val formBuilder = formInfo.form.toBuilder
    val remoteId: OfferID = "remoteId115"
    val remoteUrl: String = "http://24auto.ru/sale/115"
    formBuilder.getAdditionalInfoBuilder.setRemoteId(remoteId).setRemoteUrl(remoteUrl)

    val draft0 = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.mergeFrom(formBuilder.build())
    })
    assert(draft0.getAdditionalInfo.getRemoteId == remoteId)
    assert(draft0.getAdditionalInfo.getRemoteUrl == remoteUrl)

    val draft = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId?source=auto24")) {
      builder =>
        builder.mergeFrom(formBuilder.build())
    })
    assert(draft.getAdditionalInfo.getRemoteId == remoteId)
    assert(draft.getAdditionalInfo.getRemoteUrl == remoteUrl)

    val draftId: OfferID = draft.getId
    val draftAsOffer = checkSuccessOfferRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId/proto"))
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getPlatform == Platform.PARTNER)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getSource == Source.AUTO24)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getUserRef == formInfo.userRef.toPlain)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getRemoteId == remoteId)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getRemoteUrl == remoteUrl)

    val form = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId?source=hsd"))
    assert(form.getAdditionalInfo.getRemoteId == remoteId)
    assert(form.getAdditionalInfo.getRemoteUrl == remoteUrl)

    val offerId = form.getId
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
    assert(offer.getOfferAutoru.getSourceInfo.getPlatform == Platform.PARTNER)
    assert(offer.getOfferAutoru.getSourceInfo.getSource == Source.HSD)
    assert(offer.getOfferAutoru.getSourceInfo.getUserRef == formInfo.userRef.toPlain)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteId == remoteId)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteUrl == remoteUrl)
  }

  test(s"remote_id and remote_url save from params (invos = false)") {
    // remote_id и remote_url переданные в параметрах сохраняются при публикации и имеют приоритет над данными
    // в ченовике (которые тоже сохраняются)
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId: String = formInfo.extUserId
    val formBuilder = formInfo.form.toBuilder
    val remoteId: String = "avito|cars|1767143438"
    val remoteUrl: String = "https://www.avito.ru/miass/avtomobili/toyota_land_cruiser_prado_2013_1767143438"
    val remoteId2: String = "drom|cars|34818791"
    val remoteUrl2: String = "https://krasnoyarsk.drom.ru/kia/rio/34818791.html"
    val remoteId2Encoded: String = URLEncoder.encode("drom|cars|34818791", "UTF-8")
    val remoteUrl2Encoded: String = URLEncoder.encode("https://krasnoyarsk.drom.ru/kia/rio/34818791.html", "UTF-8")
    formBuilder.getAdditionalInfoBuilder.setRemoteId(remoteId).setRemoteUrl(remoteUrl)

    val draft0 = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.mergeFrom(formBuilder.build())
    })
    assert(draft0.getAdditionalInfo.getRemoteId == remoteId)
    assert(draft0.getAdditionalInfo.getRemoteUrl == remoteUrl)

    val draft = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId?source=desktop")) {
      builder =>
        builder.mergeFrom(formBuilder.build())
    })
    assert(draft.getAdditionalInfo.getRemoteId == remoteId)
    assert(draft.getAdditionalInfo.getRemoteUrl == remoteUrl)

    val draftId: OfferID = draft.getId
    val draftAsOffer = checkSuccessOfferRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId/proto"))
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getPlatform == Platform.DESKTOP)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getSource == Source.AVITO)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getUserRef == formInfo.userRef.toPlain)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getRemoteId == remoteId)
    assert(draftAsOffer.getOfferAutoru.getSourceInfo.getRemoteUrl == remoteUrl)

    val form = checkSuccessRequestWithOffer(
      Post(
        s"/api/v1/draft/$category/$draftId/publish/$extUserId?source=desktop&remote_id=$remoteId2Encoded&" +
          s"remote_url=$remoteUrl2Encoded"
      )
    )
    assert(form.getAdditionalInfo.getRemoteId == remoteId2)
    assert(form.getAdditionalInfo.getRemoteUrl == remoteUrl2)

    val offerId = form.getId
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
    assert(offer.getOfferAutoru.getSourceInfo.getPlatform == Platform.DESKTOP)
    assert(offer.getOfferAutoru.getSourceInfo.getSource == Source.DROM)
    assert(offer.getOfferAutoru.getSourceInfo.getUserRef == formInfo.userRef.toPlain)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteId == remoteId2)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteUrl == remoteUrl2)
  }

  for {
    customFlag <- Seq(true, false)
    category <- Seq("cars", "trucks", "moto")
  } test(s"location and contacts (category = $category, customFlag = $customFlag)") {

    val ownerId = if (customFlag) 10086 else 24813

    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true, optOwnerId = Some(ownerId)))
    val extUserId = formInfo.extUserId
    val formBuilder = formInfo.form.toBuilder
    val sellerBuilder = formBuilder.getSellerBuilder
    val salon = formInfo.optSalon.head

    val sellerData = Map(
      "address" -> Seq("44 км МКАД (внешняя сторона), владение 1", salon.salonAddress.head),
      "geobaseId" -> Seq(213, salon.geobaseId.head),
      "latitude" -> Seq(55.628967, salon.latitude.head),
      "longitude" -> Seq(37.46981, salon.longitude.head),
      "phoneCount" -> Seq(1, salon.salonPhones.size)
    )

    val sellerTestData: Map[String, Any] = sellerData.view.mapValues(_.head).toMap
    val sellerCurrentData: Map[String, Any] = sellerData.view.mapValues {
      _(if (customFlag) 0 else 1)
    }.toMap

    val phonesData = Map(
      "phone" -> (Seq("78001001017") ++ salon.salonPhones.map(_.phone)),
      "title" -> (Seq("Общий") ++ salon.salonPhones.flatMap(_.title)),
      "callFrom" -> (Seq(8) ++ salon.salonPhones.flatMap(_.callFrom)),
      "callTill" -> (Seq(21) ++ salon.salonPhones.flatMap(_.callTill))
    )

    val phonesTestData = phonesData.mapValues(_.head)
    val phonesCurrentData = phonesData.mapValues { values =>
      values.zipWithIndex.collect { case (v, i) if i != (if (customFlag) 1 else 0) => v }
    }

    sellerBuilder
      .setCustomLocation(customFlag)
      .setCustomPhones(customFlag)

    sellerBuilder.getLocationBuilder
      .setAddress(sellerTestData("address").asInstanceOf[String])
      .setGeobaseId(sellerTestData("geobaseId").asInstanceOf[Long])

    sellerBuilder.getLocationBuilder.getCoordBuilder
      .setLatitude(sellerTestData("latitude").asInstanceOf[Double])
      .setLongitude(sellerTestData("longitude").asInstanceOf[Double])

    sellerBuilder.getPhonesBuilderList.clear()
    sellerBuilder
      .addPhonesBuilder(0)
      .setPhone(phonesTestData("phone").asInstanceOf[String])
      .setTitle(phonesTestData("title").asInstanceOf[String])
      .setCallHourStart(phonesTestData("callFrom").asInstanceOf[Int])
      .setCallHourEnd(phonesTestData("callTill").asInstanceOf[Int])

    val formOffer = formBuilder.build

    val draft = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.mergeFrom(formBuilder.build)
    })

    // ничего не должно поменяться
    assert(draft.getSeller.getLocation.getAddress == formOffer.getSeller.getLocation.getAddress)
    assert(draft.getSeller.getLocation.getGeobaseId == formOffer.getSeller.getLocation.getGeobaseId)
    assert(draft.getSeller.getLocation.getCoord.equals(formOffer.getSeller.getLocation.getCoord))
    assert(draft.getSeller.getPhonesCount == formOffer.getSeller.getPhonesCount)

    for (i <- 0 until formOffer.getSeller.getPhonesCount) {
      assert(draft.getSeller.getPhones(i).getPhone == formOffer.getSeller.getPhones(i).getPhone)
      assert(draft.getSeller.getPhones(i).getTitle == formOffer.getSeller.getPhones(i).getTitle)
      assert(draft.getSeller.getPhones(i).getCallHourStart == formOffer.getSeller.getPhones(i).getCallHourStart)
      assert(draft.getSeller.getPhones(i).getCallHourEnd == formOffer.getSeller.getPhones(i).getCallHourEnd)
    }

    val draftId: OfferID = draft.getId
    val form = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId"))
    val offerId = form.getId
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))

    assert(offer.getOfferAutoru.getSeller.getPlace.getAddress == sellerCurrentData("address"))
    assert(offer.getOfferAutoru.getSeller.getPlace.getGeobaseId == sellerCurrentData("geobaseId"))
    assert(offer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLatitude == sellerCurrentData("latitude"))
    assert(offer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLongitude == sellerCurrentData("longitude"))
    assert(offer.getOfferAutoru.getSeller.getPhoneCount == sellerCurrentData("phoneCount"))

    for {
      i <- 0 until offer.getOfferAutoru.getSeller.getPhoneCount
      offerPhone = offer.getOfferAutoru.getSeller.getPhone(i)
      phoneData = phonesCurrentData.mapValues(_(i))
    } {
      assert(offerPhone.getNumber == phoneData("phone"))
      if (category == "cars") assert(offerPhone.getTitle == phoneData("title"))
      assert(offerPhone.getCallHourStart == phoneData("callFrom"))
      assert(offerPhone.getCallHourEnd == phoneData("callTill"))
    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    isDealer <- Seq(true, false)
    shouldSet = isDealer || category == "trucks" || category == "cars" || category == "moto"
  } {
    test(s"with_nds in $category for isDealer=$isDealer shouldSet: $shouldSet") {
      val now = JodaDateTime.now()
      val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
      val extUserId = formInfo.extUserId
      val formBuilder = formInfo.form.toBuilder
      val draftId = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
        builder.mergeFrom(formBuilder.build)
      }).getId
      // можно редактировать флаг в черновике
      formBuilder.getPriceInfoBuilder.getWithNdsBuilder.setValue(true)
      assert(checkSuccessRequestWithOffer(createRequest(Put(s"/api/v1/draft/$category/$extUserId/$draftId")) {
        builder =>
          builder.mergeFrom(formBuilder.build)
      }).getPriceInfo.getWithNds.getValue)
      formBuilder.getPriceInfoBuilder.getWithNdsBuilder.setValue(false)
      assert(!checkSuccessRequestWithOffer(createRequest(Put(s"/api/v1/draft/$category/$extUserId/$draftId")) {
        builder =>
          builder.mergeFrom(formBuilder.build)
      }).getPriceInfo.getWithNds.getValue)
      // при публикации он будет сброшен в false для частника в cars
      formBuilder.getPriceInfoBuilder.getWithNdsBuilder.setValue(true)
      checkSuccessRequestWithOffer(createRequest(Put(s"/api/v1/draft/$category/$extUserId/$draftId")) { builder =>
        builder.mergeFrom(formBuilder.build)
      })
      val offerId = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId")).getId
      val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
      assert(offer.getOfferAutoru.getPrice.getWithNds == shouldSet)
    }
  }

  test("with_nds set to true for new comtrans") {
    val category = "trucks"
    val now = JodaDateTime.now()
    val formInfo =
      testFormGenerator.createForm(category, TestFormParams(isDealer = true, now = now, section = Section.NEW))
    val extUserId = formInfo.extUserId
    val formBuilder = formInfo.form.toBuilder
    val draftId = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.mergeFrom(formBuilder.build)
    }).getId
    val offerId = checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId")).getId
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
    assert(offer.getOfferAutoru.getPrice.getWithNds)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"source info for parsed offer from call center: insert only (category = $category, inVos = false)") {
    val now = JodaDateTime.now()
    // корректно сохраняем источник размещения для спарсенных объявлений, не теряем при чтении, НЕ обновляем при апдейте
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false, now = now))
    val extUserId: String = formInfo.form.getUserRef
    val formBuilder = formInfo.form.toBuilder
    val remoteId: OfferID = "1083132724"
    val remoteUrl: String = "https://m.avito.ru/zavodoukovsk/gruzoviki_i_spetstehnika/prodam_stsepku_freightliner_" +
      "century_" + remoteId
    formBuilder.getAdditionalInfoBuilder
      .setRemoteId(remoteId)
      .setRemoteUrl(remoteUrl)
    val draft = checkSuccessRequestWithOffer(createRequest(Post(s"/api/v1/draft/$category/$extUserId")) { builder =>
      builder.mergeFrom(formBuilder.build)
    })
    val draftId: OfferID = draft.getId
    val offerId = checkSuccessRequest(
      Post(
        s"/api/v1/draft/$category/$draftId/publish/$extUserId?" +
          s"source=desktop&insert_new=1"
      ).withHeaders(RawHeader("X-User-Location", "lat=54.507374;lon=36.272118;acc=1414.0"))
    )
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
    assert(offer.getOfferAutoru.getSourceInfo.getIsCallcenter)
    assert(offer.getOfferAutoru.getSourceInfo.getParseUrl == remoteUrl)
    assert(offer.getOfferAutoru.getSourceInfo.getPlacement.getLatitude == 54.507374)
    assert(offer.getOfferAutoru.getSourceInfo.getPlacement.getLongitude == 36.272118)
    assert(offer.getOfferAutoru.getSourceInfo.getPlacement.getAccuracy == 1414.0)
    assert(offer.getOfferAutoru.getSourceInfo.getPlatform == Platform.DESKTOP)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteId == remoteId)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteUrl == remoteUrl)
    assert(offer.getOfferAutoru.getSourceInfo.getSource == Source.AVITO)
    assert(offer.getOfferAutoru.getSourceInfo.getUserRef == formInfo.userRef.toPlain)
    val draft2Builder =
      checkSuccessRequestWithOffer(Post(s"/api/v1/offer/$category/$extUserId/$offerId/draft")).toBuilder
    val draftId2: OfferID = draft2Builder.getId
    draft2Builder.getAdditionalInfoBuilder.clearRemoteId().clearRemoteUrl()
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/$category/$extUserId/$draftId2")) { builder =>
      builder.mergeFrom(draft2Builder.build)
    })
    val offerId2 = checkSuccessRequest(Post(s"/api/v1/draft/$category/$draftId2/publish/$extUserId?source=ios"))
    val offer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId2/proto"))
    assert(offer2.getOfferAutoru.getSourceInfo == offer.getOfferAutoru.getSourceInfo)

  }

  for {
    rawCategory <- Seq("cars", "trucks", "moto", "trucks_special")
  } {
    test(s"chatsEnabled check for private seller draft (category = $rawCategory)") {
      val category = if (rawCategory == "trucks_special") "trucks" else rawCategory
      val offerInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
      val fullOffer = offerInfo.form
      val userRef = offerInfo.extUserId
      val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$userRef?source=desktop")) {
        builder =>
          builder.mergeFrom(fullOffer)
      })
      // делаем из него черновик
      val draft1 = checkSuccessRequest(Post(s"/api/v1/offer/$category/$userRef/$offerId/draft"))

      // отредактировали черновик
      val req2 = chatsEnabledChatOnlyDraftUpdateRequest(draft1, category, userRef, true, false, 1)
      val draft2 = checkSuccessRequestWithOffer(req2)

      // успешно публикуем
      val offer = checkSuccessRequestWithOffer(
        Post(s"/api/v1/draft/$category/$draft1/publish/$userRef?can_disable_chats=1")
      )

      // читаем объявление и проверяем все поля
      val readForm3 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/${offer.getId}"))
      assert(readForm3.getSeller.getChatsEnabled)
      assert(!readForm3.getAdditionalInfo.getChatOnly)

    }
  }

  test(s"chatsEnabled check for private seller when chatsEnabled=false and chatOnly=true") {
    val category = "cars"
    val offerInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val fullOffer = offerInfo.form
    val userRef = offerInfo.extUserId
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$userRef?source=desktop")) {
      builder =>
        builder.mergeFrom(fullOffer)
    })
    // делаем из него черновик
    val draft1 = checkSuccessRequest(Post(s"/api/v1/offer/$category/$userRef/$offerId/draft"))

    // выставили chatsEnabled=false и chatOnly=false
    val req2 = chatsEnabledChatOnlyDraftUpdateRequest(draft1, category, userRef, false, false, 1)
    val draft2 = checkSuccessRequestWithOffer(req2)

    // отредактировали черновик
    val req3 = chatsEnabledChatOnlyDraftUpdateRequest(draft2.getId, category, userRef, false, true, 1)
    val draft3 = checkSuccessRequestWithOffer(req3)

    // успешно публикуем
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/draft/$category/${draft2.getId}/publish/$userRef?can_disable_chats=1")
    )

    // читаем объявление и проверяем все поля
    val readForm4 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/${offer.getId}"))
    assert(readForm4.getSeller.getChatsEnabled)
    assert(readForm4.getAdditionalInfo.getChatOnly)
  }

  test(s"chatsEnabled check for private seller when chatsEnabled=false, chatOnly=true and canDisableChats=false") {
    val category = "cars"
    val offerInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val fullOffer = offerInfo.form
    val userRef = offerInfo.extUserId
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$userRef?source=desktop")) {
      builder =>
        builder.mergeFrom(fullOffer)
    })
    // делаем из него черновик
    val draft1 = checkSuccessRequest(Post(s"/api/v1/offer/$category/$userRef/$offerId/draft"))

    // выставили chatsEnabled=false и chatOnly=false
    val req2 = chatsEnabledChatOnlyDraftUpdateRequest(draft1, category, userRef, false, false, 0)
    val draft2 = checkSuccessRequestWithOffer(req2)

    // отредактировали черновик
    val req3 = chatsEnabledChatOnlyDraftUpdateRequest(draft2.getId, category, userRef, false, true, 0)
    val draft3 = checkSuccessRequestWithOffer(req3)

    // успешно публикуем
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/draft/$category/${draft2.getId}/publish/$userRef?can_disable_chats=0")
    )

    // читаем объявление и проверяем все поля
    val readForm4 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/${offer.getId}"))
    assert(readForm4.getSeller.getChatsEnabled)
    assert(readForm4.getAdditionalInfo.getChatOnly)
  }

  test(s"chatsEnabled amendment check for private seller when canDisableChats=false") {
    val category = "cars"
    val offerInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val fullOffer = offerInfo.form
    val userRef = offerInfo.extUserId
    val offerId = checkSuccessRequest(createRequest(Post(s"/api/v1/offers/$category/$userRef?source=desktop")) {
      builder =>
        builder.mergeFrom(fullOffer)
    })
    // делаем из него черновик
    val draft1 = checkSuccessRequest(Post(s"/api/v1/offer/$category/$userRef/$offerId/draft"))

    // выставили chatsEnabled=true и chatOnly=false
    val req2 = chatsEnabledChatOnlyDraftUpdateRequest(draft1, category, userRef, true, true, 1)
    val draft2 = checkSuccessRequestWithOffer(req2)

    // отредактировали черновик
    val req3 = chatsEnabledChatOnlyDraftUpdateRequest(draft2.getId, category, userRef, false, false, 0)
    val draft3 = checkSuccessRequestWithOffer(req3)

    // успешно публикуем
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/draft/$category/${draft2.getId}/publish/$userRef?can_disable_chats=0")
    )

    // читаем объявление и проверяем все поля
    val readForm4 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/${offer.getId}"))
    assert(readForm4.getSeller.getChatsEnabled)
    assert(!readForm4.getAdditionalInfo.getChatOnly)
  }

  test("proven owner") {
    val req1 = createRequest(Post(s"/api/v1/draft/cars/user:1"), ContentTypes.`application/json`) {
      _.setDescription("тест proven owner")
    }
    val draftId = checkSuccessRequest(req1)
    //дергаем ручку установки статуса проверенного собственника
    val req2 = createRequest(
      Put(
        s"/api/v1/draft/cars/user:1/$draftId/proven_owner?" +
          s"assigment_date=${JodaDateTime.now().toString(CommonDirectives.defaultFormat)}"
      )
    )(_.getAdditionalInfoBuilder.setProvenOwnerStatus(ProvenOwnerStatus.OK))
    checkSimpleSuccessRequest(req2)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/user:1/$draftId"))
    assert(readForm.getAdditionalInfo.getProvenOwnerStatus == ProvenOwnerStatus.OK)
  }

  private def getIp(offerId: OfferID, category: String): Option[String] = {
    val autoruOfferID = AutoruOfferID.parse(offerId)
    category match {
      case "cars" =>
        components.autoruSalesDao.getOfferForMigration(autoruOfferID.id).value.ip
      case "trucks" =>
        components.autoruTrucksDao.getOfferForMigration(autoruOfferID.id).value.ip
      case "moto" =>
        components.autoruMotoDao.getOfferForMigration(autoruOfferID.id).value.ip
      case _ => sys.error(s"unexpected category $category")
    }
  }

  private def getSourcePlatform(offerId: OfferID): Option[SourceInfo] = {
    val autoruOfferID = AutoruOfferID.parse(offerId)
    components.autoruSalesDao.getOfferForMigration(autoruOfferID.id).value.sourceInfo
  }

  private def minimalDraftCreationRequest(rawCategory: String, user: String): HttpRequest = {
    val category = if (rawCategory == "trucks_special") "trucks" else rawCategory
    createRequest(Post(s"/api/v1/draft/$category/$user")) { builder =>
      rawCategory match {
        case "cars" =>
          builder.setCarInfo(
            CarInfo.newBuilder().setHorsePower(140)
          )
        case "trucks" =>
          builder.setTruckInfo(
            TruckInfo.newBuilder().setAxis(6)
          )
        case "trucks_special" =>
          builder.setTruckInfo(
            TruckInfo
              .newBuilder()
              .setOperatingHours(1)
              .setLoadHeight(2)
              .setCraneRadius(3)
              .setBucketVolume(4)
              .setTractionClass(TrucksModel.TractionClass.TRACTION_3)
          )
        case "moto" =>
          builder.setMotoInfo(
            MotoInfo.newBuilder().setDisplacement(300)
          )
        case _ => sys.error(s"unexpected category $rawCategory")
      }
    }
  }

  private def minimalDraftCheck(form: ApiOfferModel.Offer, category: String): Unit = {
    category match {
      case "cars" =>
        assert(form.getCarInfo.getHorsePower == 140)
      case "trucks" =>
        assert(form.getTruckInfo.getAxis == 6)
      case "trucks_special" =>
        assert(form.getTruckInfo.getOperatingHours == 1)
        assert(form.getTruckInfo.getLoadHeight == 2)
        assert(form.getTruckInfo.getCraneRadius == 3)
        assert(form.getTruckInfo.getBucketVolume == 4)
        assert(form.getTruckInfo.getTractionClass == TrucksModel.TractionClass.TRACTION_3)
      case "moto" =>
        assert(form.getMotoInfo.getDisplacement == 300)
      case _ => sys.error(s"unexpected category $category")
    }
  }

  private def minimalDraftUpdateRequest(draftId: String, rawCategory: String, userRef: String): HttpRequest = {
    val category = if (rawCategory == "trucks_special") "trucks" else rawCategory
    createRequest(Put(s"/api/v1/draft/$category/$userRef/$draftId")) { builder =>
      rawCategory match {
        case "cars" =>
          builder
            .setCarInfo(
              CarInfo.newBuilder().setHorsePower(150)
            )
            .setState(State.newBuilder().setMileage(100000))
        case "trucks" =>
          builder
            .setTruckInfo(
              TruckInfo.newBuilder().setAxis(7)
            )
            .setState(State.newBuilder().setMileage(110000))
        case "trucks_special" =>
          builder
            .setTruckInfo(
              TruckInfo
                .newBuilder()
                .setOperatingHours(5)
                .setLoadHeight(6)
                .setCraneRadius(7)
                .setBucketVolume(8)
                .setTractionClass(TrucksModel.TractionClass.TRACTION_4)
            )
            .setState(State.newBuilder().setMileage(110000))
        case "moto" =>
          builder
            .setMotoInfo(
              MotoInfo.newBuilder().setDisplacement(500)
            )
            .setState(State.newBuilder().setMileage(110000))
        case _ => sys.error(s"unexpected category $rawCategory")
      }
    }
  }

  private def updatedMinimalDraftCheck(form: ApiOfferModel.Offer, rawCategory: String): Unit = {
    rawCategory match {
      case "cars" =>
        assert(form.getCarInfo.getHorsePower == 150)
        assert(form.getState.getMileage == 100000)
      case "trucks" =>
        assert(form.getTruckInfo.getAxis == 7)
        assert(form.getState.getMileage == 110000)
      case "trucks_special" =>
        assert(form.getTruckInfo.getOperatingHours == 5)
        assert(form.getTruckInfo.getLoadHeight == 6)
        assert(form.getTruckInfo.getCraneRadius == 7)
        assert(form.getTruckInfo.getBucketVolume == 8)
        assert(form.getTruckInfo.getTractionClass == TrucksModel.TractionClass.TRACTION_4)
        assert(form.getState.getMileage == 110000)
      case "moto" =>
        assert(form.getMotoInfo.getDisplacement == 500)
        assert(form.getState.getMileage == 110000)
      case _ => sys.error(s"unexpected category $rawCategory")
    }
  }

  private def checkForm(formInfo: FormInfo,
                        readForm: Offer,
                        needStatus: OfferStatus,
                        dealersInfo: Option[DealersInfoMessage]): Unit = {
    formInfo match {
      case f: CarFormInfo =>
        FormTestUtils.performCarFormChecks(f, readForm, needStatus, dealersInfo)
      case f: TruckFormInfo =>
        FormTestUtils.performTruckFormChecks(f, readForm, needStatus, dealersInfo)
      case f: MotoFormInfo =>
        FormTestUtils.performMotoFormChecks(f, readForm, needStatus, dealersInfo)
    }
  }

  private def chatsEnabledChatOnlyDraftUpdateRequest(draftId: String,
                                                     category: String,
                                                     userRef: String,
                                                     chatsEnabled: Boolean,
                                                     chatOnly: Boolean,
                                                     canDisableChats: Int): HttpRequest = {
    createRequest(Put(s"/api/v1/draft/$category/$userRef/$draftId?can_disable_chats=$canDisableChats")) { builder =>
      val draft = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$userRef/$draftId"))
      builder.mergeFrom(draft)
      builder
        .setAdditionalInfo(builder.getAdditionalInfo.toBuilder.setChatOnly(chatOnly).build())
      builder
        .setSeller(
          builder.getSeller.toBuilder.setChatsEnabled(chatsEnabled).build()
        )
    }
  }
}
