package ru.auto.tests.ra

import io.restassured.builder.RequestSpecBuilder
import java.util.function.Consumer

import io.restassured.http.ContentType

object RequestSpecBuilders {

  def defaultSpec: Consumer[RequestSpecBuilder] =
    (req: RequestSpecBuilder) => req

  def acceptText: Consumer[RequestSpecBuilder] = { (req: RequestSpecBuilder) =>
    req.setAccept(ContentType.TEXT)
  }
}
