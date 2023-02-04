package ru.yandex.vertis.karp.converter.protobuf

import cats.syntax.validated._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalacheck.{magnolia, Arbitrary}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoJson._
import ru.yandex.vertis.zio_baker.scalapb_utils._
import ru.yandex.vertis.karp.model.Arbitraries._
import ru.yandex.vertis.karp.model._
import ru.yandex.vertis.karp.model.event._
import ru.yandex.vertis.karp.proto
import scalapb.GeneratedEnum

import scala.reflect.ClassTag

class ProtoFormatSpec extends AnyWordSpecLike with ScalaCheckPropertyChecks with Matchers {

  private val testCases: Seq[TestCase] = {

    import Implicits._
    import magnolia._

    Seq(
      MessageTestCase[DateTimeInterval, proto.common.DateTimeInterval](),
      MessageTestCase[Identifier, proto.common.Identifier](),
      MessageTestCase[RequestSource, proto.model.RequestSource](),
      MessageTestCase[ClientInfo, proto.model.Task.ClientInfo](),
      MessageTestCase[RequestState, proto.model.Task.RequestData.RequestState](),
      MessageTestCase[RequestData, proto.model.Task.RequestData](),
      MessageTestCase[KarpTask, proto.model.Task](),
      MessageTestCase[TaskProcessingEvent, proto.model.TaskProcessingEvent]()
    )
  }

  testCases.foreach { testCase =>
    testCase.check()
  }

  sealed trait TestCase {
    def check(): Unit
  }

  private case class MessageTestCase[T, M <: ProtoMessage[M]](
    )(implicit val format: ProtoMessageFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T])
    extends TestCase {

    override def check(): Unit =
      s"ProtoMessageFormat for $classTag" should {
        "round-trip through protobuf" in forAll { x: T =>
          format.fromProto(format.toProto(x)) shouldBe x.valid
        }
        "round-trip through JSON" in forAll { x: T =>
          decode(x.asJson.toString) shouldBe Right(x)
        }
      }
  }

  case class EnumTestCase[T, M <: GeneratedEnum](
    )(implicit val format: ProtoEnumFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T])
    extends TestCase {

    override def check(): Unit =
      s"ProtoEnumFormat for $classTag" should {
        "round-trip through protobuf" in forAll { x: T =>
          format.fromProto(format.toProto(x)) shouldBe x.valid
        }
      }
  }
}
