package ru.vertistraf.traffic_feed_tests.common.app

import ru.vertistraf.traffic_feed_tests.common.model.TestReport
import ru.vertistraf.traffic_feed_tests.common.model.TestReport.TestStatus
import ru.vertistraf.traffic_feed_tests.common.service.FeedTestSpec
import zio._
import zio.test._

import java.util.concurrent.atomic.AtomicInteger

object TestRunnerSpec extends DefaultRunnableSpec {
  private val Expected: Int = 1
  private val Ref: AtomicInteger = new AtomicInteger(Expected)

  private def expectedReportRow(i: Int) =
    TestReport.Labeled(
      s"Should use calculated once value $i",
      TestReport.GroupReport(
        Seq(TestReport.Labeled("check", TestReport.SingleTestReport(status = TestStatus.Success, info = Seq.empty)))
      )
    )

  private val ExpectedReport: Seq[TestReport] =
    (1 to 4).map(expectedReportRow)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TestRunner")(
      testM("Should only once calculate env") {
        ru.vertistraf.traffic_feed_tests.common.app.TestRunner
          .run(
            Seq(
              Stub1,
              Stub2,
              Stub3,
              Stub4
            )
          )
          .provideLayer(resources ++ Annotations.live)
          .map(res => assertTrue(res == ExpectedReport))
      }
    )

  private lazy val resources: ULayer[Has[Int]] = UIO.effectTotal(Ref.getAndIncrement()).toLayer

  abstract class StubTest(i: Int) extends FeedTestSpec[Has[Int]] {

    final override def spec: ZSpec[Has[Int], Any] = suite(s"Should use calculated once value $i")(
      this.testM("check") {
        ZIO.service[Int].map(x => assertTrue(x == Expected))
      }
    )
  }

  object Stub1 extends StubTest(1)
  object Stub2 extends StubTest(2)
  object Stub3 extends StubTest(3)
  object Stub4 extends StubTest(4)
}
