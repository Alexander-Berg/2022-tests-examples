package ru.yandex.vertis.vsquality.techsupport.service.bot.impl

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, Validated}
import cats.syntax.validated._
import com.softwaremill.tagging._
import org.scalatest.Assertion
import ru.yandex.vertis.vsquality.utils.lang_utils.interval.OptInterval
import ru.yandex.vertis.vsquality.utils.lang_utils.{Ignore, Use, UseOrIgnore}
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._
import ru.yandex.vertis.vsquality.techsupport.model.ScenarioId.Internal.{BlockedOffersModeration, Reseller}
import ru.yandex.vertis.vsquality.techsupport.model.{BotStateId, Image, Platform, ScenarioId, Tags, Version}
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action.{
  CompleteConversation,
  Escalate,
  Reply,
  Switch
}
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario.{
  Condition,
  Edge,
  EdgeLabel,
  ExpectedInput,
  Node
}
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.GraphValidation.Error._
import ru.yandex.vertis.vsquality.techsupport.util.VersionUtil.VersionOrdering
import ru.yandex.vertis.vsquality.techsupport.util.{scenarioFromFile, SpecBase}
import scalax.collection.GraphPredef._
import scalax.collection.edge.LDiEdge
import scalax.collection.immutable.Graph

class GraphValidationSpec extends SpecBase {

  import scala.language.implicitConversions
  implicit def strToBSID(s: String): String @@ Tags.BotStateId = s.taggedWith[Tags.BotStateId]
  implicit def strToBCMD(s: String): String @@ Tags.BotCommand = s.taggedWith[Tags.BotCommand]

  implicit class ValidatedTestHelper[E, A](validated: Validated[E, A]) {

    def get: A = {
      validated match {
        case Valid(a)   => a
        case Invalid(e) => throw new Exception(e.toString)
      }
    }
  }

  val scenario1 = scenarioFromFile("greeting_scenario.json")
  val scenario2 = scenarioFromFile("choose_scenario.json")
  val scenario1Id = ScenarioId.External("greeting_scenario".taggedWith[Tags.ExternalScenarioId])
  val scenario2Id = ScenarioId.External("choose_scenario".taggedWith[Tags.ExternalScenarioId])

  val scenariosMap = Map(scenario1Id -> scenario1, scenario2Id -> scenario2)

