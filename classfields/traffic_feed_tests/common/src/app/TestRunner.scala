package ru.vertistraf.traffic_feed_tests.common.app

import common.zio.logging.Logging
import ru.vertistraf.traffic_feed_tests.common.model.TestReport
import ru.vertistraf.traffic_feed_tests.common.service.{FeedTestSpec, TestReportBuilder}
import zio._
import zio.test.{Annotations, ExecutedSpec, Spec, TestAnnotationMap, TestFailure}

object TestRunner extends Logging {

  def run[R](tests: Seq[FeedTestSpec[R]]): RIO[R with Annotations, Seq[TestReport]] =
    ZLayer.requires[R with Annotations].memoize.use { execLayer =>
      ZIO
        .foreach(tests) { spec =>
          runSingleSpec(ExecutionStrategy.ParallelN(4))(spec).map { result =>
            TestReportBuilder.build(result)
          }
        }
        .provideLayer(execLayer)
    }

  private def runSingleSpec[R](defExec: ExecutionStrategy)(testSpec: FeedTestSpec[R]) = {
    val executeManaged = testSpec.spec.annotated
      .foreachExec(defExec)(
        e =>
          e.failureOrCause.fold(
            { case (failure, annotations) => ZIO.succeed((Left(failure), annotations)) },
            cause => ZIO.succeed((Left(TestFailure.Runtime(cause)), TestAnnotationMap.empty))
          ),
        { case (success, annotations) =>
          ZIO.succeed((Right(success), annotations))
        }
      )

    executeManaged.use(_.foldM[R with Annotations, Nothing, ExecutedSpec[Any]](defExec) {
      case Spec.ExecCase(_, spec) =>
        ZManaged.succeed(spec)
      case Spec.LabeledCase(label, spec) =>
        ZManaged.succeed(ExecutedSpec.labeled(label, spec))
      case Spec.ManagedCase(managed) =>
        managed
      case Spec.MultipleCase(specs) =>
        ZManaged.succeed(ExecutedSpec.multiple(specs))
      case Spec.TestCase(test, staticAnnotations) =>
        test.map { case (result, dynamicAnnotations) =>
          ExecutedSpec.test(result, staticAnnotations ++ dynamicAnnotations)
        }.toManaged_
    }.useNow)
  }
}
