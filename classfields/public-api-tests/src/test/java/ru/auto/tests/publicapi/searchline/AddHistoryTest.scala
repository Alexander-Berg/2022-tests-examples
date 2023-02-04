package ru.auto.tests.publicapi.searchline

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model.AutoApiSearchCarsSearchRequestParameters.EngineGroupEnum.DIESEL
import ru.auto.tests.publicapi.model.AutoApiSearchCarsSearchRequestParameters.GearTypeEnum.ALL_WHEEL_DRIVE
import ru.auto.tests.publicapi.model.AutoApiSearchCarsSearchRequestParameters.TransmissionEnum.AUTOMATIC
import ru.auto.tests.publicapi.model.{AutoApiSearchCarsSearchRequestParameters, AutoApiSearchSearchRequestParameters}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("POST /searchline/history/{category}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class AddHistoryTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.searchline.addHistorySearchline()
      .categoryPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create
    val sessionId = adaptor.login(account).getSession.getId
    val carsParams = new AutoApiSearchCarsSearchRequestParameters()
      .addTransmissionItem(AUTOMATIC).addGearTypeItem(ALL_WHEEL_DRIVE).addEngineGroupItem(DIESEL)

    api.searchline.addHistorySearchline().reqSpec(defaultSpec)
      .categoryPath(CARS)
      .queryQuery("Дизель полный привод автомат")
      .body(new AutoApiSearchSearchRequestParameters().carsParams(carsParams))
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeSuccess))
  }
}
