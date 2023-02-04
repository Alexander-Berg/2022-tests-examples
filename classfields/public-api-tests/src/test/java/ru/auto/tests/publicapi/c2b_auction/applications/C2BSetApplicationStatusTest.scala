package ru.auto.tests.publicapi.c2b_auction.applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.model.AutoC2bReceptionAuctionApplication.StatusEnum
import ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}

import scala.annotation.meta.getter

@DisplayName("POST /c2b-auction/application/{applicationId}/set_status")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class C2BSetApplicationStatusTest {

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
  def shouldRejectApplication(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)
    val application = adaptor.createC2BApplicationFromDraft(draft.getOfferId, sessionId)

    api
      .application()
      .setApplicationStatus()
      .reqSpec(defaultSpec)
      .applicationIdPath(application.getApplicationId)
      .statusQuery(StatusEnum.REJECTED)
      .executeAs(validatedWith(shouldBe200OkJSON()))

    val got = adaptor.getC2BApplication(application.getApplicationId, sessionId)

    Assertions.assertThat(got.getApplication.getStatus.getValue).isEqualTo(StatusEnum.REJECTED.getValue)
  }

  @Test
  def shouldNotSetWaiting(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)
    val application = adaptor.createC2BApplicationFromDraft(draft.getOfferId, sessionId)

    api
      .application()
      .setApplicationStatus()
      .reqSpec(defaultSpec)
      .applicationIdPath(application.getApplicationId)
      .statusQuery(StatusEnum.WAITING_INSPECTION)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def should403WhenNoAuth(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)
    val application = adaptor.createC2BApplicationFromDraft(draft.getOfferId, sessionId)

    api
      .application()
      .setApplicationStatus()
      .applicationIdPath(application.getApplicationId)
      .statusQuery(StatusEnum.WAITING_INSPECTION)
      .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def should404WhenNoAuth(): Unit = {
    api
      .application()
      .setApplicationStatus()
      .reqSpec(defaultSpec)
      .applicationIdPath(Long.MaxValue - 1)
      .statusQuery(StatusEnum.WAITING_INSPECTION)
      .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }
}
