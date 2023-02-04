package ru.yandex.vos2.autoru.utils

import java.time.LocalDate

import org.scalactic.source
import org.scalatest.funsuite.AnyFunSuiteLike
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CarsModel.CarInfo.ManufacturerInfo
import ru.auto.api.CommonModel.Damage.{CarPart, DamageType}
import ru.auto.api.CommonModel.DiscountPrice.DiscountPriceStatus
import ru.auto.api.CommonModel.{AutoserviceReviewInfo, DiscountOptions, Photo, SteeringWheel}
import ru.auto.api._
import ru.yandex.auto.message.AutoExtDataSchema.DealersInfoMessage
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.Dictionaries
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.dao.proxy.AdditionalData
import ru.yandex.vos2.autoru.model.{AutoruCommonLogic, AutoruSale, AutoruUser}
import ru.yandex.vos2.autoru.services.SettingAliases
import ru.yandex.vos2.autoru.utils.ApiFormUtils.{RichApiOffer, RichPriceInfoBuilder}
import ru.yandex.vos2.autoru.utils.testforms.{CarFormInfo, FormInfo, MotoFormInfo, TruckFormInfo}
import ru.yandex.vos2.model.UserRefAutoru
import ru.yandex.vos2.proto.ProtoMacro
import ru.yandex.vos2.services.mds.AutoruAllNamespaceSettings
import ru.yandex.vos2.util.{CurrencyUtils, ExternalAutoruUserRef}

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 2/14/17.
  */
class FormTestUtils(components: AutoruCoreComponents) {
  implicit val t = Traced.empty

  def sale2proto(sale: AutoruSale): Offer = {
    components.carOfferConverter.convertStrict(sale, None).convertedOrFail
  }

  // частное объявление с турбо-пакетом и стикерами
  val privateSaleId: Long = 1043270830L
  val privateSaleIdAndHash: String = s"$privateSaleId-6b56a"

  // частное объявление
  val curPrivateSale: AutoruSale = components.autoruSalesDao.getOffer(privateSaleId).get

  val curPrivateProto: Offer = {
    val builder = sale2proto(curPrivateSale).toBuilder
    builder.getOfferAutoruBuilder.getCarInfoBuilder
      .setBodyType("ALLROAD_3_DOORS")
      .setEngineType("GASOLINE")
      .setTransmission("AUTOMATIC")
      .setGearType("FORWARD_CONTROL")
      .setMark("HYUNDAI")
      .setModel("IX35")
      .setSuperGenId(2305474)
      .setConfigurationId(6143425)
      .setTechParamId(6143500)
      .setComplectationId(20082325)
      .setSteeringWheel(AutoruOffer.SteeringWheel.LEFT)

    builder.build()
  }
  val user: AutoruUser = components.autoruUsersDao.getUser(curPrivateSale.userId).get
  val userRef: UserRefAutoru = user.userRef

  private lazy val carInfo = CarsModel.CarInfo
    .newBuilder()
    .setArmored(false)
    .setBodyType("ALLROAD_3_DOORS")
    .setEngineType("GASOLINE")
    .setTransmission("AUTOMATIC")
    .setDrive("ALL_WHEEL_DRIVE")
    .setMark("HYUNDAI")
    .setModel("IX35")
    .setSuperGenId(2305474)
    .setConfigurationId(6143425)
    .setTechParamId(6143500)
    .setComplectationId(20082325)
    .putEquipment("12-inch-wheels", true)
    .putEquipment("12v-socket", true)
    .setWheelLeft(true)
    .setSteeringWheel(SteeringWheel.LEFT)
    .setManufacturerInfo(
      ManufacturerInfo
        .newBuilder()
        .setModificationCode("A2S6D1617D216")
        .setColorCode("2Q")
        .setInteriorCode("ZQ98")
        .addAllEquipmentCode(Seq("4A3", "B52").asJava)
    )

  val oldCarInfo: CarInfo.Builder = CarsModel.CarInfo
    .newBuilder()
    .setBodyType("SEDAN")
    .setEngineType("GASOLINE")
    .setTransmission("AUTOMATIC")
    .setDrive("FORWARD_CONTROL")
    .setMark("BMW")
    .setModel("5ER")
    .setSuperGenId(3473283)
    .setConfigurationId(4927451)
    .setTechParamId(5139710)
    .setComplectationId(3473283)
    .setSteeringWheel(SteeringWheel.LEFT)

