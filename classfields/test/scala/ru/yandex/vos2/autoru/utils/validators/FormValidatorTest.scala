package ru.yandex.vos2.autoru.utils.validators

import org.joda.time.{DateTime, DateTimeUtils, LocalDate}
import org.junit.runner.RunWith
import org.scalactic.source
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel.{Availability, Category, PtsStatus, Section, Offer => ApiOffer}
import ru.auto.api.CommonModel.{Date, Photo, SteeringWheel}
import ru.auto.api.ModerationFieldsModel.ModerationFields
import ru.auto.api.MotoModel.MotoCategory
import ru.auto.api.TrucksModel.TruckCategory._
import ru.auto.api.TrucksModel._
import ru.auto.api.{ApiOfferModel, CommonModel, TrucksModel}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.{Platform, Source}
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Booking, SourceInfo}
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.{AdditionalData, FormWriteParams}
import ru.yandex.vos2.autoru.model.AutoruCommonLogic
import ru.yandex.vos2.autoru.utils.FormTestUtils.RichFormBuilder
import ru.yandex.vos2.autoru.utils.testforms._
import ru.yandex.vos2.autoru.utils.time.FixedTimeService
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors._
import ru.yandex.vos2.autoru.utils.{FormTestUtils, StopWords}
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.{UserRefAutoru, UserRefAutoruClient}

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 10/26/16.
  */
@RunWith(classOf[JUnitRunner])
class FormValidatorTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfterAll {
  initDbs(true)

  val stopWords = new StopWords()

  private val formTestUtils = new FormTestUtils(components)

  import formTestUtils._

  components.featureRegistry.updateFeature(components.featuresManager.IncompatibleEquipmentCheck.name, true)

  val carTestForms = new CarTestForms(components)
  val truckTestForms = new TruckTestForms(components)
  val motoTestForms = new MotoTestForms(components)
  components.featureRegistry.updateFeature(components.featuresManager.CatalogEquipments.name, true)

