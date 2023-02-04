package ru.vertistraf.traffic_feed_tests.common.model

import ru.vertistraf.traffic_feed_tests.common.model.TestReport.TestStatus.TestStatus

sealed trait TestReport

object TestReport {

  case class Labeled(name: String, report: TestReport) extends TestReport

  case class GroupReport(
      elements: Seq[TestReport])
    extends TestReport

  object TestStatus extends Enumeration {
    type TestStatus = Value

    val Success, Failed, Ignored, RuntimeFailed = Value
  }

  case class SingleTestReport(status: TestStatus, info: Seq[String] = Seq.empty) extends TestReport

}
