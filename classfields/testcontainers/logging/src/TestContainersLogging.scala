package vertis.zio.logging

import common.zio.logging.SyncLogger

import java.util.function.Consumer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType

/**
  */
object TestContainersLogging {

  def toLogConsumer(syncLogger: SyncLogger): Consumer[OutputFrame] =
    (t: OutputFrame) =>
      t.getType match {
        case OutputType.STDOUT => syncLogger.info(t.getUtf8String)
        case OutputType.STDERR => syncLogger.error(t.getUtf8String)
        case OutputType.END => // what is that?
      }

}
