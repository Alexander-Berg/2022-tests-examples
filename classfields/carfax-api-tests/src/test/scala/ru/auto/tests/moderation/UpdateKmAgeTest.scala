package ru.auto.tests.moderation

import java.util.concurrent.TimeUnit.SECONDS

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.awaitility.Awaitility.given
import org.awaitility.Duration
import org.hamcrest.CoreMatchers.equalTo
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{After, Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200Ok, shouldBe200OkJSON}
import ru.auto.tests.commons.util.Utils.getRandomShortInt
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.AutoApiVinConfirmedKmAge
import ru.auto.tests.model.AutoApiVinConfirmedKmAge.EventTypeEnum.AUTORU_OFFER
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("POST /moderation/kmage")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateKmAgeTest {

  private val VIN = "XWFPE9DN1D0004086"
  private val EVENT_TYPE = AUTORU_OFFER
  private val OFFER_ID = "1049777022-811fc"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProduction(): Unit = {
    val kmAge = Random.between(1, 100000)

    api.moderation
      .updateKmAge()
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .body(new AutoApiVinConfirmedKmAge().eventType(EVENT_TYPE).id(OFFER_ID).value(kmAge))
      .xUserIdHeader(s"qa_user:$getRandomShortInt")
      .execute(validatedWith(shouldBe200Ok))

    val getReportRequest = api.report.rawReport
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .isPaidQuery(true)

    given
      .conditionEvaluationListener(new AllureConditionEvaluationLogger)
      .pollDelay(Duration.ZERO)
      .pollInterval(1, SECONDS)
      .atMost(60, SECONDS)
      .ignoreExceptions()
      .until(
        () =>
          getReportRequest
            .executeAs(validatedWith(shouldBe200OkJSON()))
            .getReport
            .getAutoruOffers
            .getOffers
            .stream
            .filter(offer => offer.getOfferId == OFFER_ID)
            .findFirst
            .get
            .getMileage
            .toInt,
        equalTo(kmAge)
      )
  }

  @After
  def deleteKmAge(): Unit = {
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
