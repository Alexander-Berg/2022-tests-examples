package ru.auto.tests.publicapi.safe_deal

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.{Gson, JsonObject}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.{Owner, Step}
import org.apache.http.HttpStatus
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpPut}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients
import org.awaitility.Awaitility.`given`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.config.PublicApiConfig
import ru.auto.tests.publicapi.consts.Owners.SAFE_DEAL
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model.VertisSafeDealDealView.{BuyerStepEnum, SellerStepEnum, StepEnum}
import ru.auto.tests.publicapi.model.{VertisSafeDealDealSubjectAutoruOfferCarInfo => OfferCarInfo, VertisSafeDealDealSubjectAutoruPtsCarInfo => PtsCarInfo, VertisSafeDealDealSubjectAutoruStsCarInfo => StsCarInfo, VertisSafeDealDealView => DealView, VertisSafeDealEntityBankingEntity => BankingEntity, VertisSafeDealEntityNameEntity => NameEntity, VertisSafeDealEntityPassportRfEntity => PassportRfEntity, VertisSafeDealEntityPhoneEntity => PhoneEntity, _}
import ru.auto.tests.publicapi.module.PublicApiModule

import java.io.InputStreamReader
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit.SECONDS
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._
import scala.jdk.javaapi.OptionConverters._
import scala.util.{Failure, Success, Using}

