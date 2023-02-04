package ru.yandex.auto.garage.converters.cards

import auto.carfax.common.utils.avatars.{AvatarsExternalUrlsBuilder, CarfaxNamespace, PhotoInfoId}
import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.BoolValue
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito.reset
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.PtsStatus
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel
import ru.auto.api.CommonModel.{Date, Photo}
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.Common.{IdentifierType, Photo => Image}
import ru.auto.api.vin.VinReportModel
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.auto.panoramas.PanoramasModel.Panorama
import ru.yandex.auto.garage.managers.GetCardOptions
import ru.yandex.auto.garage.utils.features.GarageFeaturesRegistry
import ru.yandex.auto.vin.decoder.amazon.{FileExternalUrlsBuilder, MdsS3StorageFactory, S3Storage}
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.CarsCatalog
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.model.{Mark, Model, SuperGeneration}
import ru.yandex.auto.vin.decoder.extdata.moderation.ProvenOwnerValidationDictionary
import ru.yandex.auto.vin.decoder.extdata.region.Tree
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.ProvenOwnerState.{DocumentsPhotos, Verdict}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.VehicleInfo.{Equipment, Mileage}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, Meta, Source, VehicleInfo}
import ru.yandex.auto.vin.decoder.proto.TtxSchema._
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.PhotoTransformation
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{MapHasAsJava, MapHasAsScala, SeqHasAsJava}

class InternalToPublicCardConverterTest extends AnyFunSuite with MockitoSupport {

  implicit val t = Traced.empty

  private val urlsBuilder = new AvatarsExternalUrlsBuilder("avatars.ru")
  private val tree = mock[Tree]
  private val catalog = mock[CarsCatalog]
  private val dictionary = mock[ProvenOwnerValidationDictionary]
  private val garageFeaturesRegistry = mock[GarageFeaturesRegistry]
  val avatarsExternalUrlsBuilder = new AvatarsExternalUrlsBuilder("avatars.mdst.yandex.net")

  private val garageMdsS3Storage: S3Storage = {
    val url = "s3-private.mds.yandex.net"
    val bucket = "garage"
    val accessKey = "some_access_key"
    val secretKey = "some_secret_key"
    MdsS3StorageFactory(url, bucket, accessKey, secretKey)
  }

  private val fileExternalUrlsBuilder = new FileExternalUrlsBuilder(garageMdsS3Storage)

  private val featureValueTrue = mock[Feature[Boolean]]
  when(featureValueTrue.value).thenReturn(true)

  private val featureValueFalse = mock[Feature[Boolean]]
  when(featureValueFalse.value).thenReturn(false)

  val insuranceConverter =
    new InsuranceConverter(avatarsExternalUrlsBuilder, fileExternalUrlsBuilder)

  val converter =
    new InternalToPublicCardConverter(
      catalog,
      tree,
      urlsBuilder,
      dictionary,
      insuranceConverter,
      garageFeaturesRegistry,
      isStableEnvironment = false
    )

  private val ts = System.currentTimeMillis()

  test("convert") {

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueFalse)

    when(catalog.getMarkByCode(eq("BMW"))).thenReturn(
      Some(Mark("BMW", 0, "БМВ", "BMW", 1, "logo_url", Some("black_logo_url")))
    )
    when(catalog.getModelByCode(eq("BMW"), eq("5ER"))).thenReturn(
      Some(Model("5ER", 0, "пятерка", "5"))
    )

    when(catalog.getSuperGeneration(eq(20856169))).thenReturn(
      Some(SuperGeneration(20856169, None, "generation name", noComplect = false))
    )

    when(catalog.getCardByTechParamId(eq(20856383))).thenReturn(None)

    when(dictionary.textCarfaxProvenOwnerOk).thenReturn(Some("подтверждено"))

    val converted = converter.convert(123, DefaultGarageCard, GetCardOptions.Default)

