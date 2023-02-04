package ru.yandex.vertis.feedprocessor.autoru.scheduler.converter

import akka.actor.{ActorSystem, Scheduler}
import cats.syntax.either._
import cats.syntax.option._
import com.google.protobuf.BoolValue
import com.typesafe.config.ConfigFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.BeforeAndAfterAll
import ru.auto.api.ApiOfferModel.Multiposting.Classified
import ru.auto.api.ApiOfferModel.Multiposting.Classified.{ClassifiedName, Service}
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel.{DiscountOptions, GeoPoint, PaidService, Photo, SteeringWheel}
import ru.auto.api.{ApiOfferModel, CommonModel}
import ru.yandex.vertis.feedprocessor.AsyncWordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.converter.ApiOfferModelToAutoruExternalOfferConverterSpec._
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer.Unification
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.ModificationString
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClientImpl
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.ExternalRedirectsService.PhoneRedirection
import ru.yandex.vertis.feedprocessor.http.{ApacheHttpClient, DisableSSL, HostPort, HttpClient, HttpClientConfig}
import ru.yandex.vertis.feedprocessor.services.geocoder.{GeocodeResult, GeocoderClient}
import ru.yandex.vertis.feedprocessor.services.vos.HttpVosClient
import ru.yandex.vertis.feedprocessor.util.JwtUtils
import ru.yandex.vertis.mockito.MockitoSupport

import java.io.Closeable
import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.annotation.nowarn

