package ru.yandex.vos2.autoru.utils.converters

import com.google.protobuf.util.Timestamps
import com.google.protobuf.util.Timestamps.fromMillis
import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.scalactic.source
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel.AdditionalInfo.ProvenOwnerStatus
import ru.auto.api.ApiOfferModel.{Category, OfferStatus, VinResolutionHidingReason}
import ru.auto.api.CommonModel.DiscountPrice.DiscountPriceStatus
import ru.auto.api.CommonModel.RecallReason
import ru.auto.api.TrucksModel.Trailer
import ru.auto.api.cert.CertModel
import ru.auto.api.cert.CertModel.BrandCertStatus
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.ResolutionEntry
import ru.auto.api.{ApiOfferModel, CarsModel, CommonModel}
import ru.yandex.vertis.moderation.proto.Model.Metadata.AutoruPhotoLicensePlate.Value._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.{Platform, Source}
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.TrailerType
import ru.yandex.vos2.AutoruModel.AutoruOffer._
import ru.yandex.vos2.BasicsModel.{CompositeStatus, Currency}
import ru.yandex.vos2.OfferModel.OfferFlag.{OF_BANNED, OF_INACTIVE, OF_NEED_ACTIVATION, OF_USER_BANNED}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.TelephonyModel.PhoneCallsCounter
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.catalog.cars.Dictionaries
import ru.yandex.vos2.autoru.config.TestAutoruApiComponents
import ru.yandex.vos2.autoru.dao.proxy.{AdditionalDataForReading, FormWriteParams}
import ru.yandex.vos2.autoru.model.AutoruModelUtils.{AutoruModelRichOffer, RichProvenOwnerModerationStateOrBuilder}
import ru.yandex.vos2.autoru.model._
import ru.yandex.vos2.autoru.services.OfferConverterCommon.{ConvertFailure, Converted}
import ru.yandex.vos2.autoru.services.SettingAliases
import ru.yandex.vos2.autoru.services.SettingAliases.ENGINE_POWER
import ru.yandex.vos2.autoru.utils.ApiFormUtils.{RichApiOffer, RichPriceInfoOrBuilder}
import ru.yandex.vos2.autoru.utils.FormTestUtils.RichFormBuilder
import ru.yandex.vos2.autoru.utils.converters.dailycounters.DailyCountersFromTo
import ru.yandex.vos2.autoru.utils.testforms._
import ru.yandex.vos2.autoru.utils.{FormTestUtils, PaidServiceUtils}
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.yandex.vos2.model.{UserRef, UserRefAutoruClient}
import ru.yandex.vos2.proto.ProtoMacro

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 11/11/16.
  */
@RunWith(classOf[JUnitRunner])
class OfferFormConverterTest extends AnyFunSuite with InitTestDbs with Matchers {
  initDbs()

  val apiComponents = new TestAutoruApiComponents {
    override lazy val coreComponents = components
  }

  val now = new DateTime(ru.yandex.vos2.getNow)
  private val formTestUtils = new FormTestUtils(components)
  private val carTestForms = new CarTestForms(components)
  private val truckTestForms = new TruckTestForms(components)

  import formTestUtils._

  private val curPrivateSaleUpd = curPrivateSale.copy(
    createDate = now.minusMonths(2),
    expireDate = AutoruCommonLogic.expireDate(now),
    // сервисы у нас с пустой expireDate или с датой плюс три дня к текущей - все должны в маппинге прийти
    // кроме одного - с датой минус три дня от текущей, его быть не должно.
    // и еще одного неактивного быть не должно
    services = curPrivateSale.services.map(services =>
      services.map(s =>
        s.copy(expireDate = {
          if (s.serviceType == "all_sale_color") None
          else if (s.serviceType == "all_sale_special") Some(now.minusDays(3))
          else Some(now.plusDays(3))
        })
      )
    ),
    recallReason = curPrivateSale.recallReason.map(rr => rr.copy(updated = now.minusDays(7)))
  )

  private val badges: List[String] =
    List("Два комплекта резины", "Камера заднего вида", "Парктроник", "Коврики в подарок", "Кожаный салон")

  private val privateOfferForm2 = privateOfferForm.toBuilder
    .withHidden(true)
    .withBadges(
      badges
    )
    .build()

  private val salonOfferForm2 = salonOfferForm.toBuilder
    .withHidden(true)
    .withBadges(
      badges
    )
    .build()

  private val lpHidingStates = Set(WAITING_FOR_PHOTO, CHECK_FAILED)

  private val formOfferConverter = components.formOfferConverter
  private val formSaleConverter = components.formAutoruSaleConverter
  private val formAutoruTruckConverter = components.formAutoruTruckConverter
  private val carOfferConverter = components.carOfferConverter
  private val truckOfferConverter = components.truckOfferConverter

  private val offerFormConverter = apiComponents.offerFormConverter

  test("toFormData") {
    // скрытое объявление
    comparePrivateOfferFormData(privateOfferForm2, OfferStatus.INACTIVE)
    // не скрытое объявление
    comparePrivateOfferFormData(privateOfferForm2.toBuilder.withHidden(false).build(), OfferStatus.NEED_ACTIVATION)
    comparePrivateOfferFormData(
      privateOfferForm2.toBuilder.withYoutubeVideo("UWYjvpfLuVs", "https://youtube.com/watch?v=UWYjvpfLuVs").build()
    )

    compareSalonOfferFormData(salonOfferForm2)
  }

  test("existingToFormData") {
    compareExistingPrivateOfferFormData(privateOfferForm2)
    // при редактировании даже если не поставили галку "скрытое", все равно статус не поменяется (останется скрытым)
    compareExistingPrivateOfferFormData(
      privateOfferForm2.toBuilder.withHidden(false).build(),
      needStatus = OfferStatus.INACTIVE
    )
    // пустые координаты
    compareExistingPrivateOfferFormData(privateOfferForm2.toBuilder.withoutCoord.build())
  }

  test("PaidServiceUtils") {
    assert(PaidServiceUtils.stringByService(ServiceType.ADD).contains("all_sale_activate"))
    assert(PaidServiceUtils.stringByService(ServiceType.TOP).contains("all_sale_toplist"))
  }

  test("forbid edition of banned") {
    // забаненное объявление нельзя редактировать
    val offer = TestUtils.createOffer().addFlag(OF_BANNED).addReasonsBan("do_not_exist").build()
    val convertedFormData = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(!convertedFormData.getActions.getEdit)
  }

  test("allow edition of banned offer for some reasons") {
    val offer = TestUtils.createOffer().addFlag(OF_BANNED).addReasonsBan("low_price").build()
    val convertedFormData = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(convertedFormData.getActions.getEdit)
  }

