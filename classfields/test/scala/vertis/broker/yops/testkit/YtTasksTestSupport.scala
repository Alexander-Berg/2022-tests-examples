package vertis.broker.yops.testkit

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import common.yt.Yt.{Attribute, Attributes}
import common.yt.schema.YtTypes
import org.scalacheck.Gen
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.yt.model.attributes.YtAttribute
import vertis.yt.model.{YtColumn, YtSchema, YtTable}
import vertis.yt.zio.Aliases.YtTask
import vertis.yt.zio.wrappers.YtZio

import java.time.{Instant, LocalDate}

/** @author kusaeva
  */
trait YtTasksTestSupport {

  def basePath: YPath

  def createDayTable(name: String, day: LocalDate, attrs: Seq[YtAttribute]): YtTable = {
    val dayStr = day.toString
    val path = basePath.child(name).child(dayStr)
    YtTable(
      dayStr,
      path,
      YtSchema(Seq(YtColumn("user", YtTypes.string))),
      expiration = None,
      attributes = attrs
    )
  }

  protected def messagesGen(descriptor: Descriptor): Gen[DynamicMessage] = RandomProtobufGenerator
    .genFor(DynamicMessage.getDefaultInstance(descriptor))
    .map(msg => DynamicMessage.parseFrom(descriptor, msg.toByteArray))
}

object YtTasksTestSupport {

  val YtTouchAttribute: Attribute[Instant] = Attribute[Instant]("touch_time")

  def getAllAttributesMap(yt: YtZio, path: YPath): YtTask[Attributes] =
    yt.cypressNoTx.listAllAttributes(path) >>= { names =>
      yt.cypressNoTx.getAttributes(path, names)
    }
}
