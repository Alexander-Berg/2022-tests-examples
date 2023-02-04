package vertis.anubis.api.test

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.Descriptor
import remove.me.by.code.NoPackage.NoPackageMessage
import common.protobuf.ProtoFdsBuilder.buildDescriptorMap
import vertis.anubis.api.services.validate.World
import vertis.anubis.api.services.validate.errors.BasicValidationError.NoJavaPackage
import vertis.anubis.api.services.validate.validators.basic.BasicValidator
import vertis.anubis.api.test.NoJavaPackage.NoJavaPackageMessage
import vertis.zio.test.ZioSpecBase

/** @author reimai
  */
class BasicValidatorSpec extends ZioSpecBase with ValidationTestSupport {
  private val validator = BasicValidator

  "BasicValidator" should {
    Iterator(NoProtoPackageBrokerMessage.getDescriptor, NoProtoPackageMessage.getDescriptor).foreach { descriptor =>
      s"allow messages without a proto package, but java_package specified, e.g. ${descriptor.getName}" in ioTest {
        checkSucceed(validator, descriptor)
      }
    }

    "allow a missing explicit java package if proto package is set" in ioTest {
      checkSucceed(validator, NoJavaPackageMessage.getDescriptor)
    }

    "fail on a completely misssing java package" in ioTest {
      val noPackageDescriptor = buildNoPackageDescriptor
      checkFail(
        validator,
        noPackageDescriptor,
        List(NoJavaPackage(noPackageDescriptor))
      )
    }
  }

  private def buildNoPackageDescriptor: Descriptor = {
    val sourceDesc = NoPackageMessage.getDescriptor
    val fdp = sourceDesc.getFile.toProto.toBuilder.clearPackage().build()
    val fds = FileDescriptorSet.newBuilder().addFile(fdp).build()
    buildDescriptorMap(fds)(sourceDesc.getName)
  }
}
