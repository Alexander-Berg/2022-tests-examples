package ru.auto.tests.hypothesis

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.junit.Assert.assertEquals
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /hypothesis")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetHypothesisTest {

  val VIN = "Z94CC41BBER184593"
  val MARK = "HYUNDAI"
  val MODEL = "SOLARIS"
  val INVALID_MARK = "KIA"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSeeOkWithCorrectMarkAndModel(): Unit = {
    val response = api.hypothesis.hypothesis
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .markQuery(MARK)
      .modelQuery(MODEL)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    assertEquals(response.get("result").getAsString, "OK")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeWmiGenWithAnotherMark(): Unit = {
    val response = api.hypothesis.hypothesis
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .markQuery(INVALID_MARK)
      .modelQuery(MODEL)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    assertEquals(response.get("result").getAsString, "WMI_GEN")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeStolenVinWithInvalidMark(): Unit = {
    val response = api.hypothesis.hypothesis
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .markQuery(getRandomString)
      .modelQuery(MODEL)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    assertEquals(response.get("result").getAsString, "STOLEN_VIN")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeCdCheckWithInvalidVin(): Unit = {
    val response = api.hypothesis.hypothesis
      .reqSpec(defaultSpec)
      .vinQuery(getRandomString)
      .markQuery(MARK)
      .modelQuery(MODEL)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    assertEquals(response.get("result").getAsString, "CD_CHECK")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeWmiGenWithInvalidModel(): Unit = {
    val response = api.hypothesis.hypothesis
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .markQuery(MARK)
      .modelQuery(getRandomString)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    assertEquals(response.get("result").getAsString, "WMI_GEN")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithoutParams(): Unit = {
    api.hypothesis.hypothesis
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }
}
