package ru.yandex.vertis.etc.dust.model

import ru.yandex.vertis.etc.dust.DustError.UnmatchedClusterTypeWithPayload
import zio.test._
import zio.test.Assertion._

object ClusteringResultSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ClusteringResult")(
      testM("ClusteringResult.make should and ClusteringResult.apply should create equal results") {
        val res = ClusteringResult(
          DialogId("dialog_id"),
          ClusterType.CallScenario,
          "domain",
          10,
          ClusterId("cluster"),
          Some(ClusteringResult.CallScenarioPayload(Some(666)))
        )
        ClusteringResult
          .make(
            DialogId("dialog_id"),
            ClusterType.CallScenario,
            "domain",
            10,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
          .map(assert(_)(equalTo(res)))
      },
      testM("ClusteringResult.make should fail if payload does not match cluster type") {
        ClusteringResult
          .make(
            DialogId("dialog_id"),
            ClusterType.CallSpamAutoru,
            "domain",
            10,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
          .flip
          .map { error =>
            assertTrue(
              error == UnmatchedClusterTypeWithPayload(
                ClusterType.CallSpamAutoru,
                ClusteringResult.CallScenarioPayload(Some(666))
              )
            )
          }
      },
      test("ClusteringResult.apply should fail if payload does not match cluster type") {
        val res = scala.util.Try {
          ClusteringResult(
            DialogId("dialog_id"),
            ClusterType.CallSpamAutoru,
            "domain",
            10,
            ClusterId("cluster"),
            Some(ClusteringResult.CallScenarioPayload(Some(666)))
          )
        }
        assert(res)(isFailure)
      }
    )
}