  val privateOfferForm = ApiOfferModel.Offer.newBuilder
    .setColorHex("660099")
    .setSection(Section.USED) // подержанная машина
    .setAvailability(Availability.IN_STOCK) // в наличии
    .setCarInfo(carInfo)
    .setPriceInfo(
      CommonModel.PriceInfo
        .newBuilder()
        .setPrice(500000.56)
        .setCurrency(CurrencyUtils.fromCurrency(Currency.RUB))
    )
    .setDiscountPrice(CommonModel.DiscountPrice.newBuilder().setPrice(100500).setStatus(DiscountPriceStatus.ACTIVE))
    .setDiscountOptions(
      DiscountOptions
        .newBuilder()
        .setCredit(100)
        .setTradein(200)
        .setInsurance(300)
        .setMaxDiscount(600)
    )
    .setDescription(
      "Своевременное обслуживание. Пройдены все ТО. Куплена не в кредит. Непрокуренный салон. " +
        "Сервисная книжка. Не участвовала в ДТП."
    )
    .setDocuments(
      ApiOfferModel.Documents
        .newBuilder()
        .setOwnersNumber(2)
        .setPtsOriginal(true)
        .setPts(PtsStatus.ORIGINAL)
        .setCustomCleared(true)
        .setPurchaseDate(CommonModel.Date.newBuilder().setYear(2012).setMonth(12).setDay(1))
        .setYear(2011)
        .setSts("01АБ012345")
        .setVin("ABCDEFGH1JKLM0123")
        .setWarranty(true)
        .setWarrantyExpire(CommonModel.Date.newBuilder().setYear(LocalDate.now().getYear + 1).setMonth(12).setDay(1))
    )
    .setState(
      ApiOfferModel.State
        .newBuilder()
        .setMileage(200000)
        .setStateNotBeaten(true)
        .setCondition(Condition.CONDITION_OK)
        .setVideo(
          CommonModel.Video
            .newBuilder()
            .setYoutubeId("-DTZfPrPagI")
            .setYoutubeUrl("https://youtube.com/watch?v=-DTZfPrPagI")
        )
        .addAllDamages(
          List(
            CommonModel.Damage
              .newBuilder()
              .setCarPart(CarPart.FRONT_BUMPER)
              .addAllType(List(DamageType.CORROSION).asJava)
              .setDescription(":(")
              .build(),
            CommonModel.Damage
              .newBuilder()
              .setCarPart(CarPart.FRONT_LEFT_DOOR)
              .addAllType(List(DamageType.DENT).asJava)
              .setDescription(":((")
              .build()
          ).asJava
        )
    )
    .setId(privateSaleIdAndHash)
    .setUserRef(ExternalAutoruUserRef.fromUserRef(user.userRef).get)
    .setSeller(
      ApiOfferModel.Seller
        .newBuilder()
        .setName("Христофор")
        .setUnconfirmedEmail("example@example.org")
        .addAllPhones(
          List(
            ApiOfferModel.Phone.newBuilder().setPhone("79161793608").setCallHourStart(9).setCallHourEnd(23).build()
          ).asJava
        )
        .setRedirectPhones(true)
        .setLocation(
          ApiOfferModel.Location
            .newBuilder()
            .setAddress("Рублёвское шоссе")
            .setCoord(CommonModel.GeoPoint.newBuilder().setLatitude(43.905251).setLongitude(30.261402))
            .setGeobaseId(213)
        )
    )
    .setPrivateSeller(
      ApiOfferModel.PrivateSeller
        .newBuilder()
        .setName("Христофор")
        .addAllPhones(
          List(
            ApiOfferModel.Phone.newBuilder().setPhone("79161793608").setCallHourStart(9).setCallHourEnd(23).build()
          ).asJava
        )
        .setRedirectPhones(true)
        .setLocation(
          ApiOfferModel.Location
            .newBuilder()
            .setAddress("Рублёвское шоссе")
            .setCoord(CommonModel.GeoPoint.newBuilder().setLatitude(43.905251).setLongitude(30.261402))
            .setGeobaseId(21652)
        )
    )
    .setAdditionalInfo(
      ApiOfferModel.AdditionalInfo
        .newBuilder()
        .setHidden(false)
        .setNotDisturb(true)
        .setExchange(false)
        .addAutoserviceReview(
          AutoserviceReviewInfo
            .newBuilder()
            .setAutoserviceId(curPrivateSale.getSetting(SettingAliases.AUTOSERVICE_ID).get)
            .setReviewId(curPrivateSale.getSetting(SettingAliases.AUTOSERVICE_REVIEW_ID).get)
        )
    )
    .addAllBadges(List("Парктроник", "Коврики в подарок", "Кожаный салон").asJava)
    .build()

