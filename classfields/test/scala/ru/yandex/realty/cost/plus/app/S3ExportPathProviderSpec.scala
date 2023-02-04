package ru.yandex.realty.cost.plus.app

import org.junit.runner.RunWith
import ru.yandex.realty.service.DeployingService
import ru.yandex.realty.traffic.model.s3.S3Path.S3DirPath
import ru.yandex.realty.traffic.utils.s3.S3PathReaders
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.{NomadRuntimeConfig, RuntimeConfig}
import zio._
import zio.test._
import zio.test.junit._
import eu.timepit.refined.auto._

@RunWith(classOf[ZTestJUnitRunner])
class S3ExportPathProviderSpec extends JUnitRunnableSpec {
  private case class TestCase(
    rootDir: String,
    returningEnv: Environments.Value,
    branch: Option[String],
    expected: S3DirPath
  )

  private val cases: Seq[TestCase] =
    Seq(
      TestCase(
        "bucket:/batches/",
        Environments.Stable,
        None,
        S3DirPath("bucket", Seq("batches", "production", "realty-cost-plus"))
      ),
      TestCase(
        "bucket:/batches/",
        Environments.Stable,
        Some("VERTISTRAF-2458"),
        S3DirPath("bucket", Seq("batches", "branches", "VERTISTRAF-2458", "production", "realty-cost-plus"))
      )
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("S3ExportPathProvider")(
      testM("Correctly provide path") {
        checkAllM(Gen.fromIterable(cases)) {
          testCase =>
            val prefix = ZIO
              .fromTry(
                S3PathReaders.readFromString(testCase.rootDir).collect { case d: S3DirPath => d }
              )
              .orDie
              .toLayer

            val runtimeConfig = ZLayer.succeed[RuntimeConfig] {
              NomadRuntimeConfig(
                testCase.returningEnv,
                "",
                "",
                ""
              )
            }

            val branch = ZLayer.succeed(testCase.branch)

            S3ExportPathProvider.provide
              .provideLayer(prefix ++ (branch ++ runtimeConfig >>> DeployingService.live))
              .map(res => assertTrue(res == testCase.expected))
        }
      }
    )
}
