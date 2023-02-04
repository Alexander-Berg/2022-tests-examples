package ru.yandex.vos2.autoru.utils.converters.formolddb

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel.{Offer, Phone}
import ru.auto.api.CommonModel.SteeringWheel
import ru.yandex.auto.message.CatalogSchema.SuperGenerationMessage
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.{Platform, Source}
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2._
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.catalog.cars.Dictionaries
import ru.yandex.vos2.autoru.dao.proxy.{FormWriteParams, FormWriter}
import ru.yandex.vos2.autoru.model.AutoruSale.{DamageItem, Price}
import ru.yandex.vos2.autoru.model.{AutoruCommonLogic, AutoruImage, AutoruSale}
import ru.yandex.vos2.autoru.services.SettingAliases
import ru.yandex.vos2.autoru.services.SettingAliases._
import ru.yandex.vos2.autoru.utils.FormTestUtils.RichFormBuilder
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.autoru.utils.converters.formoffer.FormOfferConverter
import ru.yandex.vos2.autoru.utils.testforms._
import ru.yandex.vos2.autoru.utils.{FormTestUtils, StopWords}
import ru.yandex.vos2.util.CurrencyUtils

/**
  * Created by andrey on 10/27/16.
  */
@RunWith(classOf[JUnitRunner])
class FormAutoruSaleConverterTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfterAll {
  initDbs()

  val formAutoruSaleConverter: FormAutoruSaleConverter = new FormAutoruSaleConverter(
    components.regionTree,
    components.settingsDao,
    components.carsCatalog,
    components.featuresManager
  )

  val formOfferConverter: FormOfferConverter =
    new FormOfferConverter(
      components.carsCatalog,
      components.recognizedLpUtils,
      EmptyDefaultBookingAllowedDeciderImpl,
      components.featuresManager
    )

  val formAutoruTruckConverter: FormAutoruTruckConverter =
    new FormAutoruTruckConverter(components.trucksCatalog, components.regionTree)

  val formAutoruMotoConverter: FormAutoruMotoConverter =
    new FormAutoruMotoConverter(components.motoCatalog, components.regionTree)

  val formWriter: FormWriter = new FormWriter(components)

  private val formTestUtils = new FormTestUtils(components)

  import formTestUtils._

  private val saleConverter = new FormAutoruSaleConverter(
    components.regionTree,
    components.settingsDao,
    components.carsCatalog,
    components.featuresManager
  )

  test("getSettings Availability") {
    assert(saleConverter.getSettings.contains(SettingAliases.AVAILABILITY.alias))
    assert(saleConverter.getSettingId(SettingAliases.AVAILABILITY) == 28)
  }