  val privateOfferFormWithImages: ApiOfferModel.Offer = {
    val builder = privateOfferForm.toBuilder
    builder.getStateBuilder
      .addAllImageUrls(
        List(
          Photo
            .newBuilder()
            .setName("autoru-all:101404-1e190a2f94f4f29a8eb7dc720d75ec51")
            .setNamespace(AutoruAllNamespaceSettings.namespace)
            .build(),
          Photo
            .newBuilder()
            .setName("autoru-all:117946-b6d2fb88b2628038237af5c45ae1299a")
            .setNamespace(AutoruAllNamespaceSettings.namespace)
            .build(),
          // эта картинка есть в текущем объявлении
          Photo
            .newBuilder()
            .setName("autoru-all:136387-6df3831aea9cd6df09157c86b8f3d2a0")
            .setNamespace(AutoruAllNamespaceSettings.namespace)
            .build()
        ).asJava
      )
    builder.build()
  }

  val salonSaleId: Long = 1042409964L
  val salonSaleIdAndHash: String = s"$salonSaleId-038a"

  val curSalonSale = components.autoruSalesDao
    .getOffer(salonSaleId)
    .get
    .copy(salonId = 19063, newClientId = 24813)

  val curSalonProto = {
    val b = sale2proto(curSalonSale).toBuilder
    b.getOfferAutoruBuilder.getCarInfoBuilder
      .setBodyType("ALLROAD_3_DOORS")
      .setEngineType("GASOLINE")
      .setTransmission("AUTOMATIC")
      .setGearType("FORWARD_CONTROL")
      .setMark("AUDI")
      .setModel("Q5")
      .setSuperGenId(8351293)
      .setConfigurationId(8351305)
      .setTechParamId(9254714)
      .setSteeringWheel(AutoruOffer.SteeringWheel.LEFT)
    b.build
  }
  val salon = components.autoruSalonsDao.getSalonPoi(Seq(curSalonSale.salonId))(t)(curSalonSale.salonId)
  val userRefAutoruClient = salon.client.get.userRef

  val salonOfferForm = {
    val b: ApiOfferModel.Offer.Builder = privateOfferForm.toBuilder
    b.setId(salonSaleIdAndHash)
      .setUserRef(ExternalAutoruUserRef.fromUserRef(userRefAutoruClient).get)
      .clearPrivateSeller()
      .setSalon(ApiOfferModel.Salon.newBuilder().setSalonId(curSalonSale.salonId))
      .getCarInfoBuilder
      .setMark("AUDI")
      .setModel("Q5")
      .setTransmission("ROBOT_2CLUTCH")
      .setConfigurationId(3784306)
      .setTechParamId(3784319)
    b.build()
  }

  val privateAd = AdditionalData(Some(user), None, None, Nil)
  val salonAd = AdditionalData(None, Some(salon), None, Nil)
}

object FormTestUtils extends AnyFunSuiteLike {

