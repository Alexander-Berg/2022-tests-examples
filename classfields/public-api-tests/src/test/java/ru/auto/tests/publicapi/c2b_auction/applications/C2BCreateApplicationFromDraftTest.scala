package ru.auto.tests.publicapi.c2b_auction.applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.model.AutoApiC2bInspectPlace
import ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomDraftId

import scala.annotation.meta.getter

@DisplayName("POST /c2b-auction/application/{category}/{draftId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class C2BCreateApplicationFromDraftTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val accounts: AccountManager = null

  @Test
  def shouldCreateApplicationFromValidDraft(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)

    api
      .application()
      .createC2BApplication()
      .reqSpec(defaultSpec)
      .draftIdPath(draft.getOffer.getId)
      .inspectTimeQuery("11:30")
      .inspectDateQuery("11-02-2022")
      .body(moscow)
      .categoryPath(CategoryEnum.CARS)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON()))
  }

  @Test
  def shouldCreateWithEmptyTime(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)

    api
      .application()
      .createC2BApplication()
      .reqSpec(defaultSpec)
      .draftIdPath(draft.getOffer.getId)
      .inspectTimeQuery("")
      .inspectDateQuery("12-02-2022")
      .body(moscow)
      .categoryPath(CategoryEnum.CARS)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON()))
  }

  @Test
  def shouldFailOnBadLocation(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B(account.getLogin, sessionId)

    api
      .application()
      .createC2BApplication()
      .reqSpec(defaultSpec)
      .draftIdPath(draft.getOffer.getId)
      .inspectTimeQuery("11:30")
      .inspectDateQuery("11-02-2022")
      .body(getInspectPlace(37.642475, 55.735523))
      .categoryPath(CategoryEnum.CARS)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def should403WhenNoAuth(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B(account.getLogin, sessionId)

    api
      .application()
      .createC2BApplication()
      .inspectTimeQuery("11:30")
      .inspectDateQuery("11-02-2022")
      .body(moscow)
      .categoryPath(CategoryEnum.CARS)
      .draftIdPath(draft.getOfferId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def should404WhenNoDraft(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId

    api
      .application()
      .createC2BApplication()
      .reqSpec(defaultSpec)
      .inspectTimeQuery("11:30")
      .inspectDateQuery("11-02-2022")
      .body(moscow)
      .categoryPath(CategoryEnum.CARS)
      .draftIdPath(getRandomDraftId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  private def moscow = getInspectPlace(55.735523, 37.642475)

  private def getInspectPlace(lat: Double, long: Double) = {
    val place = new AutoApiC2bInspectPlace()
    place.setLat(lat)
    place.setLon(long)
    place.setComment("Создано из интеграционных тестов")
    place
  }
}