@nowarn("msg=deprecated")
class ApiOfferModelToAutoruExternalOfferConverterSpec
  extends AsyncWordSpecBase
  with BeforeAndAfterAll
  with ScalaFutures
  with IntegrationPatience
  with MockitoSupport {

  implicit val scheduler: Scheduler = ActorSystem("test").scheduler

  def createHttpClient(config: HttpClientConfig): HttpClient with Closeable = {
    val httpClient =
      HttpAsyncClients
        .custom()
        .setSSLContext(DisableSSL.context)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build()
    httpClient.start()

    new ApacheHttpClient(httpClient, config)
  }

  val vosHttp = createHttpClient(HttpClientConfig("vos2-autoru-api-server.vrts-slb.test.vertis.yandex.net", 80))
  val unificatorHttp = createHttpClient(HttpClientConfig("auto2-searcher-api.vrts-slb.test.vertis.yandex.net", 80))

  val apiAddress = HostPort
    .fromConfig(ConfigFactory.parseString("""
      | host = "autoru-api-server.vrts-slb.test.vertis.yandex.net"
      | port = 80
      | scheme = http
      """.stripMargin))
    .toString

  val vosClient = new HttpVosClient(vosHttp)
  val unificatorClient = new UnificatorClientImpl(unificatorHttp)
  val geocoderClient: GeocoderClient = mock[GeocoderClient]

  val converter =
    new ApiOfferModelToAutoruExternalOfferConverter(vosClient, unificatorClient, apiAddress, geocoderClient)

  "ApiOfferModelToAutoruExternalOfferConverter" should {
    "resolve car model, mark and modification" in {
      val techParamIdMercedes = 4986817
      val techParamIdNissan = 7309485

      val offerMercedes = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamIdMercedes))
        .build()
      val geocodeResult = GeocodeResult(id = 1L, lat = 0.0, lng = 0.0, address = "", city = None)
      val offerNissan = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamIdNissan, armored = false))
        .build()

      when(geocoderClient.reverseGeocode(?)).thenReturn(Future.successful(Some(geocodeResult)))

      converter.convertCars(Seq(offerMercedes, offerNissan), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true
        result.last.isRight shouldBe true

        val convertedOfferMercedes = result.head.right.get
        val convertedOfferNissan = result.last.right.get

        convertedOfferMercedes.mark shouldBe "Mercedes-Benz"
        convertedOfferMercedes.model shouldBe "GL-Класс, I (X164) Рестайлинг"
        convertedOfferMercedes.modification shouldBe ModificationString("450 4.7 AT (340 л.с.) 4WD")
        convertedOfferMercedes.armored shouldBe "Да".some
        convertedOfferMercedes.complectation shouldBe 1.asLeft[String].some

        convertedOfferNissan.mark shouldBe "Nissan"
        convertedOfferNissan.model shouldBe "Juke, I"
        convertedOfferNissan.modification shouldBe ModificationString("1.6 CVT (190 л.с.) 4WD")
        convertedOfferNissan.armored shouldBe "Нет".some
        convertedOfferNissan.complectation shouldBe 1.asLeft[String].some
      }
    }

    "resolve service info" in {
      val techParamId = 4986817

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .addAllServices(
          Seq(
            createPaidService("service1", isActive = true),
            createPaidService("service2", isActive = false)
          ).asJava
        )
        .setMultiposting(
          createMultiposting(
            Seq(
              createClassified(ClassifiedName.AUTORU, isEnabled = true, Seq.empty),
              createClassified(
                ClassifiedName.AVITO,
                isEnabled = true,
                Seq(
                  createService("avitoService1", isActive = true),
                  createService("avitoService2", isActive = false)
                )
              ),
              createClassified(
                ClassifiedName.DROM,
                isEnabled = false,
                Seq(
                  createService("dromService1", isActive = true),
                  createService("dromService2", isActive = false)
                )
              )
            )
          )
        )
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.saleServices shouldBe Seq("service1")
        convertedOffer.avitoSaleServices shouldBe Seq("avitoService1")
        convertedOffer.dromSaleServices.isEmpty shouldBe true
        convertedOffer.classifieds shouldBe Seq("autoru", "avito").some
      }
    }

    "resolve car model, mark and modification where super_gen_id is not defined" in {
      val techParamId = 20475152

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.mark shouldBe "Vector"
        convertedOffer.model shouldBe "M12"
        convertedOffer.modification shouldBe ModificationString("5.7 MT (499 л.с.)")
        convertedOffer.armored shouldBe "Да".some
      }
    }

    "resolve additional info" in {
      val techParamId = 20475152

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setAdditionalInfo(createAdditionalInfo())
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.acceptedAutoruExclusive shouldBe true.some
        convertedOffer.onlineViewAvailable shouldBe true.some
        convertedOffer.bookingAllowed shouldBe true.some
        convertedOffer.exchange shouldBe "Рассмотрю варианты".some
        convertedOffer.action shouldBe "hide".some
      }
    }

    "resolve documents info" in {
      val techParamId = 20475152

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setDocuments(createDocuments())
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.sts shouldBe "STS".some
        convertedOffer.pts shouldBe "Оригинал".some
        convertedOffer.vin shouldBe "VIN".some
        convertedOffer.ownersNumber shouldBe "Один владелец".some
        convertedOffer.year shouldBe 2010
        convertedOffer.custom shouldBe "Растаможен"
        convertedOffer.registryYear shouldBe 2015.some
      }
    }

    "resolve other info" in {
      val techParamId = 20475152

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setAvailability(Availability.IN_STOCK)
        .addAllBadges(Seq("badge1", "badge2").asJava)
        .setColorHex("EE1D19")
        .setDiscountOptions(createDiscountOptions())
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.color shouldBe "Красный"
        convertedOffer.badges shouldBe Seq("badge1", "badge2")
        convertedOffer.availability shouldBe "В наличии"
        convertedOffer.creditDiscount shouldBe 1.some
        convertedOffer.insuranceDiscount shouldBe 2.some
        convertedOffer.tradeinDiscount shouldBe 3.some
        convertedOffer.maxDiscount shouldBe 4.some
      }
    }

    "resolve state" ignore { //fixme restore after format verification
      val techParamId = 20475152

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setState(
          createState(photos =
            Seq(
              createPhoto(Map("1200x900" -> "link1", "full" -> "notUsed")),
              createPhoto(Map("full" -> "link2")),
              createPhoto(Map("other" -> "link3"))
            )
          )
        )
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.state shouldBe "Отличное".some
        convertedOffer.video shouldBe "url".some
        convertedOffer.images shouldBe Seq("http:link1?webp=false", "http:link2?webp=false", "http:link3?webp=false")

      }
    }

    "resolve poiId" in {
      val techParamId = 20475152

      val offer1 = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setSellerType(SellerType.COMMERCIAL)
        .setSalon(createSalon("salonAddress"))
        .build()
      val offer2 = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setSellerType(SellerType.PRIVATE)
        .setSeller(createSeller("sellerAddress"))
        .build()

      converter.convertCars(Seq(offer1, offer2), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true
        result.last.isRight shouldBe true

        val convertedOffer1 = result.head.right.get
        val convertedOffer2 = result.last.right.get

        convertedOffer1.poiId shouldBe "salonAddress".some
        convertedOffer2.poiId shouldBe "sellerAddress".some

      }
    }

    "resolve poiId with custom address" in {
      val techParamId = 20475152

      val offer1 = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setSellerType(SellerType.COMMERCIAL)
        .setSalon(createSalon("salon_address"))
        .build()
      val offer2 = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setSellerType(SellerType.COMMERCIAL)
        .setSalon(createSalon("hidden_salon_address", editAddress = true))
        .setSeller(createSeller("shown_seller_address"))
        .build()

      converter.convertCars(Seq(offer1, offer2), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true
        result.last.isRight shouldBe true

        val convertedOffer1 = result.head.right.get
        val convertedOffer2 = result.last.right.get

        convertedOffer1.poiId shouldBe "salon_address".some
        convertedOffer2.poiId shouldBe "shown_seller_address".some
      }
    }

    "resolve contactInfo" in {
      val techParamId = 20475152

      val phoneToReplace = "+79991112233"
      val phoneWithoutReplace = "+79991111111"
      val redirectPhone = "+71111111111"
      val offer1 = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setSellerType(SellerType.COMMERCIAL)
        .setSalon(createSalon("salonAddress"))
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phoneToReplace)))
        .build()
      val offer2 = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .setSellerType(SellerType.COMMERCIAL)
        .setSalon(createSalon("salonAddress"))
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phoneWithoutReplace)))
        .build()

      val redirects = Map(
        offer1 -> List(PhoneRedirection(phone = redirectPhone, originalPhone = phoneToReplace))
      )
      converter.convertCars(Seq(offer1, offer2), redirects).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true
        result.last.isRight shouldBe true

        val convertedOffer1 = result.head.right.get
        val convertedOffer2 = result.last.right.get

        convertedOffer1.contactInfo.map(_.phone) shouldBe Seq("71111111111")
        convertedOffer2.contactInfo.map(_.phone) shouldBe Seq("79991111111")

      }
    }

    "resolve unification info" in {
      val techParamId = 4986817

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .build()
      val expectedUnification = Unification(
        mark = "Mercedes-Benz".some,
        model = "GL-Класс, I (X164) Рестайлинг".some,
        bodyType = "Внедорожник 5 дв.".some,
        transmission = "Автоматическая".some,
        superGenId = 4986814.toLong.some,
        configurationId = 4986815.toLong.some,
        techParamId = 4986817.toLong.some,
        engineType = "Бензин".some,
        gearType = "Полный".some,
        horsePower = 340.some,
        displacement = 4663.some,
        complectationId = None
      )

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isRight shouldBe true

        val convertedOffer = result.head.right.get

        convertedOffer.unification shouldBe expectedUnification.some
      }
    }

    "return Left(error) for not existing tech param id" in {
      val techParamId = 0

      val offer = baseOfferBuilder()
        .setCarInfo(createCarInfo(techParamId))
        .build()

      converter.convertCars(Seq(offer), Map.empty).map { result =>
        result.nonEmpty shouldBe true
        result.head.isLeft shouldBe true
      }
    }

    "return classified original images" in {
      val offer = baseOfferBuilder()
        .setMultiposting {
          createMultiposting {
            Seq(
              createClassifiedWithPhotos(
                ClassifiedName.AVITO,
                isEnabled = true,
                photos = Classified.Photos
                  .newBuilder()
                  .addOriginalImage(createPhoto("autoru-test", "autoru-test:111-photoHash1", isDeleted = true))
                  .addOriginalImage(createPhoto("autoru-test", "autoru-test:222-photoHash2", isDeleted = false))
                  .addOriginalImage(createPhoto("autoru-test", "autoru-test:333-photoHash3", isDeleted = false))
                  .build()
              ),
              createClassifiedWithPhotos(
                ClassifiedName.DROM,
                isEnabled = true,
                photos = Classified.Photos
                  .newBuilder()
                  .addOriginalImage(createPhoto("autoru-test", "autoru-test:444-photoHash3", isDeleted = false))
                  .build()
              )
            )
          }
        }

      def signedRawPhotoUrl(signedData: String): String = {
        s"$apiAddress/1.0/photos/raw-photo?sign=$signedData&token=raw-photos"
      }

      val expected = Seq(
        signedRawPhotoUrl(JwtUtils.sign(namespace = "autoru-test", groupId = "222", hash = "photoHash2")),
        signedRawPhotoUrl(JwtUtils.sign(namespace = "autoru-test", groupId = "333", hash = "photoHash3"))
      )

      converter.getClassifiedOrigImages(offer.build(), ClassifiedName.AVITO) should contain theSameElementsAs expected
    }
  }

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true

  override protected def afterAll(): Unit = {
    super.afterAll()
    vosHttp.close()
    unificatorHttp.close()
  }
}

