package ru.yandex.vos2.autoru.utils.moderation

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.{Category, PtsStatus}
import ru.auto.api.CommonModel
import ru.auto.api.TrucksModel.Dredge
import ru.yandex.auto.message.CatalogSchema.{CatalogCardMessage, ConfigurationMessage, TechparameterMessage}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.MotoInfo.MotoCategory
import ru.yandex.vos2.AutoruModel.AutoruOffer.MotoInfo.MotoCategory._
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.TruckCategory
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CustomHouseStatus, MotoInfo, TruckInfo}
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.dao.proxy.AdditionalDataForReading
import ru.yandex.vos2.autoru.model.SalonPoiContacts.PoiPhone
import ru.yandex.vos2.autoru.model.{SalonPoi, TestUtils}
import ru.yandex.vos2.autoru.utils.EnumUtils
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter
import ru.yandex.vos2.services.mds.{AutoruAllNamespaceSettings, MdsPhotoUtils}

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class FieldModifiersTest extends AnyFunSuite with InitTestDbs with MockitoSupport {

  private val mdsPhotoUtils =
    MdsPhotoUtils("http://avatars-int.mds.yandex.net:13000", "//avatars.mds.yandex.net", AutoruAllNamespaceSettings)
  private val carCatalog = mock[CarsCatalog]
  private val fieldModifiers = new FieldModifiers(mdsPhotoUtils, carCatalog)

  private val offerFormConverter = new OfferFormConverter(
    components.mdsPhotoUtils,
    components.regionTree,
    components.mdsPanoramasUtils,
    components.offerValidator,
    components.salonConverter,
    components.currencyRates,
    components.featuresManager,
    components.banReasons,
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog
  )

  private val CatalogCardRelated =
    Set[FieldModifiers.FieldInfo[_]](fieldModifiers.CarEngineVolume, fieldModifiers.CarDoorsCount)

  when(carCatalog.getCardByTechParamId(?)).thenReturn(
    Some(
      CarCard(
        CatalogCardMessage
          .newBuilder()
          .setVersion(1)
          .setConfiguration(ConfigurationMessage.newBuilder().setVersion(1).setDoorsCount(5))
          .setTechparameter(TechparameterMessage.newBuilder().setVersion(1).setDisplacement(2000))
          .build()
      )
    )
  )

  private def createOffer(isDealer: Boolean,
                          category: Category,
                          motoCategory: Option[MotoCategory] = None,
                          truckCategory: Option[TruckCategory] = None): Offer = {
    assert((category == Category.MOTO) == motoCategory.isDefined)
    assert((category == Category.TRUCKS) == truckCategory.isDefined)
    val builder = TestUtils.createOffer(dealer = isDealer, category = category)
    builder.getOfferAutoruBuilder.getOwnershipBuilder.setPtsOwnersCount(2)
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setIsPtsOriginal(true)
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.ORIGINAL)
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setStsCode("sts1")
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("VIN1111111")
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.CLEARED)
    builder.getOfferAutoruBuilder.getEssentialsBuilder.setYear(2001)
    builder.getOfferAutoruBuilder.getStateBuilder.setMileage(10000)
    builder.getOfferAutoruBuilder.getStateBuilder.setCondition(AutoruOffer.Condition.EXCELLENT)
    builder.getOfferAutoruBuilder.setColorHex("ffffff")
    if (isDealer) {
      builder.getOfferAutoruBuilder.getSalonBuilder.getPlaceBuilder.setAddress("ул. Льва Толстого")
      builder.getOfferAutoruBuilder.getSalonBuilder.getPlaceBuilder.setGeobaseId(255)
      builder.getOfferAutoruBuilder.getSalonBuilder.setContactName("ООО Салон")
      builder.getOfferAutoruBuilder.getSalonBuilder
        .addPhoneBuilder()
        .setNumber("111222")
        .setTitle("phone1")
        .setNumberMask("111000")
        .setCallHourStart(9)
        .setCallHourEnd(20)
    } else {
      builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setAddress("ул. Льва Толстого")
      builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(255)
      builder.getOfferAutoruBuilder.getSellerBuilder.setUserName("Иван")
      builder.getOfferAutoruBuilder.getSellerBuilder
        .addPhoneBuilder()
        .setNumber("111222")
        .setTitle("phone1")
        .setNumberMask("111000")
        .setCallHourStart(9)
        .setCallHourEnd(20)
    }
    builder.setDescription("Foo bar")
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setName("111-photoname")
      .setNamespace("autoru-test")
      .setIsMain(true)
      .setOrder(1)
      .setCreated(14000000001L)
      .setPhotoPreview(CommonModel.SmallPhotoPreview.newBuilder().setWidth(200).setHeight(100).build())
      .setCurrentTransform(BasicsModel.PhotoTransform.newBuilder().setAngle(90).setBlur(true))
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setName("111-photoname1")
      .setNamespace("autoru-test")
      .setIsMain(true)
      .setOrder(1)
      .setCreated(14000000001L)
      .setDeleted(true)
      .setPhotoPreview(CommonModel.SmallPhotoPreview.newBuilder().setWidth(200).setHeight(100).build())
      .setCurrentTransform(BasicsModel.PhotoTransform.newBuilder().setAngle(90).setBlur(true))
    builder.getOfferAutoruBuilder.getPriceBuilder
      .setPrice(500000)
      .setCurrency(BasicsModel.Currency.RUB)
      .setCreated(14000000000L)
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate("100УУ")

    category match {
      case Category.CARS =>
        val cib = builder.getOfferAutoruBuilder.getCarInfoBuilder
        cib.setTechParamId(1234)
        cib.setConfigurationId(4321)
        cib.setSteeringWheel(AutoruOffer.SteeringWheel.LEFT)
        cib.setMark("FORD")
        cib.setModel("FOCUS")
        cib.setHorsePower(200)
        cib.setDisplacement(2000)
        cib.setDoorsCount(5)
        cib.setGearType("FULL")
        cib.setTransmission("MECHANICAL")
        cib.setEngineType("DIESEL")
        cib.setSuperGenId(123)
        cib.setBodyType("SEDAN")
      case Category.TRUCKS =>
        val tib = builder.getOfferAutoruBuilder.getTruckInfoBuilder
        tib.setWheelType(AutoruOffer.SteeringWheel.LEFT)
        tib.setAutoCategory(truckCategory.get)
        tib.setMark("FORD")
        tib.setMark("TRANSIT")
        tib.setHorsePower(1000)
        tib.setEngineVolume(6000)
        if (truckCategory.contains(TruckCategory.TRUCK_CAT_SWAP_BODY)) {
          tib.setContainerType(TruckInfo.ContainerType.TRUCK_CONTAINER_SB_VAN)
        }
        tib.setDriveKey(TruckInfo.DriveType.DRIVE_BACK)
        tib.setTransmission(TruckInfo.Transmission.TRANSMISSION_AUTOMATIC)
        tib.setCabinType(TruckInfo.CabinType.SEAT_6)
        tib.setEngineType(TruckInfo.Engine.DIESEL)
        tib.setSeats(6)
        tib.setLoading(5000)
        tib.setWheelDrive(TruckInfo.WheelDrive.WD_6_2)
        if (truckCategory.contains(TruckCategory.TRUCK_CAT_TRAILER)) {
          tib.setTrailerType(TruckInfo.TrailerType.TRUCK_TRAILER_MOTO_TRAILER)
        }
        truckCategory.get match {
          case TruckCategory.TRUCK_CAT_LCV =>
            tib.setBodyType(TruckInfo.BodyType.AMBULANCE)
          case TruckCategory.TRUCK_CAT_TRUCK =>
            tib.setBodyType(TruckInfo.BodyType.REFRIGERATOR)
          case TruckCategory.TRUCK_CAT_AGRICULTURAL =>
            tib.setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_COMBAIN_HARVESTER)
          case TruckCategory.TRUCK_CAT_CONSTRUCTION =>
            tib.setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_DRILLING_PILING_MACHINE)
          case TruckCategory.TRUCK_CAT_AUTOLOADER =>
            tib.setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_FORKLIFTS_ELECTRO)
          case TruckCategory.TRUCK_CAT_DREDGE =>
            tib.setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_PLANNER_EXCAVATOR)
          case TruckCategory.TRUCK_CAT_BULLDOZERS =>
            tib.setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_WHEELS_BULLDOZER)
          case TruckCategory.TRUCK_CAT_MUNICIPAL =>
            tib.setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_GARBAGE_TRUCK)
          case _ =>
        }

      case Category.MOTO =>
        val mib = builder.getOfferAutoruBuilder.getMotoInfoBuilder
        mib.setCategory(motoCategory.get)
        mib.setMark("HONDA")
        mib.setModel("CD_5090")
        mib.setEnginePower(20)
        mib.setEngineVolume(900)
        mib.setDriveKey(MotoInfo.DriveType.MOTO_DRIVE_FULL)
        mib.setTransmission(MotoInfo.Transmission.MOTO_TRANSM_1)
        mib.setEngine(MotoInfo.Engine.MOTO_ENGINE_DIESEL)
        mib.setStrokes(MotoInfo.Strokes.MOTO_STROKES_2)
        mib.setCylinders(MotoInfo.Cylinders.MOTO_CYLINDERS_1)
        mib.setCylindersType(MotoInfo.CylindersType.MOTO_CYL_TYPE_LINE)

        motoCategory.get match {
          case MotoCategory.MOTO_CAT_MOTORCYCLE =>
            mib.setTypeKey(MotoInfo.MotoType.MOTO_TYPE_ALLROUND)
          case MotoCategory.MOTO_CAT_ATV =>
            mib.setTypeKey(MotoInfo.MotoType.MOTO_TYPE_AMPHIBIAN)
          case MotoCategory.MOTO_CAT_SNOWMOBILE =>
            mib.setTypeKey(MotoInfo.MotoType.MOTO_TYPE_CHILDISH)
          case _ =>
        }
    }
    builder.build()
  }

  for {
    isDealer <- Seq(false, true)
    category <- Seq(Category.CARS, Category.TRUCKS, Category.MOTO)
    isBlank <- Seq(true, false)
    truckCategoryOpt <- if (category == Category.TRUCKS && !isBlank) {
      TruckCategory
        .values()
        .filter(EnumUtils.isValidEnumValue)
        .filterNot(
          _.name()
            .contains("TRUCK_CAT_CATEGORY_")
        )
        .map(Some.apply)
        .toList
    } else {
      Seq(None)
    }
    motoCategoryOpt <- if (category == Category.MOTO && !isBlank)
      List(MOTO_CAT_MOTORCYCLE, MOTO_CAT_ATV, MOTO_CAT_SNOWMOBILE, MOTO_CAT_SCOOTERS).map(Some.apply)
    else
      Seq(None)
  } test(
    s"check modifiers (isBlank=$isBlank, isDealer = $isDealer, category = $category, " +
      s"truckCat=${truckCategoryOpt.orNull}, motoCat=${motoCategoryOpt.orNull})"
  ) {
    implicit val t: Traced = Traced.empty
    val offer =
      if (isBlank) TestUtils.createOffer(dealer = isDealer, category = category).build()
      else createOffer(isDealer, category, motoCategoryOpt, truckCategoryOpt)

    val ad = if (isDealer && !isBlank) {
      AdditionalDataForReading(
        Some(
          new SalonPoi(
            0L,
            None,
            0,
            None,
            None,
            None,
            None,
            None,
            None,
            Some("ул. Льва Толстого"),
            None,
            None,
            DateTime.now(),
            DateTime.now(),
            Map(SalonPoi.Title -> "ООО Салон"),
            Seq(PoiPhone(1L, 1L, Some("phone1"), "111222", "111000", Some(9), Some(20))),
            None,
            false,
            Map.empty,
            Nil
          ) {
            override def geobaseId: Option[Long] = Some(255L)
          }
        )
      )
    } else {
      AdditionalDataForReading()
    }

    val form = offerFormConverter.convert(ad, offer)
    val checkedModifiers = collection.mutable.Set.empty[FieldModifiers.FieldInfo[_]]

    fieldModifiers.fields.toSeq.sortBy(_._1).foreach {
      case (field, modifiersList) =>
        modifiersList.foreach { modifiers =>
          val modifier = modifiers.asInstanceOf[FieldModifiers.FieldInfo[AnyRef]]
          val builder = offer.toBuilder
          if (modifier.offerHas(offer)) {
            // offer modifiers test
            val value = modifier.offerGet(offer)
            assert(modifier.offerHas(builder), s"Method offerHas works correctly on offer has builder by $field")
            modifier.offerClear(builder)
            assert(!modifier.offerHas(builder), s"Method offerClear removes field value by $field")
            modifier.offerSet(builder, value)
            assert(modifier.offerHas(builder), s"Method offerSet set some value by $field")
            assert(modifier.offerGet(offer) == modifier.offerGet(builder), s"Method offerSet by $field")
            assert(modifier.offerFieldEquals(offer, builder), s"Offers field equality by $field")

            // form modifiers test
            val formBuilder = form.toBuilder
            assert(modifier.offerFormFieldEquals(offer, formBuilder), s"Offer and form field equality by $field")
            if (!CatalogCardRelated.contains(modifier)) {
              modifier.formClear(formBuilder)
              assert(!modifier.offerFormFieldEquals(offer, formBuilder), s"Offer and form field inequality by $field")
              modifier.formSet(formBuilder, value)
              assert(modifier.offerFormFieldEquals(offer, formBuilder), s"Method formSet by $field")
              info(s"$field modifiers works by $field")
            }
            checkedModifiers.add(modifier)
          } else {
            if (!CatalogCardRelated.contains(modifier)) {
              if (!modifier.offerFormFieldEquals(offer, form)) {
                val offerVal = modifier.offerGet(offer)
                val formVal = modifier.formGet(form)
                throw new AssertionError(s"Offer and form field equality by $field: $offerVal != $formVal")
              }
            }
            modifier.offerClear(builder)
            assert(!modifier.offerHas(builder), s"Allow clear already empty field by $field")
          }
        }
    }
  }

  test("don't fall on incorrect sub-category type in form") {
    implicit val t: Traced = Traced.empty
    val offer =
      createOffer(isDealer = true, Category.TRUCKS, truckCategory = Some(TruckCategory.TRUCK_CAT_AGRICULTURAL))
    val formBuilder = offerFormConverter.convert(AdditionalDataForReading(), offer).toBuilder
    formBuilder.getTruckInfoBuilder.setDredgeType(Dredge.Type.PLANNER_EXCAVATOR)
    assert(
      fieldModifiers.AgriculturalType
        .offerFormFieldEquals(offer, formBuilder)
    )
    assert(!fieldModifiers.DredgeType.offerFormFieldEquals(offer, formBuilder))
  }

  test("don't fall on incorrect sub-category type in offer") {
    implicit val t: Traced = Traced.empty
    val offer =
      createOffer(isDealer = true, Category.TRUCKS, truckCategory = Some(TruckCategory.TRUCK_CAT_AGRICULTURAL))
    val form = offerFormConverter.convert(AdditionalDataForReading(), offer)
    val offerBuilder = offer.toBuilder
    offerBuilder.getOfferAutoruBuilder.getTruckInfoBuilder
      .setSpecialTypeKey(TruckInfo.SpecialType.SPECIAL_TYPE_PLANNER_EXCAVATOR)
    assert(!fieldModifiers.AgriculturalType.offerFormFieldEquals(offerBuilder, form))
    assert(fieldModifiers.DredgeType.offerFormFieldEquals(offerBuilder, form))
  }
}
