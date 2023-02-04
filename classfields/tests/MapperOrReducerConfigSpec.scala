package common.yt.tests

import com.typesafe.config.ConfigFactory
import common.yt.operations_sugar.MapperOrReducerConfig
import common.zio.config.Configuration
import common.zio.pureconfig.Pureconfig
import ru.yandex.inside.yt.kosher.common.{DataSize, JavaOptions}
import zio._
import zio.test._
import zio.test.TestAspect.ignore

object MapperOrReducerConfigSpec extends DefaultRunnableSpec {

  private val fullCfg: MapperOrReducerConfig =
    MapperOrReducerConfig(
      memoryLimit = DataSize.fromGigaBytes(20),
      memoryReserveFactor = Some(1),
      javaOptions = JavaOptions.empty().withXmx(DataSize.fromGigaBytes(10)),
      env = Map("P1" -> "V1", "P2" -> "V2")
    )

  private def runSpec(namespace: String, expected: MapperOrReducerConfig) =
    testM(s"should correctly read '$namespace'") {
      (Configuration.load(ConfigFactory.load("mapper-or-reducer.conf")).map(Has(_)) >>>
        Pureconfig.load[MapperOrReducerConfig](namespace)).map(actual => assertTrue(actual == expected))
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("MapperOrReducerConfig")(
      runSpec("conf-with-env", fullCfg),
      runSpec("conf-without-env", fullCfg.copy(env = Map.empty))
    ) @@ ignore // no equality for DataSize
}
