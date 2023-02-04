package ru.auto.api.services.vos

import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, Inspectors}
import ru.auto.api.ApiOfferModel.Multiposting.Classified
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel.OfferStatus._
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.{GeoPoint, RegionInfo}
import ru.auto.api.MotoModel
import ru.auto.api.RequestModel.{AttributeUpdateRequest, PriceAttribute, UpdateRequisitesRequest}
import ru.auto.api.ResponseModel.{Filters, OfferIdsByVinsResponse}
import ru.auto.api.TrucksModel.{Bus, LightTruck, Transmission, TruckCategory}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{InvalidOfferIRefException, OfferNotFoundException}
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.model.CategorySelector.{Cars, Moto, StrictCategory, Trucks}
import ru.auto.api.model.CommonModelUtils._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.offer.MinimalTestOffers

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 17.01.17
  */
class DefaultVosClientIntTest
  extends HttpClientSuite
  with BeforeAndAfterAll
  with IntegrationPatience
  with Inspectors
  with MinimalTestOffers {

  override protected def config: HttpClientConfig = {
    HttpClientConfig("vos2-autoru-api.vrts-slb.test.vertis.yandex.net", 80)
  }

  val client = new DefaultVosClient(http, 30.seconds, 10.seconds, 30.seconds)

  private val testImage = MdsPhotoInfo
    .newBuilder()
    .setNamespace("autoru-orig")
    .setGroupId(65711)
    .setName("test_image")
    .build()

  implicit private val req = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r
  }

  test("activate multi posting") {
    client
      .activateMultiPosting(Cars, UserRef.user(696340L), OfferID.parse("1047072368-391c"))
      .futureValue
  }

  test("hide multi posting") {
    client
      .hideMultiPosting(Cars, UserRef.user(696340L), OfferID.parse("1047072368-391c"))
      .futureValue
  }

  test("archive multi posting") {
    client
      .archiveMultiPosting(Cars, UserRef.user(696340L), OfferID.parse("1047072368-391c"))
      .futureValue
  }

  test("get user listing") {
    val result = client
      .getListing(Cars, UserRef.user(696340L), Paging.Default, Filters.newBuilder().build(), NoSorting)
      .futureValue

    result.getOffersCount should be > 0
  }

  test("get dealer listing filtered by offer_i_ref including removed") {
    pending
    val offerIRefs = Vector(
      1075011171L, 1074997315L, 1074889339L, 1075433043L, 1075424379L, 1075402811L, 1075388115L, 1075374747L,
      1075360411L, 1075355923L
    )

    def fetchOfferIRefs(page: Int) = {
      val listing = client
        .getListing(
          Cars,
          UserRef.dealer(16453),
          Paging(page, pageSize = 10),
          Filters.newBuilder().addAllOfferIRef(offerIRefs.map(Long.box).asJava).build(),
          NoSorting,
          includeRemoved = true
        )
        .futureValue
      //listing.getPagination.getTotalOffersCount shouldBe offerIRefs.size
      listing.getOffersList.asScala.map(offer => OfferID.parse(offer.getId).id)
    }

    (fetchOfferIRefs(page = 1) ++ fetchOfferIRefs(page = 2)) should contain theSameElementsAs offerIRefs
  }

  test("get list of offer IDs for user") {
    val result = client.getOfferIds(Cars, UserRef.user(696340L), Filters.newBuilder().build()).futureValue

    result.size should be > 0
  }

  test("get offers count for user") {
    val result = client.countOffers(Cars, UserRef.user(696340L), Filters.newBuilder().build()).futureValue

    result.getCount should be > 0
  }

  test("get offer") {
    val user = UserRef.user(696340L)
    val id = OfferID.parse("1047072368-391c")
    client.getUserOffer(Cars, user, id, includeRemoved = false).futureValue
  }

  test("get offer (not found)") {
    val id = OfferID.parse("55-391c")
    val user = UserRef.user(696340L)
    client.getUserOffer(Cars, user, id, includeRemoved = false).failed.futureValue shouldBe an[OfferNotFoundException]
  }

  test("actualize offer") {
    client.actualize(Cars, UserRef.user(10591660L), OfferID.parse("1043045004-977b3")).futureValue

  }

  test("enable classified") {
    val user = UserRef.dealer(20101L)

    val draft = client.createDraft(Cars, user, minimalMultipostingCarOffer).futureValue
    val publishedOffer =
      client.publishDraft(Cars, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    client
      .enableClassified(Cars, user, publishedOffer.id, ClassifiedName.AVITO)
      .futureValue

    val updatedDraft = client.getOffer(Cars, publishedOffer.id).futureValue
    val classifieds = updatedDraft.getMultiposting.getClassifiedsList.asScala

    classifieds.size shouldBe 1
    classifieds.head.getName shouldBe ClassifiedName.AVITO
    classifieds.head.getEnabled shouldBe true
  }

  test("disable classified") {
    val user = UserRef.dealer(20101L)
    val offerTemplate = minimalMultipostingCarOffer
    val multiposting = offerTemplate.getMultiposting.toBuilder
      .addClassifieds(
        Classified.newBuilder().setName(ClassifiedName.AVITO).setEnabled(true).setStatus(OfferStatus.ACTIVE).build()
      )
      .build()
    val offer = offerTemplate.toBuilder.setMultiposting(multiposting).build

    val draft = client.createDraft(Cars, user, offer).futureValue
    val publishedOffer =
      client.publishDraft(Cars, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    client
      .disableClassified(Cars, user, publishedOffer.id, ClassifiedName.AVITO)
      .futureValue

    val updatedDraft = client.getOffer(Cars, publishedOffer.id).futureValue
    val classifieds = updatedDraft.getMultiposting.getClassifiedsList.asScala

    classifieds.size shouldBe 1
    classifieds.head.getName shouldBe ClassifiedName.AVITO
    classifieds.head.getEnabled shouldBe false
  }

  test("get similar offers") {
    val result = client.similar(Cars, UserRef.user(20523137L), OfferID.parse("1047633464-19ef9")).futureValue
    pendingUntilFixed(result shouldBe defined)
  }

  // draft create
  // draft update
  // draft move
  // draft publish
  // draft delete

  test("create empty draft for every category and private user") {
    val emptyOffer = Offer.newBuilder().build()

    forEvery(CategorySelector.categories) { category =>
      val user = PrivateUserRefGen.next
      client.createDraft(category, user, emptyOffer).futureValue
    }
  }

  test("create empty draft for every category and dealer user") {
    val emptyOffer = Offer.newBuilder().build()

    forEvery(CategorySelector.categories) { category =>
      val user = DealerUserRefGen.next
      client.createDraft(category, user, emptyOffer).futureValue
    }
  }

  test("create and read draft for dealer") {
    val user = UserRef.dealer(20101L)
    val emptyOffer = Offer.newBuilder().build()
    forEvery(CategorySelector.categories) { category =>
      val draft = client.createDraft(category, user, emptyOffer).futureValue

      draft.getUserRef shouldBe user.toPlain
      draft.getSellerType shouldBe SellerType.COMMERCIAL
      draft.getCategory shouldBe category.enum
      draft.getStatus shouldBe DRAFT
    }
  }

  test("create minimal truck offer") {
    pending
    val user = UserRef.dealer(20101L)
    val minimalOffer = minimalTruckOffer

    val createdDraft = client.createDraft(Trucks, user, minimalOffer).futureValue
    val publishedOffer =
      client.publishDraft(Trucks, user, createdDraft.id, AdditionalDraftParams(None, None, None)).futureValue

    val offer = client.getOffer(Trucks, publishedOffer.id).futureValue

    // fields from context
    offer.getId shouldBe publishedOffer.getId
    offer.getUserRef shouldBe user.toPlain
    offer.getSellerType shouldBe SellerType.COMMERCIAL
    offer.getCategory shouldBe Category.TRUCKS
    offer.getStatus shouldBe NEED_ACTIVATION

    // fields from draft
    offer.getSection shouldBe minimalOffer.getSection
    offer.getAvailability shouldBe minimalOffer.getAvailability
    offer.getColorHex shouldBe minimalOffer.getColorHex
    offer.getPriceInfo.selectPrice shouldBe minimalOffer.getPriceInfo.selectPrice
    offer.getPriceInfo.getCurrency shouldBe minimalOffer.getPriceInfo.getCurrency
    offer.getDocuments.getYear shouldBe minimalOffer.getDocuments.getYear
    offer.getTruckInfo.getMark shouldBe minimalOffer.getTruckInfo.getMark
    offer.getTruckInfo.getModel shouldBe minimalOffer.getTruckInfo.getModel
    offer.getTruckInfo.getTruckCategory shouldBe minimalOffer.getTruckInfo.getTruckCategory
  }

  test("add photo to draft") {
    val user = UserRef.dealer(20101L)
    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue

    val imageResponse = client.draftPhotoAdd(Trucks, user, draft.id, testImage).futureValue
    val updatedDraft = client.getDraft(Trucks, user, draft.id).futureValue

    updatedDraft.getState.getImageUrlsCount shouldBe 1
    updatedDraft.getState.getImageUrls(0).getName shouldBe imageResponse.getPhotoId
  }

  test("add photo and publish") {
    val user = UserRef.dealer(20101L)
    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue

    val imageResponse = client.draftPhotoAdd(Trucks, user, draft.id, testImage).futureValue

    val publishedOffer =
      client.publishDraft(Trucks, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue
    val offer = client.getOffer(Trucks, publishedOffer.id).futureValue

    offer.getState.getImageUrlsCount shouldBe 1
    offer.getState.getImageUrls(0).getName shouldBe imageResponse.getPhotoId
  }

  test("edit offer price") {
    val user = UserRef.dealer(20101L)
    val id = client.create(Trucks, user, minimalTruckOffer).futureValue

    changeOfferViaDraft(Trucks, user, id) {
      _.getPriceInfoBuilder.setPrice(1000000f).setDprice(1000000d)
    }

    val offer = client.getOffer(Trucks, id).futureValue
    offer.getPriceInfo.selectPrice shouldBe 1000000
  }

  test("edit offer requisites") {
    val user = UserRef.dealer(20101L)
    val id = client.create(Trucks, user, minimalTruckOffer).futureValue

    val phoneNumber = "7916" + Gen.listOfN(7, Gen.numChar).next.mkString
    val phone = Phone
      .newBuilder()
      .setPhone(phoneNumber)
      .setCallHourStart(11)
      .setCallHourEnd(18)
      .setTitle("user")
      .build()

    val location = Location
      .newBuilder()
      .setAddress("address")
      .setGeobaseId(1)
      .setCoord(
        GeoPoint
          .newBuilder()
          .setLatitude(1.0)
          .setLongitude(2.0)
      )
      .build()

    val requisites = UpdateRequisitesRequest
      .newBuilder()
      .addAllPhones(Seq(phone).asJava)
      .setLocation(location)
      .build()

    client.updateRequisites(Trucks, user, id, requisites).futureValue
    val offer = client.getOffer(Trucks, id).futureValue
    offer.getSeller.getPhonesList.asScala.map(_.getPhone) should contain(phoneNumber)
  }

  test("add photo to offer via draft") {
    val user = UserRef.dealer(20101L)
    val id = client.create(Trucks, user, minimalTruckOffer).futureValue

    val draftId = client.edit(Trucks, user, id).futureValue

    val imageResponse = client.draftPhotoAdd(Trucks, user, draftId, testImage).futureValue

    client.publishDraft(Trucks, user, draftId, AdditionalDraftParams(None, None, None)).futureValue.id shouldBe id

    val offer = client.getOffer(Trucks, id).futureValue
    offer.getState.getImageUrlsCount shouldBe 1
    offer.getState.getImageUrls(0).getName shouldBe imageResponse.getPhotoId
  }

  test("change color") {
    val user = UserRef.user(16958532L)
    val newColor = "FF8649"
    val id = client.create(Cars, user, minimalCarOffer).futureValue

    changeOfferViaDraft(Cars, user, id) {
      _.setColorHex(newColor)
    }

    val updatedOffer = client.getOffer(Cars, id).futureValue
    updatedOffer.getColorHex shouldBe newColor
  }

  test("save unconfirmed email in draft") {
    val user = UserRef.user(16958532L)
    val email = "darl@yandex-team.ru"

    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue
    val withEmail = draft.updated(_.getSellerBuilder.setUnconfirmedEmail(email))
    val u1 = client.updateDraft(Trucks, user, draft.id, withEmail, true, true).futureValue
    u1.getSeller.getUnconfirmedEmail shouldBe email
    val u2 = client.getDraft(Trucks, user, draft.id).futureValue
    u2.getSeller.getUnconfirmedEmail shouldBe email
  }

  test("clear price_info if price is empty") {
    val user = UserRef.user(16958532L)

    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue
    val withEmail = draft.updated(_.getPriceInfoBuilder.clearPrice().clearDprice())
    val u1 = client
      .updateDraft(Trucks, user, draft.id, withEmail, canChangePanorama = true, canDisableChats = true)
      .futureValue
    u1.hasPriceInfo shouldBe false
    val u2 = client.getDraft(Trucks, user, draft.id).futureValue
    u2.hasPriceInfo shouldBe false
  }

  test("save service to draft") {
    val user = UserRef.dealer(20101L)
    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue
    val builder = draft.toBuilder

    builder
      .addServicesBuilder()
      .setService(AutoruProduct.SpecialOffer.salesName)

    client.updateDraft(Trucks, user, draft.id, builder.build, true, true).futureValue.id shouldBe draft.id

    val updatedDraft = client.getDraft(Trucks, user, draft.id).futureValue
    updatedDraft.getServicesCount shouldBe 1
    updatedDraft.getServices(0).getService shouldBe AutoruProduct.SpecialOffer.salesName
  }

  test("ignore service after publication") {
    val user = UserRef.dealer(20101L)
    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue
    val builder = draft.toBuilder

    builder
      .addServicesBuilder()
      .setService(AutoruProduct.SpecialOffer.salesName)

    client.updateDraft(Trucks, user, draft.id, builder.build, true, true).futureValue.id shouldBe draft.id

    val publishedOffer =
      client.publishDraft(Trucks, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    val offer = client.getOffer(Trucks, publishedOffer.id).futureValue
    offer.getServicesCount shouldBe 0
  }

  test("save moto offer") {
    val user = UserRef.dealer(20101L)
    val draft = client.createDraft(Moto, user, minimalMotoOffer).futureValue

    val offer = client.publishDraft(Moto, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    offer.getCategory shouldBe Category.MOTO
    offer.getColorHex shouldBe minimalMotoOffer.getColorHex
    offer.getMotoInfo.getMark shouldBe minimalMotoOffer.getMotoInfo.getMark
    offer.getMotoInfo.getModel shouldBe minimalMotoOffer.getMotoInfo.getModel
    offer.getMotoInfo.getMotoCategory shouldBe minimalMotoOffer.getMotoInfo.getMotoCategory
  }

  test("save full moto offer") {
    pending
    val user = UserRef.user(16958532L)
    val fullDraft = minimalMotoOffer.updated { b =>
      b.setSection(Section.USED)
      b.getDocumentsBuilder
        .setCustomCleared(true)
        .setVin("aaabbbcccddde0001")
        .setPts(PtsStatus.DUPLICATE)
        .setCustomCleared(false)

      b.getStateBuilder.setMileage(8700)
      b.getSellerBuilder.setName("darl @ auto.ru")
      b.getSellerBuilder.getLocationBuilder.setGeobaseId(213L)
      b.getSellerBuilder.addPhonesBuilder().setPhone("79213027807")

      b.setDescription("lalalala haha beee")

      b.getMotoInfoBuilder
        .setEngine(MotoModel.Moto.Engine.INJECTOR)
        .setTransmission(MotoModel.Moto.Transmission.TRANSMISSION_6)
        .setHorsePower(31)
        .setDisplacement(250)
        .putEquipment("turn-signal", true)
    }
    val draft = client.createDraft(Moto, user, fullDraft).futureValue

    val offer0 = client.publishDraft(Moto, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue
    val offer = client.getUserOffer(Moto, user, offer0.id, includeRemoved = false).futureValue
    offer.getMotoInfo shouldBe fullDraft.getMotoInfo
    offer.getDocuments shouldBe fullDraft.getDocuments
    offer.getSeller shouldBe fullDraft.getSeller
    offer.getDescription shouldBe fullDraft.getDescription
  }

  test("save lcv body type") {
    val user = UserRef.dealer(20101L)
    val draft = client.createDraft(Trucks, user, minimalTruckOffer).futureValue
    val builder = draft.toBuilder

    builder.getTruckInfoBuilder
      .setTruckCategory(TruckCategory.LCV)
      .setLightTruckType(LightTruck.BodyType.ISOTHERMAL_BODY)

    client.updateDraft(Trucks, user, draft.id, builder.build, true, true).futureValue.id shouldBe draft.id

    val publishedOffer =
      client.publishDraft(Trucks, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    val offer = client.getOffer(Trucks, publishedOffer.id).futureValue
    offer.getTruckInfo.getTruckCategory shouldBe TruckCategory.LCV
    offer.getTruckInfo.getLightTruckType shouldBe LightTruck.BodyType.ISOTHERMAL_BODY
  }

  test("save transmission for buses") {
    val user = UserRef.dealer(20101L)
    val busOffer = minimalTruckOffer.updated { b =>
      b.getTruckInfoBuilder
        .setTruckCategory(TruckCategory.LCV)
        .setTransmission(Transmission.MECHANICAL)
        .setBusType(Bus.Type.URBAN)
      b.getDocumentsBuilder.setVin(VinGenerator.next)
    }

    val draft = client.createDraft(Trucks, user, busOffer).futureValue
    val builder = draft.toBuilder

    builder.getTruckInfoBuilder
      .setTransmission(Transmission.AUTOMATIC)

    client.updateDraft(Trucks, user, draft.id, builder.build, true, true).futureValue.id shouldBe draft.id

    val publishedOffer =
      client.publishDraft(Trucks, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    val offer = client.getOffer(Trucks, publishedOffer.id).futureValue

    offer.getTruckInfo.getTruckCategory shouldBe TruckCategory.LCV
    offer.getTruckInfo.getTransmission shouldBe Transmission.AUTOMATIC
  }

  private def changeOfferViaDraft(
      category: StrictCategory,
      user: RegisteredUserRef,
      id: OfferID
  )(mutation: Offer.Builder => Unit): Unit = {
    val draftId = client.edit(category, user, id).futureValue

    val draft = client.getDraft(category, user, draftId).futureValue

    val builder = draft.toBuilder
    mutation(builder)
    val updated = builder.build()

    client.updateDraft(category, user, draftId, updated, true, true).futureValue.id shouldBe draftId

    client.publishDraft(category, user, draftId, AdditionalDraftParams(None, None, None)).futureValue.id shouldBe id
  }

  //  val testImage = Resources.createFileFromResource("/skoda.jpg")

  //  override def afterAll() {
  //    testImage.delete()
  //  }

  /*test("real offer update attempt") {
    val f2 = client.draftPhotoAdd(CarsSelector, OfferID.parse("1047072368-391c"), testImage)
    whenReady(f2.failed) { res =>
      res shouldBe a[RealOfferUpdateAttemptException]
    }
  }

  test("add photo to unknown draft") {
    val f1 = client.draftPhotoAdd(CarsSelector, OfferID.parse("1050-hash"), testImage)
    whenReady(f1.failed) { res =>
      res shouldBe a[OfferNotFoundException]
    }
  }

  test("photo add") {
    val user = UserRef.user(20871600L)
    val offer = ModelGenerators.newOfferGen(user).next
    val draftId = client.draftCreate(CarsSelector, Some(user), offer).futureValue
    val res1: PhotoSaveSuccessResponse = client.draftPhotoAdd(CarsSelector, draftId, testImage).futureValue
    assert(res1.getStatus == ResponseStatus.SUCCESS)
    assert(res1.getPhotoId.nonEmpty)
  }*/

  // draft photo add fromurl

  // draft photo rotate cw

  // draft photo rotate ccw
  // draft photo blur
  // draft photo blur undo
  // draft photo restore
  // draft photo delete

  test("change price") {
    pending
    val user = UserRef.user(16958532L)
    val newCurrency = "USD"
    val newPrice = 10000
    val ar = AttributeUpdateRequest
      .newBuilder()
      .setPrice(
        PriceAttribute
          .newBuilder()
          .setCurrency(newCurrency)
          .setPrice(newPrice)
      )
      .build()

    val originalOffer = minimalCarOffer

    val id = client.create(Cars, user, originalOffer).futureValue
    val offer = client.getOffer(Cars, id).futureValue
    client.updateAttribute(Cars, user, id, ar, isPriceRequest = true).futureValue

    val updatedOffer = client.getOffer(Cars, id).futureValue
    updatedOffer.getPriceInfo.getCurrency shouldBe newCurrency
    updatedOffer.getPriceInfo.selectPrice shouldBe newPrice
    updatedOffer.getPriceHistoryCount shouldBe offer.getPriceHistoryCount + 1
    updatedOffer.getPriceHistory(0).selectPrice shouldBe originalOffer.getPriceInfo.selectPrice
    updatedOffer.getPriceHistory(0).getCurrency shouldBe originalOffer.getPriceInfo.getCurrency
    updatedOffer.getPriceHistory(1).selectPrice shouldBe newPrice
    updatedOffer.getPriceHistory(1).getCurrency shouldBe newCurrency

    //пробуем повроторно установить туже цену и валюту, ничего не должно измениться
    client.updateAttribute(Cars, user, id, ar, isPriceRequest = true)

    val updatedOffer2 = client.getOffer(Cars, id).futureValue
    updatedOffer2.getPriceInfo.getCurrency shouldBe newCurrency
    updatedOffer2.getPriceInfo.selectPrice shouldBe newPrice
    updatedOffer2.getPriceHistoryCount shouldBe updatedOffer.getPriceHistoryCount
    updatedOffer2.getPriceHistory(0).selectPrice shouldBe originalOffer.getPriceInfo.selectPrice
    updatedOffer2.getPriceHistory(0).getCurrency shouldBe originalOffer.getPriceInfo.getCurrency
    updatedOffer2.getPriceHistory(1).selectPrice shouldBe newPrice
    updatedOffer2.getPriceHistory(1).getCurrency shouldBe newCurrency
  }

  test("get hashed id") {
    val user = UserRef.user(696340L)
    val iRef = 1047072368L
    val hashed = OfferID(iRef, Some("391c"))
    client.getHashedId(Cars, user, iRef).futureValue shouldBe hashed
  }

  test("get dealer's batched hash ids") {
    val dealer = AutoruDealer(232)
    val iRef = 1114089644L
    val iRefs = Set(iRef)
    val hashed = OfferID(iRef, Some("691e8208"))

    client.getHashedIds(CategorySelector.All, dealer, iRefs).futureValue shouldBe Set(hashed)
  }

  test("get detailed offer by vin") {
    val user = UserRef.user(696340L)
    val vin = "XW8AN2NEXFH009101"
    val expected = OfferIdsByVinsResponse
      .newBuilder()
      .putResult(
        "XW8AN2NEXFH009101",
        OfferIdsByVinsResponse.OfferIdByVin
          .newBuilder()
          .setOfferId("1048926490-c306")
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .build()
      )
      .build()
    client.getDetailedOfferIdsByVins(user, Set(vin)).futureValue shouldBe expected
  }

  test("get hashed id (not found)") {
    val user = UserRef.user(696340L)
    val iRef = Long.MaxValue
    client.getHashedId(Cars, user, iRef).failed.futureValue shouldBe an[OfferNotFoundException]
  }

  test("get hashed id (bad request)") {
    val user = UserRef.user(696340L)
    val iRef = -1L
    client.getHashedId(Cars, user, iRef).failed.futureValue shouldBe an[InvalidOfferIRefException]
  }

  test("check, that offers belong to user") {
    val user = UserRef.user(696340L)
    val offerIds = List(OfferID(1047072368L, Some("391c")), OfferID(1048926490L, Some("c306")))
    client.checkBelong(user, offerIds).futureValue shouldBe true
  }

  test("check, that offers don't belong to user") {
    val user = UserRef.user(696340L)
    val offerIds = List(OfferID(1047072368L, Some("391c")), OfferID(1043045004, Some("977b3")))
    client.checkBelong(user, offerIds).futureValue shouldBe false
  }

  test("check, that offer belongs to user") {
    val user = UserRef.user(696340L)
    val offerId = OfferID(1047072368L, Some("391c"))
    client.checkBelong(user, List(offerId)).futureValue shouldBe true
  }

  test("check, that offer doesn't belong to user") {
    val user = UserRef.user(696340L)
    val offerId = OfferID(1043045004, Some("977b3"))
    client.checkBelong(user, List(offerId)).futureValue shouldBe false
  }

  test("update delivery in draft") {
    val dealer = UserRef.dealer(20101L)
    val draft = client.createDraft(Trucks, dealer, minimalTruckOffer).futureValue

    val offer = client.publishDraft(Trucks, dealer, draft.id, AdditionalDraftParams(None, None, None)).futureValue

    val deliveryInfo = DeliveryInfo
      .newBuilder()
      .addDeliveryRegions(
        DeliveryRegion
          .newBuilder()
          .setLocation(
            Location
              .newBuilder()
              .setAddress("Sadovnicheskaya 82.2")
              .setGeobaseId(1)
              .setFederalSubjectId(1)
              .setCoord(
                GeoPoint
                  .newBuilder()
                  .setLatitude(1.0)
                  .setLongitude(2.0)
              )
              .setRegionInfo(RegionInfo.getDefaultInstance)
          )
      )
      .build()

    client.updateDelivery(Trucks, dealer, offer.id, deliveryInfo).futureValue
    client.getOffer(Trucks, offer.id).futureValue.getDeliveryInfo shouldBe deliveryInfo
  }

  test("update delivery in published") {
    val dealer = UserRef.dealer(20101L)
    val offerId = client.create(Trucks, dealer, minimalTruckOffer).futureValue

    val deliveryInfo = DeliveryInfo
      .newBuilder()
      .addDeliveryRegions(
        DeliveryRegion
          .newBuilder()
          .setLocation(
            Location
              .newBuilder()
              .setAddress("Sadovnicheskaya 82.2")
              .setGeobaseId(1)
              .setFederalSubjectId(1)
              .setCoord(
                GeoPoint
                  .newBuilder()
                  .setLatitude(1.0)
                  .setLongitude(2.0)
              )
              .setRegionInfo(RegionInfo.getDefaultInstance)
          )
      )
      .build()

    client.updateDelivery(Trucks, dealer, offerId, deliveryInfo).futureValue
    client.getOffer(Trucks, offerId).futureValue.getDeliveryInfo shouldBe deliveryInfo
  }
}
