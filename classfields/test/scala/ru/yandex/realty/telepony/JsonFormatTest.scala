package ru.yandex.realty.telepony

import java.util.UUID

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import ru.yandex.realty.model.phone.PhoneRedirect
import ru.yandex.realty.model.phone.PhoneType
import ru.yandex.realty.telepony.JsonFormats._

import scala.concurrent.duration._

class JsonFormatTest extends FlatSpec with Matchers with ScalaFutures {

  it should "write serialize PhoneRedirect to json correctly" in {
    val redirect =
      PhoneRedirect(
        domain = TeleponyClient.Domain.`realty-offers`,
        id = UUID.randomUUID().toString,
        objectId = UUID.randomUUID().toString,
        tag = Some("maps"),
        createTime = DateTime.now(),
        deadline = Some(DateTime.now()),
        source = "+78123334455",
        target = "+78002000500",
        phoneType = Some(PhoneType.Local),
        geoId = Some(1),
        ttl = Some(3.seconds)
      )

    val str = Json.toJson(redirect).toString()

    str.nonEmpty shouldEqual true

  }

}
