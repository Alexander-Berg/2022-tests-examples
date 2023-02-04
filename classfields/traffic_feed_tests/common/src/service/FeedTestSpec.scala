package ru.vertistraf.traffic_feed_tests.common.service

import zio.ZIO
import zio.test.{SourceLocation, Spec, TestResult, ZSpec}

trait FeedTestSpec[-R] {

  final def suite[R1, E, T](label: String)(specs: Spec[R1, E, T]*): Spec[R1, E, T] =
    zio.test.suite(label)(specs: _*)

  final def suiteM[R1, E, T](label: String)(specs: ZIO[R1, E, Iterable[Spec[R1, E, T]]]): Spec[R1, E, T] =
    zio.test.suiteM(label)(specs)

  final def test(label: String)(assertion: => TestResult)(implicit loc: SourceLocation): ZSpec[Any, Nothing] =
    zio.test.test(label)(assertion)

  final def testM[R1, E](
      label: String
    )(assertion: => ZIO[R1, E, TestResult]
    )(implicit loc: SourceLocation): ZSpec[R1, E] =
    zio.test.testM(label)(assertion)

  def spec: ZSpec[R, Any]
}
