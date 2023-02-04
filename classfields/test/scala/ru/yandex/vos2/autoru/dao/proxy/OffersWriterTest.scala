package ru.yandex.vos2.autoru.dao.proxy

import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.RequestModel
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.scalatest.BetterEitherValues
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.AutoruModel.AutoruOffer.InteriorPanorama
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.OffersWriter.ReactivationComment
import ru.yandex.vos2.autoru.model.{AutoruOfferID, PhotoEdit, TestUtils}
import ru.yandex.vos2.autoru.utils.converters.formoffer.FormOfferConverter.reorderPhotos
import ru.yandex.vos2.autoru.utils.testforms.{TestFormParams, TestFormsGenerator}
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.model.ModelUtils.{RichClassifiedOrBuilder, RichOffer}
import ru.yandex.vos2.{getNow, OfferModel}

import scala.jdk.CollectionConverters._

class OffersWriterTest extends AnyFunSuite with InitTestDbs with Matchers with BetterEitherValues {
  initDbs()
  private val testFormGenerator = new TestFormsGenerator(components)
  components.featureRegistry.updateFeature(components.featuresManager.SendRecheckOfferMessage.name, true)

  private def save(offer: OfferModel.Offer): Unit = {
    components.getOfferDao().updateFunc(AutoruOfferID.parse(offer.getOfferID))(Traced.empty) { _ =>
      OfferUpdate.visitNow(offer)
    }
  }

  test("multiposting avito service deduplication") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    save(offer)

    components.offersWriter.addMultipostingServices(
      offer.getOfferID,
      Some(userRef),
      ClassifiedName.AVITO,
      Seq("x10_7", "service")
    )
    val offerFromDb = components.getOfferDao().findById(offer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val updated1 =
      components.offersReader.findOffer(Some(userRef), offer.getOfferID, Some(category), true, true).value

    updated1.findClassified(ClassifiedName.AVITO).value.activeServices.map(_.getService) shouldBe List(
      "X10_7",
      "SERVICE"
    )

    // ignore x10_2 - x10 is already enabled, x5_2 and x2_5 - because of x10
    components.offersWriter.addMultipostingServices(
      offer.getOfferID,
      Some(userRef),
      ClassifiedName.AVITO,
      Seq("x10_2", "x5_2", "x2_5", "another_service_7")
    )
    val offerFromDb2 = components.getOfferDao().findById(offer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb2))(Traced.empty)

    val updated2 =
      components.offersReader.findOffer(Some(userRef), offer.getOfferID, Some(category), true, true).value

