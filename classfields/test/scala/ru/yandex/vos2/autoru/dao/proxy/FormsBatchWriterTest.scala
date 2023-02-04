package ru.yandex.vos2.autoru.dao.proxy

import com.google.protobuf.UninitializedMessageException
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.DiscountPrice.DiscountPriceStatus
import ru.auto.api.{ApiOfferModel, TrucksModel}
import ru.auto.feedprocessor.FeedprocessorModel.Task
import ru.yandex.vertis.scalatest.BetterEitherValues
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.EquipmentElement
import ru.yandex.vos2.AutoruModel.AutoruOffer.{SourceInfo, TruckInfo}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormsBatchWriter.{Create, CreateOrUpdate, Update}
import ru.yandex.vos2.autoru.utils.ApiFormUtils.{RichPriceInfoBuilder, RichPriceInfoOrBuilder}
import ru.yandex.vos2.autoru.utils.FeedprocessorHashUtils
import ru.yandex.vos2.autoru.utils.testforms.{FormInfo, TestFormParams, TestFormsGenerator}
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors._
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.UserRef

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 6/29/17.
  */
@RunWith(classOf[JUnitRunner])
class FormsBatchWriterTest extends AnyFunSuite with InitTestDbs with Matchers with BetterEitherValues {
  initDbs()

  private val testFormGenerator = new TestFormsGenerator(components)
  private val batchWriter = new FormsBatchWriter(components)
  private val offersReader = components.offersReader

