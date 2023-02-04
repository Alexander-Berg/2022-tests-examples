package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import org.assertj.core.api.Java6Assertions.assertThat
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.model.AutoApiVinGarageGetPromoPartnersResponse
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("GET /garage/user/promos")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetPromoPartnersTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val testApi: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionWhenNoAuth(): Unit = {
    val getParnerPromo = (api: ApiClient) =>
      api
        .garage()
        .getPartnersPromo
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual = getParnerPromo(testApi)
    val prod = getParnerPromo(prodApi)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(prod)

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val card = adaptor.createGarageCardByVinOrLp("X9FLXXEEBLES67719", session.getId)

    val getParnerPromo = (api: ApiClient) =>
      api
        .garage()
        .getPartnersPromo
        .xSessionIdHeader(session.getId)
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual = getParnerPromo(testApi)
    val prod = getParnerPromo(prodApi)
    val equalsExpected: Matcher[JsonObject] =
      jsonEquals(prod).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)

    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, session.getId)

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldBeDistinctResult(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val card = adaptor.createGarageCardByVinOrLp("X9FLXXEEBLES67719", session.getId)

    val getPartnersPromo = (api: ApiClient) =>
      api
        .garage()
        .getPartnersPromo
        .xSessionIdHeader(session.getId)
        .reqSpec(defaultSpec())
        .targetPromoIdsQuery("yandex_market")
        .execute(validatedWith(shouldBeSuccess()))

    val actual = getPartnersPromo(testApi)

    val actualAsModel = actual.as(classOf[AutoApiVinGarageGetPromoPartnersResponse])
    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, session.getId)
    val allPromoId = actualAsModel.getPartnerPromos.asScala.map(_.getId)
    assertThat(allPromoId.distinct.size).isEqualTo(allPromoId.size)
    assertThat(actualAsModel.getPartnerPromos.get(0).getId).isEqualTo("yandex_market")
  }

}
