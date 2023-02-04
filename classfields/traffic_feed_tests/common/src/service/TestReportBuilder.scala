package ru.vertistraf.traffic_feed_tests.common.service

import ru.vertistraf.traffic_feed_tests.common.model.TestReport
import ru.vertistraf.traffic_feed_tests.common.model.TestReport.TestStatus
import zio.test.FailureRenderer.FailureMessage.Message
import zio.test.{
  AssertionResult,
  BoolAlgebra,
  ExecutedSpec,
  FailureRenderer,
  TestFailure,
  TestResult,
  TestSuccess,
  Trace
}

object TestReportBuilder {

  def build(executed: ExecutedSpec[Any]): TestReport =
    executed.caseValue match {
      case ExecutedSpec.MultipleCase(specs) =>
        TestReport.GroupReport(specs.map(build))
      case ExecutedSpec.LabeledCase(label, spec) =>
        TestReport.Labeled(label, build(spec))
      case ExecutedSpec.TestCase(test, _) =>
        test match {
          case Left(failure) => buildFailure(failure)
          case Right(value) => buildSuccess(value)
        }
    }

  private def buildFailure(testFailure: TestFailure[Any]): TestReport =
    testFailure match {
      case TestFailure.Assertion(result) =>
        TestReport.SingleTestReport(
          TestStatus.Failed,
          normalizeFailure(result).map {
            _.fold { f =>
              asLines(FailureRenderer.renderAssertionResult(f, offset = 0)).mkString(" ")
            }(_ + " && " + _, _ + " || " + _, "!(" + _ + ")")
          }.toSeq
        )
      case TestFailure.Runtime(cause) =>
        TestReport.SingleTestReport(
          TestStatus.RuntimeFailed,
          asLines(
            FailureRenderer
              .renderCause(cause, offset = 0)
          )
        )
    }

  private def buildSuccess(succeed: TestSuccess): TestReport =
    succeed match {
      case TestSuccess.Succeeded(_) => TestReport.SingleTestReport(TestStatus.Success)
      case TestSuccess.Ignored => TestReport.SingleTestReport(TestStatus.Ignored)
    }

  private def asLines(m: Message): Seq[String] = m.lines
    .map(_.fragments.map(_.text).mkString(""))

  private def normalizeFailure(result: TestResult): Option[TestResult] =
    result
      .fold[Option[TestResult]] {
        case result: AssertionResult.FailureDetailsResult => Some(BoolAlgebra.success(result))
        case AssertionResult.TraceResult(trace, genFailureDetails) =>
          Trace
            .prune(trace, negated = false)
            .map(a => BoolAlgebra.success(AssertionResult.TraceResult(a, genFailureDetails)))
      }(
        {
          case (Some(a), Some(b)) => Some(a && b)
          case (Some(a), None) => Some(a)
          case (None, Some(b)) => Some(b)
          case _ => None
        },
        {
          case (Some(a), Some(b)) => Some(a || b)
          case (Some(a), None) => Some(a)
          case (None, Some(b)) => Some(b)
          case _ => None
        },
        _.map(!_)
      )
}