  for {
    isDealer <- Seq(true, false)
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"forms creation (dealer = $isDealer, vos = false, category = $category)") {
      implicit val t = Traced.empty
      formsCreationTests(isDealer, category)
    }
  }

  test("return notices") {
    implicit val t = Traced.empty
    val (ownerId, _, userRef) = testFormGenerator.randomOwnerIds(isDealer = true)
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(
      categoryEnum.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(ownerId))
    )
    val builder = testFormGenerator.updateForm(formInfo, TestFormParams()).form.toBuilder
    builder
      .setTruckInfo(
        builder.getTruckInfoBuilder
          .setTruckCategory(TrucksModel.TruckCategory.TRAILER)
          .setMark("KRONE")
          .setModel("KRONE")
          // illegal
          .putEquipment(TruckInfo.EquipmentElement.TRUCK_EQUIP_NAVIGATION.toString.stripPrefix("TRUCK_EQUIP_"), true)
          .putEquipment(TruckInfo.EquipmentElement.TRUCK_EQUIP_ABS.toString.stripPrefix("TRUCK_EQUIP_"), true)
      ) // suitable
      .clearDiscountOptions() // Avoid discount notices
    testFormGenerator.truckTestForms
      .setBodyTypeByCategory(builder.getTruckInfoBuilder, TrucksModel.TruckCategory.TRAILER)
    val form = builder.build()
    val forms = Map("foo" -> form) // map by abstract id
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)

    val createResult =
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, FormWriteParams(), "")

    val Create(createdOffer, createNotices) = createResult.head._2.right.value
    val forms2 = Map(createdOffer.getOfferID -> form) // map by offerID

    val updateResult = batchWriter.batchUpdate(userRef, categoryEnum, forms2, getNow, ad, FormWriteParams(), "")

    val Update(updatedOffer, updateNotices) = updateResult.head._2.right.value
    val abs = EquipmentElement.TRUCK_EQUIP_ABS
    createdOffer.getOfferAutoru.getTruckInfo.getEquipmentElemsList should contain only abs
    updatedOffer.getOfferAutoru.getTruckInfo.getEquipmentElemsList should contain only abs
    createNotices.toList shouldEqual List(UnknownEquipment("NAVIGATION"))
    updateNotices.toList shouldEqual List(UnknownEquipment("NAVIGATION"))
  }

  test("can update archived offer") {
    implicit val t = Traced.empty
    val (ownerId, _, userRef) = testFormGenerator.randomOwnerIds(isDealer = true)
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(ownerId))
    )
    val formBuilder = testFormGenerator.updateForm(formInfo, TestFormParams()).form.toBuilder
    val form = formBuilder.build()
    val forms0 = Map("foo" -> form)
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms0)(Traced.empty)
    val createResult =
      batchWriter.batchCreate(userRef, category, forms0, getNow, ad, FormWriteParams(), "")
    val createdOffer = createResult.head._2.right.value.offer

    val b: Offer.Builder = form.toBuilder
    b.getStateBuilder.setMileage(145455)
    val forms = Map(createdOffer.getOfferID -> b.build())
    components.offersWriter.setArchive(
      createdOffer.getOfferID,
      Some(userRef),
      Some(category),
      archive = true,
      comment = "Test",
      None
    )

    val updateResult =
      batchWriter.batchUpdate(userRef, category, forms, getNow, ad, FormWriteParams(canUpdateRemoved = true), "")
    val currentOffer = offersReader.findOffer(
      Some(userRef),
      createdOffer.getOfferID,
      Some(category),
      includeRemoved = false,
      operateOnMaster = true
    )
    updateResult.values.head match {
      case Right(Update(offer, Nil)) =>
        offer.getOfferAutoru.getState.getMileage shouldEqual 145455
    }
    currentOffer shouldBe defined
    currentOffer.get.getOfferAutoru.getState.getMileage shouldEqual 145455
  }

  test("errors in case of unpermitted editing") {
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(
      categoryEnum.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(24813))
    )
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSellerBuilder
      .setLocation(
        Location
          .newBuilder()
          .setAddress("улица Академика Анохина, 6к5")
          .setGeobaseId(213)
      )
      .setCustomLocation(true)
    formBuilder.getSellerBuilder.addPhones(Phone.newBuilder().setPhone("12345678877")).setCustomPhones(true)

    val form = formBuilder.build()
    val userRef = formInfo.userRef
    val forms = Map("foo" -> form) // map by abstract id
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val sourceInfo = SourceInfo
      .newBuilder()
      .setUserRef(userRef.toPlain)
      .build()
    val params = FormWriteParams(sourceInfo = Some(sourceInfo))

    val createResult =
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params.copy(), "")
    val errors = createResult.head._2.left.get
    errors should have size 2
    errors.collectFirst { case ForbiddenSalonEditAddress => true } shouldBe defined
    errors.collectFirst { case ForbiddenSalonEditPhones(Seq("12345678877"), _) => true } shouldBe defined
  }

  test("notices in case of unpermitted feed editing") {
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(
      categoryEnum.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(24813))
    )
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSellerBuilder
      .setLocation(
        Location
          .newBuilder()
          .setAddress("улица Академика Анохина, 6к5")
          .setGeobaseId(213)
      )
      .setCustomLocation(true)
    formBuilder.getSellerBuilder.addPhones(Phone.newBuilder().setPhone("12345678877")).setCustomPhones(true)
    formBuilder.clearDiscountOptions() // Avoid discount notices

    val form = formBuilder.build()
    val userRef = formInfo.userRef
    val forms = Map("foo" -> form) // map by abstract id
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val sourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.FEED)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef(userRef.toPlain)
      .build()
    val params = FormWriteParams(sourceInfo = Some(sourceInfo), isFeed = true)

    val createResult =
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")
    val Create(createdOffer, createNotices) = createResult.head._2.right.value
    val forms2 = Map(createdOffer.getOfferID -> form) // map by offerID
    val updateResult = batchWriter.batchUpdate(userRef, categoryEnum, forms2, getNow, ad, params, "")
    val Update(updatedOffer, updateNotices) = updateResult.head._2.right.value

    val salonPhones = updatedOffer.getOfferAutoru.getSeller.getPhoneList.asScala.toSeq.map(_.getNumber)
    createNotices.toSet shouldEqual
      Set(ForbiddenSalonEditAddress, ForbiddenSalonEditPhones(Seq("12345678877"), salonPhones))
    updateNotices.toSet shouldEqual
      Set(ForbiddenSalonEditAddress, ForbiddenSalonEditPhones(Seq("12345678877"), salonPhones))
    assert(updatedOffer.getOfferAutoru.getSeller.getPlace.getGeobaseId != 213)
    assert(updatedOffer.getOfferAutoru.getSeller.getPhoneList.asScala.forall(_.getNumber != "12345678877"))
  }

  test("allow phone editing") {
    implicit val t = Traced.empty
    val categoryEnum = Category.CARS
    val formInfo = testFormGenerator.createForm(
      categoryEnum.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(10086))
    )
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSellerBuilder
      .clearPhones()
      .addPhones(Phone.newBuilder().setPhone("12345678877"))
      .setCustomPhones(true)

    val form = formBuilder.build()
    val userRef = formInfo.userRef
    val forms = Map("foo" -> form) // map by abstract id
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val sourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.FEED)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef(userRef.toPlain)
      .build()
    val params = FormWriteParams(sourceInfo = Some(sourceInfo))

    val createResult =
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")
    val Create(createdOffer, createNotices) = createResult.head._2.right.value

    val offer = components.offersReader
      .findOffer(
        Some(userRef),
        createdOffer.getOfferID,
        Some(categoryEnum),
        includeRemoved = false,
        operateOnMaster = true
      )
      .value

    assert(offer.getOfferAutoru.getSeller.getPhone(0).getNumber == "12345678877")

    formBuilder.getSellerBuilder.clearPhones().addPhones(Phone.newBuilder().setPhone("12345678888"))
    val form2 = formBuilder.build()
    val forms2 = Map(createdOffer.getOfferID -> form2) // map by offerID
    batchWriter.batchUpdate(userRef, categoryEnum, forms2, getNow, ad, params, "")

    val offer2 = components.offersReader
      .findOffer(
        Some(userRef),
        createdOffer.getOfferID,
        Some(categoryEnum),
        includeRemoved = false,
        operateOnMaster = true
      )
      .value
    assert(offer2.getOfferAutoru.getSeller.getPhone(0).getNumber == "12345678888")
  }

  test("no notices in case of request w/o custom phone & address checkers") {
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(
      categoryEnum.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(24813))
    )

    val formBuilder = formInfo.form.toBuilder
    formBuilder.getSellerBuilder.setLocation(
      Location
        .newBuilder()
        .setAddress("улица Академика Анохина, 6к5")
        .setGeobaseId(213)
    )
    formBuilder.getSellerBuilder.addPhonesBuilder().setPhone("12345678877")
    val form = formBuilder.build()
    val userRef = formInfo.userRef
    val forms = Map("foo" -> form) // map by abstract id
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val sourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.FEED)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef(userRef.toPlain)
      .build()
    val params = FormWriteParams(sourceInfo = Some(sourceInfo))

    val createResult =
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")
    val createdOffer = createResult.head._2.right.value.offer
    val forms2 = Map(createdOffer.getOfferID -> form) // map by offerID
    val updateResult = batchWriter.batchUpdate(userRef, categoryEnum, forms2, getNow, ad, params, "")
    val updatedOffer = updateResult.head._2.right.value.offer

    assert(createdOffer.getOfferAutoru.getConversionErrorCount == 0)
    assert(updatedOffer.getOfferAutoru.getConversionErrorCount == 0)
  }

  test("ignore some checks for truck offer updated from feed") {
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(categoryEnum.name().toLowerCase, TestFormParams())
    val form = formInfo.form
    val userRef = formInfo.userRef
    val forms = Map("foo" -> form) // map by abstract id
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val desktopSourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.DESKTOP)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef(userRef.toPlain)
      .build()
    val feedSourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.FEED)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef(userRef.toPlain)
      .build()
    val params = FormWriteParams(sourceInfo = Some(desktopSourceInfo))

    val createResult = batchWriter.batchCreate(
      userRef,
      categoryEnum,
      forms,
      getNow,
      ad,
      FormWriteParams(sourceInfo = Some(desktopSourceInfo)),
      ""
    )
    val createdOffer = createResult.head._2.right.value.offer
    assert(createdOffer.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(createdOffer.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(createdOffer.getOfferAutoru.getOwnership.hasPtsOwnersCount)

    val formBuilder = form.toBuilder
    formBuilder.getDocumentsBuilder.clearPts()
    formBuilder.getDocumentsBuilder.clearOwnersNumber()
    formBuilder.getPriceInfoBuilder.setPrice(form.getPriceInfo.selectPrice + 100)
    val formWithoutPts = formBuilder.build()

    // errors for forms w/o PTS, owners
    val forms2 = Map(createdOffer.getOfferID -> formWithoutPts)
    val updateResult = batchWriter.batchUpdate(
      userRef,
      categoryEnum,
      forms2,
      getNow,
      ad,
      FormWriteParams(sourceInfo = Some(desktopSourceInfo)),
      ""
    )
    val errors = updateResult.head._2.left.get
    assert(errors.contains(NotexistOwners))
    assert(errors.contains(NotexistPts))

    // success for forms from feed even if w/o PTS, owners
    val updateResult2 = batchWriter.batchUpdate(
      userRef,
      categoryEnum,
      forms2,
      getNow,
      ad,
      FormWriteParams(sourceInfo = Some(feedSourceInfo)),
      ""
    )
    val updatedOffer = updateResult2.head._2.right.value.offer
    assert(updatedOffer.getOfferAutoru.getPrice.getPrice == form.getPriceInfo.selectPrice + 100)
  }

  test("insert offers without vins") {
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(categoryEnum.name().toLowerCase, TestFormParams())
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getDocumentsBuilder
      .clearVin()
      .clearSts()
      .setYear(2000)
      .getPurchaseDateBuilder
      .setYear(2001)
      .setMonth(1)
      .setDay(1)
    // form1 and form2 almost equal except feedprocessor_unique_id, form3 has difference
    val form1 = formBuilder.setFeedprocessorUniqueId("1").build()
    val form2 = formBuilder.setFeedprocessorUniqueId("2").build()
    formBuilder.getDocumentsBuilder.setYear(formBuilder.getDocumentsBuilder.getYear + 1)
    val form3 = formBuilder.setFeedprocessorUniqueId("3").build()

    val userRef = formInfo.userRef
    val forms = Map("foo" -> form1, "bar" -> form2, "baz" -> form3)
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val params = FormWriteParams()

    val createResult = batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")
    createResult should have size 3
    (createResult.keys should contain).allOf("foo", "bar", "baz")

    // try recreate - should find existing offers
    val recreateResult = batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")
    recreateResult should have size 3
    assert(recreateResult("baz").isRight, s"""recreateResult("baz") is left: ${recreateResult("baz")}""")
    recreateResult("baz").right.value.offer.getOfferAutoru.getFeedprocessorUniqueId shouldEqual "3"
    // should save foo or bar - unpredictable behavior
    recreateResult.find(_._1 != "baz").get._2.right.value.offer.getOfferAutoru.getFeedprocessorUniqueId should (equal(
      "1"
    ) or equal("2"))
  }

  test("don't insert offer in case of VOS failure") {
    pending
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(categoryEnum.name().toLowerCase, TestFormParams())
    val formBuilder = formInfo.form.toBuilder
    formBuilder.getDiscountPriceBuilder.setPrice(0).setStatus(DiscountPriceStatus.ACTIVE) // unexpected combination
    val userRef = formInfo.userRef
    val forms = Map("foo" -> formBuilder.build())
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val params = FormWriteParams() // it works in any case, with or without ignoreVosDbExceptions

    an[UninitializedMessageException] shouldBe thrownBy {
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")
    }
  }

  test("don't update offer in case of VOS failure") {
    pending
    implicit val t = Traced.empty
    val categoryEnum = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(categoryEnum.name().toLowerCase, TestFormParams())
    val formBuilder = formInfo.form.toBuilder
    val userRef = formInfo.userRef
    val forms = Map("foo" -> formBuilder.build())
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val params = FormWriteParams()

    val result = batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params, "")

    formBuilder.getDiscountPriceBuilder.setPrice(0).setStatus(DiscountPriceStatus.ACTIVE) // unexpected combination
    val forms2 = Map(result.head._2.right.value.offer.getOfferID -> formBuilder.build())
    an[UninitializedMessageException] shouldBe thrownBy {
      batchWriter.batchUpdate(userRef, categoryEnum, forms2, getNow, ad, params, "")
    }
  }

  test("can find appropriate offer by unified VIN") {
    val vin1 = "Xw0ZzZ4L1EG001019"
    val vin2 = "xW0zZz4l1eg001019"

    implicit val t = Traced.empty
    val (ownerId, _, userRef) = testFormGenerator.randomOwnerIds(isDealer = true)
    val category = Category.TRUCKS
    val formInfo = testFormGenerator.createForm(
      category.name().toLowerCase,
      TestFormParams(isDealer = true, optOwnerId = Some(ownerId), wheelLeft = Some(false))
    )
    val formBuilder1 = formInfo.form.toBuilder
    formBuilder1.getDocumentsBuilder.setVin(vin1)
    val forms1 = Map("foo" -> formBuilder1.build())
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms1)(Traced.empty)
    val createResult =
      batchWriter.batchCreate(userRef, category, forms1, getNow, ad, FormWriteParams(), "")
    val createdOffer = createResult.head._2.right.value.offer

    val formBuilder2: Offer.Builder = formBuilder1.build.toBuilder
    formBuilder2.getDocumentsBuilder.setVin(vin2)
    val forms2 = Map("foo" -> formBuilder2.build())

    val updateResult = batchWriter.batchCreate(userRef, category, forms2, getNow, ad, FormWriteParams(), "")
    updateResult should have size (1)

  }

  test("section change should generates new offer") {
    implicit val t = Traced.empty
    val categoryEnum = Category.CARS
    val formInfo = testFormGenerator.createForm(categoryEnum.name().toLowerCase, TestFormParams(isDealer = true))
    val formBuilder = formInfo.form.toBuilder.setSection(Section.NEW)
    formBuilder.getStateBuilder.clearMileage()

    val form = formBuilder.build()
    val userRef = formInfo.userRef
    val clientId = userRef.toPlain.split("_")(1).toInt
    val forms = Map("foo" -> form) // map by abstract id
    val task1 = Task
      .newBuilder()
      .setCategory(ApiOfferModel.Category.CARS)
      .setId(1)
      .setFeedId("foo")
      .setSection(Section.NEW)
      .setClientId(clientId)
      .build()
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val feed1Id = FeedprocessorHashUtils.getFeedId(task1)
    val sourceInfo1 = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.FEED)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef(userRef.toPlain)
      .setFeedprocessorFeedId(feed1Id)
      .setFeedprocessorTaskId(111)
      .setFeedprocessorTimestampUpdate(getNow)
      .build()
    val params1 = FormWriteParams(sourceInfo = Some(sourceInfo1))

    val createResult1 =
      batchWriter.batchCreate(userRef, categoryEnum, forms, getNow, ad, params1, "")
    val Create(createdOffer1, createNotices1) = createResult1.head._2.right.value

    formBuilder.getStateBuilder.setMileage(1)
    val form2 = formBuilder.setSection(Section.USED).build()
    val forms2 = Map(createdOffer1.getOfferID -> form2) // map by offerID
    val task2 = task1.toBuilder.setSection(Section.USED).build()
    val feed2Id = FeedprocessorHashUtils.getFeedId(task2)
    val sourceInfo2 = sourceInfo1.toBuilder
      .setFeedprocessorFeedId(feed2Id)
      .setFeedprocessorTaskId(112)
      .setFeedprocessorTimestampUpdate(getNow)
      .build()
    val params2 = FormWriteParams(sourceInfo = Some(sourceInfo2))

    val createResult2 = batchWriter.batchCreate(userRef, categoryEnum, forms2, getNow, ad, params2, "")
    val Create(createdOffer2, createNotices2) = createResult2.head._2.right.value

    val createdOffer1Upd =
      components.getOfferDao().findById(createdOffer1.getOfferID, includeRemoved = true)(Traced.empty).value

    val createdOffer2Upd =
      components.getOfferDao().findById(createdOffer2.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(createdOffer1Upd, createdOffer2Upd))(Traced.empty)

    val handledBy1Feed =
      components
        .getOfferDao()
        .findNonModifiedByFeedprocessor(UserRef.refAutoruClient(clientId), feed1Id, 113, None, Nil)
    val handledBy2Feed =
      components
        .getOfferDao()
        .findNonModifiedByFeedprocessor(UserRef.refAutoruClient(clientId), feed2Id, 113, None, Nil)

    assert(createdOffer1.getOfferID != createdOffer2.getOfferID)
    assert(handledBy1Feed.toSet == Set(createdOffer1.getOfferID))
    assert(handledBy2Feed.toSet == Set(createdOffer2.getOfferID))
  }

  //scalastyle:off method.length
  private def formsCreationTests(isDealer: Boolean, category: String)(implicit trace: Traced): Unit = {
    val now = getNow
    val categoryEnum = testFormGenerator.categoryByString(category)
    val (ownerId, _, userRef) = testFormGenerator.randomOwnerIds(isDealer)

    // создаем пять форм и потом запишем их все
    val formInfos = (1 to 5)
      .map(i => {
        val formInfo: FormInfo =
          testFormGenerator.createForm(category, TestFormParams(isDealer = isDealer, optOwnerId = Some(ownerId)))
        (i.toString, formInfo)
      })
      .toMap
    val forms = formInfos.view.mapValues(_.form).toMap
    val ad: AdditionalData = offersReader.loadAdditionalData(userRef, forms)(Traced.empty)
    val createResult: Map[String, Either[List[ValidationError], CreateOrUpdate]] =
      batchWriter.batchCreate(userRef, categoryEnum, forms, now, ad, FormWriteParams(), "")
    assert(createResult.size == 5)
    createResult.foreach {
      case (_, Right(_)) =>
      case (id, Left(errors)) =>
        val form = forms(id)
        fail(s"Validation errors $errors for form:\n$form")
    }
    assert(createResult.forall(_._2.isRight), createResult.values.filter(_.isLeft))

    val formIds2OfferIds = createResult.map {
      case (formId, validationResult) => (formId, validationResult.right.value.offer.getOfferID)
    }

    // теперь все обновим
    val updateFormInfos = formInfos.map {
      case (formId, formInfo) => (formId, testFormGenerator.updateForm(formInfo, TestFormParams(sameGeobaseId = true)))
    }
    val updateForms = updateFormInfos.map {
      case (formId, formInfo) => (formIds2OfferIds(formId), formInfo.form)
    }
    val ad2: AdditionalData = offersReader.loadAdditionalData(userRef, updateForms)(Traced.empty)
    val updateResult: Map[String, Either[List[ValidationError], CreateOrUpdate]] =
      batchWriter.batchUpdate(userRef, categoryEnum, updateForms, now, ad2, FormWriteParams(), "")
    assert(updateResult.size == 5)
    assert(updateResult.forall(_._2.isRight), createResult.values.filter(_.isLeft))

    // попробуем передать как будто новые создаем, должны тоже обновиться
    val result3: Map[String, Either[List[ValidationError], CreateOrUpdate]] =
      batchWriter.batchCreate(userRef, categoryEnum, updateForms, now, ad2, FormWriteParams(), "")
    assert(result3.size == 5)
    result3.foreach {
      case (formId, result) =>
        assert(result.isRight, result)
    }

    // пробуем незаполненные формы передать, должны вернуться ошибки валидации
    val emptyForms = (1 to 5)
      .map(i => {
        (i.toString, ApiOfferModel.Offer.getDefaultInstance)
      })
      .toMap
    val result4: Map[String, Either[List[ValidationError], CreateOrUpdate]] =
      batchWriter.batchCreate(userRef, categoryEnum, emptyForms, now, ad, FormWriteParams(), "")
    result4.foreach {
      case (formId, result) =>
        assert(result.isLeft, result)
    }
  }
}
