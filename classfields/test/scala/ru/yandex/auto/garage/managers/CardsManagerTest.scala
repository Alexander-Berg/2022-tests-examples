package ru.yandex.auto.garage.managers

import auto.carfax.common.clients.carfax.CarfaxClient
import com.google.protobuf.BoolValue
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.PtsStatus
import ru.auto.api.vin.ResponseModel.VinDecoderError.Code
import ru.auto.api.vin.ResponseModel.{RawEssentialsReportResponse, VinDecoderError}
import ru.auto.api.vin.VinReportModel
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.auto.api.vin.garage.GarageApiModel.InsuranceSource
import ru.auto.api.vin.garage.RequestModel.ChangeCardTypeRequest.ExCarAdditionalInfo.ExCarReason
import ru.auto.api.vin.garage.RequestModel.GetListingRequest.{Filters, Sorting}
import ru.auto.api.{ApiOfferModel, CarsModel}
import auto.carfax.common.clients.journal.JournalClient
import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.avatars.AvatarsExternalUrlsBuilder
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.garage.converters.cards.{InternalToPublicCardConverter, PublicToInternalCardConverter}
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.garage.dao.cards.{CardsDao, CardsTableRow}
import ru.yandex.auto.garage.exceptions.CardNotFound
import ru.yandex.auto.garage.managers.GetCardOptions.Default
import ru.yandex.auto.garage.managers.validation.{CardValidationManager, FailedValidation, SuccessValidation}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo.State.AdditionalInfo
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo.{ChangeEvent, ChangeTypeSource}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, Meta, ProvenOwnerState}
import ru.yandex.auto.vin.decoder.model.exception.CarfaxExceptions._
import ru.yandex.auto.vin.decoder.model.{AutoruUser, UserRef}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.auto.garage.utils.features.GarageFeaturesRegistry
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class CardsManagerTest extends AnyFunSuite with MockitoSupport with BeforeAndAfter {

  implicit val t = Traced.empty

  private val dao = mock[CardsDao]
  private val cardsService = mock[CardsService]
  private val converter = mock[InternalToPublicCardConverter]
  private val publicConverter = mock[PublicToInternalCardConverter]
  private val cardsValidator = mock[CardValidationManager]
  private val carfaxClient = mock[CarfaxClient]
  private val vosClient = mock[VosClient]
  private val registrationRegionManager = mock[RegistrationRegionManager]
  private val externalUrlsBuilder = mock[AvatarsExternalUrlsBuilder]
  private val cardBuilder = new CardBuilder(registrationRegionManager, externalUrlsBuilder, converter)
  private val journalClient = mock[JournalClient]
  private val garageFeaturesRegistry = mock[GarageFeaturesRegistry]

  private val featureValueTrue = mock[Feature[Boolean]]
  when(featureValueTrue.value).thenReturn(true)

  private val featureValueFalse = mock[Feature[Boolean]]
  when(featureValueFalse.value).thenReturn(false)

  val manager = new CardsManager(
    cardsService,
    cardsValidator,
    registrationRegionManager,
    converter,
    publicConverter,
    carfaxClient,
    vosClient,
    cardBuilder,
    journalClient,
    garageFeaturesRegistry
  )

  private val DefaultUserRef = UserRef.parseOrThrow("user:123")
  private val now = System.currentTimeMillis()

  before {
    reset(dao)
    reset(converter)
    reset(publicConverter)
  }

  test("update card func: success validation") {
    val current = GarageCard.newBuilder().build()
    val updated = {
      val builder = GarageCard.newBuilder()
      builder.getVehicleInfoBuilder.getDocumentsBuilder.setVin("WAUZZZ8KXFA015693")
      builder.build()
    }
    val apiCard = GarageApiModel.Card.newBuilder().build()

    val successResult = SuccessValidation(apiCard)
    when(cardsValidator.validateExisting(?, ?, ?, ?)).thenReturn(successResult)
    when(publicConverter.convertExistingCard(?, ?, ?)).thenReturn(updated)

    val res = manager.updateCardFunc(1L, apiCard, System.currentTimeMillis())(List.empty, current)

    assert(res._1.map(_._1).contains(updated))
    assert(res._2.isEmpty)
  }

  test("update card func: failed validation") {
    val current = GarageCard.newBuilder().build()
    val apiCard = GarageApiModel.Card.newBuilder().build()

    val failResult = FailedValidation(List.empty)
    when(cardsValidator.validateExisting(?, ?, ?, ?)).thenReturn(failResult)

    val res = manager.updateCardFunc(1L, apiCard, System.currentTimeMillis())(List.empty, current)

    assert(res._1.isEmpty)
    assert(res._2.contains(failResult))
  }

  test("update status func") {
    val current = {
      val builder = GarageCard.newBuilder()
      builder.getMetaBuilder.setStatus(GarageCard.Status.ACTIVE)
      builder.build()
    }

    val updated = manager.updateStatusFunc(GarageCard.Status.DELETED)(List.empty, current)._1.get
    assert(updated._1.getMeta.getStatus == GarageCard.Status.DELETED)
  }

  test("update status: not existing card") {
    when(cardsService.updateCardT(?, ?)(?)(?)).thenReturn(Future.failed(CardNotFound("1")))

    intercept[CardNotFound] {
      manager.updateStatus(1, DefaultUserRef, GarageCard.Status.ACTIVE).await
    }
  }

  test("get card: not found in db") {
    when(cardsService.getCard(?, ?)(?)).thenReturn(Future.successful(None))

    val res = manager.getUserCard(123L, Default).await
    assert(res.isEmpty)
  }

  test("get card: found in db") {
    val apiCard = GarageApiModel.Card.newBuilder().setId("123").setUserId("user:123").build()
    val internalCard = GarageCard.newBuilder().build()

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueTrue)

    when(cardsService.getCard(?, ?)(?))
      .thenReturn(
        Future.successful(
          Some(
            CardsTableRow(
              123L,
              "user:123",
              None,
              None,
              Instant.now(),
              GarageCard.Status.ACTIVE,
              CardType.CURRENT_CAR,
              internalCard,
              None,
              None,
              None,
              Instant.ofEpochMilli(System.currentTimeMillis())
            )
          )
        )
      )
    when(converter.convert(?, ?, ?)(?)).thenReturn(apiCard)

    val res = manager.getUserCard(123L, Default).await
    assert(res.contains(apiCard))
  }

  test("get shared card: found in db") {
    val apiCard = GarageApiModel.Card.newBuilder().setId("123").setUserId("user:123").build()
    val internalCard =
      GarageCard.newBuilder().setMeta(GarageSchema.Meta.newBuilder().setCreated(Timestamps.fromMillis(0))).build()

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueTrue)

    when(cardsService.getCard(?, ?)(?))
      .thenReturn(
        Future.successful(
          Some(
            CardsTableRow(
              123L,
              "user:123",
              None,
              None,
              Instant.now(),
              GarageCard.Status.ACTIVE,
              CardType.CURRENT_CAR,
              internalCard,
              None,
              None,
              None,
              Instant.ofEpochMilli(System.currentTimeMillis())
            )
          )
        )
      )
    when(converter.convert(?, ?, ?)(?)).thenReturn(apiCard)

    val res = manager.getUserCard(123L, Default.copy(isShared = true), Some("af5570f5a1810b7a")).await
    assert(res.contains(apiCard))
  }

  test("get shared card: hash check failed") {
    val apiCard = GarageApiModel.Card.newBuilder().setId("123").setUserId("user:123").build()
    val internalCard =
      GarageCard.newBuilder().setMeta(GarageSchema.Meta.newBuilder().setCreated(Timestamps.fromMillis(0))).build()

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueTrue)

    when(cardsService.getCard(?, ?)(?))
      .thenReturn(
        Future.successful(
          Some(
            CardsTableRow(
              123L,
              "user:123",
              None,
              None,
              Instant.now(),
              GarageCard.Status.ACTIVE,
              CardType.CURRENT_CAR,
              internalCard,
              None,
              None,
              None,
              Instant.ofEpochMilli(System.currentTimeMillis())
            )
          )
        )
      )
    when(converter.convert(?, ?, ?)(?)).thenReturn(apiCard)

    intercept[CardNotFound] {
      manager.getUserCard(123L, Default.copy(isShared = true), None).await
    }
  }

  test("get shared card: found in db (feature off)") {
    val apiCard = GarageApiModel.Card.newBuilder().setId("123").setUserId("user:123").build()
    val internalCard =
      GarageCard.newBuilder().setMeta(GarageSchema.Meta.newBuilder().setCreated(Timestamps.fromMillis(0))).build()

    reset(garageFeaturesRegistry)
    when(garageFeaturesRegistry.EnableNewSharedCardLinks).thenReturn(featureValueFalse)

    when(cardsService.getCard(?, ?)(?))
      .thenReturn(
        Future.successful(
          Some(
            CardsTableRow(
              123L,
              "user:123",
              None,
              None,
              Instant.now(),
              GarageCard.Status.ACTIVE,
              CardType.CURRENT_CAR,
              internalCard,
              None,
              None,
              None,
              Instant.ofEpochMilli(System.currentTimeMillis())
            )
          )
        )
      )
    when(converter.convert(?, ?, ?)(?)).thenReturn(apiCard)

    val res = manager.getUserCard(123L, Default.copy(isShared = true), None).await
    assert(res.contains(apiCard))
  }

  private def createCard(creationDate: DateTime, cardType: CardType, saleDate: Option[DateTime] = None) =
    CardsTableRow(
      123L,
      "user:123",
      None,
      None,
      Instant.ofEpochMilli(creationDate.getMillis),
      GarageCard.Status.ACTIVE,
      cardType, {
        val builder = GarageCard.newBuilder()
        builder.setMeta(
          Meta
            .newBuilder()
            .setCardTypeInfo {
              val ctsB = CardTypeInfo.newBuilder()
              val stateB = CardTypeInfo.State.newBuilder().setCardType(cardType)
              ctsB.setCurrentState(stateB)
            }
            .setCreated(Timestamps.fromMillis(creationDate.getMillis))
        )
        saleDate.foreach(dt =>
          builder.getVehicleInfoBuilder.getDocumentsBuilder.setSaleDate(Timestamps.fromMillis(dt.getMillis))
        )
        builder.build()
      },
      None,
      None,
      None,
      Instant.ofEpochMilli(System.currentTimeMillis())
    )

  test("garage cards should be appropriately sorted") {
    val apiCard = GarageApiModel.Card.newBuilder().setId("123").setUserId("user:123").build()

    val cards = List(
      createCard(DateTime.now().minusDays(3), CardType.CURRENT_CAR),
      createCard(DateTime.now().minusDays(2), CardType.CURRENT_CAR),
      createCard(DateTime.now().withTimeAtStartOfDay(), CardType.EX_CAR, Some(DateTime.now().minusDays(10))),
      createCard(DateTime.now().withTimeAtStartOfDay(), CardType.DREAM_CAR),
      createCard(DateTime.now().minusDays(1), CardType.EX_CAR),
      createCard(DateTime.now().minusDays(3), CardType.EX_CAR),
      createCard(DateTime.now().minusDays(2), CardType.EX_CAR, Some(DateTime.now().minusDays(5)))
    )

    when(cardsService.getUserCards(?, ?, ?, ?, ?, ?)(?))
      .thenReturn(
        Future.successful(cards)
      )

    when(converter.convert(?, ?, ?)(?)).thenReturn(apiCard)
    val inOrder = Mockito.inOrder(converter)

    manager.getUserCards(AutoruUser(123L), 100, 1, Filters.newBuilder().build(), Sorting.CREATION_DATE).await

    inOrder.verify(converter).convert(?, eq(cards(1).card), ?)(?)
    inOrder.verify(converter).convert(?, eq(cards.head.card), ?)(?)
    inOrder.verify(converter).convert(?, eq(cards(3).card), ?)(?)
    inOrder.verify(converter).convert(?, eq(cards(6).card), ?)(?)
    inOrder.verify(converter).convert(?, eq(cards(2).card), ?)(?)
    inOrder.verify(converter).convert(?, eq(cards(4).card), ?)(?)
    inOrder.verify(converter).convert(?, eq(cards(5).card), ?)(?)
  }

  test("build card from offer: offer not found") {
    val offerBuilder = ApiOfferModel.Offer.newBuilder()
    offerBuilder.setCategory(ApiOfferModel.Category.MOTO)

    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))

    intercept[OfferNotFound] {
      manager.buildCardFromOffer("offer_id", AutoruUser(123), CardType.CURRENT_CAR)(Traced.empty).await
    }
  }

  test("build card from article") {
    val raw = ResourceUtils.getStringFromResources("/journal/successful_response.json")
    val article = JournalClient.apply(raw)
    val card = cardBuilder.buildInternalGarageCardFromArticle("user:1234", article)

    assert(card.getVehicleInfo.getTtx.getMark === "HAVAL")
    assert(card.getVehicleInfo.getTtx.getModel === "F7")
    assert(card.getVehicleInfo.getCatalog.getSuperGenId === 21569049)
  }

  test("build card from offer: wrong category") {
    val offerBuilder = ApiOfferModel.Offer.newBuilder()
    val user = AutoruUser(123)
    offerBuilder.setCategory(ApiOfferModel.Category.MOTO)
    offerBuilder.setUserRef(user.toPlain)

    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Option(offerBuilder.build())))

    intercept[WrongOffer] {
      manager.buildCardFromOffer("offer_id", user, CardType.CURRENT_CAR)(Traced.empty).await
    }
  }

  test("build card from offer: wrong section") {
    val offerBuilder = ApiOfferModel.Offer.newBuilder()
    val user = AutoruUser(123)
    offerBuilder
      .setCategory(ApiOfferModel.Category.CARS)
      .setSection(ApiOfferModel.Section.NEW)
      .setUserRef(user.toPlain)

    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Option(offerBuilder.build())))

    intercept[WrongOffer] {
      manager.buildCardFromOffer("offer_id", user, CardType.CURRENT_CAR)(Traced.empty).await
    }
  }

  test("build card from offer: different user in offer and request") {
    val offerBuilder = ApiOfferModel.Offer.newBuilder()
    val userFromRequest = AutoruUser(321)
    val user = AutoruUser(123)
    offerBuilder
      .setCategory(ApiOfferModel.Category.CARS)
      .setSection(ApiOfferModel.Section.NEW)
      .setUserRef(user.toPlain)

    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Option(offerBuilder.build())))

    intercept[OfferMustBelongToUser] {
      manager.buildCardFromOffer("offer_id", userFromRequest, CardType.CURRENT_CAR)(Traced.empty).await
    }
  }

  test("build card from offer without exception") {
    val offerBuilder = ApiOfferModel.Offer.newBuilder()
    val user = AutoruUser(123)
    offerBuilder.getDocumentsBuilder
      .setNotRegisteredInRussia(false)
      .setVin("X4X3D59430PS96744")
      .setLicensePlate("K718CE178")
      .setYear(2021)
      .setSts("9920088333")
      .setPtsValue(1234)
      .setPts(PtsStatus.ORIGINAL)
      .setPtsOriginal(true)

    offerBuilder.setUserRef(user.toPlain)
    offerBuilder.setColorHex("#919BA7")
    offerBuilder.setId("offer_id")
    offerBuilder.setStatus(ApiOfferModel.OfferStatus.ACTIVE)
    offerBuilder.setCategory(ApiOfferModel.Category.CARS)
    offerBuilder.setSellerType(ApiOfferModel.SellerType.PRIVATE)
    offerBuilder.setSection(ApiOfferModel.Section.USED)
    offerBuilder.setCarInfo(CarsModel.CarInfo.newBuilder().setModel("X5").setMark("BMW").setHorsePower(151))

    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Option(offerBuilder.build())))
    when(registrationRegionManager.findRegistrationInfo(?)).thenReturn(None)

    val res =
      manager.buildCardFromOffer("offer_id", user, CardType.CURRENT_CAR)(Traced.empty).await

    assert(res.getOfferInfo.getOfferId == "offer_id")
    assert(res.getVehicleInfo.getCarInfo.getMark == "BMW")
    assert(res.getVehicleInfo.getCarInfo.getModel == "X5")
    assert(res.getVehicleInfo.getColor.getId == "#919BA7")
    assert(res.getVehicleInfo.getDocuments.getVin == "X4X3D59430PS96744")
    assert(res.getVehicleInfo.getDocuments.getLicensePlate == "K718CE178")
    assert(res.getVehicleInfo.getDocuments.getYear == 2021)
  }

  test("build card: not found in gibdd") {
    val reportBuilder = RawEssentialsReportResponse.newBuilder()
    reportBuilder.getReportBuilder.getPtsInfoBuilder
      .setRegisteredInGibdd(BoolValue.of(false))

    when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.successful(reportBuilder.build()))

    intercept[VinNotFoundException] {
      manager.buildCardFromVin("Z0NZWE00054341234", AutoruUser(123), CardType.EX_CAR)(Traced.empty).await
    }
  }

  test("build card: vin report in progress") {
    val reportBuilder = RawEssentialsReportResponse.newBuilder()
    reportBuilder.setError(
      VinDecoderError
        .newBuilder()
        .setErrorCode(Code.IN_PROGRESS)
    )

    when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.successful(reportBuilder.build()))

    intercept[VinInProgressException] {
      manager.buildCardFromVin("Z0NZWE00054341234", AutoruUser(123), CardType.EX_CAR)(Traced.empty).await
    }
  }

  test("build card: vin not valid") {
    val reportBuilder = RawEssentialsReportResponse.newBuilder()
    reportBuilder.setError(
      VinDecoderError
        .newBuilder()
        .setErrorCode(Code.VIN_NOT_VALID)
    )

    when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.successful(reportBuilder.build()))

    intercept[VinNotValidException] {
      manager.buildCardFromVin("123123", AutoruUser(123), CardType.EX_CAR)(Traced.empty).await
    }
  }

  test("build card: vin not found") {
    val reportBuilder = RawEssentialsReportResponse.newBuilder()
    reportBuilder.setError(
      VinDecoderError
        .newBuilder()
        .setErrorCode(Code.VIN_NOT_FOUND)
    )

    when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.successful(reportBuilder.build()))

    intercept[VinNotFoundException] {
      manager.buildCardFromVin("Z0NZWE00054341234", AutoruUser(123), CardType.EX_CAR)(Traced.empty).await
    }
  }

  test("build card: user invalid") {
    val reportBuilder = RawEssentialsReportResponse.newBuilder()
    reportBuilder.setError(
      VinDecoderError
        .newBuilder()
        .setErrorCode(Code.VIN_NOT_FOUND)
    )

    when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.successful(reportBuilder.build()))

    intercept[VinNotFoundException] {
      manager.buildCardFromVin("Z0NZWE00054341234", AutoruUser(123), CardType.EX_CAR)(Traced.empty).await
    }
  }

  test("update verdict func") {
    val current = {
      val builder = GarageCard.newBuilder()
      builder.getProvenOwnerStateBuilder.setVerdict(ProvenOwnerState.Verdict.PENDING)
      builder.build()
    }

    val updated = manager.updateVerdictFunc(ProvenOwnerState.Verdict.PROVEN_OWNER_OK)(List.empty, current)._1.get
    assert(updated._1.getProvenOwnerState.getVerdict == ProvenOwnerState.Verdict.PROVEN_OWNER_OK)
  }

  test("update verdict: not existing card") {
    when(cardsService.updateCardT(?, ?)(?)(?)).thenReturn(Future.failed(CardNotFound("1")))

    intercept[CardNotFound] {
      manager.updateVerdict(1, DefaultUserRef, ProvenOwnerState.Verdict.PROVEN_OWNER_OK).await
    }
  }

  test("convertToInsurances: check right setting 'is_actual'") {
    val insuranceBlock = buildInsurancesBlock(
      List(
        buildInsuranceItem(
          serialOpt = Some("OSAGO-XXX"),
          numberOpt = Some("120 23 432"),
          from = now - 240.day.toMillis,
          toOpt = Some(now + 125.day.toMillis)
        ),
        buildInsuranceItem(
          serialOpt = Some("OSAGO-22"),
          numberOpt = Some("33 00 00"),
          from = now - 605.day.toMillis,
          toOpt = Some(now - 240.day.toMillis)
        ),
        buildInsuranceItem(
          serialOpt = Some("KASKO-XXX"),
          numberOpt = Some("120 23 432"),
          from = now - 245.day.toMillis,
          toOpt = Some(now + 130.day.toMillis),
          insuranceType = VinReportModel.InsuranceType.KASKO
        ),
        buildInsuranceItem(
          serialOpt = Some("KASKO-22"),
          numberOpt = Some("33 00 00"),
          from = now - 610.day.toMillis,
          toOpt = Some(now - 245.day.toMillis),
          insuranceType = VinReportModel.InsuranceType.KASKO
        )
      )
    )

    val res = manager.convertToInsuranceInfo(insuranceBlock).getInsurancesList.asScala
    assert(res.nonEmpty)
    assert(res.size === 4)
    // OSAGO
    assert(res.find(in => in.getSerial == "OSAGO-XXX" && in.getNumber == "120 23 432").get.getIsActual === true)
    assert(res.find(in => in.getSerial == "OSAGO-22" && in.getNumber == "33 00 00").get.getIsActual === false)
    // KASKO
    assert(res.find(in => in.getSerial == "KASKO-XXX" && in.getNumber == "120 23 432").get.getIsActual === true)
    assert(res.find(in => in.getSerial == "KASKO-22" && in.getNumber == "33 00 00").get.getIsActual === false)
  }

  test("convertToInsurances: when Insurances isEmpty - success") {
    val insuranceBlock = buildInsurancesBlock(List.empty)

    val res = manager.convertToInsuranceInfo(insuranceBlock).getInsurancesList.asScala
    assert(res.isEmpty)
  }

  test("convertToInsurances: without insurance expiration date. Must made - to = from + 1 year - 1 day") {
    val insuranceFrom = now - 240.day.toMillis
    val insuranceBlock = buildInsurancesBlock(
      List(
        buildInsuranceItem(
          serialOpt = Some("XXX"),
          numberOpt = Some("120 23 432"),
          from = insuranceFrom,
          toOpt = None
        )
      )
    )

    val res = manager.convertToInsuranceInfo(insuranceBlock).getInsurancesList.asScala
    assert(res.nonEmpty)
    assert(res.size === 1)
    assert(res.head.getIsActual === true)
    assert(res.head.getTo === Timestamps.fromMillis(insuranceFrom + 364.day.toMillis))
  }

  test("convertToInsurances: without number, serial or insurance start date. Must filtered such entities") {

    val insuranceFrom = now - 240.day.toMillis
    val insurancesBlock = buildInsurancesBlock(
      List(
        buildInsuranceItem(
          serialOpt = Some("XXX"),
          numberOpt = None,
          from = insuranceFrom
        ),
        buildInsuranceItem(
          serialOpt = None,
          numberOpt = None,
          from = insuranceFrom
        ),
        buildInsuranceItem(
          serialOpt = None,
          numberOpt = Some("XXX"),
          from = insuranceFrom
        )
      )
    )

    val res = manager.convertToInsuranceInfo(insurancesBlock).getInsurancesList.asScala
    assert(res.isEmpty)
  }

  test("convertToInsurances: when Insurances source is empty, must set RSA") {
    val insuranceBlock = buildInsurancesBlock(
      List(
        buildInsuranceItem(
          serialOpt = Some("OSAGO-XXX"),
          numberOpt = Some("120 23 432"),
          from = now - 240.day.toMillis,
          toOpt = Some(now + 125.day.toMillis)
        )
      )
    )

    val res = manager.convertToInsuranceInfo(insuranceBlock).getInsurancesList.asScala
    assert(res.size == 1)
    assert(res.head.getSource == InsuranceSource.RSA)
  }

  test("change card type func") {
    val current = {
      val builder = GarageCard.newBuilder()
      val ctInfoBuilder = builder.getMetaBuilder.getCardTypeInfoBuilder
      val state = CardTypeInfo.State.newBuilder().setCardType(CardType.CURRENT_CAR).build()
      ctInfoBuilder
        .setCurrentState(state)
        .addHistory(
          ChangeEvent.newBuilder().setDate(Timestamps.fromMillis(System.currentTimeMillis())).setState(state)
        )
      builder.build()
    }
    val additionalInfo = AdditionalInfo.newBuilder().setExCarReason(ExCarReason.SOLD).build()

    val targetState = CardTypeInfo.State
      .newBuilder()
      .setCardType(CardType.EX_CAR)
      .setSource(ChangeTypeSource.MANUAL)
      .setAdditionalInfo(additionalInfo)
      .build()

    val res =
      manager
        .updateCardTypeFunc(CardType.EX_CAR, ChangeTypeSource.MANUAL, Some(additionalInfo))(List.empty, current)

    assert(res._1.nonEmpty)
    val updatedCard = res._1.get._1
    assert(updatedCard.getMeta.getCardTypeInfo.getCurrentState == targetState)
    assert(updatedCard.getMeta.getCardTypeInfo.getHistoryCount == 2)
    assert(updatedCard.getMeta.getCardTypeInfo.getHistory(1).getState == targetState)
  }

  private def buildInsurancesBlock(insurances: List[VinReportModel.InsuranceItem]): VinReportModel.InsurancesBlock = {
    VinReportModel.InsurancesBlock
      .newBuilder()
      .addAllInsurances(insurances.asJava)
      .build()
  }

  private def buildInsuranceItem(
      serialOpt: Option[String],
      numberOpt: Option[String],
      from: Long = now - 240.day.toMillis,
      toOpt: Option[Long] = Some(now + 125.day.toMillis),
      insuranceType: VinReportModel.InsuranceType = VinReportModel.InsuranceType.OSAGO,
      insuranceStatus: VinReportModel.InsuranceStatus = VinReportModel.InsuranceStatus.ACTIVE,
      eventType: EventType = EventType.SH_RSA_INSURANCE_DETAILS): VinReportModel.InsuranceItem = {
    val insuranceItemBuilder = VinReportModel.InsuranceItem
      .newBuilder()
      .setFrom(from)
      .setInsuranceType(insuranceType)
      .setInsuranceStatus(insuranceStatus)
      .setMeta(
        VinReportModel.RecordMeta
          .newBuilder()
          .setSource(
            VinReportModel.RecordMeta.SourceMeta
              .newBuilder()
              .setIsAnonymous(false)
              .setEventType(eventType)
              .addAllAutoruClientIds(List.empty.asJava)
          )
          .build()
      )
      .setDate(now)
      .setInsurerName("""АО "АльфаСтрахование"""")
      .setPartnerName("Партнёр Авто.ру")
      .setRegionName("Ставропольский край, г Ставрополь")
      .setPolicyStatus("Выдан страхователю")

    numberOpt.foreach(insuranceItemBuilder.setNumber)
    serialOpt.foreach(insuranceItemBuilder.setSerial)
    toOpt.foreach(insuranceItemBuilder.setTo)

    insuranceItemBuilder.build()

  }

}
