package common.yt.proto.tests

import common.yt.proto.tests.{Diversity, Outer, Recursive}
import common.yt.proto.tests.Outer.Inner
import common.yt.proto.tests.Outer.Inner.WeNeedToGoDeeper
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import org.scalatest.Inspectors.forAll

import common.yt.proto.YqlProtoUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** @author Ratskevich Natalia reimai@yandex-team.ru
 */
class YqlProtoUtilsSpec extends AnyWordSpec with Matchers {

  private val testDescriptors = Seq(
    Diversity.javaDescriptor,
    Outer.javaDescriptor,
    Inner.javaDescriptor,
    WeNeedToGoDeeper.javaDescriptor,
    Recursive.javaDescriptor
  )

  forAll(testDescriptors) { (descriptor: Descriptor) =>
    s"${descriptor.getName} should be encoded and decoded" in {
      val encoded = YqlProtoUtils.encode(descriptor)
      val decoded = YqlProtoUtils.decode(encoded, descriptor.getFullName)
      decoded.toProto shouldBe descriptor.toProto
    }
  }
}