  def performMotoFormChecks(formInfo: MotoFormInfo,
                            convertedForm: ApiOfferModel.Offer,
                            needStatus: OfferStatus,
                            dealersInfo: Option[DealersInfoMessage]): Unit = {
    val form = formInfo.form
    val card = formInfo.card

    def check[T](func: ApiOfferModel.Offer => T): Unit = {
      assert(func(form) == func(convertedForm))
    }

    def checkMotoInfo[T](func: MotoModel.MotoInfo => T): Unit = {
      withClue(s"Expected moto:\n${form.getMotoInfo}\n\nConverted moto:\n${convertedForm.getMotoInfo}\n\n") {
        assert(func(form.getMotoInfo) == func(convertedForm.getMotoInfo))
      }
    }

    performFormChecks(formInfo, convertedForm, needStatus, dealersInfo)
    assert(convertedForm.getCategory == Category.MOTO)
    checkMotoInfo(_.getMark)
    checkMotoInfo(_.getModel)
    checkMotoInfo(_.getDisplacement)
    checkMotoInfo(_.getHorsePower)
    checkMotoInfo(_.getEngine)
    checkMotoInfo(_.getTransmission)
    checkMotoInfo(_.getGear)
    checkMotoInfo(_.getMotoType)
    checkMotoInfo(_.getAtvType)
    checkMotoInfo(_.getSnowmobileType)
    checkMotoInfo(_.getCylinderOrder)
    checkMotoInfo(_.getCylinderAmount)
    checkMotoInfo(_.getStrokeAmount)
    checkMotoInfo(_.getEquipmentMap.asScala.toSeq.sortBy(_._1))
  }

  def performTruckFormChecks(formInfo: TruckFormInfo,
                             convertedForm: ApiOfferModel.Offer,
                             needStatus: OfferStatus,
                             dealersInfo: Option[DealersInfoMessage]): Unit = {
    val form = formInfo.form
    val card = formInfo.card

    def check[T](func: ApiOfferModel.Offer => T): Unit = {
      assert(func(form) == func(convertedForm))
    }

    def checkTruckInfo[T](func: TrucksModel.TruckInfo => T): Unit = {
      withClue(s"Expected truck:\n${form.getTruckInfo}\n\nConverted truck:\n${convertedForm.getTruckInfo}\n\n") {
        assert(func(form.getTruckInfo) == func(convertedForm.getTruckInfo))
      }
    }

    performFormChecks(formInfo, convertedForm, needStatus, dealersInfo)
    assert(convertedForm.getCategory == Category.TRUCKS)
    checkTruckInfo(_.getDisplacement)
    checkTruckInfo(_.getSteeringWheel)
    checkTruckInfo(_.getCabin)
    checkTruckInfo(_.getLoading)
    checkTruckInfo(_.getSeats)
    checkTruckInfo(_.getAxis)
    checkTruckInfo(_.getSaddleHeight)
    checkTruckInfo(_.getChassisSuspension)
    checkTruckInfo(_.getCabinSuspension)
    checkTruckInfo(_.getSuspension)
    checkTruckInfo(_.getBrakes)
    checkTruckInfo(_.getWheelDrive)
    checkTruckInfo(_.getEuroClass)
    checkTruckInfo(_.getTrailerType)
    checkTruckInfo(_.getBusType)
    checkTruckInfo(_.getSwapBodyType)
    checkTruckInfo(_.getTruckCategory)
    checkTruckInfo(_.getGear)
    checkTruckInfo(_.getMark)
    checkTruckInfo(_.getModel)
    checkTruckInfo(_.getHorsePower)
    checkTruckInfo(_.getBodyType)
    checkTruckInfo(_.getEngine)
    checkTruckInfo(_.getTransmission)
    checkTruckInfo(_.getEquipmentMap.asScala.toSeq.sortBy(_._1))
    checkTruckInfo(_.getOperatingHours)
    checkTruckInfo(_.getLoadHeight)
    checkTruckInfo(_.getCraneRadius)
    checkTruckInfo(_.getBucketVolume)
    checkTruckInfo(_.getTractionClass)
  }

