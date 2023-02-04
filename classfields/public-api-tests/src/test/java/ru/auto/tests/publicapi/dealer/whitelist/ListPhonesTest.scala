package ru.auto.tests.publicapi.dealer.whitelist

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.assertj.core.api.Assertions
import org.junit.{After, Before, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiDealerWhitelistAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CUSTOMER_ACCESS_FORBIDDEN
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import org.assertj.core.api.Assertions.assertThat
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.{assertThat => checkThat}
import ru.auto.tests.publicapi.operations.dealer.whitelist.WhitelistOps
import ru.auto.tests.publicapi.testdata.DealerAccounts.getManagerAccount
import ru.auto.tests.publicapi.testdata.WhitelistDealerAccounts.listDealer

import scala.jdk.CollectionConverters._
import scala.annotation.meta.getter

@DisplayName("POST /dealer/phones/whitelist/get")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class ListPhonesTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private var testApi: ApiClient = null

  @Inject
  private var adaptor: PublicApiDealerWhitelistAdaptor = null
  private lazy val testAdaptor = new WhitelistOps { override val api: ApiClient = testApi }

  val dealerAvailable = "20101"

  @Before
  @After
  def cleanDealerPhones(): Unit = adaptor.cleanup(listDealer.account)

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldShowWhitelistedPhones(): Unit = {
    implicit val session = adaptor.login(listDealer.account).getSession
    val addedPhone = adaptor
      .addPhones(listDealer.account, Seq(listDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head
      .getPhone

    val listedPhone = testAdaptor
      .listPhones(listDealer.account)
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getPhones
      .asScala
      .head

    assertThat(addedPhone == listedPhone).isTrue

  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403OnEmptySession(): Unit = {
    val response = testApi
      .dealer()
      .getDealerPhonesToWhiteList
      .reqSpec(defaultSpec)
      .xDealerIdHeader(dealerAvailable)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    checkThat(response).hasStatus(ERROR).hasError(CUSTOMER_ACCESS_FORBIDDEN)
    Assertions
      .assertThat(response.getDetailedError)
      .contains("Permission denied to SETTINGS:Read for anon")
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSucceedWithManagerSession(): Unit = {
    val dealerSession = adaptor.login(listDealer.account).getSession
    val managerSession = adaptor.login(getManagerAccount).getSession
    adaptor.cleanup(listDealer.account)(dealerSession)

    val addedPhone = adaptor
      .addPhones(listDealer.account, Seq(listDealer.phone))(dealerSession)
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head
      .getPhone

    val managerListedPhone = testAdaptor
      .listPhones(getManagerAccount)(managerSession)
      .xDealerIdHeader(listDealer.clientId)
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getPhones
      .asScala
      .head

    assertThat(addedPhone == listDealer.formattedPhone).isTrue
    assertThat(addedPhone == managerListedPhone).isTrue
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit =
    testApi
      .dealer()
      .getDealerPhonesToWhiteList
      .reqSpec(defaultSpec)
      .xDealerIdHeader(dealerAvailable)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
}
