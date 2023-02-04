package vertis.broker.yops.tasks.repartition

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.{Descriptors, DynamicMessage, Message}
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import ru.yandex.vertis.proto.util.scalaPb.ScalaPbHelp
import vertis.broker.yops.tasks.repartition.model.StreamRepartitionConfig
import vertis.broker.yops.tasks.repartition.targeting.RepartitionMrTargeting
import vertis.proto.converter.YtTableTestHelper
import vertis.proto.converter.test.SimpleTableMessage
import vertis.yt.zio.YtZioTest

import java.time.Instant

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait RepartitionSpec extends YtZioTest with YtTableTestHelper {

  protected val descriptor: Descriptors.Descriptor = SimpleTableMessage.javaDescriptor

  protected def messageForTs(ts: Instant): DynamicMessage =
    ScalaPbHelp.toDynamic(
      SimpleTableMessage.of(timestamp = Some(Timestamp.of(ts.getEpochSecond, ts.getNano)), key = "")
    )

  protected val streamConfig: StreamRepartitionConfig = StreamRepartitionConfig(
    "too",
    7,
    "timestamp",
    PartitionPeriods.byDay,
    tmpPath,
    tmpPath,
    "things"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    ioTest {
      ytZio.use { yt =>
        yt.filesNoTx.write(
          tmpPath.child(RepartitionMrTargeting.findScript),
          getClass.getResourceAsStream("/jobs/find_row_ranges.py")
        ) *>
          yt.filesNoTx.write(
            tmpPath.child(YtRepartition.splitScript),
            getClass.getResourceAsStream("/jobs/split_to_days.py")
          )
      }
    }
  }
}