  test("fillSettings") {
    val settings = saleConverter.fillSettings(privateSaleId, privateOfferForm, None)
    val expectedSettingsCount = 34

    assert(settings.length == expectedSettingsCount)
    assert(settings.forall(_.saleId == privateSaleId))
    val aliases = Set(
      DRIVE,
      STATE,
      COLOR,
      WHEEL,
      CUSTOM,
      AVAILABILITY,
      EXCHANGE,
      METALLIC,
      GEARBOX,
      OWNERS_NUMBER,
      RUN,
      USERNAME,
      PTS,
      NOTDISTURB,
      VIRTUAL_PHONE,
      VIN,
      STS,
      WARRANTY,
      WARRANTY_EXPIRE,
      PURCHASE_DATE,
      COMPLECTATION,
      ARMORED,
      HAGGLE,
      CREDIT_DISCOUNT,
      TRADEIN_DISCOUNT,
      INSURANCE_DISCOUNT,
      AUTOSERVICE_REVIEW_ID,
      AUTOSERVICE_ID,
      MAX_DISCOUNT
    )
    aliases.foreach(x => {
      assert(settings.exists(_.alias == x))
    })
    settings.find(_.alias == HAGGLE).value.value == "2"
    settings.find(_.alias == CREDIT_DISCOUNT).value.value == "100"
    settings.find(_.alias == TRADEIN_DISCOUNT).value.value == "200"
    settings.find(_.alias == INSURANCE_DISCOUNT).value.value == "300"
    settings.find(_.alias == MAX_DISCOUNT).value.value == "600"
    settings.find(_.alias == AUTOSERVICE_ID).value.value == "autoservice_123"
    settings.find(_.alias == AUTOSERVICE_REVIEW_ID).value.value == "review_213"
    // TODO: validate values
    // проверим енумы: body_type, drive, engine_type, gearbox
    val bodyTypes = Dictionaries.bodyTypes.terms.map(_.code)
    bodyTypes.foreach(bodyType => {
      val settings =
        saleConverter.fillSettings(privateSaleId, privateOfferForm.toBuilder.withBodyType(bodyType).build(), None)
      assert(settings.length == expectedSettingsCount)
    })
    val drives = Dictionaries.gearTypes.terms.map(_.code)
    drives.foreach(drive => {
      val settings =
        saleConverter.fillSettings(privateSaleId, privateOfferForm.toBuilder.withDrive(drive).build(), None)
      assert(settings.length == expectedSettingsCount)
      assert(
        settings
          .find(_.alias == DRIVE)
          .exists(id => {
            Dictionaries.gearTypes.allByCode(drive).flatMap(_.id).contains(id.value)
          })
      )
    })
    val transmissions = Dictionaries.transmissions.terms.map(_.code)
    transmissions.foreach(transmission => {
      val settings =
        saleConverter
          .fillSettings(privateSaleId, privateOfferForm.toBuilder.withTransmission(transmission).build(), None)
      assert(settings.length == expectedSettingsCount)
      assert(
        settings
          .find(_.alias == GEARBOX)
          .exists(id => Dictionaries.transmissions.allByCode(transmission).flatMap(_.id).contains(id.value))
      )
    })
  }

  test("ownersNumber is zero") {
    val builder: Offer.Builder = privateOfferForm.toBuilder
    builder.getDocumentsBuilder.setOwnersNumber(0)
    // если ownersNumber в форме = 0, не записываем в settings
    val settings = saleConverter.fillSettings(privateSaleId, builder.build(), None)
    assert(settings.forall(_.alias != OWNERS_NUMBER))
  }

  test("set steering wheel with enum") {
    val builder: Offer.Builder = privateOfferForm.toBuilder
    // если задан енам - левый руль, ставим свойство - левый
    builder.getCarInfoBuilder.setWheelLeft(false).setSteeringWheel(SteeringWheel.LEFT)
    val settings = saleConverter.fillSettings(privateSaleId, builder.build(), None)
    assert(settings.find(_.alias == WHEEL).value.value == "1")

    // если енам не задан - не ставим свойство
    builder.getCarInfoBuilder.setWheelLeft(false).clearSteeringWheel()
    val settings2 = saleConverter.fillSettings(privateSaleId, builder.build(), None)
    assert(settings2.forall(_.alias != WHEEL))

    // если задан енам - правый руль, ставим свойство - правый
    builder.getCarInfoBuilder.setWheelLeft(false).setSteeringWheel(SteeringWheel.RIGHT)
    val settings3 = saleConverter.fillSettings(privateSaleId, builder.build(), None)
    assert(settings3.find(_.alias == WHEEL).value.value == "2")

    // если все же задано также и старое поле, то неважно какой енам - ставим левый руль
    builder.getCarInfoBuilder.setWheelLeft(true).setSteeringWheel(SteeringWheel.RIGHT)
    val settings4 = saleConverter.fillSettings(privateSaleId, builder.build(), None)
    assert(settings4.find(_.alias == WHEEL).value.value == "1")
  }