@DisplayName("POST /safe-deal/deal/update")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class SafeDealFlowTest {

  import SafeDealFlowTest._

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val config: PublicApiConfig = null

  @Test
  @Owner(SAFE_DEAL)
  def shouldFinishDealFlow(): Unit = {
    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    val sellerAccount = accountManager.create()
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    val offerId = adaptor.createOffer(sellerAccount.getLogin, sellerSessionId, CARS).getOfferId
    setOfferTag(config.getVosApiURI, sellerAccount.getId, offerId, AllowedForSafeDealTag)

    waitUntil(
      () => adaptor.getOfferWithSession(CARS, offerId, sellerSessionId).getOffer.getStatus,
      equalTo(AutoApiOffer.StatusEnum.ACTIVE)
    )

    val dealCreated = adaptor.createDealByBuyer(buyerSessionId, offerId)

    MatcherAssert.assertThat(dealCreated.getStep, equalTo(StepEnum.CREATED))
    MatcherAssert.assertThat(dealCreated.getSellerStep, equalTo(SellerStepEnum.ACCEPTING_DEAL))
    MatcherAssert.assertThat(dealCreated.getBuyerStep, equalTo(BuyerStepEnum.AWAITING_ACCEPT))

    val dealAccepted = adaptor.acceptDealBySeller(sellerSessionId, dealCreated.getId, true)

    MatcherAssert.assertThat(dealAccepted.getStep, equalTo(StepEnum.INVITE_ACCEPTED))
    MatcherAssert.assertThat(dealAccepted.getSellerStep, equalTo(SellerStepEnum.INTRODUCING_PASSPORT_DETAILS))
    MatcherAssert.assertThat(dealAccepted.getBuyerStep, equalTo(BuyerStepEnum.INTRODUCING_PASSPORT_DETAILS))

    val dealUpdatedSellerPassport = adaptor.updatePassportBySeller(
      sellerSessionId,
      dealAccepted.getId,
      new NameEntity()
        .name("Аркадий")
        .surname("Аркадиев")
        .patronymic("Аркадиевич"),
      new PassportRfEntity()
        .series("1234")
        .number("666666")
        .birthDate("01.01.1990")
        .birthPlace("г. Москва")
        .departCode("123-456")
        .departName("Отделение УФМС по Московской области")
        .issueDate("01.01.2010")
        .address("г. Москва, ул. Садовническая"),
      buildPhoneEntity(toScala(sellerAccount.getPhone))
    )

    MatcherAssert.assertThat(dealUpdatedSellerPassport.getSellerStep, equalTo(SellerStepEnum.INTRODUCING_SUBJECT_DETAILS))

    val dealUpdatedBuyerPassport = adaptor.updatePassportByBuyer(
      buyerSessionId,
      dealUpdatedSellerPassport.getId,
      new NameEntity()
        .name("Поликарп")
        .surname("Ершов")
        .patronymic("Акакиевич"),
      new PassportRfEntity()
        .series("4321")
        .number("666666")
        .birthDate("10.03.1991")
        .birthPlace("г. Москва")
        .departCode("123-456")
        .departName("Отделение УФМС по Московской области")
        .issueDate("01.01.2010")
        .address("г. Москва, Чистопрудный бульвар"),
      buildPhoneEntity(toScala(buyerAccount.getPhone))
    )

    MatcherAssert.assertThat(dealUpdatedBuyerPassport.getBuyerStep, equalTo(BuyerStepEnum.INTRODUCING_SELLING_PRICE))

    waitUntil(
      () => adaptor.getDeal(buyerSessionId, dealUpdatedBuyerPassport.getId).getSellerStep,
      equalTo(SellerStepEnum.INTRODUCING_SUBJECT_DETAILS)
    )

    val dealUpdatedSubjectInfo = adaptor.updateSubjectInfoBySeller(
      sellerSessionId,
      dealUpdatedBuyerPassport.getId,
      new OfferCarInfo()
        .color("Красный")
        .year(2014)
        .horsePower(120)
        .mileage(1337)
        .subcategory("легковая"),
      new PtsCarInfo()
        .displacement(2400)
        .bodyNumber("12345678901234567")
        .chassisNumber("Отсутствует")
        .licensePlate("А473АА164")
        .seriesNumber("11TO333333")
        .issueDate("04.06.2021")
        .issuer("Отдел ГИББД Москвы"),
      new StsCarInfo()
        .seriesNumber("12TO333333")
        .issueDate("02.11.2021")
        .issuer("Отдел ГИББД Москвы")
    )

    MatcherAssert.assertThat(dealUpdatedSubjectInfo.getSellerStep, equalTo(SellerStepEnum.INTRODUCED_SUBJECT_DETAILS))

    waitUntil(
      () => adaptor.getDeal(buyerSessionId, dealUpdatedSubjectInfo.getId).getBuyerStep,
      equalTo(BuyerStepEnum.INTRODUCING_SELLING_PRICE)
    )

    val dealUpdatedSellingPrice = adaptor.updateSellingPriceByBuyer(buyerSessionId, dealUpdatedSubjectInfo.getId, 7L)

    MatcherAssert.assertThat(dealUpdatedSellingPrice.getSellerStep, equalTo(SellerStepEnum.APPROVING_SELLING_PRICE))
    MatcherAssert.assertThat(dealUpdatedSellingPrice.getBuyerStep, equalTo(BuyerStepEnum.INTRODUCED_SELLING_PRICE))

    val dealApprovedSellingPrice = adaptor.approveSellingPriceBySeller(sellerSessionId, dealUpdatedSellingPrice.getId, 7L)

    MatcherAssert.assertThat(dealApprovedSellingPrice.getSellerStep, equalTo(SellerStepEnum.INTRODUCING_ACCOUNT_DETAILS))
    MatcherAssert.assertThat(dealApprovedSellingPrice.getBuyerStep, equalTo(BuyerStepEnum.PROVIDING_MONEY))

    val dealUpdatedBankingDetails = adaptor.updateBankingDetailsBySeller(
      sellerSessionId,
      dealApprovedSellingPrice.getId,
      new BankingEntity()
        .bic("044525225")
        .accountNumber("40817810938160925982")
        .fullName("Аркадий Аркадиев Аркадиевич")
    )

    MatcherAssert.assertThat(dealUpdatedBankingDetails.getSellerStep, equalTo(SellerStepEnum.CHECKING_ACCOUNT_DETAILS))

    val dealAddedMoney = addMoney(
      buyerSessionId,
      dealUpdatedBankingDetails.getId,
      10L,
      new BankingEntity()
        .bic("044525225")
        .accountNumber("40817810938160925982")
        .fullName("Поликарп Ершов Акакиевич")
    )

    waitUntil(
      () => adaptor.getDeal(buyerSessionId, dealAddedMoney.getId).getSellerStep,
      equalTo(SellerStepEnum.INTRODUCED_ACCOUNT_DETAILS)
    )

    MatcherAssert.assertThat(dealAddedMoney.getBuyerStep, equalTo(BuyerStepEnum.INTRODUCING_MEETING_DETAILS))

    val dealUpdatedMeetingDetails = adaptor.updateMeetingDetailsByBuyer(buyerSessionId, dealAddedMoney.getId, 213L, "15.11.2021")

    MatcherAssert.assertThat(dealUpdatedMeetingDetails.getSellerStep, equalTo(SellerStepEnum.READY_FOR_MEETING))
    MatcherAssert.assertThat(dealUpdatedMeetingDetails.getBuyerStep, equalTo(BuyerStepEnum.READY_FOR_MEETING))

    waitUntil(
      () => adaptor.getDeal(buyerSessionId, dealUpdatedMeetingDetails.getId).getBuyerStep,
      equalTo(BuyerStepEnum.READY_FOR_MEETING)
    )

    val dealUploaded1Document = adaptor.uploadDocumentByBuyer(buyerSessionId, dealUpdatedMeetingDetails.getId, PhotoNameSide1, PhotoPathSide1)

    waitUntil(
      () => adaptor.getDeal(buyerSessionId, dealUploaded1Document.getId).getBuyerStep,
      equalTo(BuyerStepEnum.READY_FOR_MEETING)
    )

    val dealUploaded2Document = adaptor.uploadDocumentByBuyer(buyerSessionId, dealUploaded1Document.getId, PhotoNameSide2, PhotoPathSide2)

    val dealApprovedDocuments = adaptor.approveDocumentsBySeller(sellerSessionId, dealUploaded2Document.getId)

    MatcherAssert.assertThat(dealApprovedDocuments.getSellerStep, equalTo(SellerStepEnum.APPROVING_DEAL))
    MatcherAssert.assertThat(dealApprovedDocuments.getBuyerStep, equalTo(BuyerStepEnum.UPLOADED_DOCS))

    val dealRequestedSellerCode = adaptor.requestCodeBySeller(sellerSessionId, dealApprovedDocuments.getId)

    MatcherAssert.assertThat(dealRequestedSellerCode.getSellerStep, equalTo(SellerStepEnum.APPROVING_DEAL))
    MatcherAssert.assertThat(dealRequestedSellerCode.getBuyerStep, equalTo(BuyerStepEnum.UPLOADED_DOCS))

    val dealConfirmedSellerCode = adaptor.confirmCodeBySeller(sellerSessionId, dealRequestedSellerCode.getId, getCode(dealRequestedSellerCode.getId, "seller"))

    MatcherAssert.assertThat(dealConfirmedSellerCode.getSellerStep, equalTo(SellerStepEnum.APPROVED_DEAL))
    MatcherAssert.assertThat(dealConfirmedSellerCode.getBuyerStep, equalTo(BuyerStepEnum.APPROVING_DEAL))

    val dealRequestedBuyerCode = adaptor.requestCodeByBuyer(buyerSessionId, dealConfirmedSellerCode.getId)

    MatcherAssert.assertThat(dealRequestedBuyerCode.getSellerStep, equalTo(SellerStepEnum.APPROVED_DEAL))
    MatcherAssert.assertThat(dealRequestedBuyerCode.getBuyerStep, equalTo(BuyerStepEnum.APPROVING_DEAL))

    val dealConfirmedBuyerCode = adaptor.confirmCodeByBuyer(buyerSessionId, dealRequestedBuyerCode.getId, getCode(dealRequestedBuyerCode.getId, "buyer"))

    MatcherAssert.assertThat(dealConfirmedBuyerCode.getStep, equalTo(StepEnum.CONFIRMED))
    MatcherAssert.assertThat(dealConfirmedBuyerCode.getSellerStep, equalTo(SellerStepEnum.APPROVED_DEAL))
    MatcherAssert.assertThat(dealConfirmedBuyerCode.getBuyerStep, equalTo(BuyerStepEnum.APPROVED_DEAL))
  }

  @Step("Вносим средства на счет для сессии {buyerSessionId} и сделке {dealId}")
  def addMoney(buyerSessionId: String, dealId: String, amount: Long, entity: BankingEntity): DealView = {
    val url = s"${config.getSafeDealApiURI}/deal/money/$dealId?amount=$amount"
    Using(HttpClients.createDefault) { httpClient =>
      val request = new HttpPost(url)
      val bankingEntity = Map(
        "bic" -> entity.getBic,
        "accountNumber" -> entity.getAccountNumber,
        "fullName" -> entity.getFullName
      )
      val json = new Gson().toJson(bankingEntity.asJava)
      request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON))
      Using(httpClient.execute(request)) { response =>
        MatcherAssert.assertThat("Add money successfully", response.getStatusLine.getStatusCode == HttpStatus.SC_OK)
      }
    }
    adaptor.getDeal(buyerSessionId, dealId)
  }

  @Step("Получаем код подверждения для участника {party} сделки {dealId}")
  private def getCode(dealId: String, party: String): String = {
    val url = s"${config.getSafeDealApiURI}/deal/raw/get/$dealId"
    Using(HttpClients.createDefault) { httpClient =>
      Using(httpClient.execute(new HttpGet(url))) { response =>
        val reader = new InputStreamReader(response.getEntity.getContent)
        val json = new Gson().fromJson(reader, classOf[JsonObject])
        json
          .getAsJsonObject(party)
          .getAsJsonObject("confirmationCode")
          .get("code")
          .getAsString
      }
    }.flatten match {
      case Success(code) => code
      case Failure(err) => throw err
    }
  }
}