  def performCarFormChecks(formInfo: CarFormInfo,
                           convertedForm: ApiOfferModel.Offer,
                           needStatus: OfferStatus,
                           dealersInfo: Option[DealersInfoMessage]): Unit = {
    val form = formInfo.form
    val card = formInfo.card

    def check[T](func: ApiOfferModel.Offer => T): Unit = {
      assert(func(form) == func(convertedForm))
    }

    def checkCarInfo[T](func: CarsModel.CarInfo => T): Unit = {
      assert(func(form.getCarInfo) == func(convertedForm.getCarInfo))
    }

    performFormChecks(formInfo, convertedForm, needStatus, dealersInfo)
    assert(convertedForm.getCategory == Category.CARS)
    checkCarInfo(_.getArmored)
    checkCarInfo(_.getBodyType)
    checkCarInfo(_.getEngineType)
    checkCarInfo(c => Dictionaries.transmissions.byCode(c.getTransmission).map(_.code))
    checkCarInfo(c => Dictionaries.gearTypes.byCode(c.getDrive).map(_.code))
    checkCarInfo(_.getMark)
    checkCarInfo(_.getModel)
    checkCarInfo(_.getConfigurationId)
    checkCarInfo(_.getTechParamId)
    checkCarInfo(_.getWheelLeft)
    check(f =>
      f.toBuilder.getCarInfoBuilder
        .setTransmission(Dictionaries.transmissions.byCode(f.getCarInfo.getTransmission).map(_.code).get)
        .setDrive(Dictionaries.gearTypes.byCode(f.getCarInfo.getDrive).map(_.code).get)
        .setSuperGenId(0)
        .clearEquipment()
        .clearComplectationId()
        .clearHorsePower()
        .clearTechParam()
        .clearMarkInfo()
        .clearModelInfo()
        .clearConfiguration()
        .build()
    )
    assert(convertedForm.getCarInfo.getSuperGenId == card.supergen.id)
    // опции уникальные и отсортированы по имени
    checkCarInfo(_.getEquipmentMap.asScala.toSeq.sorted)
    checkCarInfo(_.getComplectationId)
    assert(convertedForm.getCarInfo.getHorsePower == card.enginePower)
  }

