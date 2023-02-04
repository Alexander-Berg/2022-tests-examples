package auto.carfax.pro_auto.core.src.testkit

import auto.carfax.common.utils.logging.Logging
import auto.carfax.common.utils.tracing.Traced
import com.dimafeng.testcontainers.ForAllTestContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType
import ru.yandex.vertis.ydb.YdbContainer

trait YdbContainerKit extends Logging { this: ForAllTestContainer =>

  /* same port as in docker-compose.yml */
  private val ydbPort = 2135

  lazy val container: YdbContainer = {
    val c = YdbContainer.stable
    c.container.withExposedPorts(ydbPort)
    c.container.withLogConsumer((t: OutputFrame) =>
      t.getType match {
        case OutputType.STDOUT => log.info(t.getUtf8String)(Traced.empty)
        case OutputType.STDERR => log.error(t.getUtf8String)(Traced.empty)
        case OutputType.END => // what is that?
      }
    )
    c.container.start()
    c
  }

}