    assert(converted == DefaultApiCard)
  }

  test("convert with new shared link") {

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueTrue)

    when(catalog.getMarkByCode(eq("BMW"))).thenReturn(
      Some(Mark("BMW", 0, "БМВ", "BMW", 1, "logo_url", Some("black_logo_url")))
    )
    when(catalog.getModelByCode(eq("BMW"), eq("5ER"))).thenReturn(
      Some(Model("5ER", 0, "пятерка", "5"))
    )

    when(catalog.getSuperGeneration(eq(20856169))).thenReturn(
      Some(SuperGeneration(20856169, None, "generation name", noComplect = false))
    )

    when(catalog.getCardByTechParamId(eq(20856383))).thenReturn(None)

    when(dictionary.textCarfaxProvenOwnerOk).thenReturn(Some("подтверждено"))

    val converted = converter.convert(123, DefaultGarageCard, GetCardOptions.Default)

    assert(converted == ApiCardWithNewSharedLink)
  }

  test("convert car info for empty vehicle info") {

    val vehicle = VehicleInfo.newBuilder().build()
    val converted = converter.convertCarInfo(vehicle)

    assert(converted == CarInfo.newBuilder().build())
  }

  test("convert state if mileages history non empty") {
    val history = List(
      Mileage.newBuilder().setValue(2000).setDate(Timestamps.fromMillis(System.currentTimeMillis())).build(),
      Mileage
        .newBuilder()
        .setValue(1000)
        .setDate(Timestamps.fromMillis(System.currentTimeMillis() - 1.day.toMillis))
        .build()
    )

    val converted = converter.convertState(history)

    assert(converted.get.getMileage.getValue == 2000)
    assert(converted.get.getMileage.getDate == history.head.getDate)
  }

  test("convert state if mileages history is empty") {
    val converted = converter.convertState(List.empty)

    assert(converted.isEmpty)
  }

  test("convert images") {
    val photo1 = buildPhoto(PhotoInfoId(100, "name-1", "autoru-carfax"))
    val photo2 = buildPhoto(PhotoInfoId(200, "name-2", "autoru-carfax"))
    val deletedPhoto = buildPhoto(PhotoInfoId(300, "name-3", "autoru-carfax")).toBuilder.setIsDeleted(true).build()

    val converted = converter.convertImages(List(photo1, photo2, deletedPhoto), useBlurredImages = false)

    assert(converted.size == 2)

    assert(converted(0).getMdsPhotoInfo.getName == "name-1")
    assert(converted(0).getMdsPhotoInfo.getGroupId == 100)
    assert(converted(0).getMdsPhotoInfo.getNamespace == "autoru-carfax")
    assert(converted(0).getSizesCount == CarfaxNamespace.Aliases.size)
    converted(0).getSizesMap.asScala.foreach { case (key, value) =>
      assert(CarfaxNamespace.Aliases.contains(key))
      assert(
        value == s"//${urlsBuilder.externalHost}/get-autoru-carfax/${photo1.getMdsPhotoInfo.getGroupId}/${photo1.getMdsPhotoInfo.getName}/$key"
      )
    }

    assert(converted(1).getMdsPhotoInfo.getName == "name-2")
    assert(converted(1).getMdsPhotoInfo.getGroupId == 200)
    assert(converted(1).getMdsPhotoInfo.getNamespace == "autoru-carfax")
    assert(converted(1).getSizesCount == CarfaxNamespace.Aliases.size)
    converted(1).getSizesMap.asScala.foreach { case (key, value) =>
      assert(CarfaxNamespace.Aliases.contains(key))
      assert(
        value == s"//${urlsBuilder.externalHost}/get-autoru-carfax/${photo2.getMdsPhotoInfo.getGroupId}/${photo2.getMdsPhotoInfo.getName}/$key"
      )
    }
  }

  test("convert images with unknown namespace") {
    val photo = buildPhoto(PhotoInfoId(100, "name-1", "autoru-new"))

    intercept[RuntimeException] {
      converter.convertImages(List(photo), useBlurredImages = false)
    }
  }

  test("convert blurred images") {
    val blurredPhotoInfo = PhotoInfoId(300, "blurred-name-1", "autoru-carfax")
    val photo1 =
      buildPhoto(PhotoInfoId(100, "name-1", "autoru-carfax"), Some(blurredPhotoInfo))
    val photo2 = buildPhoto(PhotoInfoId(200, "name-2", "autoru-carfax"))

    val converted = converter.convertImages(List(photo1, photo2), useBlurredImages = true)

    assert(converted.size == 1)
    assert(converted(0).getMdsPhotoInfo == PhotoInfoId.toMdsInfo(blurredPhotoInfo))
    assert(converted(0).getSizesCount == CarfaxNamespace.Aliases.size)
    converted(0).getSizesMap.asScala.foreach { case (key, value) =>
      assert(CarfaxNamespace.Aliases.contains(key))
      assert(
        value == s"//${urlsBuilder.externalHost}/get-autoru-carfax/${blurredPhotoInfo.groupId}/${blurredPhotoInfo.name}/$key"
      )
    }
  }

  test("convert insurance, must be without insurance when all internal insurance is delete") {

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueFalse)

    when(catalog.getMarkByCode(eq("BMW"))).thenReturn(
      Some(Mark("BMW", 0, "БМВ", "BMW", 1, "logo_url", Some("black_logo_url")))
    )
    when(catalog.getModelByCode(eq("BMW"), eq("5ER"))).thenReturn(
      Some(Model("5ER", 0, "пятерка", "5"))
    )

    when(catalog.getSuperGeneration(eq(20856169))).thenReturn(
      Some(SuperGeneration(20856169, None, "generation name", noComplect = false))
    )

    when(catalog.getCardByTechParamId(eq(20856383))).thenReturn(None)

    when(dictionary.textCarfaxProvenOwnerOk).thenReturn(Some("подтверждено"))

    val builder = DefaultGarageCard.toBuilder
    val internalInsurances = Seq(
      GarageSchema.Insurance
        .newBuilder()
        .setFrom(insuranceStartDate)
        .setTo(insuranceExpirationDate)
        .setInsuranceType(InsuranceType.OSAGO)
        .setStatus(VinReportModel.InsuranceStatus.ACTIVE)
        .setNumber("12345679")
        .setSerial("97 65")
        .setIsDeleted(true)
        .setUpdateTimestamp(insuranceUpdateTimestamp)
        .setCompany(
          GarageSchema.Company
            .newBuilder()
            .setName("Рога и Копыта")
            .setPhoneNumber("+ 1 234 567 89 00")
            .build()
        )
        .build(),
      GarageSchema.Insurance
        .newBuilder()
        .setFrom(Timestamps.fromMillis(System.currentTimeMillis() - 215.day.toMillis))
        .setTo(Timestamps.fromMillis(System.currentTimeMillis() + 150.day.toMillis))
        .setInsuranceType(InsuranceType.KASKO)
        .setStatus(VinReportModel.InsuranceStatus.ACTIVE)
        .setNumber("4443334")
        .setSerial("112 324234")
        .setIsDeleted(true)
        .setUpdateTimestamp(insuranceUpdateTimestamp)
        .setCompany(
          GarageSchema.Company
            .newBuilder()
            .setName("Рога и Копыта")
            .setPhoneNumber("+ 1 234 567 89 00")
            .build()
        )
        .build()
    )

    val defaultGarageCardWithoutInsurance = {
      builder
        .clearInsuranceInfo()
        .setInsuranceInfo(GarageSchema.InsuranceInfo.newBuilder().addAllInsurances(internalInsurances.asJava).build())
        .build()
    }
    val converted = converter.convert(123, defaultGarageCardWithoutInsurance, GetCardOptions.Default)

    assert(converted == DefaultApiCard.toBuilder.clearInsuranceInfo().build())
    assert(converted.hasInsuranceInfo === false)
  }

  private def buildPhoto(id: PhotoInfoId, optBlurred: Option[PhotoInfoId] = None): PhotoInfo = {
    val builder = PhotoInfo.newBuilder()

    builder.setMdsPhotoInfo(PhotoInfoId.toMdsInfo(id))

    optBlurred.map(blurred => {
      builder.addTransformations(
        PhotoTransformation
          .newBuilder()
          .setBlurredPlates(true)
          .setMdsPhotoInfo(PhotoInfoId.toMdsInfo(blurred))
      )
    })

    builder.build()
  }

  private val insuranceStartDate = Timestamps.fromMillis(System.currentTimeMillis() - 240.day.toMillis)
  private val insuranceExpirationDate = Timestamps.fromMillis(System.currentTimeMillis() + 125.day.toMillis)
  private val insuranceUpdateTimestamp = Timestamps.fromMillis(System.currentTimeMillis())

  private lazy val DefaultApiCard = {
    val builder = GarageApiModel.Card.newBuilder()
    builder
      .setShareUrl("https://test.avto.ru/garage/excar/share/123")
      .setShareCardId("123")
      .setUserId("user:123")
      .setId("123")
      .setStatus(GarageApiModel.Card.Status.ACTIVE)
      .setCardTypeInfo(CardTypeInfo.newBuilder().setCardType(CardType.EX_CAR))

    builder.getRecallsBuilder.getCardBuilder.setCardId(100500)

    builder.getOfferInfoBuilder.setOfferId("1049777022-811fc")

    builder.getInsuranceInfoBuilder
      .addInsurances {
        GarageApiModel.Insurance
          .newBuilder()
          .setFrom(insuranceStartDate)
          .setTo(insuranceExpirationDate)
          .setInsuranceType(InsuranceType.OSAGO)
          .setStatus(VinReportModel.InsuranceStatus.ACTIVE)
          .setNumber("12345679")
          .setSerial("97 65")
          .setIsActual(true)
          .setUpdateTimestamp(insuranceUpdateTimestamp)
          .setCompany(
            GarageApiModel.Company
              .newBuilder()
              .setName("Рога и Копыта")
              .setPhoneNumber("+ 1 234 567 89 00")
              .build()
          )
          .build()
      }

    builder.setSourceInfo(
      GarageApiModel.SourceInfo
        .newBuilder()
        .setAddedByIdentifier(IdentifierType.VIN)
        .setManuallyAdded(false)
    )

    val vehicleBuilder = builder.getVehicleInfoBuilder

    vehicleBuilder.getCarInfoBuilder
      .setMark("BMW")
      .setModel("5ER")
      .setBodyType("SEDAN")
      .setTransmission("AUTOMATIC")
      .setDrive("REAR_DRIVE")
      .setSuperGenId(20856169)
      .setConfigurationId(20856206)
      .setTechParamId(20856383)
      .setHorsePower(190)
      .setEngineType("DIESEL")
      .setSteeringWheel(CommonModel.SteeringWheel.LEFT)

    vehicleBuilder.getCarInfoBuilder.getMarkInfoBuilder
      .setCode("BMW")
      .setName("BMW")
      .setRuName("БМВ")
      .setLogo(
        Photo
          .newBuilder()
          .setName("catalog_mark_icon")
          .putSizes("orig", "logo_url")
          .putSizes("black-logo", "black_logo_url")
      )

    vehicleBuilder.getCarInfoBuilder.getModelInfoBuilder
      .setCode("5ER")
      .setName("5")
      .setRuName("пятерка")

    vehicleBuilder.getDocumentsBuilder
      .setVin("WBAFG81000LJ37566")
      .setLicensePlate("B188CT716")
      .setOwnersNumber(3)
      .setPtsOriginal(true)
      .setPts(PtsStatus.ORIGINAL)
      .setYear(2017)
      .setPurchaseDate(Date.newBuilder().setYear(2015).setMonth(6).setDay(12))

    vehicleBuilder.setSaleDate(Date.newBuilder().setYear(2016).setMonth(6).setDay(12))

    vehicleBuilder.getCarInfoBuilder.getSuperGenBuilder
      .setId(20856169)
      .setName("generation name")
      .setRuName("generation name")

    vehicleBuilder.getCarInfoBuilder.getConfigurationBuilder
      .setDoorsCount(4)
      .setId(20856206)

    vehicleBuilder.getCarInfoBuilder.getTechParamBuilder.setDisplacement(1995)

    vehicleBuilder.getStateBuilder.setMileage(
      GarageApiModel.Vehicle.Mileage.newBuilder().setValue(78000).setDate(Timestamps.fromMillis(ts))
    )

    vehicleBuilder.addVehicleImages {
      val builder = Image.newBuilder()
      val mdsInfo = MdsPhotoInfo.newBuilder().setNamespace("autoru-carfax").setName("abc").setGroupId(1).build()
      builder
        .setMdsPhotoInfo(mdsInfo)
        .putAllSizes(
          Map(
            "thumb_m" -> "//avatars.ru/get-autoru-carfax/1/abc/thumb_m",
            "832x624" -> "//avatars.ru/get-autoru-carfax/1/abc/832x624",
            "full" -> "//avatars.ru/get-autoru-carfax/1/abc/full",
            "320x240" -> "//avatars.ru/get-autoru-carfax/1/abc/320x240",
            "1200x900" -> "//avatars.ru/get-autoru-carfax/1/abc/1200x900",
            "small" -> "//avatars.ru/get-autoru-carfax/1/abc/small",
            "92x69" -> "//avatars.ru/get-autoru-carfax/1/abc/92x69",
            "1200x900n" -> "//avatars.ru/get-autoru-carfax/1/abc/1200x900n",
            "orig" -> "//avatars.ru/get-autoru-carfax/1/abc/orig"
          ).asJava
        )
        .build()
    }

    vehicleBuilder.getExteriorPanoramaBuilder
      .setPanorama(Panorama.newBuilder.setId("123"))

    vehicleBuilder.getColorBuilder
      .setId("4A2197")
      .setName("Фиолетовый")

    builder.getProvenOwnerStateBuilder
      .setComment("подтверждено")
      .setStatus(GarageApiModel.ProvenOwnerState.ProvenOwnerStatus.OK)
      .setAssignmentDate(Timestamps.fromMillis(ts))
      .setDocumentsPhotos(
        GarageApiModel.ProvenOwnerState.DocumentsPhotos.newBuilder().setUploadedAt(Timestamps.fromMillis(ts))
      )
      .setTaskKey("123abc")

    vehicleBuilder.getCarInfoBuilder.putAllEquipment(
      Map("wheel-heat" -> false, "leather" -> false).view.mapValues(Boolean.box).toMap.asJava
    )

    builder.build()
  }

  private lazy val ApiCardWithNewSharedLink = DefaultApiCard.toBuilder
    .setShareUrl("https://test.avto.ru/garage/excar/share/123-af5570f5a1810b7a")
    .setShareCardId("123-af5570f5a1810b7a")
    .build()

  private lazy val DefaultGarageCard = {
    val builder = GarageCard.newBuilder()

    builder.getSourceBuilder
      .setUserId("user:123")
      .setSource(Source.AUTORU)
      .setManuallyAdded(false)
      .setAddedByIdentifier(IdentifierType.VIN)

    builder.getMetaBuilder
      .setCreated(Timestamps.fromMillis(0L))
      .setStatus(GarageCard.Status.ACTIVE)
      .setCardTypeInfo(
        Meta.CardTypeInfo
          .newBuilder()
          .setCurrentState(Meta.CardTypeInfo.State.newBuilder().setCardType(CardType.EX_CAR))
      )

    builder.getRecallInfoBuilder
      .setRecallCardId(100500)

    builder.getOfferInfoBuilder.setOfferId("1049777022-811fc")

    builder.getInsuranceInfoBuilder
      .addInsurances {
        GarageSchema.Insurance
          .newBuilder()
          .setFrom(insuranceStartDate)
          .setTo(insuranceExpirationDate)
          .setInsuranceType(InsuranceType.OSAGO)
          .setStatus(VinReportModel.InsuranceStatus.ACTIVE)
          .setNumber("12345679")
          .setSerial("97 65")
          .setIsDeleted(false)
          .setUpdateTimestamp(insuranceUpdateTimestamp)
          .setCompany(
            GarageSchema.Company
              .newBuilder()
              .setName("Рога и Копыта")
              .setPhoneNumber("+ 1 234 567 89 00")
              .build()
          )
          .build()
      }

    val vehicleBuilder = builder.getVehicleInfoBuilder

    vehicleBuilder.getDocumentsBuilder
      .setVin("WBAFG81000LJ37566")
      .setLicensePlate("B188CT716")
      .setOwnersNumber(3)
      .setPtsOriginal(BoolValue.newBuilder().setValue(true).build())
      .setPurchaseDate(Timestamps.fromMillis(1434056400000L))

    vehicleBuilder.getDocumentsBuilder.setSaleDate(Timestamps.fromMillis(1465678800000L))

    vehicleBuilder.getCatalogBuilder
      .setSuperGenId(20856169)
      .setConfigurationId(20856206)
      .setTechParamId(20856383)

    vehicleBuilder.getTtxBuilder
      .setMark("BMW")
      .setModel("5ER")
      .setYear(2017)
      .setPowerHp(190)
      .setDisplacement(1995)
      .setSteeringWheel(SteeringWheel.LEFT)
      .setGearType(GearType.REAR_DRIVE)
      .setBodyType(BodyType.SEDAN)
      .setEngineType(EngineType.DIESEL)
      .setTransmission(Transmission.AUTOMATIC)
      .setDoorsCount(4)
      .setColor(ColorInfo.newBuilder().setAutoruColorId("4A2197").setName("Фиолетовый"))

    vehicleBuilder.getExteriorPanoramaBuilder
      .setPanorama(Panorama.newBuilder.setId("123"))

    vehicleBuilder.addAllEquipments(
      List(
        Equipment.newBuilder().setVerbaCode("wheel-heat").build(),
        Equipment.newBuilder().setVerbaCode("leather").build()
      ).asJava
    )

    vehicleBuilder.addImages {
      val builder = PhotoInfo.newBuilder()
      builder.getMdsPhotoInfoBuilder
        .setGroupId(1)
        .setName("abc")
        .setNamespace("autoru-carfax")
      builder.build()
    }

    vehicleBuilder.addMileageHistory(
      Mileage.newBuilder().setValue(78000).setDate(Timestamps.fromMillis(ts))
    )

    builder.getProvenOwnerStateBuilder
      .setDocumentsPhotos(
        DocumentsPhotos
          .newBuilder()
          .setStsBack(
            PhotoInfo
              .newBuilder()
              .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name").setNamespace("namespace").setGroupId(1))
          )
          .setDrivingLicense(PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder()))
          .setStsFront(PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder()))
          .setUploadedAt(Timestamps.fromMillis(ts))
      )
      .setVerdict(Verdict.PROVEN_OWNER_OK)
      .setAssignmentDate(Timestamps.fromMillis(ts))
      .setTaskKey("123abc")

    builder.build()
  }
}
