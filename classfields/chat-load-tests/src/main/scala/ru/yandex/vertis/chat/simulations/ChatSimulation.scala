package ru.yandex.vertis.chat.simulations

import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import scala.concurrent.duration._

class ChatSimulation extends Simulation with HttpConfig {

  val scn = scenario("chat")
    .exec(withUserSession())
    .exec(RoomActions.CreateRoom)
    .pause(5)
    .repeat(_ => ThreadLocalRandom.current().nextInt(5, 25)) {
      randomSwitchOrElse(
        (5, exec(RoomActions.CreateRoom)),
        (10, exec(RoomActions.GetRooms)),
        (10, exec(MessageActions.HasUnread)),
        (10, exec(MessageActions.MarkRead)),
        (10, exec(MessageActions.GetMessages))
      ) {
        exec(MessageActions.SendMessage)
      }.pause(10)
    }

  // With scenario above each user per second leads to
  // (roughly) 100 users on-line (who sent some requests in the past
  // and will send more in future)
  setUp(
    scn
      .inject((rampUsersPerSec(1) to 10).during(1.minute), constantUsersPerSec(10).during(5.minutes))
      .protocols(httpConf)
  )

}