  test("fillImages") {
    val now = new DateTime
    val images = saleConverter.convertImages(privateSaleId, privateOfferFormWithImages, Some(curPrivateSale), now).value
    assert(images.length == 3)
    assert(images.forall(_.saleId == privateSaleId))
    assert(images.count(_.main) == 1)
    val image1 = images.head
    checkImage(
      image1,
      0,
      main = true,
      1,
      "autoru-all:101404-1e190a2f94f4f29a8eb7dc720d75ec51",
      now,
      None,
      None,
      None,
      None
    )
    val image2 = images(1)
    checkImage(
      image2,
      0,
      main = false,
      2,
      "autoru-all:117946-b6d2fb88b2628038237af5c45ae1299a",
      now,
      None,
      None,
      None,
      None
    )
    val image3 = images(2)
    checkImage(
      image3,
      0,
      main = false,
      3,
      "autoru-all:136387-6df3831aea9cd6df09157c86b8f3d2a0",
      now,
      None,
      None,
      None,
      None
    )
  }

  // TODO video, damage, badges

  test("CurrencyUtils") {
    val rur = CurrencyUtils.fromCurrency(Currency.RUB)
    assert(rur == "RUR")
    val eur = CurrencyUtils.fromCurrency(Currency.EUR)
    assert(eur == "EUR")
    val usd = CurrencyUtils.fromCurrency(Currency.USD)
    assert(usd == "USD")
  }

  test("convertNew") {
    implicit val t = Traced.empty
    val now = getNow
    val result =
      saleConverter.convertNew(privateOfferForm, privateAd, new DateTime(now), FormWriteParams.empty, None, 0)
    assert(result.id == 0)
    assert(result.userId == user.id)
    assert(result.user.contains(user))
    assert(result.salonId == 0)
    assert(result.newClientId == 0)
    assert(result.salonPoi.isEmpty)

    assert(result.currency == "RUR")
    assert(result.price == 500000.56)
    assert(result.priceRur == 500000.56)

    val poi = result.poi.value
    assert(poi.countryId.isEmpty)
    assert(poi.regionId.isEmpty)
    assert(poi.cityId.isEmpty)
    assert(poi.yaCountryId.value == 225)
    assert(poi.yaRegionId.value == 1)
    assert(poi.yaCityId.value == 213)
    val phonesRedirect = result.phonesRedirect.value
    assert(phonesRedirect.id == 0)
    assert(phonesRedirect.saleId == 0)
    assert(phonesRedirect.active)
    //assert(phonesRedirect.updated == result.createDate)
    val damage = result.damage.value
    assert(
      damage.values.sortBy(_.car_part) == Seq(
        DamageItem("frontbumper", Seq("4"), ":("),
        DamageItem("frontleftdoor", Seq("3"), ":((")
      )
    )
    val phones = result.phones.value
    assert(phones.length == 1)
    assert(phones.head.phoneId == 32624814)
    //assert(phones.head.phone.nonEmpty)
    //TODO видео
    // TODO проверить что нибудь еще
  }

  test("convertSalonNew") {
    implicit val t = Traced.empty
    val now = getNow
    val result = saleConverter.convertNew(salonOfferForm, salonAd, new DateTime(now), FormWriteParams.empty, None, 0)
    assert(result.id == 0)
    assert(result.userId == 0)
    assert(result.user.isEmpty)
    assert(result.salonId == salon.id)
    assert(result.newClientId == salon.client.value.id)
    //assert(result.salonPoi.contains(salon))
  }