    // exclude similar services
    updated2.findClassified(ClassifiedName.AVITO).value.activeServices.map(_.getService) shouldBe List(
      "X10_7",
      "SERVICE",
      "ANOTHER_SERVICE_7"
    )
  }

  test("multiposting: AUTORU/AVITO/DROM move INACTIVE status to ACTIVE") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AUTORU, enabled = true, status = CompositeStatus.CS_INACTIVE)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.DROM, enabled = true, status = CompositeStatus.CS_INACTIVE)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.AUTORU,
      enabled = true,
      status = CompositeStatus.CS_ACTIVE
    )

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.DROM,
      enabled = true,
      status = CompositeStatus.CS_ACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val foundOffer =
      components.offersReader
        .findOffer(
          Some(userRef),
          updatedOffer.getOfferID,
          Some(category),
          includeRemoved = true,
          operateOnMaster = true
        )
        .value

    foundOffer.findClassified(ClassifiedName.AUTORU).map(_.getStatus) shouldBe Some(CompositeStatus.CS_ACTIVE)
    foundOffer.findClassified(ClassifiedName.DROM).map(_.getStatus) shouldBe Some(CompositeStatus.CS_ACTIVE)
  }

  test("multiposting: AVITO/AUTORU/DROM move ACTIVE to INACTIVE") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AUTORU, enabled = true)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.DROM, enabled = true)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AVITO, enabled = true)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.AUTORU,
      enabled = true,
      status = CompositeStatus.CS_INACTIVE
    )

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.DROM,
      enabled = true,
      status = CompositeStatus.CS_INACTIVE
    )

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.AVITO,
      enabled = true,
      status = CompositeStatus.CS_INACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val foundOffer =
      components.offersReader
        .findOffer(
          Some(userRef),
          updatedOffer.getOfferID,
          Some(category),
          includeRemoved = true,
          operateOnMaster = true
        )
        .value

    foundOffer.findClassified(ClassifiedName.AUTORU).map(_.getStatus) shouldBe Some(CompositeStatus.CS_INACTIVE)
    foundOffer.findClassified(ClassifiedName.DROM).map(_.getStatus) shouldBe Some(CompositeStatus.CS_INACTIVE)
    foundOffer.findClassified(ClassifiedName.AVITO).map(_.getStatus) shouldBe Some(CompositeStatus.CS_INACTIVE)
  }

  test("multiposting: AVITO classified - ACTIVE status stayed in the same status") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AVITO, status = CompositeStatus.CS_ACTIVE)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.AVITO,
      enabled = true,
      status = CompositeStatus.CS_ACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val foundOffer =
      components.offersReader
        .findOffer(
          Some(userRef),
          updatedOffer.getOfferID,
          Some(category),
          includeRemoved = true,
          operateOnMaster = true
        )
        .value

    foundOffer.findClassified(ClassifiedName.AVITO).map(_.getStatus) shouldBe Some(CompositeStatus.CS_ACTIVE)
  }

  test("multiposting: AVITO classified - move INACTIVE to NEED_ACTIVATION") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AVITO, status = CompositeStatus.CS_INACTIVE)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.updateMultipostingClassifiedState(
      offerId = updatedOffer.getOfferID,
      optUserRef = Some(userRef),
      category = Some(category),
      classified = ClassifiedName.AVITO,
      enabled = true,
      status = CompositeStatus.CS_ACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val foundOffer =
      components.offersReader
        .findOffer(
          Some(userRef),
          updatedOffer.getOfferID,
          Some(category),
          includeRemoved = true,
          operateOnMaster = true
        )
        .value

    foundOffer.findClassified(ClassifiedName.AVITO).map(_.getStatus) shouldBe Some(CompositeStatus.CS_NEED_ACTIVATION)
  }
  test("Activate offer with inactive classifieds") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AUTORU, enabled = true, status = CompositeStatus.CS_INACTIVE)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.DROM, enabled = true, status = CompositeStatus.CS_INACTIVE)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AVITO, enabled = true, status = CompositeStatus.CS_INACTIVE)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.setMultipostingOfferStatus(
      updatedOffer.getOfferID,
      Some(userRef),
      Some(category),
      CompositeStatus.CS_ACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerWitActiveClassifieds =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    offerWitActiveClassifieds.findClassified(ClassifiedName.AUTORU).value.getStatus shouldBe CompositeStatus.CS_ACTIVE
    offerWitActiveClassifieds.findClassified(ClassifiedName.DROM).value.getStatus shouldBe CompositeStatus.CS_ACTIVE
    offerWitActiveClassifieds
      .findClassified(ClassifiedName.AVITO)
      .value
      .getStatus shouldBe CompositeStatus.CS_NEED_ACTIVATION
  }

  test("Activate offer with active classifieds") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AUTORU, enabled = true)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.DROM, enabled = true)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AVITO, enabled = true)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.setMultipostingOfferStatus(
      updatedOffer.getOfferID,
      Some(userRef),
      Some(category),
      CompositeStatus.CS_ACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerWitActiveClassifieds =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    offerWitActiveClassifieds.findClassified(ClassifiedName.AUTORU).value.getStatus shouldBe CompositeStatus.CS_ACTIVE
    offerWitActiveClassifieds.findClassified(ClassifiedName.DROM).value.getStatus shouldBe CompositeStatus.CS_ACTIVE
    offerWitActiveClassifieds
      .findClassified(ClassifiedName.AVITO)
      .value
      .getStatus shouldBe CompositeStatus.CS_ACTIVE
  }

  test("Activate offer with expired timestamp") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val updatedOffer = offer.toBuilder.setTimestampWillExpire(DateTime.now().minusDays(3).getMillis).build()
    save(updatedOffer)

    components.offersWriter.setArchive(offer.getOfferID, None, None, true, "", None)
    components.offersWriter.activate(offer.getOfferID, None, None, true, "", None, None)
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerFromBase =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    assert(offerFromBase.getTimestampWillExpire > DateTime.now().getMillis)
  }

  test("Activate private offer after 8 days of inactivity") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo =
      testFormGenerator.createForm(category.name().toLowerCase, TestFormParams(now = DateTime.now.minusDays(8)))
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val updatedOffer = offer.toBuilder.setTimestampWillExpire(DateTime.now().plusDays(3).getMillis).build()
    save(updatedOffer)

    components.offersWriter.setArchive(offer.getOfferID, None, None, true, "", None)

    DateTimeUtils.setCurrentMillisFixed(DateTime.now().plusDays(8).getMillis)

    components.offersWriter.activate(offer.getOfferID, None, None, true, "", None, None)
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerFromBase =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    assert(
      offerFromBase.getOfferAutoru.getNotificationsList.asScala
        .exists(_.getType == NotificationType.RECHECK_REACTIVATED_OFFER)
    )
  }

  test("Activate dealer offer after 8 days of inactivity") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo =
      testFormGenerator.createForm(
        category.name().toLowerCase,
        TestFormParams(isDealer = true, now = DateTime.now.minusDays(8))
      )
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val updatedOffer = offer.toBuilder.setTimestampWillExpire(DateTime.now().plusDays(3).getMillis).build()
    save(updatedOffer)

    components.offersWriter.setArchive(offer.getOfferID, None, None, true, "", None)

    DateTimeUtils.setCurrentMillisFixed(DateTime.now().plusDays(8).getMillis)

    components.offersWriter.activate(offer.getOfferID, None, None, true, "", None, None)
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerFromBase =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    assert(
      !offerFromBase.getOfferAutoru.getNotificationsList.asScala
        .exists(_.getType == NotificationType.RECHECK_REACTIVATED_OFFER)
    )
  }

  test("Activate offer after 6 days of inactivity") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo =
      testFormGenerator.createForm(category.name().toLowerCase, TestFormParams(now = DateTime.now.minusDays(8)))
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer
    val updatedOffer = offer.toBuilder.setTimestampWillExpire(DateTime.now().plusDays(3).getMillis).build()
    save(updatedOffer)

    components.offersWriter.setArchive(offer.getOfferID, None, None, true, "", None)

    DateTimeUtils.setCurrentMillisFixed(DateTime.now().plusDays(6).getMillis)

    components.offersWriter.activate(offer.getOfferID, None, None, true, "", None, None)
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerFromBase =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    assert(
      !offerFromBase.getOfferAutoru.getNotificationsList.asScala
        .exists(_.getType == NotificationType.RECHECK_REACTIVATED_OFFER)
    )
  }

  test("Activate offer by delayed reactivation worker and add recheck notification") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo =
      testFormGenerator.createForm(category.name().toLowerCase, TestFormParams(now = DateTime.now.minusDays(8)))
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val updatedOffer = offer.toBuilder.setTimestampWillExpire(DateTime.now().plusDays(3).getMillis).build()
    save(updatedOffer)

    components.offersWriter.setArchive(offer.getOfferID, None, None, true, "", None)
    components.offersWriter.activate(offer.getOfferID, None, None, true, ReactivationComment, None, None)
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerFromBase =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    assert(
      offerFromBase.getOfferAutoru.getNotificationsList.asScala
        .exists(_.getType == NotificationType.RECHECK_REACTIVATED_OFFER)
    )
  }

  test("Deactivate offer with active classifieds") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val multiposting = TestUtils
      .createMultiposting(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AUTORU, enabled = true)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.DROM, enabled = true)
      )
      .addClassifieds(
        TestUtils.createClassified(ClassifiedName.AVITO, enabled = true)
      )

    val updatedOffer = offer.toBuilder
      .setMultiposting(multiposting)
      .build()

    save(updatedOffer)

    components.offersWriter.setMultipostingOfferStatus(
      updatedOffer.getOfferID,
      Some(userRef),
      Some(category),
      CompositeStatus.CS_INACTIVE
    )
    val offerFromDb =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val offerWitActiveClassifieds =
      components.offersReader.findOffer(Some(userRef), updatedOffer.getOfferID, Some(category), true, true).value

    offerWitActiveClassifieds.findClassified(ClassifiedName.AUTORU).value.getStatus shouldBe CompositeStatus.CS_INACTIVE
    offerWitActiveClassifieds.findClassified(ClassifiedName.DROM).value.getStatus shouldBe CompositeStatus.CS_INACTIVE
    offerWitActiveClassifieds
      .findClassified(ClassifiedName.AVITO)
      .value
      .getStatus shouldBe CompositeStatus.CS_INACTIVE
  }

  test("photo quality test without changes") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val photos = for (i <- 1 until 100) yield {
      TestUtils.createPhoto("name" + i * 100)
    }

    val originalOrderedPhotoList = reorderPhotos(photos).map(_.build())
    components.getOfferDao().useOfferID(AutoruOfferID.parse(offer.getOfferID)) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.addAllPhoto(originalOrderedPhotoList.asJava)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val newMainPhoto = originalOrderedPhotoList.last

    val changeMap = Seq(newMainPhoto.getName -> PhotoEdit(None, None, None, None, None, None)).toMap

    components.offersWriter.addPhotoQuality(offer.getOfferID, changeMap)
    val offerFromDb = components.getOfferDao().findById(offer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val updatedOffer = components.getOfferDao().findById(offer.getOfferID).value

    assert(updatedOffer.getOfferAutoru.getPhotoCount == originalOrderedPhotoList.size)
  }

  test("photo quality test with new main photo") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val photos = for (i <- 1 until 100) yield {
      TestUtils.createPhoto("name" + i * 100)
    }

    val originalOrderedPhotoList = reorderPhotos(photos).map(_.build())
    components.getOfferDao().useOfferID(AutoruOfferID.parse(offer.getOfferID)) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.addAllPhoto(originalOrderedPhotoList.asJava)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val newMainPhoto = originalOrderedPhotoList.last

    val changeMap = Seq(newMainPhoto.getName -> PhotoEdit(None, None, None, Some(true), None, None)).toMap

    components.offersWriter.addPhotoQuality(offer.getOfferID, changeMap)
    val offerFromDb = components.getOfferDao().findById(offer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val updatedOffer = components.getOfferDao().findById(offer.getOfferID).value

    val newMainPhotoUpdated = updatedOffer.getOfferAutoru.getPhotoList.asScala.head
    assert(newMainPhotoUpdated.getName == newMainPhoto.getName)
  }

  test("photo quality test with hide and unhide") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val photos = for (i <- 1 until 100) yield {
      TestUtils.createPhoto("name" + i * 100)
    }

    val originalOrderedPhotoList = reorderPhotos(photos).map(_.build())
    components.getOfferDao().useOfferID(AutoruOfferID.parse(offer.getOfferID)) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.addAllPhoto(originalOrderedPhotoList.asJava)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val newMainPhoto = originalOrderedPhotoList.last

    val changeMap = Seq(newMainPhoto.getName -> PhotoEdit(None, None, None, None, None, Some(true))).toMap

    components.offersWriter.addPhotoQuality(offer.getOfferID, changeMap)
    val offerFromDb = components.getOfferDao().findById(offer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)

    val updatedOffer = components.getOfferDao().findById(offer.getOfferID).value

    val newPhotoUpdated = updatedOffer.getOfferAutoru.getPhotoList.asScala
    val newModerationPhotoUpdated = updatedOffer.getOfferAutoru.getModerationPhotoList.asScala
    assert(newModerationPhotoUpdated.size == 1)
    assert(newModerationPhotoUpdated.head == newMainPhoto)
    assert(!newPhotoUpdated.contains(newMainPhoto))

    val changeMap2 = Seq(newMainPhoto.getName -> PhotoEdit(None, None, None, None, None, None)).toMap
    components.offersWriter.addPhotoQuality(offer.getOfferID, changeMap2)
    val offerFromDb2 = components.getOfferDao().findById(offer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb2))(Traced.empty)

    val updatedOffer2 = components.getOfferDao().findById(offer.getOfferID).value
    val newPhotoUpdated2 = updatedOffer2.getOfferAutoru.getPhotoList.asScala
    val newModerationPhotoUpdated2 = updatedOffer2.getOfferAutoru.getModerationPhotoList.asScala
    assert(newModerationPhotoUpdated2.size == 1)
    assert(newModerationPhotoUpdated2.head == newMainPhoto)
    assert(!newPhotoUpdated2.contains(newMainPhoto))

    val changeMap3 = Seq(newMainPhoto.getName -> PhotoEdit(None, None, None, None, None, Some(false))).toMap
    components.offersWriter.addPhotoQuality(offer.getOfferID, changeMap3)
    val offerFromDb3 =
      components.getOfferDao().findById(updatedOffer.getOfferID, includeRemoved = true)(Traced.empty).value

    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb3))(Traced.empty)

    val updatedOffer3 = components.getOfferDao().findById(offer.getOfferID).value
    val newPhotoUpdated3 = updatedOffer3.getOfferAutoru.getPhotoList.asScala
    val newModerationPhotoUpdated3 = updatedOffer3.getOfferAutoru.getModerationPhotoList.asScala

    assert(newModerationPhotoUpdated3.isEmpty)
    assert(newPhotoUpdated3.contains(newMainPhoto))
    assert(newPhotoUpdated3.size == originalOrderedPhotoList.size)
  }

  test("photo quality test ordering") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    val photos = for (i <- 1 until 100) yield {
      TestUtils.createPhoto("name" + i * 100)
    }

    val originalOrderedPhotoList = reorderPhotos(photos).map(_.build())
    components.getOfferDao().useOfferID(AutoruOfferID.parse(offer.getOfferID)) { offer =>
      val offerBuilder = offer.toBuilder
      offerBuilder.getOfferAutoruBuilder.addAllPhoto(originalOrderedPhotoList.asJava)
      OfferUpdate.visitNow(offerBuilder.build())
    }

    val changeMap = originalOrderedPhotoList.reverse.zipWithIndex.map {
      case (photo, idx) =>
        photo.getName -> PhotoEdit(None, None, None, None, Some(idx), None)
    }.toMap

    components.offersWriter.addPhotoQuality(offer.getOfferID, changeMap)

    val updatedOffer = components.getOfferDao().findById(offer.getOfferID).value
    val newPhotoUpdated = updatedOffer.getOfferAutoru.getPhotoList.asScala

    assert(updatedOffer.getOfferAutoru.getPhotoCount == originalOrderedPhotoList.size)
    assert(
      newPhotoUpdated.find(_.getOrder == 1).value.getName == originalOrderedPhotoList
        .find(_.getOrder == 98)
        .value
        .getName
    )
  }

  test("update price: keep withNds check") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams(isDealer = true))
    val price1 = 1000000
    val price2 = 2000000
    val form = {
      val formBuilder = formInfo.form.toBuilder
      formBuilder.getPriceInfoBuilder.setDprice(price1).setPrice(price1)
      formBuilder.getPriceInfoBuilder.getWithNdsBuilder.setValue(true)
      formBuilder.build()
    }
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    assert(offer.getOfferAutoru.getPrice.getPrice == price1)
    assert(offer.getOfferAutoru.getPrice.getWithNds)

    val request = {
      val requestBuilder = RequestModel.AttributeUpdateRequest.newBuilder()
      requestBuilder.getPriceBuilder.setPrice(price2).setCurrency("RUR")
      requestBuilder.build()
    }
    components.offersWriter.updateAttribute(offer.getOfferID, Some(userRef), Some(category), DateTime.now(), request)
    val updatedOffer = components.getOfferDao().findById(offer.getOfferID).value
    assert(updatedOffer.getOfferAutoru.getPrice.getPrice == price2)
    assert(updatedOffer.getOfferAutoru.getPrice.getWithNds)
  }

  test("addInteriorPanorama: add panoramas") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfoBase = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val formBuilder = formInfoBase.form.toBuilder()
    formBuilder.getStateBuilder.clearInteriorPanorama()
    val formInfo = formInfoBase.withForm(formBuilder.build())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    save(offer)

    val panoramaId1 = "foo"
    val panorama1 = InteriorPanorama.newBuilder
      .setPublished(false)
    panorama1.getPanoramaBuilder.setId(panoramaId1)
    components.offersWriter.addInteriorPanorama(
      offer.getOfferID,
      panorama1.build(),
      Some(userRef)
    )

    val updated1 = components.offersReader.findOffer(Some(userRef), offer.getOfferID, Some(category), true, true).value
    val panoramaIds1 = updated1.getOfferAutoru.getInteriorPanoramaList.asScala.map(_.getPanorama.getId)
    panoramaIds1 shouldBe Seq(panoramaId1)

    val panoramaId2 = "bar"
    val panorama2 = InteriorPanorama.newBuilder
      .setPublished(false)
    panorama2.getPanoramaBuilder.setId(panoramaId2)
    components.offersWriter.addInteriorPanorama(
      offer.getOfferID,
      panorama2.build(),
      Some(userRef)
    )

    val updated2 = components.offersReader.findOffer(Some(userRef), offer.getOfferID, Some(category), true, true).value
    val panoramaIds2 = updated2.getOfferAutoru.getInteriorPanoramaList.asScala.map(_.getPanorama.getId)
    panoramaIds2 shouldBe Seq(panoramaId1, panoramaId2)
  }

  test("addInteriorPanorama: validate panorama IDs") {
    implicit val t = Traced.empty
    val category = Category.CARS
    val formInfo = testFormGenerator.createForm(category.name().toLowerCase, TestFormParams())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, category, formInfo.form, getNow, ad, None, FormWriteParams(), "test", None)
      .right
      .value
      .offer

    save(offer)

    val invalidId = "this_id_contains_exactly_33_chars"
    val panorama = InteriorPanorama.newBuilder
      .setPublished(false)
    panorama.getPanoramaBuilder.setId(invalidId)

    intercept[IllegalArgumentException](
      components.offersWriter.addInteriorPanorama(
        offer.getOfferID,
        panorama.build(),
        Some(userRef)
      )
    ).getMessage should include("too long")
  }
}
