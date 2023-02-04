package vertis.anubis.api.test

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.Descriptor
import ru.yandex.vertis.broker.BrokerOptions
import ru.yandex.vertis.palma.PalmaOptions
import ru.yandex.vertis.protobuf.Options
import vertis.anubis.api.services.validate.World
import vertis.zio.test.ZioSpecBase

import scala.jdk.CollectionConverters._

/** @author kusaeva
  */
trait ValidationTestSupport extends ValidationSpecBase { this: ZioSpecBase =>

  private val options = Seq(
    BrokerOptions.getDescriptor,
    PalmaOptions.getDescriptor,
    Options.getDescriptor
  )

  protected val defaultDescriptors = Seq(
    IllegalPartitioningMessage.getDescriptor, // test.proto
    InvalidMessage.getDescriptor, // another_test.proto
    NoProtoPackageBrokerMessage.getDescriptor // no_proto_package.proto
  )

  protected def fds(descriptors: Seq[Descriptor]): FileDescriptorSet = {
    val files = (descriptors ++ options).map(_.getFile)
    val deps = files.flatMap(_.getDependencies.asScala)
    val protoFiles = (files ++ deps).map(_.toProto)
    FileDescriptorSet
      .newBuilder()
      .addAllFile(protoFiles.asJava)
      .build()
  }

  private val emptyFds = FileDescriptorSet.newBuilder().build()
  val world = createWorld(defaultDescriptors)

  protected def createWorld(descriptors: Seq[Descriptor], master: FileDescriptorSet = emptyFds) =
    new World(master, fds(descriptors))
}
