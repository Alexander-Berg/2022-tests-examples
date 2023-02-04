package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_OK}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("PUT /shark/credit-application/call-center/{credit_application_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class EcreditCalculationTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null


  @Test
  @Owner(SHARK)
  def shouldHasNoDiffWithProduction(): Unit = {

    val body = new VertisSharkApiEcreditCalculationRequest()
      .dealerRef("dealer:16453")
      .used(true)
      .price(778000)
      .carName("Hyundai Santa FE")
      .initialFee(10000)
      .carAge(2007)
      .requiredLife(true)
      .requiredKasko(true)

    val req = (apiClient: ApiClient) => apiClient.shark.calculationRoute()
      .reqSpec(defaultSpec)
      .body(body)
      .execute(validatedWith(shouldBeCode(SC_OK)))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

  @Test
  @Owner(SHARK)
  def shouldSee400WhenNoExternalIdDealer(): Unit = {

    val body = new VertisSharkApiEcreditCalculationRequest()
      .dealerRef("dealer:1234")
      .used(true)
      .price(778000)
      .carName("Hyundai Santa FE")
      .initialFee(10000)
      .carAge(2007)
      .requiredLife(true)
      .requiredKasko(true)

    api.shark.calculationRoute()
      .reqSpec(defaultSpec)
      .body(body)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(SHARK)
  def shouldSee200WhenRequestIsValid(): Unit = {

    val body = new VertisSharkApiEcreditCalculationRequest()
      .dealerRef("dealer:16453")
      .used(true)
      .price(778000)
      .carName("Hyundai Santa FE")
      .initialFee(10000)
      .carAge(2007)
      .requiredLife(true)
      .requiredKasko(true)

    api.shark.calculationRoute()
      .reqSpec(defaultSpec)
      .body(body)
      .execute(validatedWith(shouldBeCode(SC_OK)))
  }
}
