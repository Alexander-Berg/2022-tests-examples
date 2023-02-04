package ru.yandex.vertis.vsquality.techsupport.conversion.proto

import cats.syntax.validated._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.common
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.vsquality.utils.lang_utils.interval.OptInterval
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatSpec.{EnumTestCase, MessageTestCase, TestCase}
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.techsupport.proto.{model => proto}
import ru.yandex.vertis.vsquality.techsupport.service.ChatService
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action.Escalate
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario.{
  BotGraph,
  Condition,
  Edge,
  EdgeLabel,
  Node
}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import scalapb.GeneratedEnum
import scalax.collection.edge.LDiEdge
import scalax.collection.immutable.Graph
import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.utils.scalapb_utils.{ProtoEnumFormat, ProtoMessage, ProtoMessageFormat}
import ru.yandex.vertis.vsquality.techsupport.util.VersionUtil.VersionOrdering

import scala.reflect.ClassTag

/**
  * @author potseluev
  */
class ProtoFormatSpec extends SpecBase {

  import ProtoFormatInstances._
  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  implicit lazy val IntervalArb: Arbitrary[OptInterval[Version]] =
    Arbitrary {
      for {
        num1 <- Gen.option(Gen.posNum[Int])
        num2 <- Gen.option(Gen.posNum[Int].filter(x => num1.forall(x >= _)))
      } yield {
        OptInterval[Version](
          num1.map(_.toString.taggedWith[Tags.Version]),
          num2.map(_.toString.taggedWith[Tags.Version])
        )
      }
    }

  val testCases: Seq[TestCase] =
    Seq(
      MessageTestCase[UserId, proto.UserId](),
      MessageTestCase[AppealState, proto.AppealState](),
      MessageTestCase[Appeal.Tags, proto.AppealTags](),
      MessageTestCase[Message.Payload, proto.MessagePayload](),
      MessageTestCase[Message, proto.Message](),
      MessageTestCase[ClientInfo, proto.ClientInfo](),
      MessageTestCase[ClientRequestContext, proto.RequestContext](),
      MessageTestCase[Request, proto.Request](),
      MessageTestCase[BatchRequest, proto.BatchRequest](),
      MessageTestCase[TechsupportRespondent, proto.TechsupportRespondent](),
      EnumTestCase[MessageType, proto.MessageType](),
      EnumTestCase[Domain, proto.Domain](),
      EnumTestCase[ChatProvider, proto.ChatProvider](),
      EnumTestCase[TechsupportProvider, proto.TechsupportProvider](),
      EnumTestCase[Option[Platform], common.Platform](),
      MessageTestCase[ScenarioId, proto.ScenarioId](),
      MessageTestCase[Conversation.MetadataSet, proto.ConversationMetadata](),
      // MessageTestCase[ChatService.ChatPayload, proto.ChatPayload](),
      MessageTestCase[Appeal, proto.Appeal](),
      MessageTestCase[Event, proto.Event](),
      MessageTestCase[Condition, proto.ExternalGraphScenario.Condition]()
    )

  "ProtoFormat" should {
    testCases.foreach { testCase =>
      testCase.description in {
        testCase.check()
      }
    }

    "work correctly with BotGraphScenario" in {
      val nodesSeq = generateSeq[Node](4)
      val n1 = nodesSeq.head
      val n2 = nodesSeq(1)
      val n3 = nodesSeq(2)
      val n4 = nodesSeq(3)
      val n4terminal = n4.copy(action = Escalate("!"))

      def nextLabelWithCommand: EdgeLabel = generate[EdgeLabel](x => x.expectedInput.nonEmpty && x.condition.isEmpty)

      val graph: BotGraph = Graph[Node, LDiEdge](
        LDiEdge(n1, n2)(nextLabelWithCommand),
        LDiEdge(n1, n3)(nextLabelWithCommand),
        LDiEdge(n2, n4terminal)(nextLabelWithCommand),
        LDiEdge(n3, n4terminal)(nextLabelWithCommand)
      )
      val scenario = ExternalGraphScenario(
        startFrom = n1.id,
        fallbackTo = Some(n4.id),
        nodesMap = graph.nodes.map(x => (x.value.id, x.value)).toMap,
        edges = graph.edges
          .filter(_.label.asInstanceOf[EdgeLabel].expectedInput.nonEmpty)
          .map { x =>
            val fromId = x.value.from.value.id
            val toId = x.value.to.value.id
            val botCommand = x.label.asInstanceOf[EdgeLabel].expectedInput
            val condition = x.label.asInstanceOf[EdgeLabel].condition
            Edge(fromId, toId, botCommand, condition)
          }
          .toSeq
      ).fold(x => throw new Exception(x.toString), identity)
      val protoScenario: proto.ExternalGraphScenario = scenario.toProtoMessage
      val jsonScenario = scenario.asJson
      jsonScenario.as[ExternalGraphScenario] shouldBe Right(scenario)
      protoScenario.fromProtoMessage shouldBe scenario.valid
    }
  }
}

object ProtoFormatSpec {

  sealed trait TestCase {
    def description: String

    def check(): Assertion
  }

  case class MessageTestCase[T, M <: ProtoMessage[M]](
    )(implicit val format: ProtoMessageFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T])
    extends TestCase {

    def description: String =
      s"convert proto msg $classTag"

    def check(): Assertion = {
      import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._

      Checkers.check(Prop.forAll { x: T =>
        format.fromProto(format.toProto(x)) == x.valid
      })

      Checkers.check(Prop.forAll { x: T =>
        decode(x.asJson.toString) == Right(x)
      })
    }
  }

  case class EnumTestCase[T, M <: GeneratedEnum](
    )(implicit val format: ProtoEnumFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T])
    extends TestCase {

    def description: String =
      s"convert proto enum $classTag"

    def check(): Assertion = {

      Checkers.check(Prop.forAll { x: T =>
        format.fromProto(format.toProto(x)) == x.valid
      })
    }
  }

}