  //scalastyle:off method.length
  //scalastyle:off cyclomatic.complexity
  private def performFormChecks(formInfo: FormInfo,
                                convertedForm: ApiOfferModel.Offer,
                                needStatus: OfferStatus,
                                dealersInfo: Option[DealersInfoMessage]): Unit = {
    val form = formInfo.form

    def check[T](func: ApiOfferModel.Offer => T)(implicit pos: source.Position): Unit = {
      val expected = func(form)
      val actual = func(convertedForm)
      assert(expected == actual)
    }

    def checkState[T](func: ApiOfferModel.State => T)(implicit pos: source.Position): Unit = {
      assert(func(form.getState) == func(convertedForm.getState))
    }

    def checkDocuments[T](func: ApiOfferModel.Documents => T)(implicit pos: source.Position): Unit = {
      val expected = func(form.getDocuments)
      val actual = func(convertedForm.getDocuments)
      assert(expected == actual)
    }

    def checkPrivateSellerAndSeller[T](
        func: ApiOfferModel.PrivateSeller => T,
        func2: ApiOfferModel.Seller => T
    )(implicit pos: source.Position): Unit = {
      assert(func(form.getPrivateSeller) == func(convertedForm.getPrivateSeller))
      assert(func(form.getPrivateSeller) == func2(convertedForm.getSeller))
    }

    def checkSeller[T](func: ApiOfferModel.Seller => T)(implicit pos: source.Position): Unit = {
      assert(func(form.getSeller) == func(convertedForm.getSeller))
    }

    def checkSalon[T](func: ApiOfferModel.Salon => T)(implicit pos: source.Position): Unit = {
      assert(func(form.getSalon) == func(convertedForm.getSalon))
    }

    def checkAdditionalInfo[T](func: ApiOfferModel.AdditionalInfo => T)(implicit pos: source.Position): Unit = {
      assert(func(form.getAdditionalInfo) == func(convertedForm.getAdditionalInfo))
    }

    check(_.getColorHex)
    check(_.getPriceInfo.toBuilder.clearRurPrice.clearRurDprice().clearCreateTimestamp().build())
    check(_.getDescription)
    checkDocuments(_.getLicensePlate)
    check(_.getDocuments)
    checkState(_.getMileage)
    checkState(_.getStateNotBeaten)
    checkState(_.getCondition)
    checkState(_.getVideo)
    checkState(_.getDamagesList)
    checkState(_.getImageUrlsList.asScala.map(_.toBuilder.clearSizes().clearTransform().build()))
    check(
      _.toBuilder.getStateBuilder
        .clearImageUrls()
        .clearOriginalImage()
        .clearModerationImage()
        .clearC2BAuctionInfo()
        .build()
    )
    assert(convertedForm.getState.getImageUrlsCount == form.getState.getImageUrlsCount)
    if (needStatus != OfferStatus.DRAFT) {
      check(_.getUserRef)
    }
    if (form.hasSeller) {
      checkSeller(_.getName)
      checkSeller(_.getUnconfirmedEmail)
      checkSeller(_.getPhonesCount)
      for (idx <- 0 until form.getSeller.getPhonesCount) {
        checkSeller(_.getPhones(idx).toBuilder.clearOriginal().clearMask().build)

        val phone = convertedForm.getSeller.getPhones(idx)
        assert(phone.getOriginal == phone.getPhone)
      }
      checkSeller(_.getRedirectPhones)
      //ToDo enable test after migration
      //checkSeller(_.getChatsEnabled)
      checkSeller(_.getLocation.getGeobaseId)
    }
    if (form.optPrivateSeller.nonEmpty) {
      checkPrivateSellerAndSeller(_.getName, _.getName)
      checkPrivateSellerAndSeller(_.getPhonesCount, _.getPhonesCount)
      for (idx <- 0 until form.getPrivateSeller.getPhonesCount) {
        checkPrivateSellerAndSeller(
          _.getPhones(idx).toBuilder.clearOriginal().build,
          _.getPhones(idx).toBuilder.clearOriginal().build
        )

        val privateSellerPhone = convertedForm.getPrivateSeller.getPhones(idx)
        assert(privateSellerPhone.getOriginal == privateSellerPhone.getPhone)
        val sellerPhone = convertedForm.getSeller.getPhones(idx)
        assert(sellerPhone.getOriginal == sellerPhone.getPhone)
      }
      checkPrivateSellerAndSeller(_.getRedirectPhones, _.getRedirectPhones)
      checkPrivateSellerAndSeller(_.getLocation.getGeobaseId, _.getLocation.getGeobaseId)
    } else {
      if (form.getSalon.getSalonId != 0 && needStatus != OfferStatus.DRAFT) {
        // если в тестовой форме проставлен salon id (с вероятностью 0.9 он не ставится, так как и без него можно)
        // если мы не сохраняем черновик, то в прочитанной форме айдишник тоже должен быть
        checkSalon(_.getSalonId)
        checkSalon(_.getName)
      } else if (formInfo.isDealer && formInfo.extUserId == convertedForm.getUserRef) {
        // если же в форме salon id нет, либо мы сохраняем черновик, но с указанием пользователя - проверим
        // данные по салону в прочитанной форме
        assert(formInfo.optSalon.map(_.id).contains(convertedForm.getSalon.getSalonId))
        dealersInfo match {
          case Some(dealersInfo) =>
            assert(dealersInfo.getFullRussianName.contains(convertedForm.getSalon.getName))
          case None =>
            assert(formInfo.optSalon.map(_.properties.getOrElse("title", "")).contains(convertedForm.getSalon.getName))
        }
      }
    }
    check(f => {
      f.toBuilder.getAdditionalInfoBuilder
        .setHidden(false)
        .clearExpireDate()
        .clearActualizeDate()
        .clearCreationDate()
        .clearUpdateDate()
        .clearCanView()
        .clearLastStatus()
        .build()
    })
    checkAdditionalInfo(_.getExchange)
    checkAdditionalInfo(_.getHaggle)
    check(f => f.getBadgesList.asScala.sorted)
    assert(convertedForm.getAdditionalInfo.getHidden == (needStatus == OfferStatus.INACTIVE))
    if (needStatus != OfferStatus.DRAFT) {
      assert(
        convertedForm.getAdditionalInfo.getExpireDate ==
          AutoruCommonLogic.expireDate(convertedForm.getAdditionalInfo.getCreationDate).getMillis
      )
    } else {
      assert(convertedForm.getAdditionalInfo.getExpireDate == 0)
    }
    assert(convertedForm.getStatus == needStatus)
    assert(ProtoMacro.opt(convertedForm.getActions).nonEmpty)
    needStatus match {
      case OfferStatus.INACTIVE =>
        assert(convertedForm.getActions.getArchive)
        assert(convertedForm.getActions.getEdit)
        assert(!convertedForm.getActions.getHide)
        assert(convertedForm.getActions.getActivate)
      case OfferStatus.NEED_ACTIVATION if formInfo.isDealer =>
        assert(convertedForm.getActions.getArchive)
        assert(convertedForm.getActions.getEdit)
        assert(convertedForm.getActions.getHide)
        assert(!convertedForm.getActions.getActivate)
      case OfferStatus.NEED_ACTIVATION =>
        assert(!convertedForm.getActions.getArchive)
        assert(convertedForm.getActions.getEdit)
        assert(convertedForm.getActions.getHide)
        assert(!convertedForm.getActions.getActivate)
      case OfferStatus.DRAFT =>
        assert(!convertedForm.getActions.getArchive)
        assert(convertedForm.getActions.getEdit)
        assert(!convertedForm.getActions.getHide)
        assert(!convertedForm.getActions.getActivate)
      case _ =>
    }
  }

