package ru.auto.tests.navigator

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.adaptor.RecallApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{
  shouldBe200Ok,
  shouldBe200OkJSON,
  shouldBeCode,
  validatedWith
}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("DELETE /navigator/vehicle/{vehicle_id}")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteVehicleTest {

  private val VIN_CODE_ID = 23706

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: RecallApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldAddVehicle(): Unit = {
    val userId = getRandomString
    val vehicleId = adaptor.addVehicle(VIN_CODE_ID, userId).getVehicle.getId

    api.navigator
      .hideVehicle()
      .reqSpec(defaultSpec)
      .vehicleIdPath(vehicleId)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBe200Ok))
  }
}
