package ru.auto.tests.publicapi.dealer.whitelist

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiDealerWhitelistAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.testdata.WhitelistDealerAccounts._
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CUSTOMER_ACCESS_FORBIDDEN
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.{assertThat => modelsAssertThat}
import ru.auto.tests.publicapi.testdata.DealerAccounts.getManagerAccount
import org.assertj.core.api.Assertions.assertThat
import ru.auto.tests.publicapi.operations.dealer.whitelist.WhitelistOps

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("POST /dealer/phones/whitelist/entries-left")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PhonesLeftTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private var testApi: ApiClient = null
  private lazy val testAdaptor = new WhitelistOps { override val api: ApiClient = testApi }

  @Inject
  private var adaptor: PublicApiDealerWhitelistAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldIncreaseAndReduceEntriesLeft(): Unit = {
    implicit val session = adaptor.login(phonesLeftDealer.account).getSession
    adaptor.cleanup(phonesLeftDealer.account)
    //there is a bug somewhere between conversion of proto to json -> converting manually
    val emptyNum = testAdaptor.entriesLeft(phonesLeftDealer.account).execute(_.getBody.print.toInt)

    adaptor
      .addPhones(phonesLeftDealer.account, Seq(phonesLeftDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(emptyNum == 200).isTrue

    val addedNum = testAdaptor.entriesLeft(phonesLeftDealer.account).execute(_.getBody.print.toInt)
    assertThat(addedNum == 199).isTrue

    adaptor
      .removePhones(phonesLeftDealer.account, Seq(phonesLeftDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val removedNum = testAdaptor.entriesLeft(phonesLeftDealer.account).execute(_.getBody.print.toInt)
    assertThat(removedNum == 200).isTrue
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403OnEmptySession(): Unit = {
    val response = testApi
      .dealer()
      .getDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    modelsAssertThat(response).hasStatus(ERROR).hasError(CUSTOMER_ACCESS_FORBIDDEN)

    assertThat(response.getDetailedError)
      .contains("Permission denied to SETTINGS:Read for anon")

  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSuceedWithManagerSession(): Unit = {
    adaptor.cleanup(phonesLeftDealer.account)
    implicit val session = adaptor.login(getManagerAccount).getSession

    val phonesLeft = adaptor
      .entriesLeft(getManagerAccount)
      .xDealerIdHeader(phonesLeftDealer.clientId)
      .execute(_.getBody.print.toInt)

    assertThat(phonesLeft == 200).isTrue
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit =
    testApi
      .dealer()
      .entriesLeftDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xDealerIdHeader(phonesLeftDealer.clientId)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))

}
