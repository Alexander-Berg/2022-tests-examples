package ru.yandex.vos2.autoru.dao.proxy

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Category, Offer, Section}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.scalatest.BetterEitherValues
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.Filters
import ru.yandex.vos2.autoru.model.AutoruOfferID
import ru.yandex.vos2.autoru.utils.testforms.{FormInfo, TestFormParams, TestFormsGenerator}
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.model.{UserRef, UserRefAutoru}
import ru.yandex.vos2.{getNow, OfferID, OfferModel, Vin}
import ru.yandex.vos2.autoru.utils.testforms.CarTestForms

/**
  * @author pnaydenov
  */
class OffersReaderTest extends AnyFunSuite with InitTestDbs with Matchers with BetterEitherValues {
  initDbs()
  private val testFormGenerator = new TestFormsGenerator(components)
  private val carTestForms = new CarTestForms(components)
  components.featureRegistry.updateFeature(components.featuresManager.IncompatibleEquipmentCheck.name, false)

  private def activate(offer: OfferModel.Offer): Unit = {
    components.offerYdbDao.updateFunc(AutoruOfferID.parse(offer.getOfferID))(Traced.empty) { offer =>
      OfferUpdate.visitNow(offer.toBuilder.clearFlag(OfferFlag.OF_NEED_ACTIVATION).build())
    }
  }

  private def setVin(offer: ApiOfferModel.Offer, vin: String): ApiOfferModel.Offer = {
    offer.toBuilder.setDocuments {
      offer.getDocuments.toBuilder
        .setVin(vin)
    }.build
  }

  private def expire(offer: OfferModel.Offer): Unit = {
    components.offerYdbDao.updateFunc(AutoruOfferID.parse(offer.getOfferID))(Traced.empty) { offer =>
      OfferUpdate.visitNow(
        offer.toBuilder
          .clearFlag(OfferFlag.OF_NEED_ACTIVATION)
          .putFlag(OfferFlag.OF_EXPIRED)
          .build()
      )
    }
  }

  test("find same offer by VIN") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val user = testFormGenerator.randomUser
    val form1 = carTestForms
      .createForm(
        TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
      )
    val form2 = form1

    val form3 =
      Iterator
        .continually {
          carTestForms
            .createForm(
              TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
            )
        }
        // avoid an infinite loop
        .take(100)
        .collectFirst { case x if (x.card.mark, x.card.model) != ((form1.card.mark, form1.card.model)) => x }
        .getOrElse(sys.error("Failed to generate a form with a unique card"))

    val userRef = user.userRef

    val ad1 = components.offersReader.loadAdditionalData(userRef, form1.form)(Traced.empty)
    val ad2 = components.offersReader.loadAdditionalData(userRef, form2.form)(Traced.empty)
    val ad3 = components.offersReader.loadAdditionalData(userRef, form3.form)(Traced.empty)

    val first = withClue(form1.form + "\n\n" + ad1) {
      components.formWriter
        .createOffer(userRef, category, form1.form, getNow, ad1, None, FormWriteParams(), "test", None)
        .right
        .value
        .offer
    }

    val updatedForm2: Offer = setVin(form2.form, first.getOfferAutoru.getDocuments.getVin)
    val second = withClue(updatedForm2 + "\n\n" + ad2) {
      components.formWriter
        .createOffer(
          userRef,
          category,
          updatedForm2,
          getNow,
          ad2,
          None,
          FormWriteParams(),
          "test",
          None
        )
        .right
        .value
        .offer
    }
    components.offerVosDao.saveMigratedFromYdb(Seq(second, first))(Traced.empty)

    Thread.sleep(100)

    val same = components.offersReader.findSameOffer(userRef, second.getOfferID, Some(category))