object ApiOfferModelToAutoruExternalOfferConverterSpec {

  def baseOfferBuilder(
      id: String = "1043045004-977b3",
      sellerAddress: String = "address 1",
      category: Category = Category.CARS,
      section: Section = Section.NEW,
      availability: Availability = Availability.IN_STOCK): Offer.Builder =
    Offer
      .newBuilder()
      .setId(id)
      .setCategory(category)
      .setSection(section)
      .setAvailability(availability)
      .setSeller(createSeller(sellerAddress))

  def createCarInfo(
      techParamId: Long,
      complectation: Long = 1,
      wheel: SteeringWheel = SteeringWheel.LEFT,
      armored: Boolean = true): CarInfo =
    CarInfo
      .newBuilder()
      .setTechParamId(techParamId)
      .setSteeringWheel(wheel)
      .setArmored(armored)
      .setComplectationId(complectation)
      .build()

  def createService(name: String, isActive: Boolean): Service =
    Service.newBuilder().setService(name).setIsActive(isActive).build()

  def createClassified(classifiedName: ClassifiedName, isEnabled: Boolean, services: Seq[Service]): Classified =
    Classified
      .newBuilder()
      .setName(classifiedName)
      .setEnabled(isEnabled)
      .addAllServices(services.asJava)
      .build()

