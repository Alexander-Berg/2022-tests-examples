package ru.auto.tests.ra

import io.restassured.builder.RequestSpecBuilder
import java.util.function.Consumer

object RequestSpecBuilders {

  def defaultSpec: Consumer[RequestSpecBuilder] =
    (req: RequestSpecBuilder) => req

}