  "GraphValidation" should {
    "switch positive test" in {
      val scenario1Id = ScenarioId.External("scenario1".taggedWith[Tags.ExternalScenarioId])
      val scenario2Id = ScenarioId.External("scenario2".taggedWith[Tags.ExternalScenarioId])
      val n1: BotStateId = "s1n1"
      val n2: BotStateId = "s2n2"
      val n3: BotStateId = "s1n3"

      val nodesMap1 = Seq(Node(n1, Switch(scenario2Id, Some(n2))), Node(n3, Escalate(""))).map(x => (x.id, x)).toMap
      val sc1 = ExternalGraphScenario(n1, Some(n3), nodesMap1, Seq.empty).get

      val nodesMap2 = Seq(Node(n2, Switch(scenario1Id, Some(n3)))).map(x => (x.id, x)).toMap
      val sc2 = ExternalGraphScenario(n2, None, nodesMap2, Seq.empty).get

      val scenarioMap = Map(scenario1Id -> sc1, scenario2Id -> sc2)
      GraphValidation.validateCompositeScenario(scenarioMap) shouldBe Validated.validNec(scenarioMap)
    }

    //
    //  s1n1 -----> s2n1
    //   ^           |
    //   |           |
    //   |           |
    //   |           v
    //  s1n2 <----- s2n2
    //
    // cyclic dependency without terminal nodes
    "switch negative test" in {
      val scenario1Id = ScenarioId.External("scenario1".taggedWith[Tags.ExternalScenarioId])
      val scenario2Id = ScenarioId.External("scenario2".taggedWith[Tags.ExternalScenarioId])
      val s1n1: BotStateId = "s1n1"
      val s1n2: BotStateId = "s1n2"
      val s2n1: BotStateId = "s2n1"
      val s2n2: BotStateId = "s2n2"

      val nodesMap1 =
        Seq(Node(s1n1, Switch(scenario2Id, None)), Node(s1n2, Reply("", Seq.empty))).map(x => (x.id, x)).toMap
      val sc1 =
        ExternalGraphScenario(s1n2, None, nodesMap1, Seq(Edge(s1n2, s1n1, ExpectedInput(""), Condition.Empty))).get

      val nodesMap2 =
        Seq(Node(s2n1, Reply("", Seq.empty)), Node(s2n2, Switch(scenario1Id, None))).map(x => (x.id, x)).toMap
      val sc2 =
        ExternalGraphScenario(s2n1, None, nodesMap2, Seq(Edge(s2n1, s2n2, ExpectedInput(""), Condition.Empty))).get

      val scenarioMap = Map(scenario1Id -> sc1, scenario2Id -> sc2)
      val res = InvalidFSM(Set("scenario1:s1n2", "scenario2:s2n2", "scenario2:s2n1", "scenario1:s1n1"))

      GraphValidation.validateCompositeScenario(scenarioMap) shouldBe Validated.invalidNec(res)
    }

    //   -----> s1n1
    //  ^        |
    //  |       s1nf -----> s2n1
    //  |                    |
    //  |                   s2nf -----> s3n1
    //  |                                |
    //  |                               s3nf
    //  |                                 |
    //   <--------------------------------
    // cyclic transitive dependency on fallback nodes
    "fallback negative test" in {
      val scenario1Id = ScenarioId.External("scenario1".taggedWith[Tags.ExternalScenarioId])
      val scenario2Id = ScenarioId.External("scenario2".taggedWith[Tags.ExternalScenarioId])
      val scenario3Id = ScenarioId.External("scenario3".taggedWith[Tags.ExternalScenarioId])
      val s1n1: BotStateId = "s1n1"
      val s1nf: BotStateId = "s1nf"
      val s2n1: BotStateId = "s2n1"
      val s2nf: BotStateId = "s2nf"
      val s3n1: BotStateId = "s3n1"
      val s3nf: BotStateId = "s3nf"

      val nodesMap1 = Seq(
        Node(s1n1, Reply("Hi", Seq.empty)),
        Node(s1nf, Switch(scenario2Id, None))
      ).map(x => (x.id, x)).toMap
      val sc1 = ExternalGraphScenario(s1n1, Some(s1nf), nodesMap1, Seq.empty).get

      val nodesMap2 = Seq(
        Node(s2n1, Reply("Hi", Seq.empty)),
        Node(s2nf, Switch(scenario3Id, None))
      ).map(x => (x.id, x)).toMap
      val sc2 = ExternalGraphScenario(s2n1, Some(s2nf), nodesMap2, Seq.empty).get

      val nodesMap3 = Seq(
        Node(s3n1, Reply("Hi", Seq.empty)),
        Node(s3nf, Switch(scenario1Id, None))
      ).map(x => (x.id, x)).toMap
      val sc3 = ExternalGraphScenario(s3n1, Some(s3nf), nodesMap3, Seq.empty).get

      val scenarioMap = Map(scenario1Id -> sc1, scenario2Id -> sc2, scenario3Id -> sc3)
      val res = InvalidFSM(
        Set(
          "scenario1:s1n1",
          "scenario2:s2n1",
          "scenario3:s3n1",
          "scenario1:s1nf",
          "scenario2:s2nf",
          "scenario3:s3nf"
        )
      )

      GraphValidation.validateCompositeScenario(scenarioMap) shouldBe Validated.invalidNec(res)
    }

    "return valid test graph" in {
      GraphValidation.validateCompositeScenario(scenariosMap) shouldBe Validated.validNec(scenariosMap)
    }

    "build composite graph" in {
      val images = Seq("image1", "image2").map(_.taggedWith[Tags.Url]).map(Image)
      val n1 = Node("choose_scenario:switch_to_greeting", Switch(scenario1Id, None))
      val n2 = Node("choose_scenario:switch_to_call_operator", Switch(scenario1Id, Some("call_operator")))
      val n3 = Node("greeting_scenario:say_name", Reply("Меня зовут Бот.", Seq.empty))
      val n4 = Node("greeting_scenario:start", Reply("{greeting}, {user-id}! Я - {usability} бот. {greeting}!", images))
      val n5 = Node("greeting_scenario:node0", Reply("", Seq.empty))
      val n6 = Node(
        "greeting_scenario:call_operator",
        Escalate("Жаль, что не смог помочь. Переключаю на человека. {additional_info}")
      )
      val n7 = Node("greeting_scenario:goodbye", CompleteConversation("Пока!", false))
      val n8 = Node("choose_scenario:switch_to_blocked_offers", Switch(BlockedOffersModeration, None))
      val n9 = Node("choose_scenario:start", Reply("Я - бот, {user-id}. Смотри, что я умею", Seq.empty))

      val n10 = Node("choose_scenario:switch_to_smt", Switch(Reseller, None))
      val n11 = Node("greeting_scenario:switch_to_choose", Switch(scenario2Id, None))
      val n12 = Node("greeting_scenario:say_my_name", Reply("Тебя зовут {user-id}.", Seq.empty))

      val graph =
        Graph(
          n1 ~> n4,
          n1 ~> n2,
          n2 ~> n6,
          n3 ~> n6,
          n3 ~> n7,
          n4 ~> n7,
          n4 ~> n3,
          n4 ~> n6,
          n4 ~> n11,
          n4 ~> n12,
          n5 ~> n4,
          n5 ~> n6,
          n9 ~> n2,
          n9 ~> n8,
          n9 ~> n1,
          n9 ~> n2,
          n9 ~> n10,
          n11 ~> n9,
          n11 ~> n6,
          n12 ~> n7,
          n12 ~> n6,
          n1,
          n2,
          n3,
          n4,
          n5,
          n6,
          n7,
          n8,
          n9,
          n10,
          n11,
          n12
        )

      GraphValidation.buildCompositeGraph(scenariosMap) shouldBe graph.validNec
    }

    "external graph validation test" in {
      val n1 = Node("n1", Reply("", Seq.empty))
      val n2 = Node("n2", Escalate(""))
      val n3 = Node("n3", Reply("", Seq.empty))

      val graph1 = Graph(n2 ~> n1, n2 ~> n3, n1 ~> n3, n1, n2, n3)
      val graph2 = Graph(n2 ~> n1, n3 ~> n2, n1 ~> n3, n1, n2, n3)

      GraphValidation.validateExternalGraph(graph1) shouldBe Validated.invalidNec(InvalidFSM(Set(n1.id, n3.id)))
      GraphValidation.validateExternalGraph(graph2) shouldBe Validated.validNec(graph2)
    }

    "edges validation test" in {
      val n1 = Node("n1", Reply("", Seq.empty))
      val n2 = Node("n2", Reply("", Seq.empty))
      val n3 = Node("n3", Reply("", Seq.empty))
      val nf = Node("nf", Escalate("Зову оператора"))

      val edge1 = Edge(n1.id, n2.id, ExpectedInput("1"), Condition.Empty)
      val edge2 = Edge(n2.id, n3.id, ExpectedInput("2"), Condition.Empty)

      GraphValidation.getValidatedEdges(
        Map(n1.id -> n1, n2.id -> n2, nf.id -> nf),
        Seq(edge1, edge2),
        Some(nf)
      ) shouldBe Validated.invalidNec(InvalidEdge(edge2))

      GraphValidation.getValidatedEdges(
        Map(n1.id -> n1, n2.id -> n2, n3.id -> n3, nf.id -> nf),
        Seq(edge1, edge2),
        Some(nf)
      ) shouldBe Validated.validNec(
        List(
          LDiEdge(n1, n2)(EdgeLabel(0, ExpectedInput("1"))),
          LDiEdge(n2, n3)(EdgeLabel(1, ExpectedInput("2"))),
          LDiEdge(n1, nf)(EdgeLabel(Int.MaxValue, ExpectedInput.Empty, Condition.Empty)),
          LDiEdge(n2, nf)(EdgeLabel(Int.MaxValue, ExpectedInput.Empty, Condition.Empty)),
          LDiEdge(n3, nf)(EdgeLabel(Int.MaxValue, ExpectedInput.Empty, Condition.Empty))
        )
      )
    }

    "edges condition validation test" in {
      implicit class RichEdge(edge: Edge) {
        def withPlatform(platform: Platform): Edge =
          edge.copy(condition = Condition(Use(platform), OptInterval[Version](None, None)))
        def withVersion(platform: Platform, minVersion: UseOrIgnore[String], maxVersion: UseOrIgnore[String]): Edge = {
          val interval =
            OptInterval(
              minVersion.map(_.taggedWith[Tags.Version]).toOption,
              maxVersion.map(_.taggedWith[Tags.Version]).toOption
            )
          edge.copy(condition = Condition(Use(platform), interval))
        }
      }

      val n1 = Node("n1", Reply("", Seq.empty))
      val n2 = Node("n2", Reply("", Seq.empty))
      val n3 = Node("n3", Reply("", Seq.empty))
      val n4 = Node("n4", Reply("", Seq.empty))
      val n5 = Node("n5", Reply("", Seq.empty))
      val n6 = Node("n6", Reply("", Seq.empty))
      val n7 = Node("n7", Reply("", Seq.empty))

      val iosEdge = Edge(n1.id, n2.id, ExpectedInput("1"), Condition.Empty).withPlatform(Platform.Ios)
      val androidEdge = Edge(n1.id, n3.id, ExpectedInput("1"), Condition.Empty).withPlatform(Platform.Android)
      val desktopEdge = Edge(n1.id, n4.id, ExpectedInput("1"), Condition.Empty).withPlatform(Platform.Desktop)
      val commonEdge = Edge(n1.id, n5.id, ExpectedInput("1"), Condition.Empty)
      val commonEdge1 = Edge(n1.id, n6.id, ExpectedInput("1"), Condition.Empty)
      val iosEdge1 = Edge(n1.id, n7.id, ExpectedInput("1"), Condition.Empty).withPlatform(Platform.Ios)
      val iosEdge2 =
        Edge(n1.id, n2.id, ExpectedInput("1"), Condition.Empty).withVersion(Platform.Ios, Use("1.1"), Use("1.2"))
      val iosEdge3 =
        Edge(n1.id, n2.id, ExpectedInput("1"), Condition.Empty).withVersion(Platform.Ios, Use("1.3"), Use("1.4"))

      def testValid(edges: Seq[Edge]): Assertion =
        GraphValidation.validateEdgeConditions(edges) shouldBe edges.valid
      def testInvalid(edges: Seq[Edge]): Assertion =
        GraphValidation.validateEdgeConditions(edges) shouldBe NonEmptyChain.fromSeq(edges.map(InvalidEdge)).get.invalid

      testValid(Seq(iosEdge, androidEdge, desktopEdge, commonEdge))
      testValid(Seq(iosEdge, commonEdge))
      testInvalid(Seq(iosEdge, iosEdge1, commonEdge))
      testInvalid(Seq(iosEdge, commonEdge, commonEdge1))
      testValid(Seq(iosEdge, iosEdge1, androidEdge, desktopEdge))
      testValid(Seq(iosEdge, iosEdge2, iosEdge3, commonEdge))

      val interval = OptInterval(Some("1.1".taggedWith[Tags.Version]), Some("1.2".taggedWith[Tags.Version]))
      val badConditionEdge = commonEdge.copy(condition = Condition(Ignore, interval))
      val expected = NonEmptyChain.fromSeq(Seq(InvalidEdge(badConditionEdge))).get.invalid
      GraphValidation.validateEdgeConditions(Seq(badConditionEdge, commonEdge)) shouldBe expected
    }

    "nodes map validation test" in {
      val n1 = Node("n1", Reply("", Seq.empty))
      val n2 = Node("n2", Reply("", Seq.empty))
      val n3 = Node("n3", Reply("", Seq.empty))

      val nodes = Seq(n1, n2, n3)
      GraphValidation.getValidatedNodeMap(
        "n1",
        Some("n3"),
        Seq(n1, n2, n3)
      ) shouldBe Validated.valid(nodes.map(x => (x.id, x)).toMap)

      GraphValidation.getValidatedNodeMap(
        "n1",
        None,
        Seq(n1, n2, n3)
      ) shouldBe Validated.valid(nodes.map(x => (x.id, x)).toMap)

      val n1dup = n1.copy(action = Reply("1", Seq.empty))
      GraphValidation.getValidatedNodeMap(
        "n1",
        Some("n3"),
        Seq(n1, n1dup, n2, n3)
      ) shouldBe Validated.invalidNec(NonUniqueNodeId(Set(n1, n1dup)))

      GraphValidation.getValidatedNodeMap(
        "n4",
        Some("n3"),
        Seq(n1, n2, n3)
      ) shouldBe Validated.invalidNec(NodeSetWithoutStartNode("n4"))

      GraphValidation.getValidatedNodeMap(
        "n1",
        Some("n4"),
        Seq(n1, n2, n3)
      ) shouldBe Validated.invalidNec(NodeSetWithoutFallbackNode("n4"))
    }
  }
}