    assert(same.exists(_.getOfferID == first.getOfferID))
    val updatedForm3 = setVin(form3.form, first.getOfferAutoru.getDocuments.getVin)
    val third = withClue(updatedForm3 + "\n\n" + ad3) {
      components.formWriter
        .createOffer(
          userRef,
          category,
          updatedForm3,
          getNow,
          ad3,
          None,
          FormWriteParams(),
          "test",
          None
        )
        .right
        .value
        .offer
    }
    components.offerVosDao.saveMigratedFromYdb(Seq(second, first, third))(Traced.empty)

    Thread.sleep(100)

    // This relies on `third` having different mark and/or model from `first` and `second`.
    val notSame = components.offersReader.findSameOffer(userRef, third.getOfferID, Some(category))
    assert(notSame.isEmpty)
  }

  test("do not find offer with different truck category") {
    implicit val t = Traced.empty

    val user = testFormGenerator.randomUser
    val userRef = user.userRef
    val category = Category.TRUCKS
    val form1 = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
    )
    val form2 = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
    )
    val ad = components.offersReader.loadAdditionalData(userRef, form1.form)(Traced.empty)

    val updatedForm = form1.form.toBuilder.setTruckInfo(form2.form.getTruckInfo).build()

    val writtenOffer = withClue(updatedForm + "\n\n" + ad) {
      components.formWriter
        .createOffer(
          userRef,
          category,
          updatedForm,
          getNow,
          ad,
          None,
          FormWriteParams(),
          "test",
          None
        )
        .right
        .value
        .offer
    }
    Thread.sleep(100)

    val offerFound = components.offersReader.findSameOffer(userRef, writtenOffer.getOfferID, Some(category))
    assert(offerFound.isEmpty)
  }

  test("do not find offer with different moto category") {
    implicit val t = Traced.empty

    val user = testFormGenerator.randomUser
    val userRef = user.userRef
    val category = Category.MOTO
    val form1 = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
    )
    val form2 = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
    )
    val ad = components.offersReader.loadAdditionalData(userRef, form1.form)(Traced.empty)

    val updatedForm = form1.form.toBuilder.setMotoInfo(form2.form.getMotoInfo).build()

    val writtenOffer = withClue(updatedForm + "\n\n" + ad) {
      components.formWriter
        .createOffer(
          userRef,
          category,
          updatedForm,
          getNow,
          ad,
          None,
          FormWriteParams(),
          "test",
          None
        )
        .right
        .value
        .offer
    }
    Thread.sleep(100)

    val offerFound = components.offersReader.findSameOffer(userRef, writtenOffer.getOfferID, Some(category))
    assert(offerFound.isEmpty)
  }

  test("find nothing without VIN") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef

    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)

    //Так как все офферы созданы из одной формы - марка и модель у них одинаковые
    val first = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val vin = first.getOfferAutoru.getDocuments.getVin

    val noVin = components.formWriter
      .createOffer(userRef, category, setVin(formInfo.form, ""), getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val nv = components.offersReader.findSameOffer(userRef, noVin.getOfferID, Some(category))

    assert(nv.isEmpty)
  }

  test("find offer ids by vin numbers") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)

    val offer1 = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val offer2 = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2))(Traced.empty)

    val result = components.offersReader.getOfferIdsByVins(
      Seq(
        offer1.getOfferAutoru.getDocuments.getVin,
        offer2.getOfferAutoru.getDocuments.getVin
      ),
      userRef,
      includeRemoved = false
    )

    val expectedResult = Seq(
      (offer1.getOfferAutoru.getDocuments.getVin, offer1.getOfferID),
      (offer2.getOfferAutoru.getDocuments.getVin, offer2.getOfferID)
    )

    assert(result.size == 2)
    assertResult(expectedResult)(result)
  }

  test("get list detailed offer ids by vins numbers") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)

    val offer1 = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val offer2 = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2))(Traced.empty)

    val vins = Seq(
      offer1.getOfferAutoru.getDocuments.getVin,
      offer2.getOfferAutoru.getDocuments.getVin
    )

    val result: Seq[(Vin, (OfferID, Int, Int))] = components.offersReader.getDetailedOfferIdsByVins(
      vins,
      userRef,
      includeRemoved = false
    )

    val expectedResult: Seq[(Vin, (OfferID, Int, Int))] = Seq(
      (
        offer1.getOfferAutoru.getDocuments.getVin,
        (offer1.getOfferID, offer1.getOfferAutoru.getCategory.getNumber, offer1.getOfferAutoru.getSection.getNumber)
      ),
      (
        offer2.getOfferAutoru.getDocuments.getVin,
        (offer2.getOfferID, offer2.getOfferAutoru.getCategory.getNumber, offer2.getOfferAutoru.getSection.getNumber)
      )
    )
    assert(result.size == vins.size, s"result should be ${vins.size}")
    assertResult(expectedResult)(result)
  }

  test("get offer ids by user ref") {
    implicit val t = Traced.empty

    val carsCategory = Category.CARS
    val carsFormInfo: FormInfo = testFormGenerator.createForm(carsCategory.name().toLowerCase, TestFormParams())
    val userRef = carsFormInfo.userRef
    val adCars: AdditionalData = components.offersReader.loadAdditionalData(userRef, carsFormInfo.form)

    val carOffer = createOfferFromForm(userRef, carsCategory, carsFormInfo, adCars)

    val actualSection = carOffer.getOfferAutoru.getSection
    val invertedSection = actualSection match {
      case Section.NEW => Section.USED
      case _ => Section.NEW
    }
    components.offerVosDao.saveMigratedFromYdb(Seq(carOffer))(Traced.empty)

    val resultAll = components.offersReader.getOfferIds(
      userRef = userRef,
      optCategory = None,
      filters = createFilter(None),
      includeRemoved = false
    )

    val resultEmpty = components.offersReader.getOfferIds(
      userRef = userRef,
      optCategory = None,
      filters = createFilter(Some(invertedSection)),
      includeRemoved = false
    )

    val resultEmpty2 = components.offersReader.getOfferIds(
      userRef = UserRef.from("a_000000001"),
      optCategory = None,
      filters = createFilter(None),
      includeRemoved = false
    )

    val resultCars = components.offersReader.getOfferIds(
      userRef = userRef,
      optCategory = Some(carsCategory),
      filters = createFilter(Some(actualSection)),
      includeRemoved = false
    )

    val expectedResultAll = Seq(carOffer.getOfferID)
    val expectedResultEmpty = Seq.empty
    val expectedResultCars = Seq(carOffer.getOfferID)

    assert(resultAll.contains(carOffer.getOfferID))
    assert(!resultEmpty.contains(carOffer.getOfferID))
    assert(resultCars.contains(carOffer.getOfferID))
    assert(resultEmpty2.isEmpty)
  }

  test("getMarkModels CARS category") {
    implicit val t = Traced.empty

    val carsCategory = Category.CARS
    val carsFormInfo: FormInfo = testFormGenerator.createForm(carsCategory.name().toLowerCase, TestFormParams())
    val userRef = carsFormInfo.userRef
    val adCars: AdditionalData = components.offersReader.loadAdditionalData(userRef, carsFormInfo.form)

    val carOffer = createOfferFromForm(userRef, carsCategory, carsFormInfo, adCars)
    components.offerVosDao.saveMigratedFromYdb(Seq(carOffer))(Traced.empty)

    val result = components.offersReader.getMarkModels(userRef, None, createFilter(None), true, false)

    assert(result.nonEmpty)
  }

  test("getMarkModels by user ref") {
    implicit val t = Traced.empty

    val carsCategory = Category.TRUCKS
    val carsFormInfo: FormInfo = testFormGenerator.createForm(carsCategory.name().toLowerCase, TestFormParams())
    val userRef = carsFormInfo.userRef
    val adCars: AdditionalData = components.offersReader.loadAdditionalData(userRef, carsFormInfo.form)

    val carOffer = createOfferFromForm(userRef, carsCategory, carsFormInfo, adCars)
    components.offerVosDao.saveMigratedFromYdb(Seq(carOffer))(Traced.empty)

    val result = components.offersReader.getMarkModels(userRef, None, createFilter(None), true, false)

    assert(result.nonEmpty)

  }

  test("find offers by filter avito_banned") {
    implicit val t = Traced.empty

    val user = testFormGenerator.randomUser
    val category = Category.CARS
    val userRef = user.userRef

    val form1 = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
    )
    val form2 = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(wheelLeft = Some(true), optOwnerId = Some(user.id))
    )

    val classified1 = ApiOfferModel.Multiposting.Classified
      .newBuilder()
      .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.AVITO)
      .setStatus(ApiOfferModel.OfferStatus.BANNED)
      .build()
    val multiposting1 = ApiOfferModel.Multiposting
      .newBuilder()
      .addClassifieds(classified1)
      .build()

    val classified2 = ApiOfferModel.Multiposting.Classified
      .newBuilder()
      .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.AVITO)
      .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
      .build()

    val multiposting2 = ApiOfferModel.Multiposting
      .newBuilder()
      .addClassifieds(classified2)
      .build()

    val offer1 = form1.form.toBuilder.setMultiposting(multiposting1).build()
    val offer2 = form2.form.toBuilder.setMultiposting(multiposting2).build()

    val ad1 = components.offersWriter.loadAdditionalData(userRef, offer1)
    val ad2 = components.offersWriter.loadAdditionalData(userRef, offer2)

    val first = components.formWriter
      .createOffer(userRef, category, offer1, getNow, ad1, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val second = components.formWriter
      .createOffer(userRef, category, offer2, getNow, ad2, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    components.offerVosDao.saveMigratedFromYdb(Seq(second, first))(Traced.empty)

    val filters: Filters = createFilter(None, Seq("AVITO_CS_BANNED"))
    val result = components.offersReader.getOffers(userRef, Some(category), filters, None)

    assert(result.size == 1)
    assert(result.head.getOfferID == first.getOfferID)
  }

  private def createOfferFromForm(userRef: UserRef, category: Category, form: FormInfo, ad: AdditionalData)(
      implicit traced: Traced
  ): OfferModel.Offer =
    components.formWriter
      .createOffer(userRef, category, form.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

  private def createFilter(section: Option[Section], classifiedStatus: Seq[String] = Seq.empty): Filters = Filters(
    truckCategory = Seq.empty,
    motoCategory = Seq.empty,
    status = Seq.empty,
    service = Seq.empty,
    multipostingStatus = Seq.empty,
    multipostingService = Seq.empty,
    tag = Seq.empty,
    excludeTag = Seq.empty,
    vin = Seq.empty,
    markModel = Seq.empty,
    priceFrom = None,
    priceTo = None,
    geobaseId = Seq.empty,
    banReasons = Seq.empty,
    section = section,
    createDateFrom = None,
    createDateTo = None,
    noActiveServices = None,
    offerIRef = Seq.empty,
    licensePlate = Seq.empty,
    exteriorPanorama = Seq.empty,
    interiorPanorama = Seq.empty,
    canSendFavoriteMessage = None,
    favoriteMessageWasSent = None,
    yearFrom = None,
    yearTo = None,
    year = Seq.empty,
    availability = Seq.empty,
    hasExteriorPanorama = None,
    hasInteriorPanorama = None,
    hasPhoto = None,
    superGen = Seq.empty,
    canBook = None,
    classifiedStatus = classifiedStatus,
    colorHex = Seq.empty,
    callsAuctionBidFrom = None,
    callsAuctionBidTo = None,
    hasCallsAuctionBid = None,
    recommendationTags = Seq.empty
  )
}
