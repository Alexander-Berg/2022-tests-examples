package auto.dealers.amoyak.logic.managers

import auto.scalapb.InstantProtoConversions._
import cats.syntax.option._
import com.google.protobuf.timestamp.Timestamp
import auto.common.clients.passport.Passport
import auto.common.clients.passport.model.{ClientId, UserId}
import common.zio.clock.MoscowClock
import auto.dealers.amoyak.model.LastSession
import ru.yandex.passport.model.api.api_model.{UserEssentials, UserIdsResult}
import common.zio.sttp.model.SttpError
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation.value
import zio.test.mock.MockClock
import zio.{Has, IO, ULayer, ZIO, ZLayer}

import java.time.{OffsetDateTime, ZoneOffset}

class DummyPassport(
    usersResponse: UserIdsResult,
    essentialsResponse: UserEssentials)
  extends Passport.Service {

  override def getUserEssentials(userId: UserId, loadLastSeen: Boolean): IO[SttpError, UserEssentials] =
    ZIO.succeed(essentialsResponse.withId(userId.toString))

  override def getUsers(clientId: ClientId): IO[SttpError, UserIdsResult] =
    ZIO.succeed(usersResponse)
}

object PassportManagerLiveSpec extends DefaultRunnableSpec {

  def dtToTs(dt: OffsetDateTime): Timestamp = dt.toInstant.asProtoTimestamp

  val now = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val email = "test@test.com"
  val clockMock = MockClock.CurrentDateTime(value(now)).optional
  val usersResponse = UserIdsResult(userIds = Seq("1", "2"))
  val essentialsResponse = UserEssentials(email = email, lastSeen = dtToTs(now).some)

  val testLastSession = testM("test last_session construction") {
    val recentTime = now.minusMinutes(2)
    val recentEssentials = essentialsResponse.withLastSeen(dtToTs(recentTime))

    val dummyPassport: ULayer[Has[Passport.Service]] =
      ZLayer.succeed(new DummyPassport(usersResponse, recentEssentials))

    val manager = (dummyPassport ++ clockMock) >>> PassportManager.live
    val expectedLastSession =
      LastSession(
        clientLogin = email.some,
        isOnline = true.some,
        time = MoscowClock.asMoscowTime(recentTime).some
      ).some
    (for {
      lastSeen <- PassportManager.getLastSession(1L)
    } yield assert(lastSeen)(equalTo(expectedLastSession))).provideCustomLayer(manager)
  }

  override def spec = suite("PassportManagerLiveSpec")(testLastSession)
}