object SafeDealFlowTest {

  val AllowedForSafeDealTag: String = "allowed_for_safe_deal"

  private val PhotoNameSide1: String = "side1"
  private val PhotoPathSide1: String = "photo/photo.jpg"
  private val PhotoNameSide2: String = "side2"
  private val PhotoPathSide2: String = "photo/photo1.jpg"

  private[this] val POLL_INTERVAL: Int = 3
  private[this] val TIMEOUT: Int = 60

  private def buildPhoneEntity(phone: Option[String]): PhoneEntity = {
    val entity = new PhoneEntity()
    phone.foreach(entity.phone)
    entity
  }

  def setOfferTag(vosApiUri: URI, userId: String, offerId: String, tag: String): Unit =
    Using(HttpClients.createDefault) { httpClient =>
      val url = s"$vosApiUri/offer/cars/user:$userId/$offerId/tags/$tag"
      val response = httpClient.execute(new HttpPut(url))
      MatcherAssert.assertThat("Set tag successfully", response.getStatusLine.getStatusCode == HttpStatus.SC_OK)
    }

  def waitUntil[T](supplier: Callable[T], matcher: Matcher[_ >: T]): T =
    `given`
      .conditionEvaluationListener(new AllureConditionEvaluationLogger)
      .pollInterval(POLL_INTERVAL, SECONDS)
      .atMost(TIMEOUT, SECONDS)
      .ignoreExceptions
      .until(supplier, matcher)
}