  test("toApiOffer") {
    val b = TestUtils.createOffer(now = now.getMillis).addFlag(OF_INACTIVE)
    val autoruBuilder: AutoruOffer.Builder = b.getOfferAutoruBuilder
    autoruBuilder.getPriceBuilder.setPrice(5571300).setCurrency(Currency.RUB).setCreated(now.getMillis)

    autoruBuilder.setColorHex("9966CC")

    autoruBuilder.getRecallInfoBuilder
      .setRecallTimestamp(now.minusDays(1).getMillis)
      .setReason(RecallReason.LITTLE_CALLS)
    autoruBuilder.getDiscountPriceBuilder.setPrice(5382900).setStatus(DiscountPriceStatus.ACTIVE)
    autoruBuilder.getDiscountOptionsBuilder
      .setCredit(1337)
      .setInsurance(13371)
      .setTradein(13372)
      .setMaxDiscount(13373)
    autoruBuilder.addPriceHistoryBuilder().setPrice(5571300).setCurrency(Currency.RUB).setCreated(now.getMillis)
    autoruBuilder.getBrandCertInfoBuilder
      .setVin("Z8NHSNDJA51521116")
      .setProgramAlias("NissanWarranty")
      .setCertStatus(BrandCertStatus.BRAND_CERT_INACTIVE)
      .setCreated(now.minusDays(4).getMillis)
      .setUpdated(now.minusDays(4).getMillis)
    autoruBuilder.getVinResolutionBuilder
      .setVersion(0)
      .getResolutionBuilder
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.UNKNOWN)
      )
    autoruBuilder.getDocumentsBuilder.setNotRegisteredInRussia(true)

    autoruBuilder.getPredictPriceBuilder.setVersion(0)
    autoruBuilder.getPredictPriceBuilder.getMarketBuilder.setCurrency(Currency.RUB).setPrice(5000000)

    val statusHistory = Seq(
      CompositeStatus.CS_NEED_ACTIVATION,
      CompositeStatus.CS_ACTIVE,
      CompositeStatus.CS_MODERATION,
      CompositeStatus.CS_BANNED
    ).map(b.addStatusHistoryBuilder().setOfferStatus(_).build())
    b.addAllStatusHistory(statusHistory.asJava)
    val offer = b.build()

    val form = offerFormConverter.convert(AdditionalDataForReading(), offer)

    assert(form.getAdditionalInfo.getLastStatus == OfferStatus.BANNED)
    assert(form.getAdditionalInfo.getCreationDate == now.getMillis)

    // TODO: sellerType test

    assert(form.getColorHex === "4A2197")

    assert(form.getRecallInfo.getRecallTimestamp == now.minusDays(1).getMillis)
    assert(form.getRecallInfo.getReason == CommonModel.RecallReason.LITTLE_CALLS)

    assert(form.getDiscountPrice.getPrice == 5382900)
    assert(form.getDiscountPrice.getStatus == CommonModel.DiscountPrice.DiscountPriceStatus.ACTIVE)

    assert(form.getDiscountOptions.getCredit == 1337)
    assert(form.getDiscountOptions.getInsurance == 13371)
    assert(form.getDiscountOptions.getTradein == 13372)
    assert(form.getDiscountOptions.getMaxDiscount == 13373)

    assert(form.getPriceHistoryCount == 1)
    assert(form.getPriceHistory(0).selectPrice == 5571300)
    assert(form.getPriceHistory(0).getCurrency == "RUR")
    assert(form.getPriceHistory(0).getCreateTimestamp == now.getMillis)

    assert(form.getBrandCertInfo.getVin == "Z8NHSNDJA51521116")
    assert(form.getBrandCertInfo.getProgramAlias == "NissanWarranty")
    assert(form.getBrandCertInfo.getCertStatus == CertModel.BrandCertStatus.BRAND_CERT_INACTIVE)
    assert(form.getBrandCertInfo.getCreated == now.minusDays(4).getMillis)
    assert(form.getBrandCertInfo.getUpdated == now.minusDays(4).getMillis)

    assert(form.getDocuments.getVinResolution == VinResolutionEnums.Status.UNKNOWN)
    assert(form.getDocuments.getNotRegisteredInRussia)

    assert(form.getCarInfo.getMarkInfo.hasName)
    assert(form.getCarInfo.getModelInfo.hasName)

    assert(form.getMarketPrice.getCurrency == "RUR")
    assert(form.getMarketPrice.getPrice == 5000000)
  }

  test("daily counters") {
    val b = TestUtils.createOffer(now = now.getMillis)
    val autoruBuilder: AutoruOffer.Builder = b.getOfferAutoruBuilder

    autoruBuilder.setPhoneCallsCounter(
      PhoneCallsCounter
        .newBuilder()
        .putAllDaily(
          Map[java.lang.Long, java.lang.Integer](
            (DateTime.parse("2020-04-10T12:10").getMillis, 1),
            (DateTime.parse("2020-04-11T12:20").getMillis, 2),
            (DateTime.parse("2020-04-17T12:30").getMillis, 4),
            (DateTime.parse("2020-04-18T12:40").getMillis, 8)
          ).asJava
        )
        .build()
    )

    val offer = b.build()
    val period = DailyCountersFromTo(Some(LocalDate.parse("2020-04-11")), Some(LocalDate.parse("2020-04-18")))

    val converted = offerFormConverter.convert(AdditionalDataForReading(), offer, dailyCountersFromTo = Some(period))
    val dailyCounters = converted.getDailyCountersList.asScala

    dailyCounters.size shouldBe 2
    dailyCounters.find(_.getDate === "2020-04-11").get.getPhoneCalls shouldBe 2
    dailyCounters.find(_.getDate === "2020-04-17").get.getPhoneCalls shouldBe 4
  }

  test("allow to archive active dealer's offer") {
    val offer = TestUtils.createOffer(dealer = true).build()
    val converted = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(converted.getActions.getArchive)
  }

  test("don't allow to archive active user's offer") {
    val offer = TestUtils.createOffer().build()
    val converted = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(!converted.getActions.getArchive)
  }

  for {
    flag <- Seq(OF_BANNED, OF_USER_BANNED)
  } test(s"allow to archive blocked dealer's offer for flag $flag") {
    val offer = TestUtils.createOffer(dealer = true).putFlag(flag).build()
    val converted = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(converted.getActions.getArchive)
  }

  test("allow to archive waiting for activation dealer's offer") {
    val offer = TestUtils.createOffer(dealer = true).putFlag(OF_NEED_ACTIVATION).build()
    val converted = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(converted.getActions.getArchive)
  }

  for {
    flag <- Seq(OF_BANNED, OF_USER_BANNED)
  } test(s"don't allow to hide blocked dealer's offer for flag $flag") {
    val offer = TestUtils.createOffer(dealer = true).putFlag(flag).build()
    val converted = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(!converted.getActions.getHide)
  }

  test("price history") {
    val now = new DateTime()
    val price1: Double = 100000
    val price2: Double = 200000
    val priceBuilder = AutoruOffer.Price
      .newBuilder()
      .setCreated(now.getMillis)
      .setCurrency(Currency.RUB)
      .setPrice(price1)
      .setPriceRub(price1)
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.setPrice(priceBuilder).addPriceHistory(priceBuilder)
    val offer0 = offerBuilder.build()

    val userRef = UserRef.from(offer0.getUserRef)
    val ad = components.offersReader.loadAdditionalData(userRef, offer0)(Traced.empty)
    val form1 = offerFormConverter.convert(ad.forReading, offer0)
    // если цена не меняется, то price history тоже
    val offer1 = formOfferConverter.convertExistingOffer(form1, offer0, None, ad, now.getMillis, FormWriteParams.empty)
    assert(offer1.getOfferAutoru.getPrice.getPrice == price1)
    assert(offer1.getOfferAutoru.getPrice.getCurrency == Currency.RUB)
    assert(offer1.getOfferAutoru.getPrice.getCreated == now.getMillis)
    assert(offer1.getOfferAutoru.getPrice.getPriceRub == price1)
    assert(offer1.getOfferAutoru.getPriceHistoryCount == 1)
    // если цена меняется, то price history увеличивается
    val form2 = form1.toBuilder.withPrice(price2).build()
    val offer2 = formOfferConverter.convertExistingOffer(
      form2,
      offer0,
      None,
      ad,
      now.plusMinutes(1).getMillis,
      FormWriteParams.empty
    )
    assert(offer2.getOfferAutoru.getPrice.getPrice == price2)
    assert(offer2.getOfferAutoru.getPrice.getCurrency == Currency.RUB)
    assert(offer2.getOfferAutoru.getPrice.getCreated == now.plusMinutes(1).getMillis)
    assert(offer2.getOfferAutoru.getPrice.getPriceRub == price2)
    assert(offer2.getOfferAutoru.getPriceHistoryCount == 2)
    // если цена возвращается к предыдущему значению, то price history увеличивается
    val form3 = form1.toBuilder.withPrice(price1).build()
    val offer3 = formOfferConverter.convertExistingOffer(
      form3,
      offer2,
      None,
      ad,
      now.plusMinutes(1).getMillis,
      FormWriteParams.empty
    )
    assert(offer3.getOfferAutoru.getPriceHistoryCount == 3)
  }

  test("mileage history") {

    val now = new DateTime()
    val mileageCurr = 100500
    val mileageLarger = 100600
    val mileageLess = 100400
    val mileageLittle = 100499

    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getStateBuilder.setMileage(mileageCurr)
    offerBuilder.getOfferAutoruBuilder.clearMileageHistory()

    val offer0 = offerBuilder.build()

    val userRef = UserRef.from(offer0.getUserRef)
    val ad = components.offersReader.loadAdditionalData(userRef, offer0)(Traced.empty)
    val form = offerFormConverter.convert(ad.forReading, offer0)

    //Добавляем новое объявление
    val now1 = now.getMillis
    val form1 = form.toBuilder.withMileage(mileageCurr).build()
    val offer1 = formOfferConverter.convertNewOffer(
      userRef,
      offer0.getOfferAutoru.getCategory,
      form1,
      ad,
      now1
    )

    //В истории должна быть одна запись
    assert(offer1.getOfferAutoru.getMileageHistoryCount == 1)
    assert(offer1.getOfferAutoru.getMileageHistory(0).getMileage == mileageCurr)
    assert(offer1.getOfferAutoru.getMileageHistory(0).getUpdateTimestamp == fromMillis(now1))

    //Поменяли пробег не менее чем на 100 в большую сторону
    val now2 = now.getMillis
    val form2 = form1.toBuilder.withMileage(mileageLarger).build()
    val offer2 = formOfferConverter.convertExistingOffer(
      form2,
      offer1,
      None,
      ad,
      now2,
      FormWriteParams.empty
    )

    //В истории должно быть уже две записи
    assert(offer2.getOfferAutoru.getMileageHistoryCount == 2)
    assert(offer2.getOfferAutoru.getMileageHistory(0).getMileage == mileageCurr)
    assert(offer2.getOfferAutoru.getMileageHistory(0).getUpdateTimestamp == fromMillis(now1))
    assert(offer2.getOfferAutoru.getMileageHistory(1).getMileage == mileageLarger)
    assert(offer2.getOfferAutoru.getMileageHistory(1).getUpdateTimestamp == fromMillis(now2))

    //Пробег не меняем
    val now3 = now.plus(1000).getMillis
    val form3 = form2
    val offer3 = formOfferConverter.convertExistingOffer(
      form3,
      offer2,
      None,
      ad,
      now3,
      FormWriteParams.empty
    )

    //Все остается по прежнему
    assert(offer3.getOfferAutoru.getMileageHistoryCount == 2)
    assert(offer3.getOfferAutoru.getMileageHistory(0).getMileage == mileageCurr)
    assert(offer3.getOfferAutoru.getMileageHistory(0).getUpdateTimestamp == fromMillis(now1))
    assert(offer3.getOfferAutoru.getMileageHistory(1).getMileage == mileageLarger)
    assert(offer3.getOfferAutoru.getMileageHistory(1).getUpdateTimestamp == fromMillis(now2))

    //Поменяли пробег не менее чем на 100 в меньшую сторону
    val now4 = now.plus(1000).getMillis
    val form4 = form3.toBuilder.withMileage(mileageLess).build()
    val offer4 = formOfferConverter.convertExistingOffer(
      form4,
      offer3,
      None,
      ad,
      now4,
      FormWriteParams.empty
    )

    //В истории должно быть три записи
    assert(offer4.getOfferAutoru.getMileageHistoryCount == 3)
    assert(offer4.getOfferAutoru.getMileageHistory(0).getMileage == mileageCurr)
    assert(offer4.getOfferAutoru.getMileageHistory(0).getUpdateTimestamp == fromMillis(now1))
    assert(offer4.getOfferAutoru.getMileageHistory(1).getMileage == mileageLarger)
    assert(offer4.getOfferAutoru.getMileageHistory(1).getUpdateTimestamp == fromMillis(now2))
    assert(offer4.getOfferAutoru.getMileageHistory(2).getMileage == mileageLess)
    assert(offer4.getOfferAutoru.getMileageHistory(2).getUpdateTimestamp == fromMillis(now4))

    //Поменяли пробег не более чем на 100
    val now5 = now.plus(1000).getMillis
    val form5 = form4.toBuilder.withMileage(mileageLittle).build()
    val offer5 = formOfferConverter.convertExistingOffer(
      form5,
      offer4,
      None,
      ad,
      now5,
      FormWriteParams.empty
    )

    //ни чего не должно меняться
    assert(offer5.getOfferAutoru.getMileageHistoryCount == 3)
    assert(offer5.getOfferAutoru.getMileageHistory(0).getMileage == mileageCurr)
    assert(offer5.getOfferAutoru.getMileageHistory(0).getUpdateTimestamp == fromMillis(now1))
    assert(offer5.getOfferAutoru.getMileageHistory(1).getMileage == mileageLarger)
    assert(offer5.getOfferAutoru.getMileageHistory(1).getUpdateTimestamp == fromMillis(now2))
    assert(offer5.getOfferAutoru.getMileageHistory(2).getMileage == mileageLess)
    assert(offer5.getOfferAutoru.getMileageHistory(2).getUpdateTimestamp == fromMillis(now4))
  }

  test("acceptCreateDate") {
    val user = carTestForms.randomUser
    val now = new DateTime()
    val creationDate = new DateTime(2017, 2, 28, 0, 0, 0)
    val curOfferCreateDate: DateTime = new DateTime(2017, 1, 1, 0, 0, 0)
    val form0 = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id), now = now)).form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form0)(Traced.empty)
    val curOffer = formOfferConverter
      .convertNewOffer(user.userRef, Category.CARS, form0, ad, curOfferCreateDate.getMillis)
      .toBuilder
      // поставим expireDate не равный стандартному + 2 месяца, чтобы убедиться, что он не меняется
      .setTimestampWillExpire(
        AutoruCommonLogic.expireDate(curOfferCreateDate).plusMonths(5).getMillis
      )
      .build()

    val form = carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id), now = now)).form
    val formWithCreateDate: ApiOfferModel.Offer = {
      form.toBuilder.setAdditionalInfo(form.getAdditionalInfo.toBuilder.setCreationDate(creationDate.getMillis)).build()
    }

    // конвертируем новое объявление с acceptCreateDate = false
    checkAcceptCreateDate(
      formOfferConverter.convertNewOffer(user.userRef, Category.CARS, formWithCreateDate, ad, now.getMillis),
      now,
      AutoruCommonLogic.expireDate(now)
    )

    // конвертируем новое объявление с acceptCreateDate = true
    checkAcceptCreateDate(
      formOfferConverter.convertNewOffer(
        user.userRef,
        Category.CARS,
        formWithCreateDate,
        ad,
        now.getMillis,
        params = FormWriteParams(acceptCreateDate = true)
      ),
      creationDate,
      AutoruCommonLogic.expireDate(creationDate)
    )

    // конвертируем новое объявление с acceptCreateDate = true, но createDate в форме не передан
    checkAcceptCreateDate(
      formOfferConverter.convertNewOffer(
        user.userRef,
        Category.CARS,
        form,
        ad,
        now.getMillis,
        params = FormWriteParams(acceptCreateDate = true)
      ),
      now,
      AutoruCommonLogic.expireDate(now)
    )

    // конвертируем существующее объявление с acceptCreateDate = false
    checkAcceptCreateDate(
      formOfferConverter
        .convertExistingOffer(formWithCreateDate, curOffer, None, ad, now.getMillis, FormWriteParams.empty),
      curOffer.getTimestampCreate,
      curOffer.getTimestampWillExpire
    )

    // конвертируем существующее объявление с acceptCreateDate = true
    checkAcceptCreateDate(
      formOfferConverter.convertExistingOffer(
        formWithCreateDate,
        curOffer,
        None,
        ad,
        now.getMillis,
        params = FormWriteParams(acceptCreateDate = true)
      ),
      creationDate,
      AutoruCommonLogic.expireDate(creationDate)
    )

    // конвертируем существующее объявление с acceptCreateDate = true, но createDate в форме не передан
    checkAcceptCreateDate(
      formOfferConverter.convertExistingOffer(
        form,
        curOffer,
        None,
        ad,
        now.getMillis,
        params = FormWriteParams(acceptCreateDate = true)
      ),
      curOffer.getTimestampCreate,
      curOffer.getTimestampWillExpire
    )

    // конвертируем новый черновик с acceptCreateDate = false
    checkAcceptCreateDate(
      formOfferConverter.convertNewDraft(user.userRef, Category.CARS, formWithCreateDate, ad, now.getMillis),
      now.getMillis,
      0
    )

    // конвертируем новый черновик с acceptCreateDate = true
    checkAcceptCreateDate(
      formOfferConverter.convertNewDraft(
        user.userRef,
        Category.CARS,
        formWithCreateDate,
        ad,
        now.getMillis,
        params = FormWriteParams(acceptCreateDate = true)
      ),
      creationDate.getMillis,
      0
    )

    val curDraft: Offer = formOfferConverter.convertNewDraft(
      user.userRef,
      Category.CARS,
      carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(user.id), now = now)).form,
      ad,
      curOfferCreateDate.getMillis
    )

    // конвертируем существующий черновик с acceptCreateDate = false
    checkAcceptCreateDate(
      formOfferConverter.convertExistingDraft(formWithCreateDate, curDraft, ad, now.getMillis),
      curDraft.getTimestampCreate,
      0
    )

    // конвертируем существующий черновик с acceptCreateDate = true
    checkAcceptCreateDate(
      formOfferConverter.convertExistingDraft(
        formWithCreateDate,
        curDraft,
        ad,
        now.getMillis,
        params = FormWriteParams(acceptCreateDate = true)
      ),
      creationDate.getMillis,
      0
    )
  }

  test("supergen conversion") {
    val card = carTestForms.randomCard
    val user = carTestForms.randomUser
    val form = carTestForms
      .generatePrivateForm(TestFormParams[CarFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(user.userRef, Category.CARS, form, ad, now.getMillis)
    assert(offer.getOfferAutoru.getCarInfo.getSuperGenId == card.supergen.id)
  }

  test("ip save") {
    val card = carTestForms.randomCard
    val user = carTestForms.randomUser
    val form = carTestForms
      .generatePrivateForm(TestFormParams[CarFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.CARS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(ip = Some("8.8.8.8"))
    )
    assert(offer.getOfferAutoru.getIp == "8.8.8.8")
  }

  test("save from parameter") {
    val card = carTestForms.randomCard
    val user = carTestForms.randomUser
    val form = carTestForms
      .generatePrivateForm(TestFormParams[CarFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.CARS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(from = Some("wizard"))
    )
    assert(offer.getOfferAutoru.getFrom == "wizard")
  }

  test("source platform save") {
    val card = carTestForms.randomCard
    val user = carTestForms.randomUser
    val form = carTestForms
      .generatePrivateForm(TestFormParams[CarFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val sourceInfo = SourceInfo
      .newBuilder()
      .setSource(SourceInfo.Source.AUTO_RU)
      .setPlatform(SourceInfo.Platform.ANDROID)
      .setUserRef("ac_123")
      .build()
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.CARS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(sourceInfo = Some(sourceInfo))
    )
    assert(offer.getOfferAutoru.getSourceInfo == sourceInfo)
  }

  test("convert other categories") {
    val user = carTestForms.randomUser
    val form: ApiOfferModel.Offer = ApiOfferModel.Offer.newBuilder().build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val moto: Category = Category.MOTO
    val offer = formOfferConverter.convertNewDraft(user.userRef, moto, form, ad, now.getMillis)
    assert(offer.getOfferAutoru.getCategory == Category.MOTO)

    val offer2 = formOfferConverter.convertExistingDraft(form, offer, ad, now.getMillis)
    assert(offer2.getOfferAutoru.getCategory == Category.MOTO)
  }

  test("negative coordinates") {
    val card = carTestForms.randomCard
    val user = carTestForms.randomUser
    val formBuilder = carTestForms
      .generatePrivateForm(TestFormParams[CarFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
      .toBuilder
    formBuilder.getSellerBuilder.getLocationBuilder.getCoordBuilder.setLongitude(-5).setLatitude(-5)
    val form = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.CARS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(ip = Some("8.8.8.8"))
    )
    assert(offer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLongitude == -5)
    assert(offer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLatitude == -5)
  }

  test("haggle possible trucks") {
    val card = components.trucksCatalog.randomCard
    val user = truckTestForms.randomUser
    val formBuilder = truckTestForms
      .generatePrivateForm(TestFormParams[TruckFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
      .toBuilder
    formBuilder.getAdditionalInfoBuilder.setHaggle(true)
    val form = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.TRUCKS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(ip = Some("8.8.8.8"))
    )
    assert(offer.getOfferAutoru.getHagglePossible)

    val form2 = offerFormConverter.convert(ad.forReading, offer)
    assert(form2.getAdditionalInfo.getHaggle)

    val autoruTruck = formAutoruTruckConverter.convertNew(form, ad, now, FormWriteParams.empty, None, 0)
    assert(autoruTruck.haggleKey == 1)
    assert(autoruTruck.getIntSetting(SettingAliases.HAGGLE).value == 1)

    assert(truckOfferConverter.convertStrict(autoruTruck, None).converted.value.getOfferAutoru.getHagglePossible)
  }

  test("haggle possible cars") {
    val card = carTestForms.randomCard
    val user = carTestForms.randomUser
    val formBuilder = carTestForms
      .generatePrivateForm(TestFormParams[CarFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
      .toBuilder
    formBuilder.getAdditionalInfoBuilder.setHaggle(true)
    val form = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)

    val autoruSale = formSaleConverter.convertNew(form, ad, now, FormWriteParams.empty, None, 0)
    assert(autoruSale.getIntSetting(SettingAliases.HAGGLE).value == 1)

    assert(carOfferConverter.convertStrict(autoruSale, None).converted.value.getOfferAutoru.getHagglePossible)
  }

  test("new offer status is waiting activation for trucks") {
    val card = components.trucksCatalog.randomCard
    val user = truckTestForms.randomUser
    val formBuilder = truckTestForms
      .generatePrivateForm(TestFormParams[TruckFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
      .toBuilder
    formBuilder.getAdditionalInfoBuilder.setHaggle(true)
    val form = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.TRUCKS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(ip = Some("8.8.8.8"))
    )
    import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
    assert(offer.autoruStatus == AutoruSaleStatus.STATUS_WAITING_ACTIVATION)

    val autoruTruck = formAutoruTruckConverter.convertNew(form, ad, now, FormWriteParams.empty, None, 0)
    assert(autoruTruck.status == AutoruSaleStatus.STATUS_WAITING_ACTIVATION)
  }

  test("original offer id") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.setOriginalOfferId("100500-hash")
    val offer = builder.build()
    val form = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(form.getAdditionalInfo.getOriginalId == offer.getOfferAutoru.getOriginalOfferId)
  }

  test("remote fields conversion") {
    val builder = TestUtils.createOffer()
    val offerBuilder = builder.getOfferAutoruBuilder
    offerBuilder.getSourceInfoBuilder
      .setRemoteId("24000000011170393")
      .setRemoteUrl("https://goo.gl/hZoiTt")
      .setSource(SourceInfo.Source.AUTO24)
      .setPlatform(SourceInfo.Platform.PLATFORM_UNKNOWN)
    val offer = builder.build()
    val form = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(form.getAdditionalInfo.getRemoteId == "24000000011170393")
    assert(form.getAdditionalInfo.getRemoteUrl == "https://goo.gl/hZoiTt")
  }

  test("custom contact fields conversion") {
    val builder = TestUtils.createOffer()
    val offerBuilder = builder.getOfferAutoruBuilder
    offerBuilder.getSellerBuilder
      .setCustomPhones(true)
      .setCustomPlace(true)
    val offer = builder.build()
    val form = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(form.getSeller.getCustomPhones)
    assert(form.getSeller.getCustomLocation)
  }

  test("services from form") {
    val builder = TestUtils.createOffer()
    val created: Long = now.getMillis - 1000
    builder.getOfferAutoruBuilder
      .addServices(PaidService.newBuilder().setServiceType(ServiceType.ADD).setIsActive(true).setCreated(created))
      .addServices(PaidService.newBuilder().setServiceType(ServiceType.FRESH).setIsActive(false).setCreated(created))
    val offer = builder.build()
    val form = ApiOfferModel.Offer
      .newBuilder()
      .addServices(CommonModel.PaidService.newBuilder().setService("all_sale_top"))
      .addServices(CommonModel.PaidService.newBuilder().setService("all_sale_color"))
      .build()
    val converted = formOfferConverter.convertExistingOffer(form, offer, None, privateAd, now.getMillis)
    assert(converted.getOfferAutoru.getServicesCount == 3)
    assert(converted.getOfferAutoru.getServices(0).getServiceType == ServiceType.TOP)
    assert(!converted.getOfferAutoru.getServices(0).getIsActive)
    assert(converted.getOfferAutoru.getServices(0).getCreated == now.getMillis)

    assert(converted.getOfferAutoru.getServices(1).getServiceType == ServiceType.COLOR)
    assert(!converted.getOfferAutoru.getServices(1).getIsActive)
    assert(converted.getOfferAutoru.getServices(1).getCreated == now.getMillis)

    assert(converted.getOfferAutoru.getServices(2).getServiceType == ServiceType.ADD)
    assert(converted.getOfferAutoru.getServices(2).getIsActive)
    assert(converted.getOfferAutoru.getServices(2).getCreated == created)

    val form2 = offerFormConverter.convert(privateAd.forReading, converted)
    assert(form2.getServicesCount == 1) // неактивные не показываем, поэтому двух неактивных нет

    assert(form2.getServices(0).getService == "all_sale_activate")
  }

  test("empty description and steering wheel") {
    val builder = TestUtils.createOffer()
    builder.setDescription("тест")
    builder.getOfferAutoruBuilder.getCarInfoBuilder.setSteeringWheel(SteeringWheel.LEFT)
    builder.getOfferAutoruBuilder.getTruckInfoBuilder.setWheelType(SteeringWheel.LEFT)
    val offer = builder.build()

    val formBuilder = ApiOfferModel.Offer.newBuilder()

    formBuilder.getCarInfoBuilder.setArmored(true)
    val form1 = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(UserRef.refAnon("pew"), form1)(Traced.empty)
    val converted1 = formOfferConverter.convertExistingDraft(form1, offer, ad, now.getMillis)
    assert(converted1.getDescription.isEmpty)
    assert(converted1.getOfferAutoru.getCarInfo.hasSteeringWheel)
    assert(converted1.getOfferAutoru.getCarInfo.getSteeringWheel == SteeringWheel.LEFT)

    formBuilder.getTruckInfoBuilder.setAxis(1)
    val form2 = formBuilder.build()
    val converted2 = formOfferConverter.convertExistingDraft(form2, offer, ad, now.getMillis)
    assert(converted2.getDescription.isEmpty)
    assert(!converted2.getOfferAutoru.getTruckInfo.hasWheelType)
  }

  test("state not beaten  and custom cleared are true by default") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    val offer1 = offerBuilder.build()
    val form1 = offerFormConverter.convert(AdditionalDataForReading(), offer1)
    assert(form1.getDocuments.getCustomCleared)
    assert(form1.getState.getStateNotBeaten)

    offerBuilder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.NEED_REPAIR)
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.NOT_CLEARED)
    val offer2 = offerBuilder.build()
    val form2 = offerFormConverter.convert(AdditionalDataForReading(), offer2)
    assert(!form2.getDocuments.getCustomCleared)
    assert(!form2.getState.getStateNotBeaten)

    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.UNKNOWN)
    val offer3 = offerBuilder.build()
    val form3 = offerFormConverter.convert(AdditionalDataForReading(), offer3)
    assert(form3.getDocuments.getCustomCleared)
  }

  test("state_not_beaten conversion") {
    val builder: Offer.Builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.EXCELLENT)
    assert(offerFormConverter.convert(AdditionalDataForReading(), builder.build()).getState.getStateNotBeaten)

    builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.GOOD)
    assert(offerFormConverter.convert(AdditionalDataForReading(), builder.build()).getState.getStateNotBeaten)

    builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.MEDIUM)
    assert(offerFormConverter.convert(AdditionalDataForReading(), builder.build()).getState.getStateNotBeaten)

    builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.NEED_REPAIR)
    assert(!offerFormConverter.convert(AdditionalDataForReading(), builder.build()).getState.getStateNotBeaten)

    builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.TO_PARTS)
    assert(!offerFormConverter.convert(AdditionalDataForReading(), builder.build()).getState.getStateNotBeaten)
  }

  test("tags") {
    val builder: Offer.Builder = TestUtils.createOffer()
    builder.addAllTag(Seq("tag1", "tag2", "tag3").asJava)
    val offer = builder.build()
    val form = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(form.getTagsList.asScala.toList == List("tag1", "tag2", "tag3"))

    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val category: Category = Category.CARS
    val offer2 = formOfferConverter.convertNewDraft(user.userRef, category, form, ad, now.getMillis)
    assert(offer2.getTagCount == 0) // не забираем теги с формы при конвертации в оффер, так что должно быть пусто

    val offer3 = offer2.toBuilder.clearTag().addAllTag(Seq("tag4", "tag5", "tag6").asJava).build()
    val offer4 = formOfferConverter.convertExistingDraft(form, offer3, ad, now.getMillis)
    // при конвертации в оффер оставляем теги, которые в нем были, теги с формы игнорим
    assert(offer4.getTagList == offer3.getTagList)
  }

  test("autoservice reviews") {
    val category: Category = Category.CARS
    val formBuilder = ApiOfferModel.Offer.newBuilder()

    val form1 = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form1)(Traced.empty)
    val offer1 = formOfferConverter.convertNewDraft(user.userRef, category, form1, ad, now.getMillis)
    assert(offer1.getOfferAutoru.getAutoserviceReviewCount == 0)

    formBuilder.getAdditionalInfoBuilder.addAutoserviceReview(
      CommonModel.AutoserviceReviewInfo
        .newBuilder()
        .setAutoserviceId("s_123")
        .setReviewId("r_321")
        .build()
    )
    val form2 = formBuilder.build()
    val offer2 = formOfferConverter.convertNewDraft(user.userRef, category, form2, ad, now.getMillis)
    assert(offer2.getOfferAutoru.getAutoserviceReviewCount == 1)
    assert(offer2.getOfferAutoru.getAutoserviceReview(0).getAutoserviceId == "s_123")
    assert(offer2.getOfferAutoru.getAutoserviceReview(0).getReviewId == "r_321")
  }

  test("salon edit_contact edit_address") {
    val userRef = UserRefAutoruClient(10086)
    val offer = getOfferById(1044159039L)
    val ad = components.offersReader.loadAdditionalDataForReading(offer)(Traced.empty)

    val readForm = offerFormConverter.convert(ad, offer)

    assert(readForm.getSalon.getEditAddress)
    assert(readForm.getSalon.getEditContact)
    // assert(readForm.getSeller.getName == readForm.getSalon.getName)
    assert(readForm.getSeller.hasLocation)
    assert(readForm.getSeller.getPhonesCount > 0)
  }

  test("action activate is true if no phones provided") {
    val userRef = UserRefAutoruClient(10086)
    val offerBuilder0: Offer.Builder = TestUtils.createOffer()
    offerBuilder0.putFlag(OF_INACTIVE)
    offerBuilder0.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber("74955554433")
    val offer0 = offerBuilder0.build()
    assert(offer0.canActivate)

    // если нет телефонов - все равно активируем
    offerBuilder0.getOfferAutoruBuilder.getSellerBuilder.clearPhone()
    val offer: Offer = offerBuilder0.build()
    assert(offer.canActivate)

    val ad = components.offersReader.loadAdditionalDataForReading(offer)(Traced.empty)
    val readForm = offerFormConverter.convert(ad, offer)
    assert(readForm.getActions.getActivate)
  }

  test("action activate is false if there were conversion errors") {
    val userRef = UserRefAutoruClient(10086)
    val offerBuilder0: Offer.Builder = TestUtils.createOffer()
    offerBuilder0.putFlag(OF_INACTIVE)
    offerBuilder0.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber("74955554433")
    val offer0 = offerBuilder0.build()
    assert(offer0.canActivate)

    offerBuilder0.getOfferAutoruBuilder.addConversionError("conversion error")
    val offer: Offer = offerBuilder0.build()
    assert(!offer.canActivate)

    val ad = components.offersReader.loadAdditionalDataForReading(offer)(Traced.empty)
    val readForm = offerFormConverter.convert(ad, offer)
    assert(!readForm.getActions.getActivate)
  }

  test("unconfirmed email") {
    val category: Category = Category.CARS
    val offerBuilder0: Offer.Builder = TestUtils.createOffer()
    offerBuilder0.getOfferAutoruBuilder.getSellerBuilder.setUnconfirmedEmail("xxx@yyy.ru")
    val offer: Offer = offerBuilder0.build()
    val ad = components.offersReader.loadAdditionalData(userRef, offer)(Traced.empty)
    val readForm = offerFormConverter.convert(ad.forReading, offer)
    assert(readForm.getSeller.getUnconfirmedEmail == "xxx@yyy.ru")

    val offerFromForm = formOfferConverter.convertNewDraft(userRef, category, readForm, ad, now.getMillis)
    assert(offerFromForm.getOfferAutoru.getSeller.getUnconfirmedEmail == "xxx@yyy.ru")
  }

  test("truck trailer types trailer_moto and overpass") {
    checkTruckTrailerType(
      ru.auto.api.TrucksModel.Trailer.Type.MOTO_TRAILER,
      TrailerType.TRUCK_TRAILER_MOTO_TRAILER,
      1470
    )
    checkTruckTrailerType(ru.auto.api.TrucksModel.Trailer.Type.OVERPASS, TrailerType.TRUCK_TRAILER_OVERPASS, 1468)
  }

  test("allow_chats_creation") {
    val offerBuilder0: Offer.Builder = TestUtils.createOffer()
    offerBuilder0.getOfferAutoruBuilder.getSellerBuilder.setAllowChatsCreation(true)

    val offer: Offer = offerBuilder0.build()
    val ad = components.offersReader.loadAdditionalData(userRef, offer)(Traced.empty)
    val readForm = offerFormConverter.convert(ad.forReading, offer)
    assert(readForm.getSeller.getChatsEnabled)

    offerBuilder0.getOfferAutoruBuilder.getSellerBuilder.setAllowChatsCreation(false)
    val readForm2 = offerFormConverter.convert(ad.forReading, offerBuilder0.build())
    assert(!readForm2.getSeller.getChatsEnabled)
  }

  private def checkTruckTrailerType(apiTrailerType: Trailer.Type,
                                    vosTrailerType: AutoruOffer.TruckInfo.TrailerType,
                                    oldDbTrailerTypeCode: Int): Unit = {
    val card = components.trucksCatalog.randomCard
    val user = truckTestForms.randomUser
    val formBuilder = truckTestForms
      .generatePrivateForm(TestFormParams[TruckFormInfo](optOwnerId = Some(user.id), optCard = Some(card)))
      .form
      .toBuilder
    formBuilder.getTruckInfoBuilder.setTrailerType(apiTrailerType)
    val form = formBuilder.build()
    val ad = components.offersReader.loadAdditionalData(user.userRef, form)(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(
      user.userRef,
      Category.TRUCKS,
      form,
      ad,
      now.getMillis,
      params = FormWriteParams(ip = Some("8.8.8.8"))
    )
    assert(offer.getOfferAutoru.getTruckInfo.getTrailerType == vosTrailerType)

    val form2 = offerFormConverter.convert(ad.forReading, offer)
    assert(form2.getTruckInfo.getTrailerType == apiTrailerType)

    val autoruTruck = formAutoruTruckConverter.convertNew(form, ad, now, FormWriteParams.empty, None, 0)
    assert(autoruTruck.trailerType == oldDbTrailerTypeCode)

    val convertedOffer: Offer = truckOfferConverter.convertStrict(autoruTruck, None).converted.value
    assert(convertedOffer.getOfferAutoru.getTruckInfo.getTrailerType == vosTrailerType)
  }

  test("was_active") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()

    def offer: Offer = offerBuilder.build()

    val ad = components.offersReader.loadAdditionalData(userRef, offer)(Traced.empty)
    assert(!offerFormConverter.convert(ad.forReading, offer).getAdditionalInfo.getWasActive)

    offerBuilder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_INACTIVE)
    assert(!offerFormConverter.convert(ad.forReading, offer).getAdditionalInfo.getWasActive)

    offerBuilder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_ACTIVE)
    assert(offerFormConverter.convert(ad.forReading, offer).getAdditionalInfo.getWasActive)

    offerBuilder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_EXPIRED)
    assert(offerFormConverter.convert(ad.forReading, offer).getAdditionalInfo.getWasActive)

    offerBuilder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_NEED_ACTIVATION)
    assert(offerFormConverter.convert(ad.forReading, offer).getAdditionalInfo.getWasActive)

    offerBuilder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_INACTIVE)
    assert(offerFormConverter.convert(ad.forReading, offer).getAdditionalInfo.getWasActive)
  }

  test("region info in location") {
    val offerBuilder = TestUtils.createOffer()

    def offer: Offer = {
      offerBuilder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(4)
      offerBuilder.build()
    }

    val ad = components.offersReader.loadAdditionalData(userRef, offer)(Traced.empty)
    val converted = offerFormConverter.convert(ad.forReading, offer)

    assert(converted.getSeller.getLocation.hasRegionInfo)
    assert(converted.getSeller.getLocation.getRegionInfo.getId == 4)
    assert(converted.getSeller.getLocation.getRegionInfo.getName.nonEmpty)
  }

  test("region info in location for dealer") {
    val userRef = UserRefAutoruClient(10086)
    val offer = getOfferById(1044159039L)
    val ad = components.offersReader.loadAdditionalDataForReading(offer)(Traced.empty)

    val converted = offerFormConverter.convert(ad, offer)

    assert(converted.getSeller.getLocation.hasRegionInfo)
    assert(converted.getSeller.getLocation.getRegionInfo.getId == 213)
    assert(converted.getSeller.getLocation.getRegionInfo.getName.nonEmpty)
  }

  test("form to offer converter: set remote_id and remote_url from params") {
    val now = new DateTime()

    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getSourceInfoBuilder.setPlatform(Platform.DESKTOP).setSource(Source.AUTO_RU)

    val offer0 = offerBuilder.build()

    val userRef = UserRef.from(offer0.getUserRef)
    val ad = components.offersReader.loadAdditionalData(userRef, offer0)(Traced.empty)
    val form = offerFormConverter.convert(ad.forReading, offer0)

    val nowMillis = now.getMillis
    val remoteId = "avito|cars|1767143438"
    val remoteUrl = "https://www.avito.ru/miass/avtomobili/toyota_land_cruiser_prado_2013_1767143438"
    val offer = formOfferConverter.convertNewOffer(
      userRef,
      offer0.getOfferAutoru.getCategory,
      form,
      ad,
      nowMillis,
      optDraft = Some(offer0),
      params = FormWriteParams(
        remoteId = Some(remoteId),
        remoteUrl = Some(remoteUrl)
      )
    )
    assert(offer.getOfferAutoru.getSourceInfo.getIsCallcenter)
    assert(offer.getOfferAutoru.getSourceInfo.getSource == Source.AVITO)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteId == remoteId)
    assert(offer.getOfferAutoru.getSourceInfo.getRemoteUrl == remoteUrl)
  }

  test("with_nds on empty price_info") {
    val now = new DateTime()
    val nowMillis = now.getMillis
    val carFormInfo = carTestForms.createForm()
    val form = carFormInfo.form.toBuilder
    form.clearPriceInfo().getPriceInfoBuilder.getWithNdsBuilder.setValue(true)
    val userRef = carFormInfo.optUser.get.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, form.build())(Traced.empty)
    val offer = formOfferConverter.convertNewOffer(userRef, Category.CARS, form.build(), ad, nowMillis, None)
    assert(offer.getOfferAutoru.hasPrice)
    assert(offer.getOfferAutoru.getPrice.getPrice == 0)
    assert(offer.getOfferAutoru.getPrice.getCurrency == Currency.RUB)
    assert(offer.getOfferAutoru.getPrice.getCreated == nowMillis)
    assert(offer.getOfferAutoru.getPrice.getWithNds)
  }

  test("license plate moderation state") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getLicensePlateModerationStateBuilder
      .setState(CHECK_OK)
    val offer0 = offerBuilder.build()
    val ad = components.offersReader.loadAdditionalDataForReading(offer0)(Traced.empty)

    val apiOffer = offerFormConverter.convert(ad, offer0)
    assert(apiOffer.getAdditionalInfo.getVinResolutionHidingReason == VinResolutionHidingReason.NO_REASON)

    for (state <- lpHidingStates) {
      offerBuilder.getOfferAutoruBuilder.getLicensePlateModerationStateBuilder
        .setState(state)
      val offer1 = offerBuilder.build()
      val apiOffer = offerFormConverter.convert(ad, offer1)
      assert(
        apiOffer.getAdditionalInfo.getNoLicensePlatePhoto
      )
    }
  }

  test("booking") {
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.bookingGen) { booking =>
      val offerBuilder = TestUtils.createOffer()
      offerBuilder.getOfferAutoruBuilder.setBooking(booking)
      val offer0 = offerBuilder.build()
      val userRef = UserRef.from(offer0.getUserRef)
      val ad = components.offersReader.loadAdditionalData(userRef, offer0)(Traced.empty)
      val offer = offerFormConverter.convert(ad.forReading, offer0)
      val apiBooking = offer.getAdditionalInfo.getBooking
      assert(apiBooking.getAllowed == booking.getAllowed)
      assert(apiBooking.getState.getUpdated == booking.getState.getUpdated)
      assert(apiBooking.getState.hasBooked == booking.getState.hasBooked)
      assert(apiBooking.getState.hasNotBooked == booking.getState.hasNotBooked)
    }
  }

  test("moderation resolution checking") {
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.provenOwnerModerationStateGen) { resolution =>
      val offerBuilder = TestUtils.createOffer()
      offerBuilder.getOfferAutoruBuilder.setProvenOwnerModerationState(resolution)
      val offer0 = offerBuilder.build()
      val userRef = UserRef.from(offer0.getUserRef)
      val ad = components.offersReader.loadAdditionalData(userRef, offer0)(Traced.empty)
      val offer = offerFormConverter.convert(ad.forReading, offer0)
      if (resolution.isProven)
        assert(offer.getAdditionalInfo.getProvenOwnerStatus == ProvenOwnerStatus.OK)
      else
        assert(offer.getAdditionalInfo.getProvenOwnerStatus == ProvenOwnerStatus.FAILED)
    }
  }

  test("setRurPrice, setRurDprice") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getPriceBuilder
      .setCurrency(Currency.RUB)
      .setPrice(1.29000014e13)
      .setCreated(System.currentTimeMillis())
    val offer0 = offerBuilder.build()
    val userRef = UserRef.from(offer0.getUserRef)
    val ad = components.offersReader.loadAdditionalData(userRef, offer0)(Traced.empty)
    val offer = offerFormConverter.convert(ad.forReading, offer0)
    assert(offer.getPriceInfo.getPrice == 1.29000014e13.toFloat)
    assert(offer.getPriceInfo.getDprice == 1.29000014e13)
    assert(offer.getPriceInfo.getRurPrice == 1.29000014e13.toFloat)
    assert(offer.getPriceInfo.getRurDprice == 1.29000014e13)
  }

  test("convert offer from without GarageInfo") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    val offer0 = offerBuilder.build()

    val offer = offerFormConverter.convert(AdditionalDataForReading(), offer0)

    assert(offer.getAdditionalInfo.getGarageInfo.getAcceptableForGarage.getValue === false)
  }

  test("removed offer with timestamp") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    offerBuilder.clearFlag()
    offerBuilder.putFlag(OfferFlag.OF_DELETED)
    offerBuilder.updateStatusHistory(CompositeStatus.CS_ACTIVE, "test")
    offerBuilder.updateStatusHistory(CompositeStatus.CS_REMOVED, "test")

    val offer0 = offerBuilder.build()

    val offer = offerFormConverter.convert(AdditionalDataForReading(), offer0)

    assert(offer.hasRemoved)

    assert(CompositeStatus.CS_REMOVED == offerBuilder.getStatusHistoryList.asScala.last.getOfferStatus)
    assert(Timestamps.toMillis(offer.getRemoved) == offerBuilder.getStatusHistoryList.asScala.last.getTimestamp)
  }

  test("not removed offer without timestamp") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    offerBuilder.clearFlag()

    val offer0 = offerBuilder.build()

    val offer = offerFormConverter.convert(AdditionalDataForReading(), offer0)

    assert(!offer.hasRemoved)
  }

  test("convert offer from with GarageInfo") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.setGarageInfo(GarageInfo.newBuilder().setAcceptableForGarage(true).build())
    val offer0 = offerBuilder.build()

    val offer = offerFormConverter.convert(AdditionalDataForReading(), offer0)

    assert(offer.getAdditionalInfo.getGarageInfo.getAcceptableForGarage.getValue === true)
  }

  test("set moderator flag in price history") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.addAllPriceHistory(
      Seq(
        Price.newBuilder().setPrice(1000).setCurrency(Currency.RUB).setCreated(System.currentTimeMillis()).build(),
        Price
          .newBuilder()
          .setPrice(10000)
          .setCurrency(Currency.RUB)
          .setCreated(System.currentTimeMillis())
          .setEditedByModerator(true)
          .build()
      ).asJava
    )

    val offer: Offer = offerBuilder.build()
    val readForm = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(readForm.getPriceHistoryCount == 2)
    assert(!readForm.getPriceHistoryList.asScala.head.getEditedByModerator.getValue)
    assert(readForm.getPriceHistoryList.asScala.last.getEditedByModerator.getValue)
  }

  test("set moderator flag in mileage history") {
    val offerBuilder: Offer.Builder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.addAllMileageHistory(
      Seq(
        Mileage
          .newBuilder()
          .setMileage(1000)
          .setUpdateTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
          .build(),
        Mileage
          .newBuilder()
          .setMileage(10000)
          .setUpdateTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
          .setEditedByModerator(true)
          .build()
      ).asJava
    )

    val offer: Offer = offerBuilder.build()
    val readForm = offerFormConverter.convert(AdditionalDataForReading(), offer)
    assert(readForm.getMileageHistoryCount == 2)
    assert(!readForm.getMileageHistoryList.asScala.head.getEditedByModerator.getValue)
    assert(readForm.getMileageHistoryList.asScala.last.getEditedByModerator.getValue)
  }

  test("fill allow_user_offers_show from user profile") {
    val offerBuilder = TestUtils.createOffer()
    val ad = AdditionalDataForReading()

    offerBuilder.getUserBuilder.getProfileBuilder.setAllowOffersShow(true)
    assert(
      offerFormConverter
        .convert(ad, offerBuilder.build())
        .getAdditionalInfo
        .getOtherOffersShowInfo
        .getAllowUserOffersShow
    )

    offerBuilder.getUserBuilder.getProfileBuilder.setAllowOffersShow(false)
    assert(
      !offerFormConverter
        .convert(ad, offerBuilder.build())
        .getAdditionalInfo
        .getOtherOffersShowInfo
        .getAllowUserOffersShow
    )

    offerBuilder.getUserBuilder.clearProfile()
    assert(
      !offerFormConverter
        .convert(ad, offerBuilder.build())
        .getAdditionalInfo
        .getOtherOffersShowInfo
        .getAllowUserOffersShow
    )

    offerBuilder.clearUser()
    assert(
      !offerFormConverter
        .convert(ad, offerBuilder.build())
        .getAdditionalInfo
        .getOtherOffersShowInfo
        .getAllowUserOffersShow
    )

  }

  test("set seller.userName from user.profile.alias if allowOffersShow=true") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getSellerBuilder.setUserName("userName")
    val ad = AdditionalDataForReading()

    offerBuilder.getUserBuilder.getProfileBuilder.setAllowOffersShow(true)
    offerBuilder.getUserBuilder.getProfileBuilder.setAlias("alias")
    assert(offerFormConverter.convert(ad, offerBuilder.build()).getSeller.getName == "alias")

    offerBuilder.getUserBuilder.getProfileBuilder.setAllowOffersShow(false)
    assert(offerFormConverter.convert(ad, offerBuilder.build()).getSeller.getName == "userName")

    offerBuilder.getUserBuilder.clearProfile()
    assert(offerFormConverter.convert(ad, offerBuilder.build()).getSeller.getName == "userName")

    offerBuilder.clearUser()
    assert(offerFormConverter.convert(ad, offerBuilder.build()).getSeller.getName == "userName")
  }

  private def checkAcceptCreateDate(offer: Offer, needCreateDate: DateTime, needExpireDate: DateTime): Unit = {
    checkAcceptCreateDate(offer, needCreateDate.getMillis, needExpireDate.getMillis)
  }

  private def checkAcceptCreateDate(offer: Offer, needCreateDate: Long, needExpireDate: Long): Unit = {
    assert(offer.getTimestampCreate == needCreateDate)
    assert(offer.getTimestampWillExpire == needExpireDate)
  }

  private def checkActivate(offer: Offer, needAction: Boolean): Unit = {
    val ad = components.offersReader.loadAdditionalDataForReading(offer)(Traced.empty)
    val convertedFormData = offerFormConverter.convert(ad, offer)
    assert(convertedFormData.getActions.getActivate == needAction)
  }

  private def compareSalonOfferFormData(form: ApiOfferModel.Offer): Unit = {
    compareOfferFormData(
      form,
      convertFunc = formData => {
        val sale = formSaleConverter.convertNew(formData, salonAd, new DateTime(now), FormWriteParams.empty, None, 0)
        val offer =
          formOfferConverter.convertNewOffer(userRefAutoruClient, Category.CARS, formData, salonAd, now.getMillis)
        (sale, offer)
      }
    )
  }

  private def comparePrivateOfferFormData(form: ApiOfferModel.Offer,
                                          needStatus: OfferStatus = OfferStatus.INACTIVE): Unit = {
    compareOfferFormData(
      form,
      needStatus,
      convertFunc = formData => {
        val sale = formSaleConverter.convertNew(formData, privateAd, new DateTime(now), FormWriteParams.empty, None, 0)
        val offer = formOfferConverter.convertNewOffer(userRef, Category.CARS, formData, privateAd, now.getMillis)
        (sale, offer)
      }
    )
  }

  private def compareExistingPrivateOfferFormData(form: ApiOfferModel.Offer,
                                                  needStatus: OfferStatus = OfferStatus.INACTIVE): Unit = {
    compareOfferFormData(
      form,
      needStatus,
      convertFunc = formData => {
        val sale = formSaleConverter
          .convertExisting(formData, curPrivateSaleUpd, privateAd, new DateTime(now), acceptCreateDate = false)
        val curOffer: Offer = carOfferConverter.convertStrict(curPrivateSaleUpd, None).converted.value
        val offer = formOfferConverter
          .convertExistingOffer(formData, curOffer, None, privateAd, now.getMillis, FormWriteParams.empty)
        (sale, offer)
      },
      moreChecks = convertedOfferFormData => {
        assert(convertedOfferFormData.getServicesCount > 0)
        val services = convertedOfferFormData.getServicesList.asScala
        assert(services.length == 3)
        (services.map(_.getService) should contain).allOf("package_turbo", "all_sale_color", "all_sale_toplist")
        assert(convertedOfferFormData.getAdditionalInfo.getAutoserviceReviewCount == 1)
        assert(convertedOfferFormData.getAdditionalInfo.getAutoserviceReview(0).getAutoserviceId == "autoservice_123")
        assert(convertedOfferFormData.getAdditionalInfo.getAutoserviceReview(0).getReviewId == "review_213")
      }
    )
  }

  private def compareOfferFormData(form: ApiOfferModel.Offer,
                                   needStatus: OfferStatus = OfferStatus.INACTIVE,
                                   convertFunc: ApiOfferModel.Offer => (AutoruSale, Offer),
                                   moreChecks: ApiOfferModel.Offer => Unit = form => {}): Unit = {

    val (autoruSale, offer2) = convertFunc(form)

    val card = components.carsCatalog.getCardByTechParamId(form.getCarInfo.getTechParamId).value
    val carInfo = offer2.getOfferAutoru.getCarInfo
    assert(carInfo.getHorsePower == card.enginePower)
    assert(carInfo.getDisplacement == card.engineVolume)
    assert(carInfo.getDoorsCount == card.doorsCount)
    assert(card.engineFeeding.contains(carInfo.getEngineFeeding))
    assert(card.feeding.contains(carInfo.getFeedingType))

    val offer = carOfferConverter.convertStrict(autoruSale, Some(offer2)) match {
      case Converted(result) => result
      case ConvertFailure(error) => throw new RuntimeException("Conversion failure", error)
    }
    val ad1 = components.offersReader.loadAdditionalDataForReading(offer)(Traced.empty)
    val convertedFormData = offerFormConverter.convert(ad1, offer)
    val ad2 = components.offersReader.loadAdditionalDataForReading(offer2)(Traced.empty)
    val convertedFormData2 = offerFormConverter.convert(ad2, offer2)
    performConvertedFormChecks(form, convertedFormData, autoruSale, needStatus, moreChecks)
    performConvertedFormChecks(form, convertedFormData2, autoruSale, needStatus, moreChecks)
  }

  //scalastyle:off method.length
  private def performConvertedFormChecks(
      form: ApiOfferModel.Offer,
      convertedFormData: ApiOfferModel.Offer,
      autoruSale: AutoruSale,
      needStatus: OfferStatus,
      moreChecks: ApiOfferModel.Offer => Unit = form => {}
  )(implicit pos: source.Position): Unit = {
    def check[T](func: ApiOfferModel.Offer => T): Unit = {
      assert(func(form) == func(convertedFormData))
    }

    def checkCarInfo[T](func: CarsModel.CarInfo => T): Unit = {
      assert(func(form.getCarInfo) == func(convertedFormData.getCarInfo))
    }

    def checkState[T](func: ApiOfferModel.State => T): Unit = {
      assert(func(form.getState) == func(convertedFormData.getState))
    }

    def checkPrivateSeller[T](func: ApiOfferModel.PrivateSeller => T): Unit = {
      assert(func(form.getPrivateSeller) == func(convertedFormData.getPrivateSeller))
    }

    def checkSalon[T](func: ApiOfferModel.Salon => T): Unit = {
      assert(func(form.getSalon) == func(convertedFormData.getSalon))
    }

    checkCarInfo(_.getArmored)
    checkCarInfo(_.getBodyType)
    checkCarInfo(_.getEngineType)
    checkCarInfo(c => Dictionaries.transmissions.byCode(c.getTransmission).map(_.code))
    checkCarInfo(c => Dictionaries.gearTypes.byCode(c.getDrive).map(_.code))
    checkCarInfo(_.getMark)
    checkCarInfo(_.getModel)
    //checkCarInfo(_.super_gen)
    checkCarInfo(_.getConfigurationId)
    checkCarInfo(_.getComplectationId)
    checkCarInfo(_.getTechParamId)
    //checkCarInfo(_.modification_id)
    //checkCarInfo(_.complectation)
    //checkCarInfo(_.getEquipmentMap)
    //    checkCarInfo(_.getWheelLeft)
    checkCarInfo(_.getSteeringWheel)
    //    check(f => f.toBuilder.getCarInfoBuilder
    //      .setTransmission(Dictionaries.transmissions.byCode(f.getCarInfo.getTransmission).map(_.code).get)
    //      .setDrive(Dictionaries.gearTypes.byCode(f.getCarInfo.getDrive).flatMap(_.id).flatMap(
    //        Dictionaries.gearTypes.byId
    //      ).get.code)
    //      .setSuperGenId(0).clearComplectationId().clearHorsePower().build()
    //    )
    assert(convertedFormData.getCarInfo.getHorsePower == autoruSale.getIntTechProperty(ENGINE_POWER).value)
    assert(convertedFormData.getCategory == Category.CARS)
    check(_.getPriceInfo.toBuilder.clearRurPrice().clearRurDprice().clearCreateTimestamp().build())
    check(_.getDescription)
    check(_.getDocuments)
    checkState(_.getMileage)
    checkState(_.getStateNotBeaten)
    checkState(_.getVideo)
    checkState(_.getDamagesList)
    checkState(_.getImageUrlsList.asScala.map(_.toBuilder.clearSizes().clearTransform().build()))
    check(
      _.toBuilder.getStateBuilder
        .clearImageUrls()
        .clearOriginalImage()
        .clearC2BAuctionInfo()
        .clearDeletedVideos()
        .build()
    )
    assert(convertedFormData.getState.getImageUrlsList.size == 0)
    //check(_.id)
    check(_.getUserRef)
    //checkSeller(_.email)
    if (form.optPrivateSeller.nonEmpty) {
      checkPrivateSeller(_.getName)
      checkPrivateSeller(_.getPhonesCount)
      for (idx <- 0 until form.getPrivateSeller.getPhonesCount) {
        checkPrivateSeller(_.getPhones(idx).toBuilder.clearOriginal().build)

        val phone = convertedFormData.getPrivateSeller.getPhones(idx)
        assert(phone.getOriginal == phone.getPhone)
      }
      checkPrivateSeller(_.getRedirectPhones)
    }
    if (form.optSalon.nonEmpty) {
      checkSalon(_.getSalonId)
    }
    check(f => {
      f.toBuilder.getAdditionalInfoBuilder
        .setHidden(false)
        .clearExpireDate()
        .clearActualizeDate()
        .clearCreationDate()
        .clearUpdateDate()
        .clearAutoserviceReview()
        .clearCanView()
        .build()
    })
    check(f => f.getBadgesList.asScala.sorted)
    assert(convertedFormData.getAdditionalInfo.getHidden == (needStatus == OfferStatus.INACTIVE))
    withClue(s"${new DateTime(convertedFormData.getAdditionalInfo.getExpireDate)} != ${autoruSale.expireDate}") {
      assert(convertedFormData.getAdditionalInfo.getExpireDate == autoruSale.expireDate.getMillis)
    }
    assert(convertedFormData.getStatus == needStatus)
    assert(convertedFormData.getAdditionalInfo.getActualizeDate == autoruSale.actualizationDate)
    assert(convertedFormData.getAdditionalInfo.getCreationDate == autoruSale.createDate.getMillis)
    assert(ProtoMacro.opt(convertedFormData.getActions).nonEmpty)
    needStatus match {
      case OfferStatus.INACTIVE =>
        assert(convertedFormData.getActions.getArchive)
        assert(convertedFormData.getActions.getEdit)
        assert(!convertedFormData.getActions.getHide)
        assert(convertedFormData.getActions.getActivate)
      case OfferStatus.NEED_ACTIVATION =>
        assert(!convertedFormData.getActions.getArchive)
        assert(convertedFormData.getActions.getEdit)
        assert(convertedFormData.getActions.getHide)
        assert(!convertedFormData.getActions.getActivate)
      case _ =>
    }
    moreChecks(convertedFormData)
  }
}
