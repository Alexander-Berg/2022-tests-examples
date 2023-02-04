package ru.auto.tests.ra

import io.restassured.builder.ResponseSpecBuilder
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.hamcrest.CoreMatchers.equalTo
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode}

object ResponseSpecBuilders {

  def shouldBeEmptyJson: ResponseSpecBuilder =
    shouldBe200OkJSON.expectBody(equalTo("{}"))

}
