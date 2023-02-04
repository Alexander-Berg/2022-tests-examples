package ru.yandex.vertis.vsquality.callgate.converters

import java.time.Instant
import com.google.protobuf.duration.{Duration => ProtoDuration}
import com.google.protobuf.timestamp.{Timestamp => ProtoTimestamp}
import io.circe.parser.decode
import io.circe.syntax._
import org.scalacheck.Test.Parameters
import org.scalacheck.{Arbitrary, Prop}
import org.scalactic.Prettifier
import org.scalactic.source.Position
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.CheckerAsserting
import org.scalatestplus.scalacheck.CheckerAsserting._
import ru.yandex.vertis.vsquality.callgate.converters.ProtoFormat._
import ru.yandex.vertis.vsquality.callgate.generators.Arbitraries._
import ru.yandex.vertis.vsquality.callgate.model._
import ru.yandex.vertis.vsquality.callgate.model.api.{ApiRequest, ApiResponse}
import ru.yandex.vertis.vsquality.callgate.proto.inner
import ru.yandex.vertis.vsquality.callgate.proto.inner.{Api => innerApi}
import ru.yandex.vertis.hobo.proto.model.QueueId
import ru.yandex.vertis.hobo.proto.{model => hoboModel}
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoFormatInstances._
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.vsquality.utils.scalapb_utils.{ProtoMessage, ProtoMessageFormat}
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

import scala.concurrent.duration.FiniteDuration

/**
  * @author mpoplavkov
  */
class ProtoFormatSpec extends SpecBase {

  val checker: CheckerAsserting[Assertion] = implicitly[CheckerAsserting[Assertion]]
  val prettifier: Prettifier = implicitly[Prettifier]
  val position: Position = implicitly[Position]

  case class TestProps(protoProp: Prop, jsonProp: Prop)

  case class TestCase(name: String, prop: TestProps)

  val testCases: Seq[TestCase] =
    Seq(
      TestCase("Instant", propsByType[Instant, ProtoTimestamp]),
      TestCase("FiniteDuration", propsByType[FiniteDuration, ProtoDuration]),
      TestCase("HoboCallgateResolution", propsByType[HoboCallgateResolution, hoboModel.CallgateResolution]),
      TestCase("TaskInfo", propsByType[TaskInfo, inner.TaskInfo]),
      TestCase("Payload", propsByType[Payload, inner.TaskInfo.Payload]),
      TestCase(
        "PayloadAutoruOffer",
        propsByType[Payload.AutoruOffers.Offer, inner.TaskInfo.Payload.AutoruOffers.Offer]
      ),
      TestCase(
        "PayloadRealtyOffer",
        propsByType[Payload.RealtyOffers.Offer, inner.TaskInfo.Payload.RealtyOffers.Offer]
      ),
      TestCase(
        "PayloadRealtyOfferAreaInfo",
        propsByType[Payload.RealtyOffers.AreaInfo, inner.TaskInfo.Payload.RealtyOffers.AreaInfo]
      ),
      TestCase(
        "PayloadRealtyOfferPriceInfo",
        propsByType[Payload.RealtyOffers.PriceInfo, inner.TaskInfo.Payload.RealtyOffers.PriceInfo]
      ),
      TestCase(
        "AutoruCleanWebInfo",
        propsByType[Payload.AutoruCleanWebInfo, inner.TaskInfo.Payload.AutoruCleanWebInfo]
      ),
      TestCase(
        "RealtyCleanWebInfo",
        propsByType[Payload.RealtyCleanWebInfo, inner.TaskInfo.Payload.RealtyCleanWebInfo]
      ),
      TestCase("ApiErrorResponse", propsByType[ApiResponse.Error, innerApi.EmptyResponse]),
      TestCase("ApiEmptyResponse", propsByType[ApiResponse.Empty.type, innerApi.EmptyResponse]),
      TestCase("ApiPutTaskResultRequest", propsByType[ApiRequest.ApplyTaskResult, innerApi.ApplyTaskResultRequest])
    )

  private def check(p: Prop) = {
    checker.check(p, Parameters.default, prettifier, position)
  }

  "ProtoFormat" should {
    testCases.foreach { case TestCase(name, TestProps(protoProp, jsonProp)) =>
      s"correctly convert $name to/from protobuf" in {
        check(protoProp)
      }
      s"correctly convert $name to/from json" in {
        check(jsonProp)
      }
    }

    "not ignore empty arrays" in {
      import io.circe.parser._
      import ru.yandex.vertis.vsquality.callgate.converters.ProtoFormat.TaskInfoFormat

      val descriptor = TaskDescriptor(QueueId.REALTY_CALLGATE_OFFERS_CALL, "abc")
      val taskInfo = TaskInfo(descriptor, Payload.RealtyOffers(Seq.empty))
      val expected =
        s"""
           |{
           |  "key": "${descriptor.toString}",
           |  "payload": {
           |    "realtyOffers": {
           |      "offers": []
           |    }
           |  }
           |}
        """.stripMargin
      val expectedNoSpaces = parse(expected).getOrElse(???).noSpaces
      taskInfo.asJson.noSpaces shouldBe expectedNoSpaces
    }

  }

  private def propsByType[T, M <: ProtoMessage[M]](
      implicit a: Arbitrary[T],
      pf: ProtoMessageFormat[T, M]): TestProps = {
    val protoProp =
      Prop.forAll { x: T =>
        x.toProtoMessage.fromProtoMessage.toOption.get == x
      }

    val jsonProp =
      Prop.forAll { x: T =>
        decode(x.asJson.toString)(fromProtoReads(pf)) == Right(x)
      }
    TestProps(protoProp, jsonProp)
  }
}
