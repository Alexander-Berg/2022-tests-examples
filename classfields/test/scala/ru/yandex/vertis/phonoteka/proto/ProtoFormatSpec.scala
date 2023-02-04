package ru.yandex.vertis.phonoteka.proto

import cats.syntax.validated._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalacheck.Arbitrary
import org.scalatest.Assertion
import ru.yandex.vertis.phonoteka.model.Arbitraries._
import ru.yandex.vertis.phonoteka.model.api.{ApiRequest, ApiResponse}
import ru.yandex.vertis.phonoteka.model.metadata.{Metadata, MetadataType}
import ru.yandex.vertis.phonoteka.proto.{model => proto}
import ru.yandex.vertis.quality.scalapb_utils.ProtoJson._
import ru.yandex.vertis.quality.scalapb_utils._
import ru.yandex.vertis.quality.test_utils.SpecBase
import scalapb.GeneratedEnum

import scala.reflect.ClassTag

class ProtoFormatSpec extends SpecBase {

  private val testCases: Seq[TestCase] = {
    import ProtoFormatInstances._
    Seq(
      MessageTestCase[Metadata, proto.Metadata],
      MessageTestCase[ApiResponse.GetMetadata, proto.Api.GetMetadataResponse],
      MessageTestCase[ApiRequest.GetMetadata, proto.Api.GetMetadataRequest],
      EnumTestCase[MetadataType, proto.MetadataType]
    )
  }

  "ProtoFormat" should {
    testCases.foreach { testCase =>
      testCase.description in {
        testCase.check()
      }
    }
  }

  sealed trait TestCase {
    def description: String
    def check(): Assertion
  }

  private case class MessageTestCase[T, M <: ProtoMessage[M]]()(
      implicit val format: ProtoMessageFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T]
  ) extends TestCase {

    def description: String = s"convert proto msg $classTag"

    def check(): Assertion = {
      forAll { x: T =>
        format.fromProto(format.toProto(x)) shouldBe x.valid
      }
      forAll { x: T =>
        decode(x.asJson.toString) shouldBe Right(x)
      }
    }
  }

  case class EnumTestCase[T, M <: GeneratedEnum]()(
      implicit val format: ProtoEnumFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T]
  ) extends TestCase {

    def description: String = s"convert proto enum $classTag"

    def check(): Assertion = {

      forAll { x: T =>
        format.fromProto(format.toProto(x)) shouldBe x.valid
      }
    }
  }
}
