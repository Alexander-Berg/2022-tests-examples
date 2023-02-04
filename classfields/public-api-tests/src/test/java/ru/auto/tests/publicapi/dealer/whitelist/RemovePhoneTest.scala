package ru.auto.tests.publicapi.dealer.whitelist

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiDealerWhitelistAdaptor
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.{assertThat => checkThat}
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.testdata.WhitelistDealerAccounts._
import ru.auto.tests.publicapi.model.AutoApiDealerPhoneNumberResult.StatusEnum
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.testdata.DealerAccounts.getManagerAccount
import org.apache.http.HttpStatus._
import ru.auto.tests.publicapi.model.{AutoApiDealerSimplePhonesList, AutoApiErrorResponse}
import ru.auto.tests.publicapi.operations.dealer.whitelist.WhitelistOps
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.jdk.CollectionConverters._
import scala.annotation.meta.getter

@DisplayName("POST /dealer/phones/whitelist/delete")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class RemovePhoneTest {

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
  def shouldRemovePhones(): Unit = {
    implicit val session = adaptor.login(removeDealer.account).getSession

    val firstResult = adaptor
      .addPhones(removeDealer.account, Seq(removeDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala

    val secondResult = testAdaptor
      .removePhones(removeDealer.account, Seq(removeDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala

    assertThat(firstResult.size - 1 == secondResult.size)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldAlertAboutRemovedPhones(): Unit = {
    implicit val session = adaptor.login(removeDealer.account).getSession

    val addResult = adaptor
      .addPhones(removeDealer.account, Seq(removeDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head

    val deleteResult = testAdaptor
      .removePhones(removeDealer.account, Seq(removeDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head

    val secondDeleteResult = testAdaptor
      .removePhones(removeDealer.account, Seq(removeDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head

    checkThat(addResult).hasStatus(StatusEnum.SUCCESS)
    checkThat(deleteResult).hasStatus(StatusEnum.SUCCESS)

    checkThat(secondDeleteResult)
      .hasStatus(StatusEnum.ERROR)
      .hasErrorMessage(s"${removeDealer.formattedPhone} can't be deleted")

  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldNotRemoveNotOwnedPhones(): Unit = {
    implicit val session = adaptor.login(removeDealer.account).getSession

    adaptor.addPhones(removeDealer.account, Seq(removeDealer.phone)).executeAs(validatedWith(shouldBe200OkJSON))
    val res = testAdaptor
      .removePhones(addPhoneDealer.account, Seq(removeDealer.phone))
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getResult
      .asScala
      .head

    checkThat(res).hasErrorMessage(s"${removeDealer.formattedPhone} can't be deleted")
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403OnEmptySession(): Unit = {
    val bodyPhones = new AutoApiDealerSimplePhonesList()
    bodyPhones.addPhonesItem(removeDealer.phone)

    testApi
      .dealer()
      .deleteDealerPhonesToWhiteList()
      .body(bodyPhones)
      .reqSpec(defaultSpec)
      .xDealerIdHeader(removeDealer.clientId)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSuceedWithManagerSession(): Unit = {

    adaptor
      .addPhones(removeDealer.account, Seq(removeDealer.phone))(adaptor.login(removeDealer.account).getSession)
      .executeAs(validatedWith(shouldBe200OkJSON))

    adaptor
      .removePhones(getManagerAccount, Seq(removeDealer.phone))(adaptor.login(getManagerAccount).getSession)
      .xDealerIdHeader(removeDealer.clientId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit = {
    val bodyPhones = new AutoApiDealerSimplePhonesList()
    bodyPhones.addPhonesItem(removeDealer.phone)

    testApi
      .dealer()
      .deleteDealerPhonesToWhiteList()
      .body(bodyPhones)
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])
  }
}