  test("convertExisting") {
    implicit val t = Traced.empty
    val now = new DateTime(getNow)
    val result =
      saleConverter.convertExisting(privateOfferForm, curPrivateSale, privateAd, now, acceptCreateDate = false)
    assert(result.id == curPrivateSale.id)
    assert(result.id == 1043270830)
    assert(result.userId == user.id)
    assert(result.user.contains(user))
    assert(result.salonId == 0)
    assert(result.newClientId == 0)
    assert(result.salonPoi.isEmpty)

    assert(result.currency == "RUR")
    assert(result.price == 500000.56)
    assert(result.priceRur == 500000.56)
    // если последняя цена в форме такая же, то новая запись в prices не создается

    val poi = result.poi.value
    assert(poi.countryId.isEmpty)
    assert(poi.regionId.isEmpty)
    assert(poi.cityId.isEmpty)
    assert(poi.yaCountryId.value == 225)
    assert(poi.yaRegionId.value == 1)
    assert(poi.yaCityId.value == 213)
    val badges = result.badges.value
    assert(badges.length == 5)
    assert(badges.count(_.id == 0) == 2)
    assert(badges.count(_.id > 0) == 3)
    assert(badges.count(_.isActivated) == 3)
    // TODO: проверка phones_redirect когда в текущем объявлении он тоже есть
    /*val phonesRedirect = result.phonesRedirect.value
    val curPhonesRedirect = curSale.phonesRedirect.value
    assert(phonesRedirect.id == curPhonesRedirect.id)
    assert(phonesRedirect.saleId == curPhonesRedirect.id)
    assert(phonesRedirect.status)
    assert(phonesRedirect.updated == result.anyUpdateTime)*/
    //TODO видео
    // TODO проверить что нибудь еще
  }

  test("convertSalonExisting") {
    implicit val t = Traced.empty
    val now = getNow
    val result =
      saleConverter.convertExisting(salonOfferForm, curSalonSale, salonAd, new DateTime(now), acceptCreateDate = false)
    assert(result.id == 1042409964)
    assert(result.salonId == salon.id)
    assert(result.newClientId == salon.client.value.id)
    //assert(result.salonPoi.contains(salon))
  }

  test("acceptCreateDate") {
    val remoteId: NotificationID = "1083132724"
    val remoteUrl: String = "https://m.avito.ru/zavodoukovsk/gruzoviki_i_spetstehnika/prodam_stsepku_freightliner" +
      "_century_" + remoteId

    implicit val t = Traced.empty
    val now = new DateTime()
    val creationDate = new DateTime(2017, 2, 28, 0, 0, 0)
    val updatedForm = privateOfferForm.toBuilder
      .setAdditionalInfo(privateOfferForm.getAdditionalInfo.toBuilder.setCreationDate(creationDate.getMillis))
      .build()

    // конвертируем новое с acceptCreateDate = false
    checkAcceptCreateDate(
      saleConverter.convertNew(updatedForm, privateAd, now, FormWriteParams.empty, None, 0),
      now,
      AutoruCommonLogic.expireDate(now)
    )

    // конвертируем новое с acceptCreateDate = true
    checkAcceptCreateDate(
      saleConverter.convertNew(updatedForm, privateAd, now, FormWriteParams(acceptCreateDate = true), None, 0),
      creationDate,
      AutoruCommonLogic.expireDate(creationDate)
    )

    // конвертируем новое с acceptCreateDate = true, но createDate в форме не передан
    checkAcceptCreateDate(
      saleConverter.convertNew(privateOfferForm, privateAd, now, FormWriteParams(acceptCreateDate = true), None, 0),
      now,
      AutoruCommonLogic.expireDate(now)
    )

    // конвертируем существующее с acceptCreateDate = false
    checkAcceptCreateDate(
      saleConverter.convertExisting(updatedForm, curPrivateSale, privateAd, now, acceptCreateDate = false),
      curPrivateSale.createDate,
      curPrivateSale.expireDate
    )

    // конвертируем существующее с acceptCreateDate = true
    checkAcceptCreateDate(
      saleConverter.convertExisting(updatedForm, curPrivateSale, privateAd, now, acceptCreateDate = true),
      creationDate,
      AutoruCommonLogic.expireDate(creationDate)
    )

    // конвертируем существующее с acceptCreateDate = true, но createDate в форме не передан
    checkAcceptCreateDate(
      saleConverter.convertExisting(privateOfferForm, curPrivateSale, privateAd, now, acceptCreateDate = true),
      curPrivateSale.createDate,
      curPrivateSale.expireDate
    )
  }

