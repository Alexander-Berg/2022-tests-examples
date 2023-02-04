package ru.auto.tests.moderation

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.adaptor.CarfaxApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import ru.auto.tests.model.AutoApiVinConfirmedKmAge.EventTypeEnum.AUTORU_OFFER
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("DELETE /moderation/kmage")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteKmAgeTest {

  private val VIN = "XWFPE9DN1D0004086"
  private val EVENT_TYPE = AUTORU_OFFER
  private val OFFER_ID = "1049777022-811fc"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: CarfaxApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithoutVin(): Unit = {
    api.moderation
      .deleteKmAge()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithoutEventType(): Unit = {
    api.moderation
      .deleteKmAge()
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithoutId(): Unit = {
    api.moderation
      .deleteKmAge()
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .eventTypeQuery(EVENT_TYPE)
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProduction(): Unit = {
    adaptor.updateMillageInReport(VIN, EVENT_TYPE, OFFER_ID)

    api.moderation
      .deleteKmAge()
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .eventTypeQuery(EVENT_TYPE)
      .idQuery(OFFER_ID)
      .xUserIdHeader(s"qa_user:$getRandomShortInt")
      .execute(validatedWith(shouldBe200Ok))
  }
}
