package common.zio.schedule

import zio.test.Assertion._
import zio.test._
import zio.{Schedule, UIO, ZIO}

/**
  */
object SchedulesSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("foldM")(
      zio.test.testM("simply work") {
        val schedule = Schedules.unfoldM(0)(v => UIO(v + 2)) && Schedule.recurs(3)
        assertM(ZIO.unit.repeat(schedule))(equalTo(8 -> 3L))
      }
    )
}