  test("Moderation protection fields for salon offer") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, true)
    val oldPrice = 10000L
    val newPrice = 20000L

    val form = {
      val builder = salonOfferForm.toBuilder
      builder.getPriceInfoBuilder.setPrice(newPrice)
      builder.clearDiscountPrice()
      builder.build()
    }

    val curOffer = {
      val builder = curSalonProto.toBuilder
      val seq = Iterable(
        ModerationFields.Fields.PRICE_INFO
      )
      builder.getOfferAutoruBuilder.addAllModerationProtectedFields(seq.asJava)
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(oldPrice)
      builder.build()
    }

    val params = FormWriteParams(isFeed = true)

    val validResult = salonFormValidator.validate(
      userRefAutoruClient,
      Category.CARS,
      form,
      Some(curOffer),
      None,
      Option(salon),
      params,
      getNow
    )

    checkSuccess(validResult)

    val result = validResult match {
      case ValidResult(Seq(ModerationProtection(t, f, e: CommonModel.PriceInfo, n: CommonModel.PriceInfo)), _) =>
        t == ModerationFields.Fields.PRICE_INFO &&
          f == FormFields.Price &&
          e.getPrice == oldPrice &&
          n.getPrice == newPrice
      case _ =>
        false
    }

    assert(result)

    components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, false)
  }

  test("Moderation protection fields for private offer") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, true)
    val oldPrice = 10000L
    val newPrice = 20000L

    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getPriceInfoBuilder.setPrice(newPrice)
      builder.build()
    }

    val curOffer = {
      val builder = curPrivateProto.toBuilder
      val seq = Iterable(
        ModerationFields.Fields.PRICE_INFO
      )
      builder.getOfferAutoruBuilder.addAllModerationProtectedFields(seq.asJava)
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(oldPrice)
      builder.build()
    }

    val params = FormWriteParams(isFeed = true)

    val validResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(curOffer),
      components.autoruUsersDao.getUser(userRef.id),
      params,
      getNow
    )

    assert(validResult.isValid)

    val result = validResult match {
      case ValidResult(Seq(ModerationProtection(t, f, e: CommonModel.PriceInfo, n: CommonModel.PriceInfo)), _) =>
        t == ModerationFields.Fields.PRICE_INFO &&
          f == FormFields.Price &&
          e.getPrice == oldPrice &&
          n.getPrice == newPrice
      case _ =>
        false
    }

    assert(result)

    components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, false)
  }

  test("vin and license plate are not required in region 73") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.VinAndGrzRequired.name, true)
    val validationResult2 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.withoutVIN.withoutLicensePlate.withGeoId(79L).build(),
      None,
      Option(user),
      FormWriteParams.empty,
      getNow
    )
    checkSuccess(validationResult2)
    components.featureRegistry.updateFeature(components.featuresManager.VinAndGrzRequired.name, false)
  }

  test("vin and license plate requirement") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.VinAndGrzRequired.name, true)
    val validationResult1 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.build(),
      None,
      Option(user),
      FormWriteParams.empty,
      getNow
    )
    val validationResult2 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.withoutVIN.withoutLicensePlate.withGeoId(3L).build(),
      None,
      Option(user),
      FormWriteParams.empty,
      getNow
    )
    val validationResult3 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.withoutVIN.withoutLicensePlate.withNotRegisteredInRussia(true).build(),
      None,
      Option(user),
      FormWriteParams.empty,
      getNow
    )
    val validationResult4 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.withGeoId(30L).build(),
      None,
      Option(user),
      FormWriteParams.empty,
      getNow
    )
    val validationResult5 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.withoutVIN.build(),
      None,
      Option(user),
      FormWriteParams(noLicensePlateSupport = Some(true)),
      getNow
    )
    val validationResult6 = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.withoutLicensePlate.withoutVIN.build(),
      None,
      Option(user),
      FormWriteParams.empty,
      getNow
    )
    components.featureRegistry.updateFeature(components.featuresManager.VinAndGrzRequired.name, false)
    checkErrors(validationResult1, RequiredLicensePlate)
    checkErrors(validationResult2, RequiredVinOrLicensePlate)
    checkErrors(validationResult3, RequiredVin)
    checkSuccess(validationResult4)
    checkErrors(validationResult5, RequiredVin)
    checkErrors(validationResult6, RequiredVin, RequiredLicensePlate)

  }

  test("validateOffer") {
    val fixture = new Fixture()
    import fixture._

    // успешная валидация нового частного объявления, когда все хорошо
    val newPrivateOfferValidationResult = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.withoutId.build(),
      None,
      Some(user),
      FormWriteParams.empty,
      getNow
    )
    checkSuccess(newPrivateOfferValidationResult)
    // успешная валидация отредактированного частного объявления
    val updatedPrivateOfferValidationResult = privateFormValidator.validate(
      Category.CARS,
      privateOfferForm.toBuilder.setDescription("ada").build,
      Some(curPrivateProto),
      Some(user),
      FormWriteParams.empty,
      getNow
    )
    checkSuccess(updatedPrivateOfferValidationResult)

    // успешная валидация нового объявления от автосалона, когда все хорошо
    val newSalonOfferValidationResult = salonFormValidator.validate(
      userRefAutoruClient,
      Category.CARS,
      salonOfferForm.toBuilder.withoutId.build(),
      None,
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    checkSuccess(newSalonOfferValidationResult)
    // успешная валидация отредактированного объявления от автосалона
    val updatedSalonOfferValidationResult = salonFormValidator.validate(
      userRefAutoruClient,
      Category.CARS,
      salonOfferForm,
      Some(curSalonProto),
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    checkSuccess(updatedSalonOfferValidationResult)

    // телефоны
    defaultCheckPrivateValidation(privateOfferForm.toBuilder.withoutPhones.build(), NotexistPhone)
    List((-1, 15), (24, 15), (1, -1), (1, 25)).foreach {
      case (callFrom, callTill) =>
        defaultCheckPrivateValidation(
          privateOfferForm.toBuilder.withCallFromCallTill(callFrom, callTill).build(),
          WrongCallHours
        )
    }
    defaultCheckPrivateValidation(privateOfferForm.toBuilder.withCallFromCallTill(20, 10).build())
    defaultCheckPrivateValidation(privateOfferForm.toBuilder.withPhoneNumber("79161793688").build(), NotownPhoneUser)
    // почта и poi
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder.withGeoId(1005000).build(),
      UnknownGeoId,
      ForbiddenEditGeoId
    )
    // неизвестный пользователь
    withoutUserCheckValidation(privateOfferForm, UnknownUser, NotownOfferUnknownUser)
    withoutCurSaleCheckValidation(privateOfferForm, WrongId)
    withCustomCategoryPrivateValidation(
      privateOfferForm,
      Some(curPrivateProto.toBuilder.setUserRef("a_111").build),
      user.userRef,
      NotownOfferUser
    )
    // цена
    List(
      ((1000, 2000000001, Currency.RUB), WrongPriceRub(1500, 1000000000)),
      ((16, 12500001, Currency.USD), WrongPriceUsd(20, 12500000)),
      ((10, 10000001, Currency.EUR), WrongPriceEur(15, 10000000)),
      ((10000, 1000, Currency.BYR), WrongPriceCur)
    ).foreach {
      case ((from, to, currency), message) =>
        defaultCheckPrivateValidation(privateOfferForm.toBuilder.withPrice(from, currency).build(), message)
        defaultCheckPrivateValidation(privateOfferForm.toBuilder.withPrice(to, currency).build(), message)
    }

    List(
      ((1000, 2000000001, Currency.RUB), WrongPriceRub(1500, 1000000000)),
      ((16, 12500001, Currency.USD), WrongPriceUsd(20, 12500000)),
      ((10, 10000001, Currency.EUR), WrongPriceEur(15, 10000000)),
      ((10000, 1000, Currency.BYR), WrongPriceCur)
    ).foreach {
      case ((from, to, currency), message) =>
        defaultCheckPrivateValidation(privateOfferForm.toBuilder.withOriginalPrice(from, currency).build(), message)
        defaultCheckPrivateValidation(privateOfferForm.toBuilder.withOriginalPrice(to, currency).build(), message)
    }
    // количество собственников, дата покупки, СТС, VIN, окончание гарантии, пробег, марка, модель, модификация, год
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder.withoutName
        .withOwnersNumber(-1)
        .withPurchaseDate(0, 0)
        .withWarrantyExpire(-1, 0)
        .withMileage(100)
        .withMark("AUDI")
        .withUsed(used = false)
        .withoutUnconfirmedEmail()
        .build(),
      WrongOwners(0, 4),
      WrongPurchaseDateYear,
      WrongPurchaseDateMonth,
      WrongWarrantyExpireYear,
      WrongWarrantyExpireMonth,
      ForbiddenPrivateNew,
      ForbiddenMark("HYUNDAI", "AUDI"),
      WrongMark,
      WrongMileageNew,
      NotexistSellerEmail,
      ForbiddenMileageDecrease(143700, 100)
    )
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder.withOwnersNumber(0).withUnconfirmedEmail("ab").build(),
      NotexistOwners,
      WrongSellerEmail
    )
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder
        .withOwnersNumber(4)
        .withPurchaseDate(2011, 13)
        .withWarrantyExpire(2018, 13)
        .withMileage(1000001)
        .withModel("A8")
        .withColor("ololo")
        .withPhoto("test")
        .withoutSuperGenId()
        .build(),
      WrongPurchaseDateMonth,
      WrongWarrantyExpireMonth,
      WrongMileage(1000000),
      ForbiddenModel("IX35", "A8"),
      WrongModel,
      UnknownColor,
      WrongPhotoName("test"),
      NotexistGeneration
    )
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder
        .withOwnersNumber(5)
        .withPurchaseDate(2011, 13)
        .withWarrantyExpire(2018, 13)
        .withMileage(1000001)
        .withModel("A8")
        .withColor("ololo")
        .withPhoto("test")
        .withoutSuperGenId()
        .build(),
      WrongOwners(1, 4),
      WrongPurchaseDateMonth,
      WrongWarrantyExpireMonth,
      WrongMileage(1000000),
      ForbiddenModel("IX35", "A8"),
      WrongModel,
      UnknownColor,
      WrongPhotoName("test"),
      NotexistGeneration
    )
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder
        .withPurchaseDate(2004, 12, 35)
        .withWarrantyExpire(2003, 2, 29)
        .build(),
      WrongPurchaseDateDay(31),
      WrongWarrantyExpireDay(28)
    )
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder
        .withPurchaseDate(2004, 12)
        .withSTS("012345789")
        .withVIN("ABCDEFGH1KLM0123")
        .withYear(2005)
        .withBadges(List("tinyurl.com xyz"))
        .withMileage(0)
        .withBodyType("xxx")
        .withEngineType("xxx")
        .withTransmission("xxx")
        .withDrive("xxx")
        .withoutWarrantyExpireDate()
        .build(),
      WrongSts,
      WrongVin,
      WrongPurchaseDateLtYear,
      WrongModificationProductionYear(2005, 2010, Some(2013)),
      WrongBadge,
      NotexistMileage,
      UnknownBodyType,
      UnknownEngineType,
      UnknownTransmission,
      UnknownDrive,
      NotexistWarrantyExpire
    )

    val nextYear = LocalDate.now().getYear + 1
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder
        .withPurchaseDate(now.getYear + 1, now.getMonthOfYear)
        .withWarrantyExpire(now.getYear - 1, now.getMonthOfYear)
        .withBadges(List("", "a", "123456789012345678901234567890", "пиупиу vazhno2011.ru"))
        .withYoutubeVideo("pewpew", "http://pewpew")
        .withYear(nextYear)
        .withAvailability(Availability.AVAILABILITY_UNKNOWN)
        .withSection(Section.SECTION_UNKNOWN)
        .withPts(PtsStatus.PTS_UNKNOWN)
        .withEquipment("tydysh") // не вызывает ошибку
        .withEquipment("trip-computer")
        .withEquipment("engine-start")
        .build(),
      WrongPurchaseDateGtNow,
      WrongWarrantyExpireLtNow,
      WrongWarrantyExpireLtYear,
      WrongModificationProductionYear(nextYear, 2010, Some(2013)),
      WrongProductionYearGtNow(nextYear),
      WrongBadgeEmpty,
      WrongBadgeLengthLt(3),
      WrongBadgeLengthGt(25),
      WrongBadge,
      WrongVideoYoutube,
      NotexistSection,
      ForbiddenPrivateNew,
      NotexistPts
    )

    withoutModificationCheckValidation(privateOfferForm, UnknownModification)
    // дополнительная валидация объявлений от салонов
    checkSalonValidation(
      salonOfferForm,
      Some {
        val b = curSalonProto.toBuilder
        b.setUserRef("ac_8514")
        b.getOfferAutoruBuilder.getSalonBuilder.setSalonId("8471")
        b.build
      },
      userRefAutoruClient,
      Category.CARS,
      NotownOfferSalon
    )
    defaultCheckSalonValidation(
      salonOfferForm.toBuilder
        .withSalonId(111)
        .withoutVIN
        .withoutBodyType
        .withoutEngineType
        .withoutTransmission
        .withoutDrive
        .build(),
      NotownOfferUnknownSalon,
      NotexistBodyType,
      NotexistEngineType,
      NotexistTransmission,
      NotexistDrive,
      UnknownSalon
    )
    defaultCheckSalonValidation(salonOfferForm.toBuilder.withoutVIN.build(), RequiredVin)
    // если машина на заказ, то vin не требуем
    defaultCheckSalonValidation(salonOfferForm.toBuilder.withAvailability(Availability.ON_ORDER).withoutVIN.build())
    defaultCheckSalonValidation(
      salonOfferForm.toBuilder.withoutVIN.withUsed(used = false).withMileage(0).build(),
      RequiredVinNew
    )

    // не кидаем ошибку "не указан птс оригинал или дубликат", если новая машина
    checkPrivateValidation(
      privateOfferForm.toBuilder.withSection(Section.NEW).withPts(PtsStatus.PTS_UNKNOWN).build(),
      Some(curPrivateProto),
      user.userRef,
      Category.CARS,
      ForbiddenPrivateNew,
      WrongMileageNew
    )
  }
  test("validateOffer with warranty") {
    val now = DateTime.now()
    val lastDayOfMonth = now.dayOfMonth().withMaximumValue()

    DateTimeUtils.setCurrentMillisFixed(lastDayOfMonth.getMillis)

    val fixture = new Fixture(DateTime.now())
    import fixture._
    defaultCheckPrivateValidation(
      privateOfferForm.toBuilder
        .withWarrantyExpire(now.getYear, now.getMonthOfYear, lastDayOfMonth.getDayOfMonth)
        .build()
    )
    DateTimeUtils.setCurrentMillisSystem()
  }

  test("allow cars manufactured before 1998 through with no strict VIN check (for dealers and users)") {
    val fixture = new Fixture()
    import fixture._

    // https://st.yandex-team.ru/AUTORUAPI-5656
    // для старых машин не делаем строгую проверку на формат ВИНа
    val offer = privateOfferForm.toBuilder
      .setCarInfo(oldCarInfo)
      .withoutId
      .withTechParam(5139710)
      .withVIN("ABCDELM0123")
      .withYear(1997)

    checkSuccess(
      privateFormValidator.validate(Category.CARS, offer.build(), None, Option(user), FormWriteParams.empty, getNow)
    )

    // аналогично для валидации на форме дилера
    val formInfo: CarFormInfo = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(8514)))
    val userRef: UserRefAutoruClient = formInfo.optSalon.value.client.value.userRef

    checkSuccess(
      salonFormValidator
        .validate(userRef, Category.CARS, offer.build(), None, None, formInfo.optSalon, FormWriteParams.empty, getNow)
    )

    // для машин начиная с 1998 строгая проверка на ВИН код
    val wrongOffer = offer.withYear(1998).build()
    checkErrors(
      privateFormValidator.validate(Category.CARS, wrongOffer, None, Option(user), FormWriteParams.empty, getNow),
      WrongVin
    )
    checkErrors(
      salonFormValidator
        .validate(userRef, Category.CARS, wrongOffer, None, None, formInfo.optSalon, FormWriteParams.empty, getNow),
      WrongVin
    )
  }

  test("WrongVin for I, O and Q") {
    val fixture = new Fixture()
    import fixture._

    val offer = privateOfferForm.toBuilder
      .setCarInfo(oldCarInfo)
      .withoutId
      .withTechParam(5139710)
      .withYear(1998)

    checkErrors(
      privateFormValidator.validate(
        Category.CARS,
        offer.withVIN("01TCC22F2IP490239").build(),
        None,
        Option(user),
        FormWriteParams.empty,
        getNow
      ),
      WrongVin
    )

    checkErrors(
      privateFormValidator.validate(
        Category.CARS,
        offer.withVIN("01OBT00UM86020129").build(),
        None,
        Option(user),
        FormWriteParams.empty,
        getNow
      ),
      WrongVin
    )

    checkErrors(
      privateFormValidator.validate(
        Category.CARS,
        offer.withVIN("03GW926Q07Z343821").build(),
        None,
        Option(user),
        FormWriteParams.empty,
        getNow
      ),
      WrongVin
    )
  }

  test("Forbid banned offer edit") {
    val fixture = new Fixture()
    import fixture._

    checkPrivateValidation(
      privateOfferForm,
      Some(curPrivateProto.toBuilder.addFlag(OfferFlag.OF_BANNED).addReasonsBan("do_not_exist").build()),
      user.userRef,
      Category.MOTO,
      StatusBanned,
      WrongCategory,
      NotexistMark,
      NotexistModel,
      NotexistMotoCategory,
      NotexistEngineVolume,
      ForbiddenMotoCategoryChange
    )
  }

  test("Allow banned offer edit") {
    val fixture = new Fixture()
    import fixture._

    checkPrivateValidation(
      privateOfferForm,
      Some(curPrivateProto.toBuilder.addFlag(OfferFlag.OF_BANNED).addReasonsBan("low_price").build()),
      user.userRef,
      Category.MOTO,
      WrongCategory,
      NotexistMark,
      NotexistModel,
      NotexistMotoCategory,
      NotexistEngineVolume,
      ForbiddenMotoCategoryChange
    )
  }

  private def checkSuccess(result: ValidationResult)(implicit pos: source.Position): Unit = {
    result match {
      case InvalidResult(errors) =>
        fail(s"validation must passed but is failed with errors: ${errors.mkString(", ")}")
      case _ =>
    }
  }

  test("trucks validation for users") {
    val fixture = new Fixture()
    import fixture._

    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getTruckInfoBuilder.clearMark().clearModel().clearTruckCategory()
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)
    checkPrivateValidation(
      truckForm,
      None,
      user.userRef,
      Category.TRUCKS,
      NotexistMark,
      NotexistModel,
      NotexistTruckCategory
    )
    val truckForm2 = builder
      .withTruckLoading(-1)
      .withTruckAxis(-1)
      .withTruckSeats(-1)
      .withTruckDisplacement(-1)
      .withTruckHorsePower(-1)
      .build()
    checkPrivateValidation(
      truckForm2,
      None,
      user.userRef,
      Category.TRUCKS,
      NotexistMark,
      NotexistModel,
      NotexistTruckCategory,
      WrongLoading(450000),
      WrongAxis(127),
      WrongSeats(8388607),
      WrongEngineVolume(100000),
      WrongEnginePower(8388607)
    )
    val truckForm3 = builder
      .withTruckLoading(2147483648L)
      .withTruckAxis(128)
      .withTruckSeats(8388608)
      .withTruckDisplacement(2147483648L)
      .withTruckHorsePower(8388608)
      .build()
    checkPrivateValidation(
      truckForm3,
      None,
      user.userRef,
      Category.TRUCKS,
      NotexistMark,
      NotexistModel,
      NotexistTruckCategory,
      WrongLoading(450000),
      WrongAxis(127),
      WrongSeats(8388607),
      WrongEngineVolume(100000),
      WrongEnginePower(8388607)
    )
  }

  test("trucks validation for dealers") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.TRUCKS
    val salon = truckTestForms.randomSalon
    val userRef = salon.client.get.userRef
    val clientId: Long = salon.client.get.id

    val builder = truckTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(clientId))).form.toBuilder
    val truckForm = builder.build()
    val wheelLeft: Boolean = truckForm.getTruckInfo.getSteeringWheel == SteeringWheel.LEFT
    val ad = components.offersReader.loadAdditionalData(userRef, truckForm)(Traced.empty)

    val offer = components.formOfferConverter.convertNewOffer(userRef, category, truckForm, ad, now.getMillis)

    // в комтранспорте можно изменять vin
    val result = salonFormValidator.validate(
      userRef,
      Category.TRUCKS,
      truckForm.toBuilder.withVIN(CommonTestForms.randomVin(wheelLeft)).build(),
      Some(offer),
      None,
      ad.optSalon,
      FormWriteParams.empty,
      now.getMillis
    )
    checkSuccess(result)

    // в комтранспорте можно не указывать VIN
    val result2 = salonFormValidator.validate(
      userRef,
      category,
      truckForm.toBuilder.withoutVIN.build(),
      Some(offer),
      None,
      ad.optSalon,
      FormWriteParams.empty,
      now.getMillis
    )
    checkSuccess(result2)
  }

  test("photo count validation for dealers: is not feed") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val salon = carTestForms.randomSalon
    val userRef = salon.client.get.userRef
    val clientId: Long = salon.client.get.id

    val builder = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(clientId))).form.toBuilder
    builder
      .getStateBuilder()
      .clearImageUrls()
      .addAllImageUrls(Seq.fill(41)(Photo.newBuilder().setName("1234-hash").build()).asJava)
    val form = builder.build()

    checkSalonValidation(
      form,
      None,
      userRef,
      category,
      WrongPhotoCount(40)
    )
  }

  test("owners number validation: used car with no_pts") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val salon = carTestForms.randomSalon
    val userRef = salon.client.get.userRef
    val clientId: Long = salon.client.get.id

    val builder = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(clientId))).form.toBuilder

    builder.getDocumentsBuilder.setOwnersNumber(0)
    builder.setSection(Section.USED)
    builder.getDocumentsBuilder.setPts(PtsStatus.NO_PTS)
    checkSuccessSalonValidation(builder.build(), None, userRef, category)
    builder.getDocumentsBuilder.setPts(PtsStatus.ORIGINAL)
    checkSalonValidation(builder.build(), None, userRef, category, NotexistOwners)
    builder.getDocumentsBuilder.setPts(PtsStatus.NO_PTS)
    builder.setSection(Section.NEW)
    builder.getStateBuilder.setMileage(0)
    checkSuccessSalonValidation(builder.build(), None, userRef, category)
  }

  test("photo count validation for dealers: is feed") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val salon = carTestForms.randomSalon
    val userRef = salon.client.get.userRef
    val clientId: Long = salon.client.get.id

    val builder = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(clientId))).form.toBuilder
    builder
      .getStateBuilder()
      .clearImageUrls()
      .addAllImageUrls(Seq.fill(41)(Photo.newBuilder().setName("1234-hash").build()).asJava)
    val form = builder.build()

    assert(form.getState.getImageUrlsCount == 41)

    val result = checkSuccessSalonValidationWithParams(
      form,
      None,
      userRef,
      category,
      FormWriteParams.empty
        .copy(sourceInfo = Some(SourceInfo.newBuilder().setPlatform(SourceInfo.Platform.FEED).build())),
      WrongPhotoCount(40)
    )

    assert(result.getState.getImageUrlsCount == 40)
  }

  test("cars validation for dealers") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val salon = carTestForms.randomSalon
    val userRef = salon.client.get.userRef
    val clientId: Long = salon.client.get.id

    val builder = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(clientId))).form.toBuilder
    val form = builder.build()

    val wheelLeft: Boolean = form.getCarInfo.getSteeringWheel == SteeringWheel.LEFT
    val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)

    val offer = components.formOfferConverter.convertNewOffer(userRef, category, form, ad, now.getMillis)

    // если цена = цене со скидкой, то возвращаеи нотис
    val formWithEqDiscountPriceBuilder = form.toBuilder
    formWithEqDiscountPriceBuilder.getDiscountOptionsBuilder.clearCredit().clearInsurance().clearTradein()
    formWithEqDiscountPriceBuilder.getDiscountPriceBuilder
      .setPrice(formWithEqDiscountPriceBuilder.getPriceInfo.getPrice)
    checkSuccessSalonValidation(
      formWithEqDiscountPriceBuilder.build(),
      Some(offer),
      userRef,
      category,
      WrongDiscountEqPrice
    )

    // в легковых дилерам нельзя изменять vin
    checkSalonValidation(
      form.toBuilder.withVIN(CommonTestForms.randomVin(wheelLeft)).build(),
      Some(offer),
      userRef,
      category,
      ForbiddenEditVinCommercial
    )

    // можно изменять вин, если прежний был пустой
    val offerWithoutVin = offer.toBuilder
    offerWithoutVin.getOfferAutoruBuilder.getDocumentsBuilder.clearVin()
    checkSalonValidation(
      form.toBuilder.withVIN(CommonTestForms.randomVin(wheelLeft)).build(),
      Some(offerWithoutVin.build()),
      userRef,
      category
    )
  }

  test("dealers validateCanEditAddress, validateCanEditContact") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    // создаем объявление от имени дилера, которому запрещено менять адрес и указывать телефоны
    val formInfo: CarFormInfo = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(8514)))
    val userRef: UserRefAutoruClient = formInfo.optSalon.value.client.value.userRef
    val builder = formInfo.form.toBuilder
    val sellerBuilder = builder.getSellerBuilder
    sellerBuilder.getLocationBuilder.setAddress("Другой адрес")
    sellerBuilder.addPhonesBuilder().setPhone("79065554433")
    sellerBuilder.setCustomLocation(true).setCustomPhones(true)
    val form = builder.build()
    val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
    checkSalonValidation(
      form,
      None,
      userRef,
      category,
      ForbiddenSalonEditAddress,
      ForbiddenSalonEditPhones(Seq("79065554433"), Seq("74952254487", "74951250722"))
    )

    // теперь создаем объявление от имени дилера, который все это может
    val formInfo2: CarFormInfo = carTestForms.generateDealerForm(TestFormParams(optOwnerId = Some(10086)))
    val userRef2: UserRefAutoruClient = formInfo2.optSalon.value.client.value.userRef
    val builder2 = formInfo2.form.toBuilder
    builder2.getSellerBuilder.getLocationBuilder.setAddress("Другой адрес")
    builder2.getSellerBuilder.addPhonesBuilder().setPhone("79065554433")
    val form2 = builder2.build()
    val ad2 = components.offersReader.loadAdditionalData(userRef2, form2)(Traced.empty)

    // валидация проходит успешно
    val result =
      salonFormValidator.validate(userRef2, category, form2, None, None, ad2.optSalon, FormWriteParams.empty, getNow)
    checkSuccess(result)
  }

  test("private seller noPhoneOwnerCheck") {
    val fixture = new Fixture()
    import fixture._

    val form = privateOfferForm.toBuilder.withPhoneNumber("79161793688").build()
    val result = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(curPrivateProto),
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams(noPhoneOwnerCheck = true),
      getNow
    )
    checkSuccess(result)
  }

  test("address length") {
    val fixture = new Fixture()
    import fixture._

    val address = "A" * 256
    val form = privateOfferForm.toBuilder.withAddress(address).build()
    val result = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(curPrivateProto),
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams(),
      getNow
    )
    checkErrors(result, WrongAddressLengthGt(255))
  }

  test("truck subcategory") {
    val fixture = new Fixture()
    import fixture._

    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    val needSubcategory = builder.getTruckInfo.getTruckCategory
    val wrongCategory = needSubcategory match {
      case TruckCategory.ARTIC => TruckCategory.LCV
      case TruckCategory.BUS => TruckCategory.TRAILER
      case TruckCategory.LCV => TruckCategory.ARTIC
      case TruckCategory.SWAP_BODY => TruckCategory.TRAILER
      case TruckCategory.TRAILER => TruckCategory.TRUCK
      case TruckCategory.TRUCK => TruckCategory.SWAP_BODY
      case _ => TruckCategory.LCV
    }
    builder.getTruckInfoBuilder.setTruckCategory(wrongCategory)
    truckTestForms.setBodyTypeByCategory(builder.getTruckInfoBuilder, wrongCategory)
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)

    val result =
      privateFormValidator.validate(Category.TRUCKS, truckForm, None, ad.optPhpUser, FormWriteParams.empty, getNow)

    checkErrors(result, WrongSubcategory(wrongCategory.name(), needSubcategory.name()))
  }

  test("truck unknown mark model") {
    val fixture = new Fixture()
    import fixture._

    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getTruckInfoBuilder.setMark("pew")
    builder.getTruckInfoBuilder.setModel("pewpew")
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)

    val result =
      privateFormValidator.validate(Category.TRUCKS, truckForm, None, ad.optPhpUser, FormWriteParams.empty, getNow)

    checkErrors(result, UnknownMarkModel)
  }

  test("moto unknown mark model") {
    val fixture = new Fixture()
    import fixture._

    val user = motoTestForms.randomUser
    val builder = motoTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getMotoInfoBuilder.setMark("pew")
    builder.getMotoInfoBuilder.setModel("pewpew")
    val motoForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, motoForm)(Traced.empty)

    val result =
      privateFormValidator.validate(Category.MOTO, motoForm, None, ad.optPhpUser, FormWriteParams.empty, getNow)

    checkErrors(result, UnknownMarkModel)
  }

  test("comtrans mileage validation") {
    val fixture = new Fixture()
    import fixture._

    val formInfo: TruckFormInfo = truckTestForms.generateDealerForm(TestFormParams(isDealer = false))
    val userRef = formInfo.optSalon.value.client.value.userRef
    // для прицепов и съемных кузовов не надо проверять пробег
    val builder = formInfo.form.toBuilder
    builder.getStateBuilder.setMileage(10000001) // невалидный пробег
    def truckForm: ApiOfferModel.Offer = builder.build()

    val ad = components.offersReader.loadAdditionalData(userRef, truckForm)(Traced.empty)

    def checkTruckFailed(truckCategory: TruckCategory): Unit = {
      builder.getTruckInfoBuilder.setTruckCategory(truckCategory)
      builder.getTruckInfoBuilder.setModel("")
      truckTestForms.setBodyTypeByCategory(builder.getTruckInfoBuilder, truckCategory)
      val result = salonFormValidator.validate(
        userRef,
        Category.TRUCKS,
        truckForm,
        None,
        None,
        ad.optSalon,
        FormWriteParams.empty,
        getNow
      )
      checkErrors(result, WrongMileage(10000000), NotexistModel)
    }

    def checkTruckSuccess(truckCategory: TruckCategory): Unit = {
      builder.getTruckInfoBuilder.setTruckCategory(truckCategory)
      truckTestForms.setBodyTypeByCategory(builder.getTruckInfoBuilder, truckCategory)
      val result = salonFormValidator.validate(
        userRef,
        Category.TRUCKS,
        truckForm,
        None,
        None,
        ad.optSalon,
        FormWriteParams.empty,
        getNow
      )
      checkErrors(result, NotexistModel)
    }

    checkTruckFailed(TruckCategory.ARTIC)
    checkTruckFailed(TruckCategory.BUS)
    checkTruckFailed(TruckCategory.LCV)
    checkTruckFailed(TruckCategory.TRUCK)

    checkTruckSuccess(TruckCategory.SWAP_BODY)
    checkTruckSuccess(TruckCategory.TRAILER)
  }

  test("comtrans category change") {
    val fixture = new Fixture()
    import fixture._

    val card = components.trucksCatalog.getCardByMarkModel("MERCEDES", "817").value // TRUCK
    val category: Category = Category.TRUCKS
    val user = truckTestForms.randomUser
    val truckFormInfo: TruckFormInfo = truckTestForms.generatePrivateForm(
      TestFormParams[TruckFormInfo](optOwnerId = Some(user.id), optCard = Some(card))
    )
    val builder = truckFormInfo.form.toBuilder
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, truckForm, ad, now.getMillis)

    // нельзя менять основную категорию комтс
    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.BUS)
    checkPrivateValidation(
      builder.build(),
      Some(offer),
      user.userRef,
      Category.TRUCKS,
      ForbiddenTruckCategoryChange,
      WrongSubcategory("BUS", "TRUCK"),
      NotexistBusType
    )
  }

  test("moto category change") {
    val fixture = new Fixture()
    import fixture._

    val card = components.motoCatalog.getCardByMarkModel("TRIUMPH", "DAYTONA_600").value // MOTORCYCLE
    val category: Category = Category.MOTO
    val user = motoTestForms.randomUser
    val motoFormInfo: MotoFormInfo =
      motoTestForms.generatePrivateForm(TestFormParams[MotoFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
    val builder = motoFormInfo.form.toBuilder
    val motoForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, motoForm)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, motoForm, ad, now.getMillis)

    // нельзя менять основную категорию мото
    builder.getMotoInfoBuilder.setMotoCategory(MotoCategory.ATV)
    checkPrivateValidation(
      builder.build(),
      Some(offer),
      user.userRef,
      Category.MOTO,
      ForbiddenMotoCategoryChange,
      WrongSubcategory("ATV", "MOTORCYCLE")
    )
  }

  test("comtrans body type validation") {
    val fixture = new Fixture()
    import fixture._

    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)
    builder.getTruckInfoBuilder.setModel("")

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.TRUCK)
    builder.getTruckInfoBuilder.setTruckType(Truck.BodyType.BODY_TYPE_UNKNOWN)
    val truckFormCategoryTruck = builder.build()
    checkPrivateValidation(
      truckFormCategoryTruck,
      None,
      user.userRef,
      Category.TRUCKS,
      NotexistTruckType,
      NotexistModel
    )

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.BUS)
    builder.getTruckInfoBuilder.setBusType(Bus.Type.TRUCK_BUS_UNKNOWN)
    val truckFormCategoryBus = builder.build()
    checkPrivateValidation(truckFormCategoryBus, None, user.userRef, Category.TRUCKS, NotexistBusType, NotexistModel)

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.LCV)
    builder.getTruckInfoBuilder.setLightTruckType(LightTruck.BodyType.BODY_TYPE_UNKNOWN)
    val truckFormCategoryLCV = builder.build()
    checkPrivateValidation(
      truckFormCategoryLCV,
      None,
      user.userRef,
      Category.TRUCKS,
      NotexistLightTruckType,
      NotexistModel
    )

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.TRAILER)
    builder.getTruckInfoBuilder.setTrailerType(Trailer.Type.TRUCK_TRAILER_UNKNOWN)
    val truckFormCategoryTrailer = builder.build()
    checkPrivateValidation(
      truckFormCategoryTrailer,
      None,
      user.userRef,
      Category.TRUCKS,
      NotexistTrailerType,
      NotexistModel
    )
  }

  test("comtrans engine volume validation") {
    val fixture = new Fixture()
    import fixture._

    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.TRUCK)
    builder.getTruckInfoBuilder.setModel("")
    truckTestForms.setBodyTypeByCategory(builder.getTruckInfoBuilder, TruckCategory.TRUCK)
    builder.getTruckInfoBuilder.setDisplacement(0)
    val form = builder.build()
    checkPrivateValidation(form, None, user.userRef, Category.TRUCKS, NotexistEngineVolume, NotexistModel)
  }

  test("special comtrans validation") {
    val fixture = new Fixture()
    import fixture._

    val user = truckTestForms.randomUser
    val builder =
      truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id), specialTruck = true)).form.toBuilder
    builder.getDiscountOptionsBuilder.clear
    builder.getTruckInfoBuilder.setTruckCategory(CRANE)
    builder.getTruckInfoBuilder.setLoadHeight(-1).setCraneRadius(-1).setOperatingHours(-1)
    checkPrivateValidation(
      builder.build(),
      None,
      user.userRef,
      Category.TRUCKS,
      WrongLoadHeight(180),
      WrongCraneRadius(180),
      WrongOperatingHours(100000)
    )

    builder.getTruckInfoBuilder.setLoadHeight(190).setCraneRadius(190).setOperatingHours(110000)
    checkPrivateValidation(
      builder.build(),
      None,
      user.userRef,
      Category.TRUCKS,
      WrongLoadHeight(180),
      WrongCraneRadius(180),
      WrongOperatingHours(100000)
    )

    builder.getTruckInfoBuilder.setTruckCategory(AUTOLOADER).setAutoloaderType(TrucksModel.Autoloader.Type.FORKLIFTS)
    checkPrivateValidation(
      builder.build(),
      None,
      user.userRef,
      Category.TRUCKS,
      WrongLoadHeight(20),
      WrongOperatingHours(100000)
    )

    builder.getTruckInfoBuilder.setTruckCategory(DREDGE).setDredgeType(TrucksModel.Dredge.Type.CAREER_EXCAVATOR)
    builder.getTruckInfoBuilder.setBucketVolume(-1).setOperatingHours(20000)
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, WrongBucketVolume(40))
    builder.getTruckInfoBuilder.setBucketVolume(50)
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, WrongBucketVolume(40))

    builder.getTruckInfoBuilder.setBucketVolume(10)
    checkSuccessPrivateValidation(
      builder.build(),
      None,
      user.userRef,
      Category.TRUCKS,
      ForbiddenLoadHeight("DREDGE"),
      ForbiddenCraneRadius("DREDGE")
    )

    builder.getTruckInfoBuilder
      .setLoadHeight(9)
      .clearCraneRadius()
      .setTractionClass(TrucksModel.TractionClass.TRACTION_3)
    builder.getTruckInfoBuilder.setTruckCategory(AUTOLOADER).setAutoloaderType(TrucksModel.Autoloader.Type.FORKLIFTS)
    checkSuccessPrivateValidation(
      builder.build(),
      None,
      user.userRef,
      Category.TRUCKS,
      ForbiddenBucketVolume("AUTOLOADER"),
      ForbiddenTractionClass("AUTOLOADER")
    )

    builder.getTruckInfoBuilder.setLoadHeight(0).setCraneRadius(0).setBucketVolume(0).setOperatingHours(0)
    builder.getTruckInfoBuilder.setTruckCategory(AGRICULTURAL).clearAgriculturalType()
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, NotexistAgriculturalType)
    builder.getTruckInfoBuilder.setTruckCategory(CONSTRUCTION).clearConstructionType()
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, NotexistConstructionType)
    builder.getTruckInfoBuilder.setTruckCategory(AUTOLOADER).clearAutoloaderType()
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, NotexistAutoloaderType)
    builder.getTruckInfoBuilder.setTruckCategory(DREDGE).clearDredgeType()
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, NotexistDredgeType)
    builder.getTruckInfoBuilder.setTruckCategory(BULLDOZERS).clearBulldozerType()
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, NotexistBulldozerType)
    builder.getTruckInfoBuilder.setTruckCategory(MUNICIPAL).clearMunicipalType()
    checkPrivateValidation(builder.build(), None, user.userRef, Category.TRUCKS, NotexistMunicipalType)
  }

  test("moto engine volume validation") {
    val fixture = new Fixture()
    import fixture._

    val user = motoTestForms.randomUser
    val builder = motoTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    val motoForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, motoForm)(Traced.empty)

    builder.getMotoInfoBuilder.setDisplacement(0)
    val form = builder.build()
    checkPrivateValidation(form, None, user.userRef, Category.MOTO, NotexistEngineVolume)
  }

  test("moto subcategory") {
    val fixture = new Fixture()
    import fixture._

    val user = motoTestForms.randomUser
    val builder = motoTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder

    val needSubcategory = builder.getMotoInfo.getMotoCategory
    val wrongCategory = needSubcategory match {
      case MotoCategory.ATV => MotoCategory.SNOWMOBILE
      case MotoCategory.MOTORCYCLE => MotoCategory.SCOOTERS
      case MotoCategory.SCOOTERS => MotoCategory.MOTORCYCLE
      case MotoCategory.SNOWMOBILE => MotoCategory.ATV
      case _ => MotoCategory.ATV
    }
    builder.getMotoInfoBuilder.setMotoCategory(wrongCategory)
    val motoForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, motoForm)(Traced.empty)

    val result =
      privateFormValidator.validate(Category.MOTO, motoForm, None, ad.optPhpUser, FormWriteParams.empty, getNow)

    checkErrors(result, WrongSubcategory(wrongCategory.name(), needSubcategory.name()))
  }

  test("email check test") {
    val fixture = new Fixture()
    import fixture._

    assert(!privateFormValidator.checkEmail(""))
    assert(!privateFormValidator.checkEmail("a@"))
    assert(!privateFormValidator.checkEmail("abc"))
    assert(!privateFormValidator.checkEmail("@ab"))
    assert(!privateFormValidator.checkEmail("ab@"))
    assert(privateFormValidator.checkEmail("a@b"))
  }

  test("skip warranty expire validation for trucks") {
    val fixture = new Fixture()
    import fixture._

    // не валидируем для комтранса отсутствие даты окончания гарантии при warranty=true
    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    val truckForm = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, truckForm)(Traced.empty)

    builder.getDocumentsBuilder.setWarranty(true).clearWarrantyExpire()
    val form = builder.build()
    val result =
      privateFormValidator.validate(Category.TRUCKS, form, None, ad.optPhpUser, FormWriteParams.empty, getNow)
    checkSuccess(result)
  }

  test("skip email existence validation for call center offers") {
    val fixture = new Fixture()
    import fixture._

    // для создаваемых коллцентром объявлений не требуем почту. При редактировании требуем
    val category: Category = Category.TRUCKS
    val user = truckTestForms.randomUser
    val builder = truckTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getSourceInfoBuilder.setSource("avito").setPlatform("desktop")
    builder.getSellerBuilder.clearUnconfirmedEmail()
    val form = builder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val result = privateFormValidator.validate(
      Category.TRUCKS,
      form,
      None,
      ad.optPhpUser,
      FormWriteParams(
        sourceInfo = Some(SourceInfo.newBuilder().setSource(Source.AVITO).setPlatform(Platform.DESKTOP).build())
      ),
      getNow
    )
    checkSuccess(result)

    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis)
    val result2 = privateFormValidator.validate(
      Category.TRUCKS,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(
        sourceInfo = Some(SourceInfo.newBuilder().setSource(Source.AVITO).setPlatform(Platform.DESKTOP).build())
      ),
      getNow
    )
    checkErrors(result2, NotexistSellerEmail)
  }

  test("forbid to edit unpaid offer for reseller") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val form = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis).toBuilder
    offer.getOfferAutoruBuilder.setReseller(true)
    offer.getOfferAutoruBuilder.clearServices()
    val result2 =
      privateFormValidator.validate(Category.CARS, form, Some(offer.build()), ad.optPhpUser, FormWriteParams(), getNow)
    checkErrors(result2, ForbiddenEdit)
  }

  test("forbid to edit unpaid inactive offer from reseller if it was placed by call center") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val form = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis).toBuilder
    offer.getOfferAutoruBuilder.setReseller(true)
    offer.getOfferAutoruBuilder.clearServices()
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
    val result2 =
      privateFormValidator.validate(Category.CARS, form, Some(offer.build()), ad.optPhpUser, FormWriteParams(), getNow)
    checkErrors(result2, ForbiddenEdit)
  }

  test("allow to edit offer from protected reseller") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val form = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis).toBuilder
    offer.getOfferAutoruBuilder.setReseller(true)
    offer.getOfferAutoruBuilder.setIsProtectedReseller(true)
    offer.getOfferAutoruBuilder.clearServices()
    offer.clearFlag()
    val result2 =
      privateFormValidator.validate(Category.CARS, form, Some(offer.build()), ad.optPhpUser, FormWriteParams(), getNow)
    checkSuccess(result2)
  }

  test("license plate") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val userRef = user.userRef
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getDocumentsBuilder.setLicensePlate("Х936РА77")
    checkPrivateValidation(builder.build(), None, userRef, category)
    // указываем латиницей и мелкими буквами
    builder.getDocumentsBuilder.setLicensePlate("x936pa77")
    val result = checkPrivateValidation(builder.build(), None, userRef, category)
    assert(result.valid.value.validForm.getDocuments.getLicensePlate == "Х936РА77")
    // другие форматы номера
    builder.getDocumentsBuilder.setLicensePlate("АА123163")
    checkPrivateValidation(builder.build(), None, userRef, category)
    builder.getDocumentsBuilder.setLicensePlate("1234АА01")
    checkPrivateValidation(builder.build(), None, userRef, category)
    // неверный формат
    builder.getDocumentsBuilder.setLicensePlate("зуцзуц")
    checkPrivateValidation(builder.build(), None, userRef, category, WrongLicensePlate)
  }

  test("vin and license plate forbidden edit") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS

    def getFormAndOffer(f: ApiOfferModel.Offer.Builder => Unit,
                        optOfferVin: Option[String] = None,
                        optOfferLicensePlate: Option[String] = None,
                        optVinEditCounter: Option[Int] = None,
                        optLicensePlateEditCounter: Option[Int] = None): (ApiOfferModel.Offer, Option[Offer]) = {
      val form = {
        val b = privateOfferForm.toBuilder
        b.setId("111111-XXXX")
        f(b)
        b.build()
      }

      val optOffer = Option {
        val b = components.formOfferConverter
          .convertNewOffer(
            user.userRef,
            category,
            form,
            AdditionalData.empty,
            DateTime.now().getMillis
          )
          .toBuilder

        b.setOfferID(form.getId)

        optOfferVin.foreach(b.getOfferAutoruBuilder.getDocumentsBuilder.setVin)
        optOfferLicensePlate.foreach(b.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate)
        optVinEditCounter.foreach(b.getOfferAutoruBuilder.setVinEditCounter)
        optLicensePlateEditCounter.foreach(b.getOfferAutoruBuilder.setLicensePlateEditCounter)

        b.build()
      }

      (form, optOffer)
    }

    val (form1, optOffer1) = getFormAndOffer(
      b => {
        b.getCarInfoBuilder
          .setMark("VOLKSWAGEN")
          .setModel("GOLF")
          .setTechParamId(6332904L)

        b.getDocumentsBuilder
          .setYear(2005)
          .setVin("X4XKV294200W50642")
          .setLicensePlate("Х936РА77")
      },
      optOfferVin = Some("X4XKV294200W50600"),
      optOfferLicensePlate = Some("АА123163")
    )

    // vin и номер поменялись, но их можно менять по одному разу, поэтому ошибок нет
    checkPrivateValidation(form1, optOffer1, userRef, category)

    val (form2, optOffer2) = getFormAndOffer(
      b => {
        b.getCarInfoBuilder
          .setMark("BMW")
          .setModel("X6")
          .setTechParamId(20158772)

        b.getDocumentsBuilder
          .setYear(2015)
          .setVin("X4XKV294200W50642")
          .setLicensePlate("Х936РА77")

        b.getDocumentsBuilder.getPurchaseDateBuilder
          .setYear(2016)
          .setMonth(3)
          .setDay(2)
      },
      optOfferVin = Some("X4XKV294200W50600"),
      optOfferLicensePlate = Some("Х936РА77")
    )

    // поменялся только вин, но его 1 раз можно, поэтому вообще ошибок нет
    checkPrivateValidation(form2, optOffer2, userRef, category)

    val (form3, optOffer3) = getFormAndOffer(
      b => {
        b.getCarInfoBuilder
          .setMark("LAND_ROVER")
          .setModel("EVOQUE")
          .setTechParamId(20103127)

        b.getDocumentsBuilder
          .setYear(2013)
          .setVin("X4XKV294200W50600")
          .setLicensePlate("Х936РА77")

        b.getDocumentsBuilder.getPurchaseDateBuilder
          .setYear(2013)
          .setMonth(3)
          .setDay(2)
      },
      optOfferVin = Some("X4XKV294200W50600"),
      optOfferLicensePlate = Some("АА123163")
    )

    // поменялся только номер, но его 1 раз можно, поэтому вообще ошибок нет
    checkPrivateValidation(form3, optOffer3, userRef, category)

    val (form4, optOffer4) = getFormAndOffer(
      b => {
        b.getCarInfoBuilder
          .setMark("LAND_ROVER")
          .setModel("EVOQUE")
          .setTechParamId(20103127)

        b.getDocumentsBuilder
          .setYear(2013)
          .setVin("X4XKV294200W50642")
          .setLicensePlate("Х936РА77")

        b.getDocumentsBuilder.getPurchaseDateBuilder
          .setYear(2013)
          .setMonth(3)
          .setDay(2)
      },
      optOfferVin = Some("X4XKV294200W50600"),
      optOfferLicensePlate = Some("Х936РА77"),
      optVinEditCounter = Some(1)
    )

    // поменялся только вин, но уже второй раз, это нельзя
    checkPrivateValidation(form4, optOffer4, userRef, category, ForbiddenEditVinPrivate)

    val (form5, optOffer5) = getFormAndOffer(
      b => {
        b.getCarInfoBuilder
          .setMark("LAND_ROVER")
          .setModel("EVOQUE")
          .setTechParamId(20103127)

        b.getDocumentsBuilder
          .setYear(2013)
          .setVin("X4XKV294200W50642")
          .setLicensePlate("Х936РА77")

        b.getDocumentsBuilder.getPurchaseDateBuilder
          .setYear(2013)
          .setMonth(3)
          .setDay(2)
      },
      optOfferVin = Some("X4XKV294200W50642"),
      optOfferLicensePlate = Some("АА123163"),
      optLicensePlateEditCounter = Some(1)
    )

    // поменялся только номер, но уже второй раз, это нельзя
    checkPrivateValidation(form5, optOffer5, userRef, category, ForbiddenEditLicensePlate)
  }

  test("not validate user") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val userRef = user.userRef

    val offer = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = None)).form
    val result = privateFormValidator.validate(
      category,
      offer,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams.empty,
      getNow,
      validateUser = false
    )
    checkErrors(result)
  }

  test("validateDay") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val userRef = user.userRef
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getDocumentsBuilder.getPurchaseDateBuilder.setYear(1884).setMonth(4).clearDay()
    val offer = builder.build()
    val result = privateFormValidator.validate(
      category,
      offer,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams.empty,
      getNow,
      validateUser = false
    )
    checkErrors(result, WrongPurchaseDateLtYear)
  }

  test("validate unknown equipment") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val userRef = user.userRef
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getDiscountOptionsBuilder.clearCredit().clearInsurance().clearTradein()
    builder.getCarInfoBuilder.putEquipment("tydysh", true)
    val offer = builder.build()
    val result = privateFormValidator.validate(
      category,
      offer,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams.empty,
      getNow,
      validateUser = false
    )
    checkNotices(result, UnknownEquipment("tydysh"))
    assert(!result.valid.value.validForm.getCarInfo.containsEquipment("tydysh"))
  }

  test("validate discounts") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.TRUCKS
    val formInfo: TruckFormInfo = truckTestForms.generateDealerForm()
    val userRef: UserRefAutoruClient = formInfo.optSalon.value.client.value.userRef
    val builder = formInfo.form.toBuilder.withPrice(10000)
    // Полный список скидок доступен только для LCV
    builder.getTruckInfoBuilder
      .setTruckCategory(TrucksModel.TruckCategory.LCV)
      .setLightTruckType(LightTruck.BodyType.CAMPER)
      .setMark("FORD")
      .setModel("TRANSIT_LT")

    builder.clearDiscountOptions().getDiscountOptionsBuilder.setMaxDiscount(10000)
    checkSalonValidation(builder.build(), None, userRef, category, WrongMaxDiscountGtePrice)

    builder.clearDiscountOptions().getDiscountOptionsBuilder.setMaxDiscount(5000)
    checkSalonValidation(builder.build(), None, userRef, category, WrongMaxDiscountGteHalfPrice(5000, 10000))

    builder
      .clearDiscountOptions()
      .getDiscountOptionsBuilder
      .clearMaxDiscount()
      .setCredit(10001)
      .setInsurance(10001)
      .setTradein(10000)
    checkSalonValidation(
      builder.build(),
      None,
      userRef,
      category,
      WrongMaxDiscountGtePrice,
      WrongCreditDiscountGtePrice,
      WrongInsuranceDiscountGtePrice,
      WrongTradeinDiscountGtePrice
    )

    builder
      .clearDiscountOptions()
      .getDiscountOptionsBuilder
      .clearMaxDiscount()
      .setCredit(9000)
      .setInsurance(9000)
      .setTradein(9000)
    checkSalonValidation(builder.build(), None, userRef, category, WrongMaxDiscountGtePrice)
  }

  test("validate axis for trailer") {
    val fixture = new Fixture()
    import fixture._

    val formInfo = truckTestForms.generateDealerForm()
    val builder = formInfo.form.toBuilder
    val userRef: UserRefAutoruClient = formInfo.optSalon.value.client.value.userRef
    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.TRAILER)
    builder.getTruckInfoBuilder.setAxis(33)

    val result = salonFormValidator.validate(
      userRef,
      Category.TRUCKS,
      builder.build(),
      None,
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    assert(extractErrors(result).collect { case WrongAxis(_) => }.nonEmpty)
  }

  test("validate for lcv") {
    val fixture = new Fixture()
    import fixture._

    val formInfo = truckTestForms.generateDealerForm()
    val builder = formInfo.form.toBuilder
    val userRef: UserRefAutoruClient = formInfo.optSalon.value.client.value.userRef
    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.LCV)
    builder.getTruckInfoBuilder.setSeats(33)
    builder.getTruckInfoBuilder.setLoading(3333)

    val result1 = salonFormValidator.validate(
      userRef,
      Category.TRUCKS,
      builder.build(),
      None,
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    assert(extractErrors(result1).collect { case WrongSeats(_) => }.nonEmpty)
    assert(extractErrors(result1).collect { case WrongLoading(_) => }.nonEmpty)

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.TRUCK).clearSeats()
    val result2 = salonFormValidator.validate(
      userRef,
      Category.TRUCKS,
      builder.build(),
      None,
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    assert(extractErrors(result2).collect { case WrongSeats(_) | WrongLoading(_) => }.isEmpty)

    builder.getTruckInfoBuilder.setTruckCategory(TruckCategory.BUS).clearLoading().setSeats(33)
    val result3 = salonFormValidator.validate(
      userRef,
      Category.TRUCKS,
      builder.build(),
      None,
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    assert(extractErrors(result3).collect { case WrongSeats(_) | WrongLoading(_) => }.isEmpty)
  }

  test("do not allow to decrease mileage for private users for cars") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getStateBuilder.setMileage(100000)

    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis)

    builder.getStateBuilder.setMileage(90000)
    val result =
      privateFormValidator.validate(Category.CARS, form, Some(offer), ad.optPhpUser, FormWriteParams(), getNow)
    checkErrors(result, ForbiddenMileageDecrease(100000, 90000))
  }

  test("validate phones max count") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder

    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis)

    def result: ValidationResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(noPhoneOwnerCheck = true),
      getNow
    )

    fillPhones(builder, AutoruCommonLogic.maxPhonesCount)
    checkErrors(result)

    fillPhones(builder, AutoruCommonLogic.maxPhonesCount + 1)
    checkErrors(result, WrongPhonesCount(AutoruCommonLogic.maxPhonesCount))
  }

  test("skip notexist.pts and notexist.owners validation for scooters") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.MOTO
    val user = motoTestForms.randomUser
    val builder = motoTestForms
      .generatePrivateForm(
        TestFormParams[MotoFormInfo](
          optOwnerId = Some(user.id),
          optCard = Some(components.motoCatalog.getCardByMarkModel("AJS", "MODENA").value)
        )
      )
      .form
      .toBuilder

    assert(builder.getMotoInfo.getMotoCategory == MotoCategory.SCOOTERS)

    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, now.getMillis)

    def result: ValidationResult = privateFormValidator.validate(
      Category.MOTO,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(noPhoneOwnerCheck = true),
      getNow
    )

    builder.getDocumentsBuilder.clearPts()
    builder.getDocumentsBuilder.clearOwnersNumber()

    checkErrors(result)
  }

  test("validate photo required") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.PhotoRequired.name, true)
    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder

    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, getNow)

    def result: ValidationResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(noPhoneOwnerCheck = true),
      getNow
    )

    checkErrors(result, RequiredPhoto)

    builder.getSourceInfoBuilder.setIsCallcenter(true)
    checkErrors(result)

    components.featureRegistry.updateFeature(components.featuresManager.PhotoRequired.name, false)
  }

  test("validate photo required when create android") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.PhotoRequired.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.AllowWithoutPhotosFromDesktop.name, true)
    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getSourceInfoBuilder.setPlatform(Platform.ANDROID.toString)
    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, getNow)

    val sourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.ANDROID)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef("a_123")
      .build()

    def result: ValidationResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(noPhoneOwnerCheck = true, sourceInfo = Some(sourceInfo)),
      getNow
    )

    checkErrors(result, RequiredPhoto)

    components.featureRegistry.updateFeature(components.featuresManager.AllowWithoutPhotosFromDesktop.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.PhotoRequired.name, false)
  }

  test("validate photo not required when create desktop") {
    val fixture = new Fixture()
    import fixture._

    components.featureRegistry.updateFeature(components.featuresManager.PhotoRequired.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.AllowWithoutPhotosFromDesktop.name, true)
    val category: Category = Category.CARS
    val user = carTestForms.randomUser
    val builder = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id))).form.toBuilder
    builder.getSourceInfoBuilder.setPlatform(Platform.DESKTOP.toString)
    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, getNow)

    def result: ValidationResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(noPhoneOwnerCheck = true),
      getNow
    )

    assert(result.isValid)

    components.featureRegistry.updateFeature(components.featuresManager.AllowWithoutPhotosFromDesktop.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.PhotoRequired.name, false)
  }

  test("validate steering wheel from configuration id") {
    val fixture = new Fixture()
    import fixture._

    val category: Category = Category.CARS
    components.featureRegistry.updateFeature(components.featuresManager.ValidateSteeringWheel.name, true)
    val user = carTestForms.randomUser
    val builder = carTestForms
      .generatePrivateForm(
        TestFormParams(
          optOwnerId = Some(user.id),
          wheelLeft = Some(true)
        )
      )
      .form
      .toBuilder
    builder.getCarInfoBuilder
      .setMark("AUDI")
      .setModel("A1")
      .setSuperGenId(21428669)
      .setConfigurationId(21428843)
      .setTechParamId(21459905)
    builder.getDocumentsBuilder
      .setYear(2018)
      .setPurchaseDate(
        Date.newBuilder().setYear(2018).setMonth(3).setDay(12)
      )

    def form: ApiOffer = builder.build()

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = components.formOfferConverter.convertNewOffer(user.userRef, category, form, ad, getNow)

    def result: ValidationResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(offer),
      ad.optPhpUser,
      FormWriteParams(noPhoneOwnerCheck = true),
      getNow
    )

    checkErrors(result)

    builder.getCarInfoBuilder.clearConfigurationId()
    checkErrors(result)

    builder.getCarInfoBuilder.setSteeringWheel(SteeringWheel.RIGHT)
    checkErrors(result, WrongSteeringWheel)
  }

  test("validate insurance, credit and tradein discount clear") {
    val fixture = new Fixture()
    import fixture._

    val formInfo = truckTestForms.generateDealerForm()
    val builder = formInfo.form.toBuilder
    builder
      .setCategory(Category.TRUCKS)
      .getTruckInfoBuilder
      .setTruckCategory(TrucksModel.TruckCategory.DREDGE)
      .setDredgeType(TrucksModel.Dredge.Type.LOADER_EXCAVATOR)
      .setMark("JCB")
      .setModel("5CX")
    builder.getDiscountOptionsBuilder
      .setCredit(1)
      .setInsurance(2)
      .setTradein(3)
      .setLeasingDiscount(4)
      .setMaxDiscount(5)

    val result1 = salonFormValidator.validate(
      userRefAutoruClient,
      Category.TRUCKS,
      builder.build(),
      None,
      None,
      Option(salon),
      FormWriteParams.empty,
      getNow
    )
    assert(extractErrors(result1).collect { case NonEmptyInsuranceDiscountForThisCategory => }.nonEmpty)
    assert(extractErrors(result1).collect { case NonEmptyCreditDiscountForThisCategory => }.nonEmpty)
    assert(extractErrors(result1).collect { case NonEmptyTradeinDiscountForThisCategory => }.nonEmpty)
  }

  test("license plate moderation protection") {
    val fixture = new Fixture()
    import fixture._

    // https://st.yandex-team.ru/VOS-3265
    components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, true)
    val oldPrice = 10000L
    val newPrice = 20000L

    val form = {
      val builder = privateOfferForm.toBuilder
      builder.getPriceInfoBuilder.setPrice(newPrice)
      builder.getDocumentsBuilder.setLicensePlate("о793кв40")
      builder.build()
    }

    val curOffer = {
      val builder = curPrivateProto.toBuilder
      val seq = Iterable(
        ModerationFields.Fields.LICENSE_PLATE
      )
      builder.getOfferAutoruBuilder.addAllModerationProtectedFields(seq.asJava)
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(oldPrice)
      builder.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate("О793КВ40")
      builder.build()
    }

    val params = FormWriteParams()

    val validResult = privateFormValidator.validate(
      Category.CARS,
      form,
      Some(curOffer),
      components.autoruUsersDao.getUser(userRef.id),
      params,
      getNow
    )

    assert(validResult.isValid)

    components.featureRegistry.updateFeature(components.featuresManager.ModerationProtection.name, false)

  }

  test("autoru_expert in new offer") {
    val fixture = new Fixture()
    import fixture._

    val form = privateOfferForm.toBuilder.withoutId.withAutoruExpert.build

    val params = FormWriteParams()

    val validResult = privateFormValidator.validate(
      Category.CARS,
      form,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      params,
      getNow
    )

    assert(validResult.isValid)
  }

  test("autoru_expert commercial valid category & section") {
    val fixture = new Fixture()
    import fixture._

    val form = salonOfferForm.toBuilder
      .withSection(Section.USED)
      .withoutId
      .withAutoruExpert
      .build
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      Category.CARS,
      form,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    assert(actual.isValid)
  }

  test("autoru_expert private valid category CARS") {
    val fixture = new Fixture()
    import fixture._

    val form = privateOfferForm.toBuilder.withoutId.withAutoruExpert.build
    val actual = privateFormValidator.validate(
      Category.CARS,
      form,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams(),
      getNow
    )
    assert(actual.isValid)
  }

  test("booking private forbidden") {
    val fixture = new Fixture()
    import fixture._

    val form = {
      val builder = carTestForms
        .generatePrivateForm(TestFormParams(optOwnerId = Some(user.id)))
        .form
        .toBuilder
        .withoutId
        .withAutoruExpert
      builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(true)
      builder.build
    }
    val actual = privateFormValidator.validate(
      Category.CARS,
      form,
      None,
      Some(user),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, ForbiddenBooking)
  }

  test("booking state forbidden") {
    val fixture = new Fixture()
    import fixture._

    val formBuilder = carTestForms.generateDealerForm().form.toBuilder
    val stateBuilder = ApiOfferModel.AdditionalInfo.Booking.State.newBuilder
    stateBuilder.getNotBookedBuilder.build
    val bookingBuilder = formBuilder.getAdditionalInfoBuilder.getBookingBuilder
    bookingBuilder.setAllowed(true)
    bookingBuilder.setState(stateBuilder)
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      Category.CARS,
      formBuilder.build,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, ForbiddenBookingState)
  }

  test("allow next production year") {
    val fixture = new Fixture(new DateTime(2020, 12, 1, 0, 0, 0, 0).withMonthOfYear(12))
    import fixture._

    val nextYear = now.getYear + 1
    val formBuilder = salonOfferForm.toBuilder
    val category = Category.CARS
    val form = formBuilder.withoutId
      .withYear(nextYear)
      .withoutPurchaseDate
      .withSection(Section.NEW)
      .withAvailability(Availability.ON_ORDER)
      .withMileage(0)
      .build
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      category,
      form,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, WrongModificationProductionYear(2021, 2008, Some(2011)))
  }

  test("forbidden next production year: wrong moment") {
    val fixture = new Fixture(new DateTime(2020, 12, 1, 0, 0, 0, 0).withMonthOfYear(9))
    import fixture._

    val nextYear = now.getYear + 1
    val formBuilder = salonOfferForm.toBuilder
    val category = Category.CARS
    val form = formBuilder.withoutId
      .withYear(nextYear)
      .withoutPurchaseDate
      .withSection(Section.NEW)
      .withAvailability(Availability.ON_ORDER)
      .withMileage(0)
      .build
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      category,
      form,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, WrongModificationProductionYear(2021, 2008, Some(2011)), WrongProductionYearGtNow(2021))
  }

  test("forbidden next production year: section=used") {
    val fixture = new Fixture(new DateTime(2020, 12, 1, 0, 0, 0, 0).withMonthOfYear(12))
    import fixture._

    val nextYear = now.getYear + 1
    val formBuilder = salonOfferForm.toBuilder
    val category = Category.CARS
    val form = formBuilder.withoutId
      .withYear(nextYear)
      .withoutPurchaseDate
      .withSection(Section.USED)
      .withAvailability(Availability.ON_ORDER)
      .withMileage(10000)
      .build
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      category,
      form,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, WrongModificationProductionYear(2021, 2008, Some(2011)), WrongProductionYearGtNow(2021))
  }

  test("forbidden next production year: availability=in stock") {
    val fixture = new Fixture(new DateTime(2020, 12, 1, 0, 0, 0, 0).withMonthOfYear(12))
    import fixture._

    val nextYear = now.getYear + 1
    val formBuilder = salonOfferForm.toBuilder
    val category = Category.CARS
    val form = formBuilder.withoutId
      .withYear(nextYear)
      .withoutPurchaseDate
      .withSection(Section.NEW)
      .withAvailability(Availability.IN_STOCK)
      .withMileage(0)
      .build
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      category,
      form,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, WrongModificationProductionYear(2021, 2008, Some(2011)), WrongProductionYearGtNow(2021))
  }

  test("forbidden next production year: availability=in transit") {
    val fixture = new Fixture(new DateTime(2020, 12, 1, 0, 0, 0, 0).withMonthOfYear(12))
    import fixture._

    val nextYear = now.getYear + 1
    val formBuilder = salonOfferForm.toBuilder
    val category = Category.CARS
    val form = formBuilder.withoutId
      .withYear(nextYear)
      .withoutPurchaseDate
      .withSection(Section.NEW)
      .withAvailability(Availability.IN_TRANSIT)
      .withMileage(0)
      .build
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      category,
      form,
      None,
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )
    checkErrors(actual, WrongModificationProductionYear(2021, 2008, Some(2011)), WrongProductionYearGtNow(2021))
  }

  test("booking state not forbidden") {
    val fixture = new Fixture()
    import fixture._

    val formBuilder = carTestForms.generateDealerForm().form.toBuilder
    val stateBuilder = ApiOfferModel.AdditionalInfo.Booking.State.newBuilder
    stateBuilder.getBookedBuilder.build
    val bookingBuilder = formBuilder.getAdditionalInfoBuilder.getBookingBuilder
    bookingBuilder
      .setAllowed(true)
      .setState(stateBuilder)
    val category = Category.CARS
    val form = formBuilder.build
    val clientUserRef = salon.client.get.userRef
    val ad = components.offersReader.loadAdditionalData(clientUserRef, form)(Traced.empty)
    val offerBuilder =
      components.formOfferConverter.convertNewOffer(clientUserRef, category, form, ad, getNow).toBuilder
    offerBuilder.getOfferAutoruBuilder.getBookingBuilder.getStateBuilder
      .setBooked(Booking.State.Booked.newBuilder.build)
    val actual = salonFormValidator.validate(
      userRefAutoruClient,
      category,
      form,
      Some(offerBuilder.build),
      None,
      Option(salon),
      FormWriteParams(),
      getNow
    )

    println(actual.toString)
    assert(actual.isValid)
  }

  test("validate at least calls or chats") {
    val fixture = new Fixture()
    import fixture._

    val builder = privateOfferForm.toBuilder.withoutId

    builder.getSellerBuilder.setChatsEnabled(false)
    builder.getAdditionalInfoBuilder.setChatOnly(true)
    val form = builder.build()

    val result1 = privateFormValidator.validate(
      Category.CARS,
      form,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams(),
      getNow
    )
    assert(extractErrors(result1).collect { case AtLeastCallsOrChats => }.nonEmpty)
  }

  test("daylight savings time gap successful") {
    val fixture = new Fixture()
    import fixture._

    val builder = privateOfferForm.toBuilder.withoutId

    /**
      * AUTORUBACK-1920
      */
    builder.getDocumentsBuilder.setPurchaseDate(Date.newBuilder().setYear(1981).setDay(1).setMonth(4))
    val form = builder.build()

    val result1 = privateFormValidator.validate(
      Category.CARS,
      form,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams(),
      getNow
    )
    assert(!result1.isValid)
  }

  test("equipment codes excluding test") {
    val fixture = new Fixture()
    import fixture._

    val builder = privateOfferForm.toBuilder.withoutId

    /**
      * AUTORUBACK-2554
      */
    builder.getCarInfoBuilder.putEquipment("passenger-seat-electric", true)
    builder.getCarInfoBuilder.putEquipment("driver-seat-electric", true)
    builder.getDocumentsBuilder.setLicensePlate("Х936РА77")

    val form = builder.build()
    components.featureRegistry.updateFeature(components.featuresManager.IncompatibleEquipmentCheck.name, true)

    val result1 = privateFormValidator.validate(
      Category.CARS,
      form,
      None,
      components.autoruUsersDao.getUser(userRef.id),
      FormWriteParams(),
      getNow
    )
    assert(!result1.isValid)
  }

  test("validate panoramas") {
    val fixture = new Fixture()
    import fixture._

    val builder = privateOfferForm.toBuilder.withoutId

    val invalidId = "this_id_contains_exactly_33_chars"
    builder.getStateBuilder.getInteriorPanoramaBuilder.addPanoramas(
      ru.auto.panoramas.InteriorModel.InteriorPanorama.newBuilder.setId(invalidId)
    )

    val form = builder.build()

    val exception = intercept[IllegalArgumentException](
      privateFormValidator.validate(
        Category.CARS,
        form,
        None,
        components.autoruUsersDao.getUser(userRef.id),
        FormWriteParams(),
        getNow
      )
    )
    assert(exception.getMessage.contains("too long"))
  }

  private def fillPhones(b: ApiOfferModel.Offer.Builder, quantity: Int): Unit = {
    b.getSellerBuilder.clearPhones()
    for (i <- 0 until quantity) {
      val phone = "7906%07d".format(i)
      b.getSellerBuilder.addPhonesBuilder().setPhone(phone)
    }
  }

  def checkNotices(result: ValidationResult, notices: ValidationError*)(implicit pos: source.Position): Unit = {
    if (notices.isEmpty) {
      result match {
        case ValidResult(resultNotices, _) =>
          if (resultNotices.nonEmpty) {
            fail(
              s"validation must passed with zero notices but is passed with notices:" +
                s" ${resultNotices.mkString(", ")}"
            )
          }
        case InvalidResult(resultErrors) =>
          fail(s"validation must passed with zero notices but is failed with errors: ${resultErrors.mkString(", ")}")
      }
    } else {
      result match {
        case ValidResult(resultNotices, _) =>
          assert(
            notices.forall(resultNotices.contains),
            s"validation must pass with notices" +
              s"\n${notices.mkString(", ")},\nbut some or all of these notices wasn't " +
              s"found:\n${notices.diff(resultNotices).mkString(", ")}\n" +
              s"these notices found: ${resultNotices.mkString(", ")}"
          )
          assert(
            resultNotices.forall(notices.contains),
            s"unexpected notices found:" +
              s"\n${resultNotices.diff(notices).mkString(", ")}"
          )
        case InvalidResult(resultErrors) =>
          fail(
            s"validation must passed with notices\n${notices.mkString(", ")},\n" +
              s"but is failed with errors: ${resultErrors.mkString(", ")}"
          )
      }
    }
  }

  private def checkErrors(
      result: ValidationResult,
      errors: ValidationError*
  )(implicit pos: source.Position): ValidationResult = {
    if (errors.isEmpty) {
      result match {
        case InvalidResult(resultErrors) =>
          fail(s"validation must passed but is failed with errors: ${resultErrors.mkString(", ")}")
        case _ =>
      }
    } else {
      result match {
        case ValidResult(_, _) =>
          fail(s"validation must fail with messages\n${errors.mkString(", ")}\nbut its passed")
        case InvalidResult(resultErrors) =>
          assert(
            errors.forall(resultErrors.contains),
            s"validation must fail with messages" +
              s"\n${errors.mkString(", ")},\nbut some or all of these messages wasn't " +
              s"found:\n${errors.diff(resultErrors).mkString(", ")}\n" +
              s"these messages found: ${resultErrors.mkString(", ")}"
          )
          assert(
            resultErrors.forall(errors.contains),
            s"unexpected messages found:" +
              s"\n${resultErrors.diff(errors).mkString(", ")}"
          )
      }
    }
    result
  }

  private def extractErrors(result: ValidationResult): Seq[ValidationError] = result match {
    case ValidResult(notices, _) => notices
    case InvalidResult(errors) => errors
  }

  private class Fixture(val now: DateTime = DateTime.now()) {
    val timeService = new FixedTimeService(now)

    val privateFormValidator = new PrivateFormValidator(components, timeService)

    val salonFormValidator = new CommercialFormValidator(components, timeService)

    def withCustomCategoryPrivateValidation(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoru,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      checkPrivateValidation(form, optCurProto, userRef, Category.CARS, errors: _*)
    }

    def checkPrivateValidation(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoru,
        category: Category,
        errors: ValidationError*
    )(implicit pos: source.Position): ValidationResult = {
      val result = privateFormValidator.validate(
        category,
        form,
        optCurProto,
        components.autoruUsersDao.getUser(userRef.id),
        FormWriteParams.empty,
        getNow
      )
      checkErrors(result, errors: _*)
    }

    def checkSuccessSalonValidation(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoruClient,
        category: Category,
        notices: ValidationError*
    )(implicit pos: source.Position): ApiOffer = {
      checkSuccessSalonValidationWithParams(form, optCurProto, userRef, category, FormWriteParams.empty, notices: _*)
    }

    def checkSuccessSalonValidationWithParams(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoruClient,
        category: Category,
        params: FormWriteParams,
        notices: ValidationError*
    )(implicit pos: source.Position): ApiOffer = {

      val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
      val result = salonFormValidator.validate(
        userRefAutoruClient,
        category,
        form,
        optCurProto,
        None,
        ad.optSalon,
        params,
        getNow
      )
      checkNotices(result, notices: _*)
      result.valid.value.validForm
    }

    def checkSuccessPrivateValidation(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoru,
        category: Category,
        notices: ValidationError*
    )(implicit pos: source.Position): ApiOffer = {
      val result = privateFormValidator.validate(
        category,
        form,
        optCurProto,
        components.autoruUsersDao.getUser(userRef.id),
        FormWriteParams.empty,
        getNow
      )
      checkNotices(result, notices: _*)
      result.valid.value.validForm
    }

    def checkSalonValidation(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoruClient,
        category: Category,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      checkSalonValidationWithParams(form, optCurProto, userRef, category, FormWriteParams.empty, errors: _*)
    }

    def checkSalonValidationWithParams(
        form: ApiOfferModel.Offer,
        optCurProto: Option[Offer],
        userRef: UserRefAutoruClient,
        category: Category,
        params: FormWriteParams,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
      val result = salonFormValidator.validate(
        userRef,
        category,
        form,
        optCurProto,
        None,
        ad.optSalon,
        params,
        getNow
      )
      checkErrors(result, errors: _*)
    }

    def defaultCheckPrivateValidation(
        form: ApiOfferModel.Offer,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      withCustomCategoryPrivateValidation(form, Some(curPrivateProto), user.userRef, errors: _*)
    }

    def checkPrivateValidationWithNotices(
        form: ApiOfferModel.Offer
    )(errors: ValidationError*)(notices: ValidationError*)(implicit pos: source.Position): Unit = {
      withCustomCategoryPrivateValidation(form, Some(curPrivateProto), user.userRef, errors: _*)
    }

    def defaultCheckSalonValidation(
        form: ApiOfferModel.Offer,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      checkSalonValidation(form, Some(curSalonProto), userRefAutoruClient, Category.CARS, errors: _*)
    }

    def withoutUserCheckValidation(
        form: ApiOfferModel.Offer,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      withCustomCategoryPrivateValidation(form, Some(curPrivateProto), UserRefAutoru(100500), errors: _*)
    }

    def withoutCurSaleCheckValidation(
        form: ApiOfferModel.Offer,
        errors: ValidationError*
    )(implicit pos: source.Position): Unit = {
      withCustomCategoryPrivateValidation(form, None, user.userRef, errors: _*)
    }

    def withoutModificationCheckValidation(form: ApiOfferModel.Offer, errors: ValidationError*): Unit = {
      withCustomCategoryPrivateValidation(
        form.toBuilder.withTechParam(1).build(),
        Some(curPrivateProto),
        userRef,
        errors: _*
      )
    }
  }
}
