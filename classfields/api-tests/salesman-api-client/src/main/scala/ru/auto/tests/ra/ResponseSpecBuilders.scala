package ru.auto.tests.ra

import io.restassured.builder.ResponseSpecBuilder
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.hamcrest.CoreMatchers.equalTo
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{
  shouldBe200OkJSON,
  shouldBeCode
}

object ResponseSpecBuilders {

  def shouldBe400WithMissedSalesmanUser: ResponseSpecBuilder =
    shouldBeCode(SC_BAD_REQUEST).expectBody(
      equalTo("Request is missing required HTTP header 'X-Salesman-User'")
    )

  def shouldBeEmptyJson: ResponseSpecBuilder =
    shouldBe200OkJSON.expectBody(equalTo("{}"))

  def shouldBe200WithMessageOK: ResponseSpecBuilder =
    shouldBe200OkJSON.expectBody("message", equalTo("OK"))
}
