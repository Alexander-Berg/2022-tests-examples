package common.id

import common.id.{IdGenerator, SnowflakeIdGenerator}

import java.time.ZoneOffset
import zio.ZIO
import zio.clock.Clock
import zio.duration._
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock

object IdGeneratorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("IdGenerator")(
      testM("should produce sequential ids on one machine within one millisecond") {
        for {
          _ <- TestClock.setDateTime(SnowflakeIdGenerator.DefaultEpoch.plus(1.day).atOffset(ZoneOffset.UTC))
          id1 <- IdGenerator.nextId
          id2 <- IdGenerator.nextId
        } yield assert(id2 - id1)(equalTo(1L))
      },
      testM("should produce non-sequential ids on one machine within one millisecond") {
        for {
          _ <- TestClock.setDateTime(SnowflakeIdGenerator.DefaultEpoch.plus(1.day).atOffset(ZoneOffset.UTC))
          id1 <- IdGenerator.nextId
          _ <- TestClock.adjust(1.millisecond)
          id2 <- IdGenerator.nextId
        } yield assert(id2 - id1)(isGreaterThan(1L))
      },
      testM("Генерация в несколько потоков") {
        for {
          idGenerator <- (Clock.live ++ Random.live >>> IdGenerator.snowflake).build.useNow
          ids <- ZIO.foreachPar(1 to 100)(_ => idGenerator.get.nextId)
        } yield assert(ids.distinct)(hasSize(equalTo(ids.size)))
      }
    ).provideCustomLayer(IdGenerator.snowflake)
  }
}
