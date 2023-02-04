package ru.yandex.vos2.autoru.api.v1.offers

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.RequestModel.AddServicesRequest
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api.{ApiOfferModel, TrucksModel}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.{Platform, Source}
import ru.yandex.vos2.AutoruModel.AutoruOffer.{SourceInfo, TruckInfo}
import ru.yandex.vos2.autoru.api.utils.UserLocation
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.model.{AutoruCommonLogic, AutoruOfferID, AutoruSale, AutoruSaleStatus}
import ru.yandex.vos2.autoru.utils.ApiFormUtils.RichPriceInfoOrBuilder
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils._
import ru.yandex.vos2.autoru.utils.testforms._
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors._
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.{OfferRef, UserRef}
import ru.yandex.vos2.util.{ExternalAutoruUserRef, Protobuf}

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 2/20/17.
  */
@RunWith(classOf[JUnitRunner])
class OffersHandlerWriteTest extends AnyFunSuite with Vos2ApiSuite {

  implicit private val t = Traced.empty

  private val now = new DateTime()

  implicit private val successResultParser = Json.reads[SuccessResponse]
  components.featureRegistry.updateFeature(components.featuresManager.IncompatibleEquipmentCheck.name, false)

  test("keep custom address for dealer offer if he allowed to") {
    val category = "cars"
    val testFormParams =
      TestFormParams(isDealer = true, hidden = false, now = now, optOwnerId = Some(10086), customAddress = true)
    val formInfo = testFormGenerator.createForm(category, testFormParams)
    val extUserId: String = formInfo.form.getUserRef

    val offerId = withClue(formInfo.json) {
      checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=android")
          .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
      )
    }
    val readForm0 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm0.getSeller.getLocation.getAddress == formInfo.form.getSeller.getLocation.getAddress)
    components
      .getOfferDao()
      .migrateAndSave(components.autoruSalesDao.getOffers(Seq(AutoruOfferID.parse(offerId).id)), "test") {
        case (sale, optOffer) => components.carOfferConverter.convertStrict(sale, optOffer).converted
      }(Traced.empty)
    // после миграции адрес тоже не должен поменяться на дефолтный
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getSeller.getLocation.getAddress == formInfo.form.getSeller.getLocation.getAddress)
  }

  for {
    isDealer <- Seq(true, false)
    hidden <- Seq(true, false)
    rawCategory <- Seq("cars", "trucks", "moto", "trucks_special")
  } test(s"offer save (dealer = $isDealer, hidden = $hidden, vos = false, category = $rawCategory)") {
    // тест создания объявления в скрытом состоянии
    // создаем новое объявление, сохраняем, читаем, сравниваем поля
    // редактируем объявление, сохраняем, читаем, сравниваем поля
    val testFormParams = if (isDealer) {
      TestFormParams(isDealer = isDealer, hidden = hidden, now = now, optOwnerId = Some(24813))
    } else TestFormParams(isDealer = isDealer, hidden = hidden, now = now)

    val formInfo = testFormGenerator.createForm(rawCategory, testFormParams)
    val extUserId: String = formInfo.form.getUserRef

    val category = if (rawCategory == "trucks_special") "trucks" else rawCategory

    val offerId = withClue(formInfo.json) {
      checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=android")
          .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
      )
    }

    val readForm: Offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))

    checkForm(hidden, formInfo, readForm, category)
    // проверяем сохранение истории цен
    assert(readForm.getPriceHistoryCount == 1)
    assert(readForm.getPriceHistory(0).selectPrice == formInfo.form.getPriceInfo.selectPrice)
    assert(readForm.getPriceHistory(0).getCurrency == formInfo.form.getPriceInfo.getCurrency)
    assert(readForm.getPriceHistory(0).getCreateTimestamp > 0)
    // Проверяем скидки
    checkDiscounts(formInfo.form, readForm)

    // редактируем объявление (меняем все, что можем) и сохраняем
    val formInfo2 = testFormGenerator.updateForm(formInfo.withForm(readForm), TestFormParams(now = now))
    //logFormDescription(card, form2)
    checkSuccessRequest(
      Put(s"/api/v1/offers/$category/$extUserId/$offerId?source=android")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo2.json))
    )

    val readForm2 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    checkForm(hidden, formInfo2, readForm2, category)

    // проверяем сохранение истории цен
    assert(readForm2.getPriceHistoryCount == 2)
    assert(readForm2.getPriceHistory(0).selectPrice == formInfo.form.getPriceInfo.selectPrice)
    assert(readForm2.getPriceHistory(0).getCurrency == formInfo.form.getPriceInfo.getCurrency)
    assert(readForm2.getPriceHistory(0).getCreateTimestamp > 0)
    assert(readForm2.getPriceHistory(1).selectPrice == formInfo2.form.getPriceInfo.selectPrice)
    assert(readForm2.getPriceHistory(1).getCurrency == formInfo2.form.getPriceInfo.getCurrency)
    assert(readForm2.getPriceHistory(1).getCreateTimestamp > 0)

    // Проверяем сохранение скидок
    checkDiscounts(formInfo2.form, readForm2)

    // удаляем, чтобы не мешать другим тестам
    deleteEverywhere(offerId, category)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"ip save (inVos = false, category = $category)") {
      // сохраняем новое объявление, передав айпишник. Айпишник должен сохраниться в оффере
      val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false, now = now))
      val extUserId: String = formInfo.form.getUserRef
      val userRef = ExternalAutoruUserRef.fromExt(extUserId).value
      val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withHeaders(RawHeader("X-UserIp", "223.227.26.179"))
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
      val offerId = checkSuccessRequest(req)
      val offerRef: OfferRef = OfferRef.ref(userRef, offerId)
      val ip = getIp(offerRef, category).value
      assert(ip == "223.227.26.179")

      // обновляем объявление, передав другой айпишник.
      // Айпишник должен НЕ поменяться в оффере, он ставится только при сохранении
      val formInfo2 = testFormGenerator.updateForm(formInfo, TestFormParams(now = now))
      val req2 = Put(s"/api/v1/offers/$category/$extUserId/$offerId")
        .withHeaders(RawHeader("X-UserIp", "9.9.9.9"))
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo2.json))
      checkSuccessRequest(req2)
      val ip2 = getIp(offerRef, category).value
      assert(ip2 == "223.227.26.179")

      // удаляем, чтобы не мешать другим тестам
      deleteEverywhere(offerId, category)
    }
  }

  {
    test(s"source platform save (invos = false)") {
      // сохраняем новое объявление, передав источник. Источник должен сохраниться
      implicit val formInfo = testFormGenerator.createForm("cars", TestFormParams(isDealer = false, now = now))
      implicit val extUserId: String = formInfo.form.getUserRef
      implicit val userRef = ExternalAutoruUserRef.fromExt(extUserId).value

      val sourceInfoBuilder = SourceInfo
        .newBuilder()
        .setUserRef(userRef.toPlain)
        .setPlatform(Platform.DESKTOP)
        .setSource(Source.HSD)

      checkSource("hsd", sourceInfoBuilder.setPlatform(Platform.PARTNER).setSource(Source.HSD).build())
      checkSource("auto24", sourceInfoBuilder.setPlatform(Platform.PARTNER).setSource(Source.AUTO24).build())
      checkSource("desktop", sourceInfoBuilder.setPlatform(Platform.DESKTOP).setSource(Source.AUTO_RU).build())
      checkSource("mobile", sourceInfoBuilder.setPlatform(Platform.MOBILE).setSource(Source.AUTO_RU).build())
      checkSource("ios", sourceInfoBuilder.setPlatform(Platform.IOS).setSource(Source.AUTO_RU).build())
      checkSource("android", sourceInfoBuilder.setPlatform(Platform.ANDROID).setSource(Source.AUTO_RU).build())
      checkSource("any-api-user", sourceInfoBuilder.setPlatform(Platform.PARTNER).setSource(Source.AUTO_RU).build())
    }
  }

  private def checkSource(
      source: String,
      expectedSource: SourceInfo
  )(implicit extUserId: String, formInfo: FormInfo, userRef: UserRef): Unit = {
    val req = Post(s"/api/v1/offers/cars/$extUserId?insert_new=1&source=$source")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    val offerRef: OfferRef = OfferRef.ref(userRef, offerId)
    val sourceInOffer = getSourcePlatform(offerRef).value
    assert(sourceInOffer.getPlatform == expectedSource.getPlatform)
    assert(sourceInOffer.getSource == expectedSource.getSource)
    assert(sourceInOffer.getUserRef == expectedSource.getUserRef)
    // удаляем, чтобы не мешать другим тестам
    deleteEverywhere(offerId, "cars")
  }

  test("tags") {
    // проверяем сохранение тегов
    val category = "cars"
    val formInfo: FormInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true, now = now))
    val formBuilder: Offer.Builder = formInfo.form.toBuilder
    // при создании нового объявления теги игнорируются, на запись их не принимаем
    formBuilder.clearTags().addTags("tag1").addTags("tag1")
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    val offer = checkSuccessRequestWithOffer(req)
    assert(offer.getTagsCount == 0)
    val offerId = offer.getId

    val tagForm1: String = "a,b,c"
    val tagForm2: String = "d,e,f"
    val tagForm3: String = "g,h"
    val tagForm4: String = "d,e"
    val tagForm5: String = "i,i"

    // проверим ошибки
    checkErrorRequest(
      Delete(s"/api/v1/offer/$category/100500-hash/tags/$tagForm1"),
      StatusCodes.NotFound,
      unknownOfferError
    )
    checkErrorRequest(
      Delete(s"/api/v1/offer/$category/user:11/$offerId/tags/$tagForm1"),
      StatusCodes.NotFound,
      unknownOfferError
    )
    checkErrorRequest(
      Put(s"/api/v1/offer/$category/100500-hash/tags/$tagForm1"),
      StatusCodes.NotFound,
      unknownOfferError
    )
    checkErrorRequest(
      Put(s"/api/v1/offer/$category/user:11/$offerId/tags/$tagForm1"),
      StatusCodes.NotFound,
      unknownOfferError
    )
    checkErrorRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/tags/2+2"),
      StatusCodes.BadRequest,
      invalidTagsError
    )
    checkErrorRequest(
      Delete(s"/api/v1/offer/$category/$extUserId/$offerId/tags/5*7"),
      StatusCodes.BadRequest,
      invalidTagsError
    )

    // добавляем теги
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/$category/$extUserId/$offerId/tags/$tagForm1"))

    // читаем форму и проверяем, что наши теги на месте
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "a",
        "b",
        "c"
      )
    )

    // удаляем теги
    checkSimpleSuccessRequest(Delete(s"/api/v1/offer/$category/$extUserId/$offerId/tags/$tagForm1"))

    // вставляем новые
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/$category/$offerId/tags/$tagForm2"))

    // читаем форму и проверяем, что наши теги на месте
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "d",
        "e",
        "f"
      )
    )

    // добавляем еще
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/$category/$extUserId/$offerId/tags/$tagForm3"))

    // читаем форму и проверяем, что наши теги на месте
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "d",
        "e",
        "f",
        "g",
        "h"
      )
    )

    // удаляем часть тегов
    checkSimpleSuccessRequest(Delete(s"/api/v1/offer/$category/$offerId/tags/$tagForm4"))

    // читаем форму и проверяем, что наши теги на месте
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "f",
        "g",
        "h"
      )
    )

    // добавляем дублированный тег
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/$category/$extUserId/$offerId/tags/$tagForm5"))

    // читаем форму и проверяем, что наши теги на месте и ничего не задублировалось
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "f",
        "g",
        "h",
        "i"
      )
    )

    // еще разок добавляем дублированный тег
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/$category/$extUserId/$offerId/tags/$tagForm5"))

    // еще раз читаем форму и проверяем, что наши теги на месте и ничего не задублировалось
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "f",
        "g",
        "h",
        "i"
      )
    )

    // апдейтим что-то еще в форме и удаляем теги. проверяем, что теги не пропали
    val b = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).toBuilder
    val mileage = b.getState.getMileage
    val newMileage = math.min(mileage + 100000, 999999)
    b.clearTags().getStateBuilder.setMileage(newMileage)
    checkSuccessRequest(
      Put(s"/api/v1/offers/$category/$extUserId/$offerId")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(b.build())))
    )

    val form: Offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(form.getTagsList.asScala.toList == List("f", "g", "h", "i"))
    assert(form.getState.getMileage == newMileage)

    // вставляем другие теги - не должны вставиться, прежние должны сохраниться
    b.addAllTags(Seq("x", "y", "z").asJava)
    checkSuccessRequest(
      Put(s"/api/v1/offers/$category/$extUserId/$offerId")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(b.build())))
    )
    assert(
      checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).getTagsList.asScala.toList == List(
        "f",
        "g",
        "h",
        "i"
      )
    )
  }

  test("404 on update request if offer is unknown") {
    val formInfo = testFormGenerator.createForm("cars", TestFormParams(isDealer = false, now = now))
    val extUserId: String = formInfo.form.getUserRef
    val req2 = Put(s"/api/v1/offers/cars/$extUserId/100500-hash")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    checkErrorRequest(req2, StatusCodes.NotFound, unknownOfferError)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
  } {
    test(s"services save (invos = false, category = $category, useCategoryInUrl = $useCategoryInUrl)") {
      // сохраняем объявление
      val form = testFormGenerator
        .createForm(category, TestFormParams(isDealer = false, now = now))
        .form
        .toBuilder
        .clearBadges()
        .clearServices()
        .build()
      val extUserId: String = form.getUserRef
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
      )

      // /services handler should work correctly with both all and explicit cars/moto/trucks category
      val categoryInUrl = if (useCategoryInUrl) category else "all"

      // добавляем неактивный сервис
      val req1 = createAddServicesRequest("all_sale_color")
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services?source=ios")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req1)))
      )

      // читаем объявление - сервис должен быть
      checkAddedServices(
        req1,
        checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")),
        servicesCount = 0,
        serviceDatesLikeOnForm = true
      )

      // редактируем этот сервис - он должен отредактироваться
      val req2 = createAddServicesRequest("all_sale_color", active = true)
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req2)))
      )

      // читаем объявление - сервис должен отредактироваться и стать активным
      checkAddedServices(
        req2,
        checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")),
        servicesCount = 1,
        serviceDatesLikeOnForm = true
      )

      // пробуем снова его обновить - сервис обновляется ещё раз
      val req3 = createAddServicesRequest("all_sale_color", active = true)
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req3)))
      )

      // читаем объявление - сервис должен измениться
      checkAddedServices(req2, checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")), servicesCount = 1)

      // если пользователь дилер - возвращаем ошибку
      val form2 = testFormGenerator
        .createForm(category, TestFormParams(isDealer = true, now = now))
        .form
        .toBuilder
        .clearBadges()
        .clearServices()
        .build()
      val extUserId2: String = form2.getUserRef
      val offerId2 = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId2?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form2)))
      )

      checkErrorRequest(
        Put(s"/api/v1/offer/$category/$extUserId2/$offerId2/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req1))),
        StatusCodes.BadRequest,
        unsupportedUserType(s"Unexpected user type: $extUserId2")
      )

      // если неизвестный тип сервиса - возвращаем ошибку
      val req4 = createAddServicesRequest("all_sale_something", active = true)
      checkErrorRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req4))),
        StatusCodes.BadRequest,
        unsupportedServiceType(s"Unexpected service type: all_sale_something")
      )

      // если сервис активный, но протух, то обновляем
      val req6 = createAddServicesRequest("all_sale_toplist", active = true, expired = true)
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req6)))
      )

      val req7 = createAddServicesRequest("all_sale_toplist", active = true, expired = false)
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req7)))
      )

      checkAddedServices(req7, checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")), servicesCount = 2)

      // если сервис не прислал даты - дата создания равна текущей, дата истечения - плюс месяц
      val req8 = createAddServicesRequest("all_sale_fresh", active = true, absentDays = true)
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req8)))
      )

      checkAddedServices(req8, checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")), servicesCount = 3)

      // если приходит запрос от hsd или auto24, а объявление создано не ими - ничего не меняем
      val req9 = createAddServicesRequest("all_sale_fresh", active = true, absentDays = false)
      checkSimpleSuccessRequest(
        Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services?source=hsd")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req9)))
      )

      checkAddedServices(req8, checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")), servicesCount = 3)
    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
  } test(
    "should return 200 and activate offer on adding placement for removed offer " +
      s"(category = $category, invos = false, useCategoryInUrl = $useCategoryInUrl)"
  ) {

    val form = testFormGenerator
      .createForm(category, TestFormParams(isDealer = false, now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId: String = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    checkSimpleSuccessRequest(Delete(s"/api/v1/offer/$category/$extUserId/$offerId"))
    val req = createAddServicesRequest("all_sale_add", active = true)
    val categoryInUrl = if (useCategoryInUrl) category else "all"
    val request =
      Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req)))
    checkSimpleSuccessRequest(request)
    checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
  } test(
    "should return 404 on adding non-placement for removed offer " +
      s"(category = $category, invos = false, useCategoryInUrl = $useCategoryInUrl)"
  ) {

    val form = testFormGenerator
      .createForm(category, TestFormParams(isDealer = false, now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId: String = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    checkSimpleSuccessRequest(Delete(s"/api/v1/offer/$category/$extUserId/$offerId"))
    val req = createAddServicesRequest("all_sale_premium", active = true)
    val categoryInUrl = if (useCategoryInUrl) category else "all"
    val request =
      Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req)))
    checkErrorRequest(request, StatusCodes.NotFound, unknownOfferError)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
  } test(
    s"should not activate inactive offer on all_sale_add with already existing active all_sale_add " +
      s"(category = $category, invos = false, useCategoryInUrl = $useCategoryInUrl)"
  ) {

    val form = testFormGenerator
      .createForm(category, TestFormParams(now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId: String = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    val categoryInUrl = if (useCategoryInUrl) category else "all"
    val addServiceRequest =
      Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
        .withEntity(
          HttpEntity(
            ContentTypes.`application/json`,
            Protobuf.toJson(createAddServicesRequest("all_sale_add", active = true))
          )
        )
    checkSimpleSuccessRequest(addServiceRequest)
    checkSimpleSuccessRequest(Post(s"/api/v1/offer/$category/$extUserId/$offerId/hide"))
    checkSimpleSuccessRequest(addServiceRequest)
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(offer.getStatus == OfferStatus.INACTIVE)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    inVos <- Seq(false)
  } test(s"all_sale_fresh should update fresh_date (category = $category, invos = false)") {

    // сохраняем объявление
    val form = testFormGenerator
      .createForm(category, TestFormParams(isDealer = false, now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId: String = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )

    val offer0 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(offer0.getOfferAutoru.getFreshDate == 0)

    val req9 = createAddServicesRequest("all_sale_fresh", active = true, absentDays = false)

    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req9)))
    )

    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    withClue(req9.getServices(0).getService + ":" + offer.getOfferAutoru.getFreshDate) {
      req9.getServices(0).getCreateDate shouldBe offer.getOfferAutoru.getFreshDate +- 1000
    }

  }

  for {
    category <- Seq("cars", "trucks", "moto")
    inVos <- Seq(false)
  } test(s"should deactivate service (category = $category, invos = false)") {

    // создаём объявление
    val form = testFormGenerator
      .createForm(category, TestFormParams(now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    // тестируем на услуге fresh
    val service = "all_sale_fresh"
    // создаём активную услугу
    val activateRequest = createAddServicesRequest("all_sale_fresh", active = true)
    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(activateRequest)))
    )
    // деактивируем услугу
    val deactivateRequest = createAddServicesRequest("all_sale_fresh")
    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(deactivateRequest)))
    )
    // проверяем, что услуга неактивна
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(offer.getServicesList.asScala.forall(s => s.getService != service))
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    inVos <- Seq(false)
  } test(s"should not remove not passed services (category = $category, invos = false)") {

    // создаём объявление
    val form = testFormGenerator
      .createForm(category, TestFormParams(now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    // первую услугу передадим в ручку дважды, вторую только первый раз
    val servicePassedTwice = "all_sale_fresh"
    val servicePassedOnce = "all_sale_premium"
    val servicePassedTwiceRequest = AddServiceRequest(servicePassedTwice, active = true)
    // создаём две активных услуги
    val firstRequest = createMultipleAddServicesRequests(
      servicePassedTwiceRequest,
      AddServiceRequest(servicePassedOnce, active = true)
    )
    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(firstRequest)))
    )
    // делаем ещё один запрос, передаём только одну услугу
    val secondRequest = createMultipleAddServicesRequests(servicePassedTwiceRequest)
    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(secondRequest)))
    )
    // проверяем, что обе услуги есть в списке и активны, несмотря на то, что второй раз передали только одну
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    val services = offer.getServicesList.asScala
    assert {
      services.exists { s =>
        s.getService == servicePassedTwice && s.getIsActive
      }
    }
    assert {
      services.exists { s =>
        s.getService == servicePassedOnce && s.getIsActive
      }
    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
    inVos <- Seq(false)
  } test(
    s"should activate offer on active placement " +
      s"(category = $category, useCategoryInUrl = $useCategoryInUrl, invos = false)"
  ) {

    // создаём объявление
    val form = testFormGenerator
      .createForm(category, TestFormParams(now = now, hidden = true))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId = form.getUserRef

    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    // создаём активную услугу размещения
    val service = "all_sale_activate"
    val request = createAddServicesRequest(service, active = true)
    val categoryInUrl = if (useCategoryInUrl) category else "all"

    checkSimpleSuccessRequest(
      Put(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(request)))
    )

    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(offer.getStatus == OfferStatus.ACTIVE)

    // у оффера и услуги размещения должен быть одинаковый expire_date
    val offerExpireDate = components.offersReader
      .findOffer(
        optUserRef = None,
        offerId,
        optCategory = Some(Category.valueOf(category.toUpperCase)),
        includeRemoved = false,
        operateOnMaster = false
      )
      .value
      .getTimestampWillExpire
    val s = offer.getServices(0)
    withClue(s"unexpected service ${s.getService} expire date") {
      offerExpireDate shouldBe s.getExpireDate +- 1000
    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
    isDealer <- Seq(true, false)
  } test(
    s"should hide offer " +
      s"(category = $category, useCategoryInUrl = $useCategoryInUrl, invos = false, isDealer = $isDealer)"
  ) {

    // создаём объявление
    val form = testFormGenerator
      .createForm(category, TestFormParams(now = now, isDealer = isDealer, hidden = true))
      .form
      .toBuilder
      .build()
    val extUserId = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    val categoryInUrl = if (useCategoryInUrl) category else "all"
    checkSimpleSuccessRequest(Post(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId/hide"))
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(offer.getStatus == OfferStatus.INACTIVE)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    useCategoryInUrl <- Seq(true, false)
    isDealer <- Seq(true, false)
  } test(
    s"should archive offer " +
      s"(category = $category, useCategoryInUrl = $useCategoryInUrl, invos = false, isDealer = $isDealer)"
  ) {

    // создаём объявление
    val form = testFormGenerator
      .createForm(category, TestFormParams(now = now, isDealer = isDealer, hidden = true))
      .form
      .toBuilder
      .build()
    val extUserId = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    val categoryInUrl = if (useCategoryInUrl) category else "all"
    checkSimpleSuccessRequest(Delete(s"/api/v1/offer/$categoryInUrl/$extUserId/$offerId"))
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId?include_removed=1"))
    assert(offer.getStatus == OfferStatus.REMOVED)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"noPhoneOwnerCheck (invos = false, category = $category)") {
      pending

      // создаем объявление
      val formBuilder =
        testFormGenerator.createForm(category, TestFormParams(isDealer = false, now = now)).form.toBuilder
      // меняем в нем телефон
      formBuilder.getSellerBuilder
        .clearPhones()
        .addPhones(ApiOfferModel.Phone.newBuilder().setPhone("89265769321").setCallHourStart(9).setCallHourEnd(23))
      val form = formBuilder.build()
      val extUserId: String = form.getUserRef
      // пытаемся сохранить, не передав параметр
      checkValidationErrorRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form))),
        validationError(NotownPhoneUser)
      )
      // передаем параметр и снова пытаемся сохранить. Должен быть успех
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&no_phone_owner_check=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
      )
      // попробуем прочитать и проверить телефон
      val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
      assert(readForm.getSeller.getPhonesCount == 1)
      assert(readForm.getSeller.getPhones(0).getPhone == "79265769321")
      assert(readForm.getSeller.getPhones(0).getCallHourStart == 9)
      assert(readForm.getSeller.getPhones(0).getCallHourEnd == 23)
      // попробуем апдейтнуть форму
      val mileage = readForm.getState.getMileage
      val newMileage = math.min(mileage + 56000, 999999)
      formBuilder.getStateBuilder.setMileage(newMileage)
      val form2 = formBuilder.build()
      // пытаемся обновить, не передав параметр
      checkValidationErrorRequest(
        Put(s"/api/v1/offers/$category/$extUserId/$offerId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form2))),
        validationError(NotownPhoneUser)
      )
      // передаем параметр и снова пытаемся обновить. Должен быть успех
      checkSuccessRequest(
        Put(s"/api/v1/offers/$category/$extUserId/$offerId?insert_new=1&no_phone_owner_check=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form2)))
      )

    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"price history size (vos = false, category = $category)") {
      pending
      // создаем новое объявление
      val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false, now = now))
      val extUserId: String = formInfo.form.getUserRef

      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
      )

      // 20 раз меняем цену
      val formBuilder = formInfo.form.toBuilder
      (1501 to 1520).foreach(price => {
        formBuilder.getPriceInfoBuilder.setPrice(price)
        checkSuccessRequest(
          Put(s"/api/v1/offers/$category/$extUserId/$offerId")
            .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
        )
      })

      // должно остаться 10 последних цен
      val form = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))
      assert(form.getPriceHistoryCount == 10)
      assert(form.getPriceHistoryList.asScala.map(_.selectPrice) == (1511 to 1520))

    }
  }

  test(s"production year less than minimal (category = cars)") {
    testProductionYearLessThanMinimal(
      "cars",
      WrongProductionYearLt(1890, 1700),
      WrongModificationProductionYear(1700, 0, None)
    )
  }

  for {
    category <- Seq("trucks", "moto")
  } {
    test(s"production year less than minimal (category = $category)") {
      testProductionYearLessThanMinimal(category, WrongProductionYearLt(1890, 1700))
    }
  }

  private def testProductionYearLessThanMinimal(category: String, errors: ValidationError*): Unit = {
    val formInfo = testFormGenerator.createForm(category, TestFormParams())
    val formBuilder = formInfo.form.toBuilder
    val productionYear = 1700
    formBuilder.getDocumentsBuilder.setYear(productionYear)
    val form = formBuilder.build()

    val extUserId = formInfo.extUserId
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))

    checkValidationErrorRequest(req, validationError(errors: _*))
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"address length (category = $category)") {
      val maxAddressLength = 255
      val formInfo = testFormGenerator.createForm(category, TestFormParams())
      val formBuilder = formInfo.form.toBuilder
      val address = "A" * 256
      formBuilder.getSellerBuilder.getLocationBuilder.setAddress(address)
      val form = formBuilder.build()
      val extUserId = formInfo.extUserId
      val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
      //checkSuccessRequest(req)
      checkValidationErrorRequest(req, validationError(WrongAddressLengthGt(maxAddressLength)))
    }
  }

  test("REAR_DRIVE, FORWAR_CONTROL") {
    // https://st.yandex-team.ru/AUTORUAPI-3064
    // создаем объявление, привод указываем REAR_DRIVE. После публикации должно возвращаться значение REAR_DRIVE
    val category = "cars"

    {
      val formInfo = testFormGenerator.createForm(category, TestFormParams(gearType = "REAR_DRIVE"))
      val formBuilder = formInfo.form.toBuilder
      formBuilder.getCarInfoBuilder.setDrive("REAR_DRIVE")
      val extUserId = formInfo.extUserId
      val offer = checkSuccessRequestWithOffer(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
      )
      assert(offer.getCarInfo.getDrive == "REAR_DRIVE")
    }

    {
      val formInfo = testFormGenerator.createForm(category, TestFormParams(gearType = "REAR_DRIVE"))
      val extUserId = formInfo.extUserId
      val formBuilder = formInfo.form.toBuilder
      formBuilder.getCarInfoBuilder.setDrive("REAR")
      val offer = checkSuccessRequestWithOffer(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
      )
      assert(offer.getCarInfo.getDrive == "REAR_DRIVE")
    }

    {
      val formInfo = testFormGenerator.createForm(category, TestFormParams(gearType = "FORWARD_CONTROL"))
      val extUserId = formInfo.extUserId
      val formBuilder = formInfo.form.toBuilder
      formBuilder.getCarInfoBuilder.setDrive("FORWARD_CONTROL")
      val offer = checkSuccessRequestWithOffer(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
      )
      assert(offer.getCarInfo.getDrive == "FORWARD_CONTROL")
    }

    {
      val formInfo = testFormGenerator.createForm(category, TestFormParams(gearType = "FORWARD_CONTROL"))
      val extUserId = formInfo.extUserId
      val formBuilder = formInfo.form.toBuilder
      formBuilder.getCarInfoBuilder.setDrive("FRONT")
      val offer = checkSuccessRequestWithOffer(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
      )
      assert(offer.getCarInfo.getDrive == "FORWARD_CONTROL")
    }
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"phones deleted from user (category = $category)") {
    // пользователь разместил объявление, а потом изменил/удалил телефон в своем личном кабинете.
    // После этого не получается отредактировать объявление, пишет: телефон не принадлежит пользователю.
    val randomUserRef = testFormGenerator.randomOwnerIds(isDealer = false)._1

    val formInfo =
      testFormGenerator.createForm(category, TestFormParams(isDealer = false, optOwnerId = Some(randomUserRef)))
    val user = formInfo.optUser.value
    val formBuilder = formInfo.form.toBuilder
    val extUserId = formInfo.extUserId
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )

    val offerId = offer.getId
    // сносим у пользователя телефон
    val phoneNumber = offer.getSeller.getPhones(0).getOriginal.toLong
    val userPhone = user.phones.find(_.phone == phoneNumber).value
    val phoneId = userPhone.id
    components.oldOfficeDatabase.master.jdbc.update("delete from users.phone_numbers where id = ?", Long.box(phoneId))

    // пробуем отредактировать до миграции
    {
      // читаем объявление из базы
      val readFormBuilder = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).toBuilder
      val mileage = readFormBuilder.getState.getMileage
      val newMileage = math.min(mileage + 100500, 999999)
      readFormBuilder.getStateBuilder.setMileage(newMileage)
      // телефон должен просто пропасть из выдачи. Не можем отредактировать по причине: телефон  не принадлежит пользователю
      //
      checkValidationErrorRequest(
        Put(s"/api/v1/offers/$category/$extUserId/$offerId")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(readFormBuilder.build()))),
        validationError(NotownPhoneUser)
      )
    }

    {
      migrateOfferToVos(offerId, category)

      // читаем объявление из базы
      val readFormBuilder = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId")).toBuilder
      val mileage = readFormBuilder.getState.getMileage
      val newMileage = math.min(mileage + 100500, 999999)
      readFormBuilder.getStateBuilder.setMileage(newMileage)

      // пробуем отредактировать после миграции
      checkValidationErrorRequest(
        Put(s"/api/v1/offers/$category/$extUserId/$offerId")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(readFormBuilder.build()))),
        validationError(NotexistPhone)
      )
    }

    // возвращаем телефон
    import ru.yandex.vos2.autoru.dao.old.utils.PreparedStatementUtils._
    val isMain: Int = if (userPhone.isMain) 1 else 0
    components.oldOfficeDatabase.master.jdbc.update(
      "insert into users.phone_numbers (id, user_id, number, phone, status, is_main, code, create_date, update_date) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
      userPhone.id.anyRef,
      userPhone.userId.anyRef,
      userPhone.number.anyRef,
      userPhone.phone.anyRef,
      userPhone.status.anyRef,
      isMain.anyRef,
      userPhone.code.anyRef,
      userPhone.created.anyRef,
      userPhone.updated.anyRef
    )
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"emoji in description in old db (category = $category)") {
    // пробуем сохранить объявление с emoji в описании и потом читаем. Сохранение должно быть успешным,
    // emoji должно быть заменено в описании на вопросик
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val formBuilder = formInfo.form.toBuilder

    val emoji = "T\uD83D\uDE06"

    formBuilder.setDescription(emoji)
    val extUserId = formInfo.extUserId
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    val offerId = offer.getId
    // читаем объявление из базы
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    assert(readForm.getDescription == emoji)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"location save (invos = false, category = $category)") {

    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId = formInfo.extUserId

    // если source не передан, то источник вообще не сохраняем
    {
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))
          .withHeaders(RawHeader("X-User-Location", UserLocation(5, 6, 7).toString))
      )
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
      assert(!readOffer.getOfferAutoru.hasSourceInfo)
    }

    // а если передан, то сохраняем
    {
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))
          .withHeaders(RawHeader("X-User-Location", UserLocation(5, 6, 7).toString))
      )
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLatitude == 5)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLongitude == 6)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getAccuracy == 7)
    }

    // нули можно передать
    {
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formInfo.form)))
          .withHeaders(RawHeader("X-User-Location", UserLocation(0, 0, 7).toString))
      )
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLatitude == 0)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getLongitude == 0)
      assert(readOffer.getOfferAutoru.getSourceInfo.getPlacement.getAccuracy == 7)
    }

  }

  test("truck_info.body_type should not be defined for TRUCK_CAT_ARTIC") {
    // https://st.yandex-team.ru/VOS-1913
    val category = "trucks"
    // делаем объявление в категории ARTIC
    val formInfo = testFormGenerator.truckTestForms.createForm(
      TestFormParams[TruckFormInfo](
        isDealer = false,
        optCard = components.trucksCatalog.getCardByMarkModel("iveco", "stralis_tractor")
      )
    )

    // неверно заполняем TruckType (для ARTIC не заполняется!)
    val builder = formInfo.form.toBuilder
    builder.getTruckInfoBuilder.setTruckType(TrucksModel.Truck.BodyType.AUTOTRANSPORTER)

    // сохраняем
    val extUserId = formInfo.extUserId
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(builder.build())))
    )
    val offerId = offer.getId
    val autoruOfferId = AutoruOfferID.parse(offerId)

    // категория комтранса нужная
    assert(offer.getTruckInfo.getTruckCategory == TruckCategory.ARTIC)

    // ничего не сохранилось - маппинг не прошел
    {
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
      assert(readOffer.getOfferAutoru.getTruckInfo.getBodyType == TruckInfo.BodyType.BODY_TYPE_UNKNOWN)
      val readOffer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto?fromvos=1"))
      assert(readOffer2.getOfferAutoru.getTruckInfo.getBodyType == TruckInfo.BodyType.BODY_TYPE_UNKNOWN)
    }

    // сохраняем руками в базе
    components.oldOfficeDatabase.master.jdbc
      .update("update all.sale3 set body_key = ? where id = ?", Long.box(857), Long.box(autoruOfferId.id))
    components.getOfferDao().useRef(OfferRef.ref(formInfo.userRef, offerId)) { o =>
      val b = o.toBuilder
      b.getOfferAutoruBuilder.getTruckInfoBuilder.setBodyType(TruckInfo.BodyType.AUTOTRANSPORTER)
      OfferUpdate.visitNow(b.build())
    }

    // в базе лежит левый body_type
    {
      val readOffer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
      assert(readOffer.getOfferAutoru.getTruckInfo.getBodyType == TruckInfo.BodyType.AUTOTRANSPORTER)
      val readOffer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto?fromvos=1"))
      assert(readOffer2.getOfferAutoru.getTruckInfo.getBodyType == TruckInfo.BodyType.AUTOTRANSPORTER)
    }

    // при чтении объявления как формы ничего не заполнено
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId"))
    assert(readForm.getTruckInfo.getTruckType == TrucksModel.Truck.BodyType.BODY_TYPE_UNKNOWN)
    assert(readForm.getTruckInfo.getLightTruckType == TrucksModel.LightTruck.BodyType.BODY_TYPE_UNKNOWN)

    // успешно делаем из оффера черновик
    val draftId = checkSuccessRequest(Post(s"/api/v1/offer/$category/$extUserId/$offerId/draft"))

    // внутри черновика значение body_type заполнено
    val readDraft = checkSuccessOfferRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId/proto"))
    assert(readDraft.getOfferAutoru.getTruckInfo.getBodyType == TruckInfo.BodyType.AUTOTRANSPORTER)

    // при чтении черновика как формы ничего не заполнено
    val readDraftForm = checkSuccessReadRequest(Get(s"/api/v1/draft/$category/$extUserId/$draftId"))
    assert(readDraftForm.getTruckInfo.getTruckType == TrucksModel.Truck.BodyType.BODY_TYPE_UNKNOWN)
    assert(readDraftForm.getTruckInfo.getLightTruckType == TrucksModel.LightTruck.BodyType.BODY_TYPE_UNKNOWN)

    // публикуем черновик
    checkSuccessRequestWithOffer(Post(s"/api/v1/draft/$category/$draftId/publish/$extUserId"))

    // теперь в объявлении нет body_type
    val readOffer2 = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$extUserId/$offerId/proto"))
    assert(readOffer2.getOfferAutoru.getTruckInfo.getBodyType == TruckInfo.BodyType.BODY_TYPE_UNKNOWN)
  }

  test("case-insensitive mark and model codes conversion in moto and comtrans") {
    // сохраняем и редактируем обьявления с кодами марки и модели в lowercase
    {
      val formInfo = testFormGenerator.truckTestForms.createForm(TestFormParams(isDealer = false))
      val builder = formInfo.form.toBuilder
      builder.getTruckInfoBuilder.setMark(formInfo.form.getTruckInfo.getMark.toLowerCase())
      builder.getTruckInfoBuilder.setModel(formInfo.form.getTruckInfo.getModel.toLowerCase())
      val extUserId = formInfo.extUserId
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/trucks/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(builder.build())))
      )
      val mileage = builder.getState.getMileage
      val newMileage = math.min(mileage + 100000, 999999)
      builder.getStateBuilder.setMileage(newMileage)
      val offer = checkSuccessRequestWithOffer(
        Put(s"/api/v1/offers/trucks/$extUserId/$offerId")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(builder.build())))
      )
      assert(offer.getState.getMileage == newMileage)
    }

    {
      val formInfo = testFormGenerator.motoTestForms.createForm(TestFormParams(isDealer = false))
      val builder = formInfo.form.toBuilder
      builder.getMotoInfoBuilder.setMark(formInfo.form.getMotoInfo.getMark.toLowerCase())
      builder.getMotoInfoBuilder.setModel(formInfo.form.getMotoInfo.getModel.toLowerCase())
      val extUserId = formInfo.extUserId
      val offerId = checkSuccessRequest(
        Post(s"/api/v1/offers/moto/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(builder.build())))
      )
      val mileage = builder.getState.getMileage
      val newMileage = math.min(mileage + 100000, 999999)
      builder.getStateBuilder.setMileage(newMileage)
      val offer = checkSuccessRequestWithOffer(
        Put(s"/api/v1/offers/moto/$extUserId/$offerId")
          .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(builder.build())))
      )
      assert(offer.getState.getMileage == newMileage)
    }
  }

  test(s"update car techParamId (invos = false)") {

    // https://st.yandex-team.ru/AUTORUAPI-3292 должна быть возможность изменить техпарам
    // создаем объявление, а потом меняем в нем техпарам на другой, но от той же марки-модели
    val category = "cars"
    val formInfo: FormInfo = testFormGenerator.createForm(
      category,
      TestFormParams[CarFormInfo](
        isDealer = false,
        now = now,
        optCard = components.carsCatalog.getCardByTechParamId(20251425)
      )
    )
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    val readOffer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))

    assert(readOffer.getCarInfo.getTechParamId == 20251425)

    val formInfo2 = {
      val techParamId = 20251431
      val user = formInfo.optUser.get
      val card = components.carsCatalog.getCardByTechParamId(techParamId).get
      val builder = formInfo.form.toBuilder
      builder.getCarInfoBuilder.setTechParamId(techParamId)
      val form = builder.build()
      FormInfo.privateForm(user, form, card)
    }
    val req2 = Put(s"/api/v1/offers/$category/$extUserId/$offerId")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo2.json))
    checkSuccessRequest(req2)
    val readOffer2 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))

    assert(readOffer2.getCarInfo.getTechParamId == 20251431)

  }

  test("do not throw error if failed to insert to vos during saving to old db") {
    // из-за проблем с каталогом не можем сохранить в vos, но это не мешает сохранить в старую базу и отдать успех
    // с проблемами сохранения в vos дальше будет разбираться шедулер
    val category = "cars"
    // берем техпарам, которого нет в базе autoruCatalog (catalog7_yandex)
    val techParamId: Long = 20704049
    assert(!testFormGenerator.carTestForms.techParamIds.all.contains(techParamId))
    val card: CarCard = components.carsCatalog.getCardByTechParamId(techParamId).value
    val formInfo: FormInfo = testFormGenerator.createForm(
      category,
      TestFormParams[CarFormInfo](isDealer = false, now = now, optCard = Some(card))
    )
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    assert(
      components.offersReader
        .findOffer(None, offerId, Some(Category.CARS), includeRemoved = false, operateOnMaster = true)
        .nonEmpty
    )
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(offer.getOfferAutoru.getConversionErrorCount == 0)
    // также можем успешно поредактировать
    checkSuccessRequest(
      Put(s"/api/v1/offers/$category/$extUserId/$offerId")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    )
  }

  test("throw error for unacceptable contact/address change") {
    val category = "cars"
    // сохраняем объявление
    val formInfo =
      testFormGenerator.createForm(category, TestFormParams(isDealer = true, optOwnerId = Some(24813), now = now))
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSellerBuilder
      .setLocation(
        Location
          .newBuilder()
          .setAddress("улица Академика Анохина, 6к5")
          .setGeobaseId(213)
      )
      .setCustomLocation(true)
    formBuilder.getSellerBuilder.addPhones(Phone.newBuilder().setPhone("12345678901")).setCustomPhones(true)
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    val request = Post(s"/api/v1/offers/$category/$extUserId")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    checkValidationErrorRequest(
      request,
      validationError(ForbiddenSalonEditAddress, ForbiddenSalonEditPhones(Seq("12345678901"), Seq("78124263564")))
    )
  }

  test("allow contact/address change if permissions exist") {
    val category = "cars"
    val formInfo =
      testFormGenerator.createForm(category, TestFormParams(isDealer = true, optOwnerId = Some(10086), now = now))
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSellerBuilder
      .setLocation(
        Location
          .newBuilder()
          .setAddress("улица Академика Анохина, 6к5")
          .setGeobaseId(213)
      )
      .setCustomLocation(true)
    formBuilder.getSellerBuilder.addPhones(Phone.newBuilder().setPhone("12345678877")).setCustomPhones(true)
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    val request = Post(s"/api/v1/offers/$category/$extUserId")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    val offerId = checkSuccessRequest(request)
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(offer.getOfferAutoru.getConversionErrorCount == 0)
    assert(offer.getOfferAutoru.getSeller.getPlace.getGeobaseId == 213)
    assert(offer.getOfferAutoru.getSeller.getPhoneList.asScala.exists(_.getNumber == "12345678877"))
  }

  test("do not add conversion errors in case of private seller custom phone & contact") {
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false, now = now))
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSalonBuilder.setSalonId(1).setName("Test")
    formBuilder.getSellerBuilder
      .setLocation(
        Location
          .newBuilder()
          .setAddress("улица Академика Анохина, 6к5")
          .setGeobaseId(213)
      )
      .setCustomLocation(true)
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    val request = Post(s"/api/v1/offers/$category/$extUserId")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    val offerId = checkSuccessRequest(request)
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(offer.getOfferAutoru.getConversionErrorCount == 0)
    assert(offer.getOfferAutoru.getSeller.getPlace.getGeobaseId == 213)
  }

  test("with_nds in comtrans for dealer") {
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true, now = now))
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getPriceInfoBuilder.getWithNdsBuilder.setValue(true)
    val form = formBuilder.build()
    val extUserId: String = form.getUserRef
    val request = Post(s"/api/v1/offers/$category/$extUserId")
      .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    val offerId = checkSuccessRequest(request)
    val offer = checkSuccessOfferRequest(Get(s"/api/v1/offer/$category/$offerId/proto"))
    assert(offer.getOfferAutoru.getPrice.getWithNds)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    service <- Seq("refresh", "reset")
  } test(s"should reset counters start date when $service service is added in $category") {
    val form = testFormGenerator
      .createForm(category, TestFormParams(isDealer = false, now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId: String = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )

    val req = createAddServicesRequest(service, active = true)
    val createDate = req.getServices(0).getCreateDate
    val request =
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req)))
    checkSimpleSuccessRequest(request)
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))

    assert(offer.getAdditionalInfo.getCountersStartDate == createDate)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
    service <- Seq("refresh", "reset")
  } test(s"should leave counters start date untouched when $service is being deactivated in $category") {
    val form = testFormGenerator
      .createForm(category, TestFormParams(isDealer = false, now = now))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extUserId: String = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )

    val req = createAddServicesRequest(service, active = true)
    val request =
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(req)))
    checkSimpleSuccessRequest(request)
    val offer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    val createDate = offer.getAdditionalInfo.getCountersStartDate

    val newReq = createAddServicesRequest(service, active = false)
    val newRequest =
      Put(s"/api/v1/offer/$category/$extUserId/$offerId/services")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(newReq)))
    checkSimpleSuccessRequest(newRequest)

    val sameOffer = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    val sameCreateDate = sameOffer.getAdditionalInfo.getCountersStartDate

    assert(createDate == sameCreateDate)
  }

  test(s"do not update instead of insert of offer from different section") {
    val category = "cars"
    val categoryEnum: Category = testFormGenerator.categoryByString(category)
    // создаем новое объявление  о продаже легковой машины (insert_new по умолчанию = 1)
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true, now = now))
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    // меняем секцию
    val formInfo4 =
      testFormGenerator.updateForm(formInfo, TestFormParams(section = Section.NEW, sameGeobaseId = true, now = now))
    // пытаемся разместить как новое
    val offerId4 = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=0")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo4.json))
    )
    // действительно размещается новое
    assert(offerId != offerId4)
  }

  test(s"update instead of insert of deleted offers in old db") {
    val category = "cars"
    val categoryEnum: Category = testFormGenerator.categoryByString(category)
    // создаем объявление, сохраняем, меняем что-то и пробуем сохранить как новое
    // создаем новое объявление  о продаже легковой машины (insert_new по умолчанию = 1)
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true, now = now))
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    val readForm0 = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    readForm0.getStatus shouldBe OfferStatus.NEED_ACTIVATION
    // удаляем
    components.offersWriter.setArchive(offerId, None, Option(categoryEnum), archive = true, "", None)
    // в старой базе оно удаленное
    components.autoruSalesDao
      .getLightOffer(AutoruOfferID.parse(offerId).id)
      .value
      .status shouldBe AutoruSaleStatus.STATUS_DELETED_BY_USER

    val offerFromDb =
      components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    // что-то меняем
    val formInfo4 = testFormGenerator.updateForm(formInfo, TestFormParams(sameGeobaseId = true, now = now))
    // пытаемся разместить как новое
    val offerId4 = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extUserId?insert_new=0")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo4.json))
    )
    // на самом деле апдейтится старое
    assert(offerId == offerId4)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    // оно становится активным
    readForm.getStatus shouldBe OfferStatus.NEED_ACTIVATION
    assert(readForm.getPriceInfo.selectPrice == formInfo4.form.getPriceInfo.selectPrice) // обновили
    assert(readForm.getPriceInfo.selectPrice == formInfo.form.getPriceInfo.selectPrice + 1000) // обновили
    // и в старой базе оно активное
    components.autoruSalesDao
      .getLightOffer(AutoruOfferID.parse(offerId).id)
      .value
      .status shouldBe AutoruSaleStatus.STATUS_WAITING_ACTIVATION
  }

  for {
    isDealer <- Seq(false, true)
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"update instead of insert (invos = false, isDealer = $isDealer, category = $category)") {

      val categoryEnum: Category = testFormGenerator.categoryByString(category)
      // создаем объявление, сохраняем, меняем что-то и пробуем сохранить как новое
      // создаем новое объявление  о продаже легковой машины (insert_new пол умолчанию = 1)
      val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, now = now))
      val extUserId: String = formInfo.form.getUserRef
      val req = Post(s"/api/v1/offers/$category/$extUserId?source=mobile")
        .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))

      val offerId = checkSuccessRequest(req)
      components.offerVosDao.saveMigratedFromYdb(
        Seq(components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value)
      )(Traced.empty)

      // создаем еще новое объявление, но такое же как старое за мелким исключением
      val formInfo2 = testFormGenerator.updateForm(formInfo, TestFormParams(sameGeobaseId = true, now = now))
      // пытаемся разместить как новое
      withClue(formInfo.json) {
        withClue(formInfo2.json) {
          val offerId2 = checkSuccessRequest(
            Post(s"/api/v1/offers/$category/$extUserId?insert_new=0")
              .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo2.json))
          )
          // вместо этого старое оказалось проапдейчено
          assert(offerId == offerId2)
          val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
          assert(readForm.getPriceInfo.selectPrice == formInfo2.form.getPriceInfo.selectPrice) // обновили
          assert(readForm.getPriceInfo.selectPrice == formInfo.form.getPriceInfo.selectPrice + 1000) // обновили
        }
      }

      // проверяем параметр insert_new=1
      val formInfo3 = testFormGenerator.updateForm(formInfo2, TestFormParams(sameGeobaseId = true, now = now))
      components.offerVosDao.saveMigratedFromYdb(
        Seq(components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value)
      )(Traced.empty)
      // пытаемся разместить как новое
      val offerId3 = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=1")
          .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo3.json))
      )
      assert(offerId != offerId3)

      // удаленное учитывается - восстановим удаленное
      components.offersWriter.setArchive(offerId, None, Option(categoryEnum), archive = true, "", None)
      val formInfo4 = testFormGenerator.updateForm(formInfo2, TestFormParams(sameGeobaseId = true, now = now))

      components.offerVosDao.saveMigratedFromYdb(
        Seq(components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value)
      )(Traced.empty)
      // пытаемся разместить как новое
      val offerId4 = checkSuccessRequest(
        Post(s"/api/v1/offers/$category/$extUserId?insert_new=0")
          .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo4.json))
      )
      assert(offerId == offerId4)
      val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
      assert(readForm.getPriceInfo.selectPrice == formInfo4.form.getPriceInfo.selectPrice) // обновили
      assert(readForm.getPriceInfo.selectPrice == formInfo2.form.getPriceInfo.selectPrice + 1000) // обновили
    }
  }

  private def checkAddedServices(req: AddServicesRequest,
                                 form: Offer,
                                 servicesCount: Int,
                                 serviceDatesLikeOnForm: Boolean = false): Unit = {
    assert(form.getServicesCount == servicesCount)
    if (servicesCount > 0) {
      val service: PaidService = req.getServices(0)
      val formService = form.getServicesList.asScala.find(_.getService == service.getService).value
      if (serviceDatesLikeOnForm) {
        if (service.getCreateDate > 0) {
          formService.getCreateDate shouldBe service.getCreateDate +- 1000
        } else {
          formService.getCreateDate shouldBe getNow +- 1000
        }
        if (service.getExpireDate > 0) {
          formService.getExpireDate shouldBe service.getExpireDate +- 1000
        } else {
          val expectedExpireDate: Long = AutoruCommonLogic.plusOneMonth(DateTime.now()).getMillis
          formService.getExpireDate shouldBe expectedExpireDate +- 1000
        }
      }
      assert(formService.getIsActive == service.getIsActive)
    }
  }

  private def getIp(offerRef: OfferRef, category: String): Option[String] = {

    val autoruOfferID = AutoruOfferID.parse(offerRef.offerId)
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

  private def getSourcePlatform(offerRef: OfferRef): Option[SourceInfo] = {
    val autoruOfferID = AutoruOfferID.parse(offerRef.offerId)
    val sale: AutoruSale = components.autoruSalesDao.getOfferForMigration(autoruOfferID.id).value
    sale.sourceInfo
  }

  private def deleteEverywhere(offerId: String, category: String): Unit = {
    val autoruOfferId: AutoruOfferID = AutoruOfferID.parse(offerId)
    val categoryEnum: Category = testFormGenerator.categoryByString(category)
    components.getOfferDao().setArchive(autoruOfferId, None, Some(categoryEnum), archive = true, "", None)
    category match {
      case "cars" =>
        components.autoruSalesDao.setArchive(autoruOfferId.id, autoruOfferId.hash.getOrElse(""), None, archive = true)
      case "trucks" =>
        components.autoruTrucksDao.setArchive(
          autoruOfferId.id,
          autoruOfferId.hash.getOrElse(""),
          None,
          archive = true
        )
      case "moto" =>
        components.autoruMotoDao.setArchive(autoruOfferId.id, autoruOfferId.hash.getOrElse(""), None, archive = true)
      case _ =>
        sys.error(s"unexpected category $category")
    }
  }

  private def checkForm(hidden: Boolean, formInfo: FormInfo, readForm: Offer, category: String): Unit = {
    val needStatus: OfferStatus =
      if (hidden) OfferStatus.INACTIVE
      else OfferStatus.NEED_ACTIVATION
    formInfo match {
      case f: CarFormInfo =>
        FormTestUtils.performCarFormChecks(f, readForm, needStatus, None)
      case f: TruckFormInfo =>
        FormTestUtils.performTruckFormChecks(f, readForm, needStatus, None)
      case f: MotoFormInfo =>
        FormTestUtils.performMotoFormChecks(f, readForm, needStatus, None)
    }
  }

  private def checkDiscounts(expected: ApiOfferModel.Offer, actual: Offer): Unit = {
    (expected.getCategory, expected.getTruckInfo.getTruckCategory) match {
      case (Category.TRUCKS, TrucksModel.TruckCategory.LCV) =>
        assert(actual.getDiscountOptions.getCredit == expected.getDiscountOptions.getCredit)
        assert(actual.getDiscountOptions.getInsurance == expected.getDiscountOptions.getInsurance)
        assert(actual.getDiscountOptions.getTradein == expected.getDiscountOptions.getTradein)
        assert(actual.getDiscountOptions.getLeasingDiscount == expected.getDiscountOptions.getLeasingDiscount)
        assert(actual.getDiscountOptions.getMaxDiscount == expected.getDiscountOptions.getMaxDiscount)
      case (Category.TRUCKS, _) =>
        assert(actual.getDiscountOptions.getCredit == 0)
        assert(actual.getDiscountOptions.getInsurance == 0)
        assert(actual.getDiscountOptions.getTradein == 0)
        assert(actual.getDiscountOptions.getLeasingDiscount == expected.getDiscountOptions.getLeasingDiscount)
        assert(actual.getDiscountOptions.getMaxDiscount == expected.getDiscountOptions.getMaxDiscount)
      case (_, _) =>
        assert(actual.getDiscountOptions.getCredit == expected.getDiscountOptions.getCredit)
        assert(actual.getDiscountOptions.getInsurance == expected.getDiscountOptions.getInsurance)
        assert(actual.getDiscountOptions.getTradein == expected.getDiscountOptions.getTradein)
        assert(actual.getDiscountOptions.getMaxDiscount == expected.getDiscountOptions.getMaxDiscount)
        assert(actual.getDiscountOptions.getLeasingDiscount == 0)
    }
  }
}
