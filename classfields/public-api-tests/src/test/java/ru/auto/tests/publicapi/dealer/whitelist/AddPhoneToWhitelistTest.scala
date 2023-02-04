package ru.auto.tests.publicapi.dealer.whitelist

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus._
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.{After, Before, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiDealerWhitelistAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.model.AutoApiDealerPhoneNumberResult.StatusEnum
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.{AutoApiDealerSimplePhonesList, AutoApiErrorResponse}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getManagerAccount
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.{assertThat => checkThat}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum._
import ru.auto.tests.publicapi.operations.dealer.whitelist.WhitelistOps
import ru.auto.tests.publicapi.testdata.WhitelistDealerAccounts._

import scala.jdk.CollectionConverters._
import scala.annotation.meta.getter

@DisplayName("POST /dealer/phones/whitelist/add")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AddPhoneToWhitelistTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private var testApi: ApiClient = null

  private lazy val testAdaptor = new WhitelistOps { override val api: ApiClient = testApi }

  @Inject
  private var adaptor: PublicApiDealerWhitelistAdaptor = null

  val invalidPhone = "not a phone"

  @Before
  @After
  def cleanDealerPhones(): Unit = adaptor.cleanup(addPhoneDealer.account)

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessOnAddingPhone(): Unit = {
    implicit val session = adaptor.login(addPhoneDealer.account).getSession
    val response =
      testAdaptor
        .addPhones(addPhoneDealer.account, Seq("+7 987 654 33 10"))
        .executeAs(validatedWith(shouldBe200OkJSON))
        .getResult
        .get(0)

    checkThat(response)
      .hasStatus(StatusEnum.SUCCESS)
      .hasPhone("+79876543310")
      .hasErrorMessage(null)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldBeIdempotentOnPhonesAdding(): Unit = {
    implicit val session = adaptor.login(addPhoneDealer.account).getSession
    val firstExecList = adaptor
      .addPhones(addPhoneDealer.account, Seq(addPhoneDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala

    adaptor
      .addPhones(addPhoneDealer.account, Seq(addPhoneDealer.phone))
      .executeAs(identity)

    val thirdExecList = testAdaptor
      .addPhones(addPhoneDealer.account, Seq(addPhoneDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala

    val zippedResults = firstExecList.zip(thirdExecList)

    //verifying, answers are the same, answers succeed
    assertThat(zippedResults.forall {
      case (a, b) =>
        a.getPhone == b.getPhone &&
          a.getStatus == b.getStatus &&
          b.getStatus == StatusEnum.SUCCESS
    }).isTrue

  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeFailOnWrongPhoneFormat(): Unit = {
    implicit val session = adaptor.login(addPhoneDealer.account).getSession
    val result = testAdaptor
      .addPhones(addPhoneDealer.account, Seq(invalidPhone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head

    checkThat(result)
      .hasStatus(StatusEnum.ERROR)
      .hasErrorMessage(s"Failed to parse phone $invalidPhone")
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeFailOnTooLargePhoneBatch(): Unit = {
    val phonesBatch = (0 to 222).map(i => s"+${7_988_765_53_21L + i}") //generate bunch of phones
    implicit val session = adaptor.login(addPhoneDealer.account).getSession

    testAdaptor
      .addPhones(addPhoneDealer.account, phonesBatch)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST))) //pre-condition fail

    val listResult = adaptor
      .listPhones(addPhoneDealer.account)
      .executeAs(validatedWith(shouldBe200OkJSON))

    //modelAssertions.hasNoPhones expects empty collection instead of null -> npe
    assertThat(listResult.getPhones == null).isTrue
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeBothValidAndNonValidPhonesResult(): Unit = {
    implicit val session = adaptor.login(addPhoneDealer.account).getSession
    val phones = Seq(addPhoneDealer.phone, invalidPhone)
    testAdaptor
      .addPhones(addPhoneDealer.account, phones)
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .toList match {
      case a :: b :: Nil =>
        checkThat(a).hasStatus(StatusEnum.SUCCESS).hasPhone(addPhoneDealer.formattedPhone)
        checkThat(b).hasStatus(StatusEnum.ERROR).hasErrorMessage(s"Failed to parse phone $invalidPhone")
      case _ => fail("Expecting exactly two messages in response")
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeFailOnLimitExceeded(): Unit = {
    val phonesBatch = (0 to 222).map(i => s"+${7_988_765_53_21L + i}") //generate bunch of phones
    implicit val session = adaptor.login(addPhoneDealer.account).getSession
    val smallerBatchAdd = adaptor
      .addPhones(addPhoneDealer.account, phonesBatch.slice(0, 198))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala

    assertThat(smallerBatchAdd.forall(_.getStatus == StatusEnum.SUCCESS))

    testAdaptor
      .addPhones(addPhoneDealer.account, phonesBatch.slice(199, 220))
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WithoutSessionId(): Unit = {
    val bodyPhones = new AutoApiDealerSimplePhonesList()
    bodyPhones.addPhonesItem(addPhoneDealer.phone)

    val response = testApi
      .dealer()
      .addDealerPhonesToWhiteList()
      .body(bodyPhones)
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    checkThat(response).hasStatus(ERROR).hasError(CUSTOMER_ACCESS_FORBIDDEN)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSucceedWithManagerSession(): Unit = {
    implicit val session = adaptor.login(getManagerAccount).getSession
    adaptor
      .addPhones(getManagerAccount, Seq(addPhoneDealer.phone))
      .xDealerIdHeader(addPhoneDealer.clientId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit = {
    val bodyPhones = new AutoApiDealerSimplePhonesList()
    bodyPhones.addPhonesItem(addPhoneDealer.phone)

    val response = testApi
      .dealer()
      .addDealerPhonesToWhiteList()
      .body(bodyPhones)
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    checkThat(response).hasStatus(ERROR).hasError(CUSTOMER_ACCESS_FORBIDDEN)
  }
}