  test("save ip") {
    implicit val t = Traced.empty
    // при сохранении нового добавляем айпишник, если передан
    val now = new DateTime()
    val result =
      saleConverter.convertNew(privateOfferForm, privateAd, now, FormWriteParams(ip = Some("8.8.8.8")), None, 0)
    assert(result.ip.contains("8.8.8.8"))

    // при редактировании айпишник не меняем, но и не теряем
    val result2 = saleConverter.convertExisting(privateOfferForm, result, privateAd, now, acceptCreateDate = false)
    assert(result2.ip.contains("8.8.8.8"))
  }

  test("save source platform") {
    implicit val t = Traced.empty
    val now = new DateTime()
    val sourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.ANDROID)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef("a_123")
      .build()
    val result =
      saleConverter.convertNew(
        privateOfferForm,
        privateAd,
        now,
        FormWriteParams(sourceInfo = Some(sourceInfo)),
        None,
        0
      )
    assert(result.sourceInfo.contains(sourceInfo))

    // при редактировании источник не меняем, но и не теряем
    val result2 = saleConverter.convertExisting(privateOfferForm, result, privateAd, now, acceptCreateDate = false)
    assert(result2.sourceInfo.contains(sourceInfo))

    // если ничего не передавать, то ничего не сохраним
    val result3 = saleConverter.convertNew(privateOfferForm, privateAd, now, FormWriteParams.empty, None, 0)
    assert(result3.sourceInfo.isEmpty)
  }

  test("negative coordinates") {
    implicit val t = Traced.empty
    // проверим, что мы корректно воспринимаем с формы отрицательные координаты
    val formBuilder = privateOfferForm.toBuilder
    formBuilder.getSellerBuilder.getLocationBuilder.getCoordBuilder.setLongitude(-5).setLatitude(-5)
    val now = new DateTime()
    val result =
      saleConverter.convertNew(formBuilder.build(), privateAd, now, FormWriteParams(ip = Some("8.8.8.8")), None, 0)
    assert(result.poi.value.latitude.value == -5)
    assert(result.poi.value.longitude.value == -5)
  }

  private val carTestForms = new CarTestForms(components)
  private val truckTestForms = new TruckTestForms(components)
  private val motoTestForms = new MotoTestForms(components)

  test("trucks catalog model id") {
    implicit val t = Traced.empty
    // коды моделей не уникальны сами по себе, у разных марок может быть одинаковый код моделей, при этом их Autoru ID
    // будут разные. Поэтому при запросе id для записи в старую базу следует учитывать марку.
    // Тестируем, что марка учитывается.

    val cards = components.trucksCatalog.cards
      .filter(c => {
        c.message.getModel.getCode == "POLUPRICEP_TENT"
      })
      .toSeq

    assume(cards.length > 1)
  }

  test(s"autoruFolderId calculation when getSuperGenForUrl is defined (techParamId = 6143500)") {
    // https://st.yandex-team.ru/VOS-1926
    val card = carTestForms.catalog.getCardByTechParamId(6143500).value
    val gen: SuperGenerationMessage = card.message.getSuperGeneration
    val genIdAutoru = Option(gen.getGenIdAutoru).filter(_.nonEmpty).map(_.toLong).filter(_ > 0)
    assert(genIdAutoru.isEmpty)
    assert(gen.getSuperGenForUrl.nonEmpty)
    assert(card.autoruFolderId == gen.getId + 1000000L)
  }

  test("source info for parsed offer") {
    // корректно конвертируем спарсенное объявление, сохраняя все нужные поля для sales_from_api
    // детектируем его как спарсенное по условию платформа=DESKTOP, есть remoteId и remoteUrl
    val formInfo = carTestForms.generatePrivateForm(TestFormParams[CarFormInfo]())
    val form = formInfo.form.toBuilder
    val remoteId: NotificationID = "1083132724"
    val remoteUrl: String = "https://m.avito.ru/zavodoukovsk/gruzoviki_i_spetstehnika/prodam_stsepku_freightliner" +
      "_century_" + remoteId
    form.getAdditionalInfoBuilder.setRemoteId(remoteId)
    form.getAdditionalInfoBuilder.setRemoteUrl(remoteUrl)
    val now = new DateTime(ru.yandex.vos2.getNow)
    val ad = components.offersReader.loadAdditionalData(user.userRef, form.build())(Traced.empty)
    val sale = formAutoruSaleConverter.convertNew(
      form.build(),
      ad,
      now,
      FormWriteParams(
        sourceInfo = Some(SourceInfo.newBuilder().setPlatform(Platform.DESKTOP).setSource(Source.AUTO_RU).build())
      ),
      None,
      0
    )
    val saleSourceInfo: AutoruOffer.SourceInfo = sale.sourceInfo.value
    assert(saleSourceInfo.getSource == Source.AVITO)
    assert(saleSourceInfo.getIsCallcenter)
    assert(saleSourceInfo.getParseUrl == remoteUrl)

    // если платформа не DESKTOP, не считаем спарсенным
    val sale2 = formAutoruSaleConverter.convertNew(
      form.build(),
      ad,
      now,
      FormWriteParams(
        sourceInfo = Some(SourceInfo.newBuilder().setPlatform(Platform.IOS).setSource(Source.AUTO_RU).build())
      ),
      None,
      0
    )
    val sale2SourceInfo: AutoruOffer.SourceInfo = sale2.sourceInfo.value
    assert(sale2SourceInfo.getSource == Source.AUTO_RU)
    assert(!sale2SourceInfo.getIsCallcenter)
    assert(sale2SourceInfo.getParseUrl == "")

    // при апдейте sourceInfo не меняется
    val sale3 = formAutoruSaleConverter.convertExisting(form.build(), sale, ad, now, acceptCreateDate = false)
    val sale3SourceInfo: AutoruOffer.SourceInfo = sale3.sourceInfo.value
    assert(sale3SourceInfo == saleSourceInfo)
  }

  test("remote id and remote url for parsed offer from params") {
    // корректно конвертируем спарсенное объявление, сохраняя все нужные поля для sales_from_api
    // детектируем его как спарсенное по условию платформа=DESKTOP, есть remoteId и remoteUrl
    val formInfo = carTestForms.generatePrivateForm(TestFormParams[CarFormInfo]())
    val form = formInfo.form.toBuilder
    val remoteId: NotificationID = "1083132724"
    val remoteUrl: String = "https://m.avito.ru/zavodoukovsk/gruzoviki_i_spetstehnika/prodam_stsepku_freightliner" +
      "_century_" + remoteId
    val now = new DateTime(ru.yandex.vos2.getNow)
    val ad = components.offersReader.loadAdditionalData(user.userRef, form.build())(Traced.empty)
    val sale = formAutoruSaleConverter.convertNew(
      form.build(),
      ad,
      now,
      FormWriteParams(
        sourceInfo = Some(SourceInfo.newBuilder().setPlatform(Platform.DESKTOP).setSource(Source.AUTO_RU).build()),
        remoteId = Some(remoteId),
        remoteUrl = Some(remoteUrl)
      ),
      None,
      0
    )
    val saleSourceInfo: AutoruOffer.SourceInfo = sale.sourceInfo.value
    assert(saleSourceInfo.getSource == Source.AVITO)
    assert(saleSourceInfo.getIsCallcenter)
    assert(saleSourceInfo.getParseUrl == remoteUrl)

    // если платформа не DESKTOP, не считаем спарсенным
    val sale2 = formAutoruSaleConverter.convertNew(
      form.build(),
      ad,
      now,
      FormWriteParams(
        sourceInfo = Some(SourceInfo.newBuilder().setPlatform(Platform.IOS).setSource(Source.AUTO_RU).build())
      ),
      None,
      0
    )
    val sale2SourceInfo: AutoruOffer.SourceInfo = sale2.sourceInfo.value
    assert(sale2SourceInfo.getSource == Source.AUTO_RU)
    assert(!sale2SourceInfo.getIsCallcenter)
    assert(sale2SourceInfo.getParseUrl == "")

    // при апдейте sourceInfo не меняется
    val sale3 = formAutoruSaleConverter.convertExisting(form.build(), sale, ad, now, acceptCreateDate = false)
    val sale3SourceInfo: AutoruOffer.SourceInfo = sale3.sourceInfo.value
    assert(sale3SourceInfo == saleSourceInfo)
  }

  test("normalize custom phones") {
    def addPhone(formBuilder: Offer.Builder, phones: String*): Unit = {
      phones.toSeq.foreach(phone => formBuilder.getSellerBuilder.addPhones(Phone.newBuilder().setPhone(phone)))
    }

    val now = getNow
    val phone1 = "+79168402330"
    val phone2 = "89168402330"
    val phone3 = "79168402330"
    val phone4 = "8 (916)840-23-30"

    val formOfferBuilder: Offer.Builder = salonOfferForm.toBuilder
    formOfferBuilder.getSellerBuilder.clearPhones()
    addPhone(formOfferBuilder, phone1, phone2, phone3, phone4)
    formOfferBuilder.getSellerBuilder.setCustomPhones(true).build()

    val result =
      saleConverter.convertNew(formOfferBuilder.build(), salonAd, new DateTime(now), FormWriteParams.empty, None, 0)

    assert(result.salonPoiContacts.nonEmpty)
    val poiContacts = result.salonPoiContacts.get
    assert(poiContacts.contacts.forall(_.phone == "79168402330"))
  }

  private def checkAcceptCreateDate(sale: AutoruSale, needCreateDate: DateTime, needExpireDate: DateTime): Unit = {
    assert(sale.createDate == needCreateDate)
    assert(sale.expireDate == needExpireDate)
  }

  test("empty youtubeId") {
    val formInfo = carTestForms.generatePrivateForm(TestFormParams[CarFormInfo](isYandexVideo = false))
    val form = formInfo.form.toBuilder
    assert(form.getState.getVideo.getYoutubeId.nonEmpty)
    val now = new DateTime(ru.yandex.vos2.getNow)
    val ad = components.offersReader.loadAdditionalData(user.userRef, form.build())(Traced.empty)

    val sale1 = formAutoruSaleConverter.convertNew(form.build(), ad, now, FormWriteParams.empty, None, 0)
    assert(sale1.videos.value.length == 1)
    assert(sale1.videos.value.head.value == s"https://youtube.com/watch?v=${form.getState.getVideo.getYoutubeId}")
    assert(sale1.videos.value.head.parseValue == form.getState.getVideo.getYoutubeId)

    form.getStateBuilder.getVideoBuilder.setYoutubeId("").setYoutubeUrl("")
    val sale2 = formAutoruSaleConverter.convertNew(form.build(), ad, now, FormWriteParams.empty, None, 0)
    assert(sale2.videos.isEmpty)
  }

  //scalastyle:off parameter.number
  def checkImage(image: AutoruImage,
                 id: Long,
                 main: Boolean,
                 order: Int,
                 name: String,
                 created: DateTime,
                 cvHash: Option[String],
                 exifLat: Option[Double],
                 exifLon: Option[Double],
                 exifDate: Option[DateTime]): Unit = {
    assert(image.id == id)
    assert(image.main == main)
    assert(image.order == order)
    assert(image.name == name)
    assert(image.created == created)
    assert(image.cvHash == cvHash)
    assert(image.exifLat == exifLat)
    assert(image.exifLon == exifLon)
    assert(image.exifDate == exifDate)
  }
}
