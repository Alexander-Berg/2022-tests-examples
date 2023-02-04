package vertis.proto.converter

import vertis.proto.converter.test.NotAlwaysAllowRecursion
import vertis.proto.converter.yt.ProtoColumn
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class YtColumnProtoDescriptorSpec extends ZioSpecBase {

  "YtColumnProtoDescriptor" should {
    "set ignoreRecursionInProto if field allows recursion" in {
      val fd = NotAlwaysAllowRecursion.javaDescriptor.findFieldByName("allow")
      val column = ProtoColumn(fd)
      column.toColumn.ignoreRecursionInProto shouldBe true
    }
    "not set ignoreRecursionInProto if field disallows recursion" in {
      val fd = NotAlwaysAllowRecursion.javaDescriptor.findFieldByName("not_allow")
      val column = ProtoColumn(fd)
      column.toColumn.ignoreRecursionInProto shouldBe false
    }
  }
}
