package vertis.sraas.model

import com.google.protobuf.Message
import org.scalatest.Assertion
import ru.yandex.vertis.protobuf.ProtoFormat
import vertis.sraas.BaseSpec
import vertis.sraas.generator.Generators._

/** @author neron
  */
class ModelProtoFormatSpec extends BaseSpec with ModelProtoFormats {

  "ModelProtoFormats" should {
    "transform FileDescriptorSetResponse" in {
      forAll(fileDescriptorSetResponseGen[SchemaVersion])(test(_))
    }
    "transform VersionsResponse" in {
      forAll(versionsResponseGen[SchemaVersion])(test(_))
    }
    "transform MessageNamesResponse" in {
      forAll(messageNamesResponseGen[SchemaVersion])(test(_))
    }
  }

  private def test[M, P <: Message](model: M)(implicit c: ProtoFormat[M, P]): Assertion = {
    val actualProto = c.write(model)
    val actualModel = c.read(actualProto)
    actualModel shouldEqual model
    val actualProtoAgain = c.write(actualModel)
    actualProtoAgain shouldEqual actualProto
  }

}
