package ru.yandex.vertis.chat.simulations

import java.lang.System.currentTimeMillis

import com.google.protobuf.util.JsonFormat
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import ru.yandex.vertis.chat.model.RoomId
import ru.yandex.vertis.chat.model.api.ApiModel.Room
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.ServiceProtoFormats.CreateRoomParametersFormat
import ru.yandex.vertis.chat.util.logging.Logging

import scala.concurrent.duration._

object RoomActions extends Logging {

  val CreateRoom =
    doIf(session => isCreationAllowed(session)) {
      exec(
        http("create_room")
          .post("rooms")
          .withUser()
          .withTrace()
          .body(StringBody(roomParameters()))
          .check(
            bodyString
              .transform(v => parseCreateRoomResponse(v))
              .transform(room => room.getId)
              .saveAs(RoomKey)
          )
      ).exec(session => saveRoomId(session))
        .exec(session => session.set(LastRoomTsKey, currentTimeMillis()))
    }

  val GetRooms = exec(
    http("get_rooms")
      .get(s"rooms?user_id=$UserEl")
      .withUser()
      .withTrace()
  )

  val GetUnreadRooms = exec(
    http("get_unread")
      .get(s"rooms/unread?user_id=$UserEl")
      .withUser()
      .withTrace()
  )

  private val RoomCreationInterval = 1.minute.toMillis

  private def isCreationAllowed(session: Session): Boolean = {
    session.attributes
      .get(LastRoomTsKey)
      .map(_.asInstanceOf[Long])
      .forall(_ < currentTimeMillis() - RoomCreationInterval)
  }

  private def parseCreateRoomResponse(value: String): Room = {
    val builder = Room.newBuilder()
    JsonFormat.parser().merge(value, builder)
    builder.build()
  }

  private def saveRoomId(session: Session): Session = {
    session.attributes
      .get(RoomKey)
      .map(_.asInstanceOf[String])
      .map(id => {
        session.set(
          RoomsKey,
          session.attributes
            .get(RoomsKey)
            .map(_.asInstanceOf[Vector[RoomId]]) match {
            case Some(rooms) =>
              rooms :+ id
            case None =>
              Vector(id)
          }
        )
      })
      .getOrElse(session)
  }

  private def roomParameters(): String = {
    val parameters = createRoomParameters.next
      .withUserId(UserEl)
      .copy(id = None)
    val message = CreateRoomParametersFormat.write(parameters)
    JsonFormat.printer().omittingInsignificantWhitespace().print(message)
  }
}