  def createClassifiedWithPhotos(
      classifiedName: ClassifiedName,
      isEnabled: Boolean,
      photos: Classified.Photos): Classified =
    Classified
      .newBuilder()
      .setName(classifiedName)
      .setEnabled(isEnabled)
      .setPhotos(photos)
      .build()

  def createPhoto(namespace: String, name: String, isDeleted: Boolean): Photo =
    Photo
      .newBuilder()
      .setNamespace(namespace)
      .setName(name)
      .setIsDeleted(isDeleted)
      .build()

  def createMultiposting(classifieds: Seq[Classified]): Multiposting = {
    ApiOfferModel.Multiposting
      .newBuilder()
      .addAllClassifieds(classifieds.asJava)
      .build()
  }

  private def createPaidService(name: String, isActive: Boolean): PaidService =
    PaidService.newBuilder().setService(name).setIsActive(isActive).build()

  private def createAdditionalInfo(
      exchange: Boolean = true,
      hidden: Boolean = true,
      onlineView: Boolean = true,
      autoruExclusive: Boolean = true,
      booking: Boolean = true): AdditionalInfo =
    AdditionalInfo
      .newBuilder()
      .setExchange(exchange)
      .setHidden(hidden)
      .setOnlineViewAvailable(onlineView)
      .setAcceptedAutoruExclusive(BoolValue.of(autoruExclusive))
      .setBooking(AdditionalInfo.Booking.newBuilder().setAllowed(booking).build())
      .build()

  private def createDocuments(
      customCleared: Boolean = true,
      year: Int = 2010,
      purchaseDateYear: Int = 2015,
      purchaseDateMonth: Int = 1,
      ptsStatus: PtsStatus = PtsStatus.ORIGINAL,
      sts: String = "STS",
      ownersNumber: Int = 1,
      vin: String = "VIN"): Documents =
    Documents
      .newBuilder()
      .setCustomCleared(customCleared)
      .setYear(year)
      .setPurchaseDate(CommonModel.Date.newBuilder().setYear(purchaseDateYear).setMonth(purchaseDateMonth).build())
      .setPts(ptsStatus)
      .setSts(sts)
      .setOwnersNumber(ownersNumber)
      .setVin(vin)
      .build()

  private def createDiscountOptions(
      credit: Int = 1,
      insurance: Int = 2,
      tradeIn: Int = 3,
      maxDiscount: Int = 4): DiscountOptions =
    DiscountOptions
      .newBuilder()
      .setCredit(credit)
      .setInsurance(insurance)
      .setMaxDiscount(maxDiscount)
      .setTradein(tradeIn)
      .build()

  private def createState(
      condition: Condition = Condition.CONDITION_OK,
      videoUrl: String = "url",
      photos: Seq[Photo]): State =
    State
      .newBuilder()
      .setCondition(condition)
      .setVideo(CommonModel.Video.newBuilder().setYoutubeId(videoUrl).build())
      .addAllImageUrls(photos.asJava)
      .build()

  private def createPhoto(photoSizes: Map[String, String]): Photo =
    Photo
      .newBuilder()
      .putAllSizes(photoSizes.asJava)
      .build()

  private def createSalon(
      address: String,
      lat: Double = 0.0,
      lng: Double = 0.0,
      editAddress: Boolean = false): Salon = {
    val point = GeoPoint
      .newBuilder()
      .setLatitude(lat)
      .setLongitude(lng)
      .build()
    val location = Location
      .newBuilder()
      .setAddress(address)
      .setCoord(point)
      .build()

    Salon
      .newBuilder()
      .setPlace(location)
      .setEditAddress(editAddress)
      .build()
  }

  private def createSeller(address: String, lat: Double = 0.0, lng: Double = 0.0): Seller = {
    val point = GeoPoint
      .newBuilder()
      .setLatitude(lat)
      .setLongitude(lng)
      .build()
    val location = Location
      .newBuilder()
      .setAddress(address)
      .setCoord(point)
      .build()

    Seller
      .newBuilder()
      .setLocation(location)
      .build()
  }
}