  //scalastyle:off number.of.methods
  implicit class RichFormBuilder(formBuilder: ApiOfferModel.Offer.Builder) {

    def withTechParam(techParam: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setTechParamId(techParam)
      formBuilder
    }

    def withoutPhones: ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.clearPhones()
      formBuilder
    }

    def withoutId: ApiOfferModel.Offer.Builder = {
      formBuilder.clearId()
    }

    def withCallFromCallTill(callHourStart: Int, callHourEnd: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.addPhones(
        formBuilder.getSeller
          .getPhones(0)
          .toBuilder
          .setCallHourStart(callHourStart)
          .setCallHourEnd(callHourEnd)
      )

      formBuilder
    }

    def withAddress(address: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.getLocationBuilder.setAddress(address)
      formBuilder
    }

    def withPhoneNumber(number: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.addPhones(
        formBuilder.getSeller
          .getPhones(0)
          .toBuilder
          .setPhone(number)
      )
      formBuilder
    }

    def withGeoId(geobaseId: Long): ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(geobaseId)
      formBuilder
    }

    def withPrice(price: BigDecimal, currency: Currency = Currency.RUB): ApiOfferModel.Offer.Builder = {
      formBuilder.getPriceInfoBuilder.setPrice(price).setCurrency(CurrencyUtils.fromCurrency(currency))
      formBuilder
    }

    def withOriginalPrice(price: BigDecimal, currency: Currency = Currency.RUB): ApiOfferModel.Offer.Builder = {
      formBuilder.getOriginalPriceBuilder.setPrice(price).setCurrency(CurrencyUtils.fromCurrency(currency))
      formBuilder
    }

    def withDescription(description: String): ApiOfferModel.Offer.Builder = {
      formBuilder.setDescription(description)
      formBuilder
    }

    def withOwnersNumber(ownersNumber: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.setOwnersNumber(ownersNumber)
      formBuilder
    }

    def withPurchaseDate(year: Int, month: Int, day: Int = 1): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.getPurchaseDateBuilder.setYear(year).setMonth(month).setDay(day)
      formBuilder
    }

    def withoutPurchaseDate: ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.clearPurchaseDate
      formBuilder
    }

    def withSTS(sts: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.setSts(sts)
      formBuilder
    }

    def withVIN(vin: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.setVin(vin)
      formBuilder
    }

    def withoutVIN: ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.clearVin()
      formBuilder
    }

    def withoutLicensePlate: ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.clearLicensePlate()
      formBuilder
    }

    def withoutName: ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.clearName()
      formBuilder
    }

    def withWarrantyExpire(year: Int, month: Int, day: Int = 1): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.getWarrantyExpireBuilder.setYear(year).setMonth(month).setDay(day)
      formBuilder
    }

    def withMileage(run: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getStateBuilder.setMileage(run)
      formBuilder
    }

    def withUsed(used: Boolean): ApiOfferModel.Offer.Builder = {
      if (used) formBuilder.setSection(Section.USED)
      else formBuilder.setSection(Section.NEW)
    }

    def withoutUnconfirmedEmail(): ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.clearUnconfirmedEmail()
      formBuilder
    }

    def withUnconfirmedEmail(email: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getSellerBuilder.setUnconfirmedEmail(email)
      formBuilder
    }

    def withoutWarrantyExpireDate(): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.clearWarrantyExpire()
      formBuilder
    }

    def withoutSuperGenId(): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.clearSuperGenId()
      formBuilder
    }

    def withMark(mark: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setMark(mark)
      formBuilder
    }

    def withModel(model: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setModel(model)
      formBuilder
    }

    def withColor(color: String): ApiOfferModel.Offer.Builder = {
      formBuilder.setColorHex(color)
    }

    def withAvailability(availability: Availability): ApiOfferModel.Offer.Builder = {
      formBuilder.setAvailability(availability)
    }

    def withSection(section: Section): ApiOfferModel.Offer.Builder = {
      formBuilder.setSection(section)
    }

    def withPts(pts: PtsStatus): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.setPts(pts)
      formBuilder
    }

    def withEquipment(equipmentCode: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.putEquipment(equipmentCode, true)
      formBuilder
    }

    def withYear(year: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.setYear(year)
      formBuilder
    }

    def withBadges(badges: List[String]): ApiOfferModel.Offer.Builder = {
      formBuilder.clearBadges()
      formBuilder.addAllBadges(badges.asJava)
    }

    def withYoutubeVideo(id: String, url: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getStateBuilder.getVideoBuilder.clearYandexId().setYoutubeUrl(url).setYoutubeId(id)
      formBuilder
    }

    def withSalonId(salonId: Long): ApiOfferModel.Offer.Builder = {
      formBuilder.getSalonBuilder.setSalonId(salonId)
      formBuilder
    }

    def withNotRegisteredInRussia(value: Boolean): ApiOfferModel.Offer.Builder = {
      formBuilder.getDocumentsBuilder.setNotRegisteredInRussia(value)
      formBuilder
    }

    def withBodyType(bodyType: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setBodyType(bodyType)
      formBuilder
    }

    def withoutBodyType: ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.clearBodyType()
      formBuilder
    }

    def withEngineType(engineType: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setEngineType(engineType)
      formBuilder
    }

    def withoutEngineType: ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.clearEngineType()
      formBuilder
    }

    def withDrive(drive: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setDrive(drive)
      formBuilder
    }

    def withoutDrive: ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.clearDrive()
      formBuilder
    }

    def withTransmission(transmission: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.setTransmission(transmission)
      formBuilder
    }

    def withoutTransmission: ApiOfferModel.Offer.Builder = {
      formBuilder.getCarInfoBuilder.clearTransmission()
      formBuilder
    }

    def withHidden(hidden: Boolean): ApiOfferModel.Offer.Builder = {
      formBuilder.getAdditionalInfoBuilder.setHidden(hidden)
      formBuilder
    }

    def withoutCoord: ApiOfferModel.Offer.Builder = {
      formBuilder.getPrivateSellerBuilder.getLocationBuilder.clearCoord()
      formBuilder
    }

    def withPhoto(name: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getStateBuilder.addImageUrlsBuilder().setName(name)
      formBuilder
    }

    def withVideo(id: String, youtubeId: Option[String], url: String): ApiOfferModel.Offer.Builder = {
      formBuilder.getStateBuilder.getVideoBuilder.setYandexId(id).setYoutubeUrl(url)
      youtubeId.foreach(formBuilder.getStateBuilder.getVideoBuilder.setYoutubeId)
      formBuilder
    }

    def withoutVideo: ApiOfferModel.Offer.Builder = {
      formBuilder.getStateBuilder.clearVideo()
      formBuilder
    }

    def withTruckLoading(loading: Long): ApiOfferModel.Offer.Builder = {
      formBuilder.getTruckInfoBuilder.setLoading(loading.toInt)
      formBuilder
    }

    def withTruckAxis(axis: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getTruckInfoBuilder.setAxis(axis)
      formBuilder
    }

    def withTruckSeats(seats: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getTruckInfoBuilder.setSeats(seats)
      formBuilder
    }

    def withTruckDisplacement(displacement: Long): ApiOfferModel.Offer.Builder = {
      formBuilder.getTruckInfoBuilder.setDisplacement(displacement.toInt)
      formBuilder
    }

    def withTruckHorsePower(horsePower: Int): ApiOfferModel.Offer.Builder = {
      formBuilder.getTruckInfoBuilder.setHorsePower(horsePower)
      formBuilder
    }

    def withAutoruExpert: ApiOfferModel.Offer.Builder = {
      formBuilder.getAdditionalInfoBuilder.setAutoruExpert(true)
      formBuilder
    }
  }
}
