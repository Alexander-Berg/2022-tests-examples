package ru.yandex.vertis.chat

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import ru.yandex.vertis.generators.ProducerProvider

//noinspection ScalaStyle
// scalastyle:off
package object simulations extends ProducerProvider {

  val UserKey = "user"
  val UserEl = "${" + UserKey + "}"
  val RoomKey = "room"
  val RoomEl = "${" + RoomKey + "}"
  val RoomsKey = "rooms"
  val RoomsEl = "${" + RoomsKey + "}"
  val RandomRoomEl = "${" + RoomsKey + ".random()}"
  val AnyRoomExistsEl = "${" + RoomsKey + ".size()}"
  val LastRoomTsKey = "last_room_ts"

  def withUserSession(): ChainBuilder = {
    exec { session =>
      session.set(UserKey, ThreadLocalRandom.current().nextLong(10000, 1000000))
    }
  }

  implicit class RichRequestBuilder(builder: HttpRequestBuilder) {

    def withUser(): HttpRequestBuilder = {
      builder
        .header("X-User-ID", UserEl)
        .header("X-Passport-User-ID", UserEl)
    }

    def withTrace(): HttpRequestBuilder = {
      builder.header("x-request-id", _ => UUID.randomUUID().toString)
    }
  }

}

// scalastyle:on
