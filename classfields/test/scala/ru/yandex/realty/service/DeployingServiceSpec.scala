package ru.yandex.realty.service

import org.junit.runner.RunWith
import ru.yandex.realty.service.DeployingService.{DeployingService, SuffixFormat}
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.{RuntimeConfig, RuntimeConfigImpl}
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.{ZIO, ZLayer}

@RunWith(classOf[ZTestJUnitRunner])
class DeployingServiceSpec extends JUnitRunnableSpec {

  private def serviceLayer(env: Environments.Value, branch: Option[String]) = {
    val runtimeConfig: RuntimeConfig = RuntimeConfigImpl(env, "", "", Deploys.Container, None)

    ZLayer.succeed(runtimeConfig) ++ ZLayer.succeed(branch) >>> DeployingService.live
  }

  private def check(env: Environments.Value, branch: Option[String])(prefix: String, suffix: SuffixFormat)(
    expected: String
  ) =
    testM(s"correctly return for prefix `$prefix`, env $env, branch: ${branch.orNull} for $suffix") {
      ZIO
        .accessM[DeployingService] {
          _.get.addEnvDependSuffix(prefix, suffix)
        }
        .map(assert(_)(equalTo(expected)))
        .provideLayer(serviceLayer(env, branch))
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DeployingService")(
      check(Environments.Stable, None)("data", SuffixFormat.EnvOnly)("data_production"),
      check(Environments.Stable, None)("data", SuffixFormat.BranchOnly)("data"),
      check(Environments.Stable, None)("data", SuffixFormat.EnvWithBranch)("data_production"),
      check(Environments.Stable, Some("VERTISTRAF-2324"))("data", SuffixFormat.EnvOnly)("data_production"),
      check(Environments.Stable, Some("VERTISTRAF-2324"))("data", SuffixFormat.BranchOnly)("data_VERTISTRAF-2324"),
      check(Environments.Stable, Some("VERTISTRAF-2324"))("data", SuffixFormat.EnvWithBranch)(
        "data_production_VERTISTRAF-2324"
      ),
      check(Environments.Stable, Some("VERTISTRAF-2324"))("", SuffixFormat.EnvOnly)("production"),
      check(Environments.Stable, Some("VERTISTRAF-2324"))("", SuffixFormat.BranchOnly)("VERTISTRAF-2324"),
      check(Environments.Stable, Some("VERTISTRAF-2324"))("", SuffixFormat.EnvWithBranch)(
        "production_VERTISTRAF-2324"
      )
    )
}
