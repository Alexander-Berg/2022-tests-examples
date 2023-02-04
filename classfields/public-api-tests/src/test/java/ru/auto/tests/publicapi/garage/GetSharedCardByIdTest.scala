package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
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
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("GET /garage/user/card/{cardId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetSharedCardByIdTest {

  @(Rule @getter)
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
  def shouldHasNoDiffWithProduction(): Unit = {
    val ownerAccount = am.create()
    val ownerSession = adaptor.login(ownerAccount).getSession
    val anotherAccount = am.create()
    val anotherSession = adaptor.login(anotherAccount).getSession

    val card = adaptor.createGarageCardByVinOrLp("X9FLXXEEBLES67719", ownerSession.getId)

    val getCardById = (api: ApiClient) =>
      api
        .garage()
        .getSharedCard
        .xSessionIdHeader(anotherSession.getId)
        .reqSpec(defaultSpec())
        .cardIdPath(card.getCard.getId)
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual = getCardById(testApi)
    val equalsExpected: Matcher[JsonObject] = 
      jsonEquals(getCardById(prodApi)).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)

    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, ownerSession.getId)

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val getCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .getSharedCard
        .xSessionIdHeader(session.getId)
        .xDeviceUidHeader(session.getDeviceUid)
        .cardIdPath(123)
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    val actual: JsonObject = getCard(testApi)
    val matchExpected: Matcher[JsonObject] = jsonEquals(getCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }
}
