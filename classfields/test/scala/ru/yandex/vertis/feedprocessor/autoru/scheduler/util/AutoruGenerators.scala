package ru.yandex.vertis.feedprocessor.autoru.scheduler.util

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.feedprocessor.autoru.model.{SaleCategories, Task, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.TruckExternalOffer.{BusInfo, LcvInfo, TruckInfo}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.{
  AutoruExternalOffer,
  CarExternalOffer,
  Currency,
  Panoramas,
  TruckExternalOffer
}
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.scheduler.converter.AutoruOfferConverter
import ru.yandex.vertis.feedprocessor.autoru.scheduler.converter.TruckInfoConverter
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.{DiscountPrice, ModificationString}

/**
  * @author pnaydenov
  */
object AutoruGenerators {
  private val identifier = Gen.identifier.filter(_.nonEmpty)

  def autoruOfferGen(taskGen: Gen[Task]): Gen[AutoruExternalOffer] =
    Gen.oneOf(carExternalOfferGen(taskGen), truckExternalOfferGen(taskGen))

  val wheelGen: Gen[String] = Gen.oneOf("Левый", "Правый")

  //scalastyle:off
  def carExternalOfferGen(taskGen: Gen[Task]): Gen[CarExternalOffer] =
    for {
      task <- taskGen
      position <- Gen.posNum[Int]
      saleCategoryId = 15
      sectionId <- sectionIdGen
      mark <- Gen.identifier
      model <- Gen.identifier
      modification <- Gen.identifier
      bodyType <- Gen.identifier
      wheel <- wheelGen
      color <- Gen.identifier
      metalic <- Gen.oneOf("Да", "Нет")
      availability <- Gen.oneOf("В наличии", "На заказ")
      custom <- Gen.oneOf("Растаможен", "Не растаможен")
      state <- Gen.oneOf("Отличное", "Хорошее", "Среднее", "Требует ремонта")
      run <- if (sectionId == 1) Gen.posNum[Int] else Gen.const(0)
      year <- Gen.posNum[Int]
      price <- Gen.posNum[Int]
      ownersNumber <- Gen.oneOf(
        "Не было владельцев",
        "Один владелец",
        "Два владельца",
        "Три владельца",
        "Три и более",
        "Четыре и более"
      )
      color <- Gen.oneOf(AutoruOfferConverter.COLOR_MAP.keys.toSeq)
      discountPrice <- Gen.oneOf(None, Some((Gen.posNum[Double], Gen.oneOf("hide", "show"))))
      vin <- Gen.identifier
      currency <- Gen.identifier.map(Currency.from)
      description <- Gen.alphaNumStr
      sts <- Gen.alphaNumStr
      pts <- Gen.oneOf("оригинал", "дубликат", "")
      video <- Gen.oneOf(
        "https://youtu.be/MJIC9MWhrrs",
        "youtube.com/watch?v=MJIC9MWhrrs",
        "https://www.youtube.com/embed/MJIC9MWhrrs"
      )
      modificationCode <- Gen.const(Some("A2S6D1617D216"))
      colorCode <- Gen.const(Some("9C9C"))
      interiorCode <- Gen.const(Some("ZBRJ"))
      equipmentCodes <- Gen.const(Some(Seq("5DL, 3AG")))
      badgesCount <- Gen.chooseNum(0, 3)
      badges <- Gen.listOfN(badgesCount, identifier)
      creditDiscount <- Gen.oneOf(None, Some(Gen.posNum[Int]))
      tradeInDiscount <- Gen.oneOf(None, Some(Gen.posNum[Int]))
      insuranceDiscount <- Gen.oneOf(None, Some(Gen.posNum[Int]))
      maxDiscount <- Gen.oneOf(None, Some(Gen.posNum[Int]))
      spinCarUrlGen <- Gen.oneOf(Gen.identifier, idWithHashtagGen)
      originalPrice <- Gen.option(Gen.posNum[Int])
      onlineViewAvailable <- Gen.oneOf(true, false)
      bookingAllowed <- Gen.oneOf(true, false)
      avitoSaleServices <- Gen.listOf(Gen.alphaStr)
      dromSaleServices <- Gen.listOf(Gen.alphaStr)
      avitoDescription <- Gen.option(Gen.alphaStr)
      dromDescription <- Gen.option(Gen.alphaStr)
      classifieds <- Gen.option(Gen.listOf(Gen.oneOf("drom", "autoru", "avito")))
      multipostingEnabled <- Gen.oneOf(false, true)
      pledgeNumber <- Gen.option(Gen.const("0000-111-222222-333"))
      notRegisteredInRussia <- Gen.option(Gen.oneOf(false, true))
      geobaseId <- Gen.posNum[Long]
      city <- Gen.option(Gen.identifier)
    } yield {
      val innerTask = task.copy(
        serviceInfo = task.serviceInfo.copy(
          autoru = task.serviceInfo.autoru.copy(saleCategoryId = saleCategoryId)
        )
      )
      CarExternalOffer(
        position = position,
        category = Category.CARS,
        section = Section.forNumber(sectionId),
        uniqueId = None,
        mark = mark,
        model = model,
        modification = ModificationString(modification),
        complectation = None,
        bodyType = bodyType,
        wheel = wheel,
        color = color,
        metallic = Some(metalic),
        availability = availability,
        custom = custom,
        state = Some(state),
        ownersNumber = Some(ownersNumber),
        run = Some(run),
        year = year,
        registryYear = None,
        price = price,
        creditDiscount = creditDiscount.map(_.next),
        tradeinDiscount = tradeInDiscount.map(_.next),
        insuranceDiscount = insuranceDiscount.map(_.next),
        maxDiscount = maxDiscount.map(_.next),
        currency = currency,
        withNds = None,
        vin = Some(vin),
        description = Some(description),
        extras =
          Some(Seq("Штатная аудиосистема с CD", "Подушки безопасности боковые", "Электропривод крышки багажника")),
        optionIds = Some(Seq("audiosystem-cd", "airbag-side", "electro-trunk")),
        images = Seq.empty,
        avitoImages = Seq.empty,
        dromImages = Seq.empty,
        video = Some(video),
        poiId = None,
        deliveryInfo = None,
        saleServices = Seq.empty,
        serviceAutoApply = None,
        contactInfo = Seq.empty,
        warrantyExpire = None,
        pts = Some(pts),
        sts = Some(sts),
        armored = None,
        modificationCode = modificationCode,
        colorCode = colorCode,
        interiorCode = interiorCode,
        equipmentCodes = equipmentCodes,
        action = None,
        exchange = None,
        discountPrice = {
          if (discountPrice.isEmpty) None
          else Some(DiscountPrice(Math.min(discountPrice.get._1.next, price - 1), discountPrice.get._2.next))

        },
        badges = badges,
        taskContext = TaskContext(innerTask),
        createdAt = System.currentTimeMillis,
        unification = Some(carUnificationGen(mark, model, bodyType).next),
        panoramas = Some(Panoramas(Some(spinCarUrlGen), None)),
        originalPrice = originalPrice,
        onlineViewAvailable = Some(onlineViewAvailable),
        bookingAllowed = Some(bookingAllowed),
        avitoSaleServices = avitoSaleServices,
        dromSaleServices = dromSaleServices,
        avitoDescription = avitoDescription,
        dromDescription = dromDescription,
        classifieds = classifieds,
        multipostingEnabled = Some(multipostingEnabled),
        pledgeNumber = pledgeNumber,
        notRegisteredInRussia = notRegisteredInRussia,
        geobaseId = Some(geobaseId),
        city = city
      )
    }

  def idWithHashtagGen: Gen[String] =
    for {
      id <- Gen.identifier
      hashTag <- Gen.identifier
    } yield "%s#%s".format(id, hashTag)

  def carUnificationGen(mark: String, model: String, bodyType: String) =
    for {
      transmission <- Gen.alphaStr
      superGenId <- Gen.posNum[Long]
      configurationId <- Gen.posNum[Long]
      techParamId <- Gen.posNum[Long]
      engineType <- Gen.alphaStr
      gearType <- Gen.alphaStr
      horsePower <- Gen.posNum[Int]
      displacement <- Gen.posNum[Int]
      complectationId <- Gen.posNum[Long]
    } yield {
      AutoruExternalOffer.Unification(
        mark = Some(mark),
        model = Some(model),
        bodyType = Some(bodyType),
        transmission = Some(transmission),
        superGenId = Some(superGenId),
        configurationId = Some(configurationId),
        techParamId = Some(techParamId),
        engineType = Some(engineType),
        gearType = Some(gearType),
        horsePower = Some(horsePower),
        displacement = Some(displacement),
        complectationId = Some(complectationId)
      )
    }

  def truckExternalOfferGen(
      taskGen: Gen[Task],
      infoGen: Gen[TruckExternalOffer.Info] = truckLcvInfoGen()): Gen[TruckExternalOffer] =
    for {
      task <- taskGen
      position <- Gen.posNum[Int]
      sectionId <- sectionIdGen
      mark <- Gen.alphaNumStr
      model <- Gen.alphaNumStr
      modification <- Gen.option(Gen.alphaNumStr)
      price <- Gen.posNum[Int]
      availability <- Gen.oneOf("В наличии", "На заказ")
      imagesCount <- Gen.chooseNum(1, 10)
      images <- Gen.listOfN(imagesCount, identifier.filter(_.nonEmpty).map("http://" + _ + ".jpg"))
      description <- Gen.alphaNumStr
      vin <- Gen.alphaNumStr
      year <- Gen.chooseNum[Int](1900, 2017)
      info <- infoGen
      badgesCount <- Gen.chooseNum(0, 3)
      badges <- Gen.listOfN(badgesCount, identifier)
      avitoImagesCount <- Gen.chooseNum(1, 10)
      avitoImages <- Gen.listOfN(avitoImagesCount, identifier.filter(_.nonEmpty).map("http://" + _ + ".jpg"))
      dromImagesCount <- Gen.chooseNum(1, 10)
      dromImages <- Gen.listOfN(dromImagesCount, identifier.filter(_.nonEmpty).map("http://" + _ + ".jpg"))
      avitoSaleServices <- Gen.listOf(Gen.alphaStr)
      dromSaleServices <- Gen.listOf(Gen.alphaStr)
      avitoDescription <- Gen.option(Gen.alphaStr)
      dromDescription <- Gen.option(Gen.alphaStr)
      classifieds <- Gen.option(Gen.listOf(Gen.alphaStr))
      geobaseId <- Gen.posNum[Long]
      city <- Gen.option(Gen.identifier)
    } yield {
      def setCategory(task: Task, category: SaleCategories): Task = {
        task.copy(
          serviceInfo = task.serviceInfo.copy(
            autoru = task.serviceInfo.autoru.copy(saleCategoryId = category.id)
          )
        )
      }

      val innerTask = info match {
        case _: LcvInfo => setCategory(task, SaleCategories.Lcv)
        case _: BusInfo => setCategory(task, SaleCategories.Bus)
        case _: TruckInfo => setCategory(task, SaleCategories.Truck)
        case _ => task
      }

      TruckExternalOffer(
        position = position,
        category = Category.TRUCKS,
        section = Section.forNumber(sectionId),
        mark = mark,
        model = model,
        modification = modification,
        price = price,
        currency = Currency.from("RUB"),
        withNds = None,
        exchange = None,
        saleServices = Nil,
        serviceAutoApply = None,
        availability = availability,
        images = images,
        color = "Белый",
        state = None,
        description = Some(description),
        extras = None,
        vin = Some(vin),
        year = year,
        run = None,
        custom = None,
        uniqueId = None,
        haggle = None,
        action = None,
        poiId = None,
        deliveryInfo = None,
        contactInfo = Nil,
        badges = badges,
        info = info,
        taskContext = TaskContext(innerTask),
        avitoImages = avitoImages,
        dromImages = dromImages,
        avitoSaleServices = avitoSaleServices,
        dromSaleServices = dromSaleServices,
        avitoDescription = avitoDescription,
        dromDescription = dromDescription,
        classifieds = classifieds,
        geobaseId = Some(geobaseId),
        city = city
      )
    }

  def truckLcvInfoGen(): Gen[TruckExternalOffer.Info] =
    for {
      wheel <- wheelGen
      engineVolume <- Gen.chooseNum(1000, 4000)
      bodyType <- Gen.oneOf(TruckInfoConverter.LCV_BODY_TYPES.keys.toSeq)
      drive <- Gen.oneOf(TruckInfoConverter.DRIVE_TYPES.keys.toSeq)
    } yield LcvInfo(
      wheel = wheel,
      enginePower = None,
      engineVolume = engineVolume.toString,
      engineType = None,
      gearbox = None,
      loading = None,
      seats = None,
      bodyType = bodyType,
      drive = drive
    )

  def truckInfoGen(): Gen[TruckExternalOffer.Info] =
    for {
      wheel <- wheelGen
      engineVolume <- Gen.chooseNum(1000, 4000)
      bodyType <- identifier
      drive <- identifier
    } yield TruckInfo(
      wheel = wheel,
      enginePower = None,
      engineVolume = engineVolume.toString,
      engineType = None,
      gearbox = None,
      loading = None,
      wheelDrive = None,
      suspensionCabin = None,
      bodyType = bodyType,
      ecoClass = None,
      fuelTanks = None,
      fuelTanksVolume = None,
      fuelTanksMaterial = None,
      cabinType = None,
      suspensionChassis = None
    )

  def truckBusInfoGen(): Gen[TruckExternalOffer.Info] =
    for {
      busType <- identifier
      wheel <- wheelGen
      wheelDrive <- identifier
      seats <- Gen.chooseNum(10, 20)
      enginePower <- identifier
      engineVolume <- Gen.chooseNum(1000, 4000)
      engineType <- identifier
      gearbox <- identifier
    } yield BusInfo(
      busType = busType,
      wheel = wheel,
      wheelDrive = Some(wheelDrive),
      seats = Some(seats),
      enginePower = Some(enginePower),
      engineVolume = engineVolume.toString,
      engineType = Some(engineType),
      gearbox = Some(gearbox)
    )
}
