package bootstrap.testcontainers

import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType.*

import java.util.function.Consumer

case object LogConsumer extends Consumer[OutputFrame] {
  private val unsafeLog = LoggerFactory.getLogger("testcontainers")

  override def accept(frame: OutputFrame): Unit =
    frame.getType match {
      case STDERR =>
        unsafeLog.warn(frame.getUtf8String.stripTrailing())
      case STDOUT =>
        unsafeLog.info(frame.getUtf8String.stripTrailing())
      case END =>
    }

}
