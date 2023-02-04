package ru.yandex.vos2.autoru.utils.formidx

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.scalatest.BetterEitherValues
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.testforms.{TestFormParams, TestFormsGenerator}

/**
  * Created by andrey on 7/3/17.
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FormHashUtilsTest extends AnyFunSuite with InitTestDbs with BetterEitherValues {
  implicit val t = Traced.empty
  initDbs()

  private val testFormsGenerator = new TestFormsGenerator(components)

  for {
    category <- Seq("cars", "trucks", "moto")
  } {
    test(s"hash tests (inVos = false, category = $category)") {
      hashTests(category)
    }
  }

  test("unified VINs should give equal hash") {
    val vin1 = "Xw0ZzZ4L1EG001019"
    val vin2 = "xW0zZz4l1eg001019"
    val category = "cars"
    val (ownerId, _, userRef) = testFormsGenerator.randomOwnerIds(isDealer = false)
    // создаем форму
    val formInfo = testFormsGenerator.createForm(category, TestFormParams(optOwnerId = Some(ownerId), isDealer = false))
    val categoryEnum = testFormsGenerator.categoryByString(category)
    val form: ApiOfferModel.Offer = formInfo.form
    // в форме есть vin и sts
    assume(form.getDocuments.getVin.nonEmpty)
    assume(form.getDocuments.getSts.nonEmpty)

    // сохраняем из нее объявление
    val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
    val now = ru.yandex.vos2.getNow
    val createResult = components.formWriter
      .createOffer(userRef, categoryEnum, form, now, ad, None, FormWriteParams(), "", None)
    val offer = createResult.right.value.offer

    val formBuider = form.toBuilder
    formBuider.getDocumentsBuilder.setVin(vin1)
    val form1 = formBuider.build()
    formBuider.getDocumentsBuilder.setVin(vin2)
    val form2 = formBuider.build()

    val offerBuilder = offer.toBuilder
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin(vin1)
    val offer1 = offerBuilder.build()
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin(vin2)
    val offer2 = offerBuilder.build()

    assert(
      FormHashUtils.getHash(offer1)
        == FormHashUtils.getHash(offer2)
    )
    assert(
      FormHashUtils.getHash(userRef, categoryEnum, form1)
        == FormHashUtils.getHash(userRef, categoryEnum, form2)
    )
  }

  test("offers with different section should have different hash") {
    val category = "cars"
    val (ownerId, _, userRef) = testFormsGenerator.randomOwnerIds(isDealer = false)
    val formInfo = testFormsGenerator.createForm(category, TestFormParams(optOwnerId = Some(ownerId), isDealer = false))
    val categoryEnum = testFormsGenerator.categoryByString(category)
    val form: ApiOfferModel.Offer = formInfo.form

    // сохраняем из нее объявление
    val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
    val now = ru.yandex.vos2.getNow
    val createResult = components.formWriter
      .createOffer(userRef, categoryEnum, form, now, ad, None, FormWriteParams(), "", None)
    val offer = createResult.right.value.offer

    val formBuider = form.toBuilder
    formBuider.setSection(Section.USED)
    val form1 = formBuider.build()
    formBuider.setSection(Section.NEW)
    val form2 = formBuider.build()

    val offerBuilder = offer.toBuilder
    offerBuilder.getOfferAutoruBuilder.setSection(Section.USED)
    val offer1 = offerBuilder.build()
    offerBuilder.getOfferAutoruBuilder.setSection(Section.NEW)
    val offer2 = offerBuilder.build()

    assert(FormHashUtils.getHash(offer1) != FormHashUtils.getHash(offer2))
    assert(FormHashUtils.getHash(userRef, categoryEnum, form1) != FormHashUtils.getHash(userRef, categoryEnum, form2))

    assert(FormHashUtils.getHash(offer1) == FormHashUtils.getHash(userRef, categoryEnum, form1))
    assert(FormHashUtils.getHash(offer2) == FormHashUtils.getHash(userRef, categoryEnum, form2))
  }

  private def hashTests(category: String): Unit = {
    val (ownerId, _, userRef) = testFormsGenerator.randomOwnerIds(isDealer = false)
    // создаем форму
    val formInfo = testFormsGenerator.createForm(category, TestFormParams(optOwnerId = Some(ownerId), isDealer = false))
    val categoryEnum = testFormsGenerator.categoryByString(category)
    val form: ApiOfferModel.Offer = formInfo.form
    // в форме есть vin и sts
    assume(form.getDocuments.getVin.nonEmpty)
    assume(form.getDocuments.getSts.nonEmpty)

    // сохраняем из нее объявление
    val ad = components.offersReader.loadAdditionalData(userRef, form)(Traced.empty)
    val now = ru.yandex.vos2.getNow
    val createResult =
      components.formWriter.createOffer(userRef, categoryEnum, form, now, ad, None, FormWriteParams(), "", None)
    val offer = createResult.right.value.offer

    val formHash = FormHashUtils.getHash(userRef, categoryEnum, form)
    val offerHash = FormHashUtils.getHash(offer)

    // хеш от формы равен хешу от объявления, созданного из формы
    assert(formHash == offerHash)

    // повторный вызов функции дает тот же результат
    assert(FormHashUtils.getHash(userRef, categoryEnum, form) == formHash)
    assert(FormHashUtils.getHash(offer) == formHash)

    val formBuilder: ApiOfferModel.Offer.Builder = form.toBuilder
    val offerBuilder = offer.toBuilder
    // сносим вин и стс
    formBuilder.getDocumentsBuilder.clearVin().clearSts()
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.clearVin().clearStsCode()
    val formNoVinSts = formBuilder.build()
    val offerNoVinSts = offerBuilder.build()
    // хеш изменился
    val formNoVinStsHash: String = FormHashUtils.getHash(userRef, categoryEnum, formNoVinSts)
    assert(formNoVinStsHash != formHash)
    // хеши от формы и объявления по прежнему равны между собой
    assert(formNoVinStsHash == FormHashUtils.getHash(offerNoVinSts))
    // при изменении описания хеш меняется
    formBuilder.setDescription("x")
    assert(FormHashUtils.getHash(userRef, categoryEnum, formBuilder.build()) != formNoVinStsHash)
    // при изменении года хеш меняется
    formBuilder.getDocumentsBuilder.setYear(1905)
    assert(FormHashUtils.getHash(userRef, categoryEnum, formBuilder.build()) != formNoVinStsHash)
    // при наличии remoteId в вычислениях используется только он
    formBuilder.getAdditionalInfoBuilder.setRemoteId("24000000011211133")
    assert(FormHashUtils.getHash(userRef, categoryEnum, formBuilder.build()) != formNoVinStsHash)
  }
}
