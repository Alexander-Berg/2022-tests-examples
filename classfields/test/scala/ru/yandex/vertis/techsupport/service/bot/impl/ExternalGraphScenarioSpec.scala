package ru.yandex.vertis.vsquality.techsupport.service.bot.impl

import cats.kernel.Monoid
import com.softwaremill.tagging._
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._
import ru.yandex.vertis.vsquality.utils.lang_utils.interval.OptInterval
import ru.yandex.vertis.vsquality.utils.lang_utils.{Ignore, Use}
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase
import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action
import ru.yandex.vertis.vsquality.techsupport.service.bot.ExternalScenario.{ExternalContext, Replacements}
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario.{
  BotGraph,
  Condition,
  Edge,
  EdgeLabel,
  ExpectedInput,
  Node
}
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenarioSpec.TestCase
import ru.yandex.vertis.vsquality.techsupport.service.bot.{BotScenario, ExternalScenario}
import ru.yandex.vertis.vsquality.techsupport.util.VersionUtil.VersionOrdering
import ru.yandex.vertis.vsquality.techsupport.util.scenarioFromFile
import scalax.collection.edge.LDiEdge
import scalax.collection.immutable.Graph

/**
  * @author potseluev
  */
class ExternalGraphScenarioSpec extends SpecBase {

  private val scenarioFileName = "greeting_scenario_with_conditions.json"
  private val scenario: ExternalScenario = scenarioFromFile(scenarioFileName)
  private val fallbackStateId: BotStateId = "call_operator".taggedWith[Tags.BotStateId]

  val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "transit to state specified by command",
      scenario = scenario,
      stateId = "say_name",
      command = "Пока!",
      expectedNextStateId = Some("goodbye")
    ),
    TestCase(
      description = "transit from initial state correctly",
      scenario = scenario,
      stateId = scenario.startFrom,
      command = "Скажи свое имя",
      expectedNextStateId = Some("say_name")
    ),
    TestCase(
      description = "return fallback state if transition doesn't exist",
      scenario = scenario,
      stateId = "say_name",
      command = "Бот, ты мне надоел, пока!",
      expectedNextStateId = Some(fallbackStateId)
    ),
    TestCase(
      description = "return fallback state if provided state doesn't exist",
      scenario = scenario,
      stateId = "incorrect_state",
      command = "/finish",
      expectedNextStateId = Some(fallbackStateId)
    )
  )

  "BotGraphScenario" should {
    testCases.foreach { case TestCase(description, scenario, stateId, command, expectedNextStateId, ctx) =>
      description in {
        val botStateId = stateId.taggedWith[Tags.BotStateId]
        val payload = Message.Payload(command, Seq.empty, Seq.empty, None)
        val actualNextState = scenario.transit(botStateId, payload)(ctx)
        actualNextState.map(_.id) shouldBe expectedNextStateId
      }
    }

    "substitute placeholders correctly" in {
      val stateId = "start".taggedWith[Tags.BotStateId]
      implicit val context: ExternalContext = ExternalContext(
        Map(
          "greeting" -> "Привет",
          "usability" -> "полезный",
          "user-id" -> "Анфиса"
        )
      )
      val expectedMessage = "Привет, Анфиса! Я - полезный бот. Привет!"
      scenario.getState(stateId).map(_.action) match {
        case Some(Action.Reply(msg, _)) =>
          msg shouldBe expectedMessage
        case other =>
          fail(s"Unexpected action $other")
      }
    }

    "get fallback state if there are no state with specified id" in {
      implicit val ctx: ExternalContext = Monoid[ExternalContext].empty
      val actualState = scenario.getState("unknown_state_id".taggedWith[Tags.BotStateId])
      val expectedState = scenario.getState(fallbackStateId).get
      actualState shouldBe Some(expectedState)
    }

    "get only commands with empty condition if context is empty" in {
      val actualState = scenario.getState(scenario.startFrom)(Monoid[ExternalContext].empty)
      val expectedImages = Seq("image1", "image2").map(_.taggedWith[Tags.Url]).map(Image)
      val expectedState = BotScenario.State(
        id = scenario.startFrom,
        action = Action.Reply("{greeting}, {user-id}! Я - {usability} бот. {greeting}!", expectedImages),
        availableCommands = Seq(
          "Скажи свое имя",
          "Позови человека"
        ).map(_.taggedWith)
      )
      actualState shouldBe Some(expectedState)
    }

    "get commands matched by non empty condition as well" in {
      implicit val ctx: ExternalContext =
        ExternalContext(os = Some(Platform.Ios), appVersion = None, replacements = Map.empty)
      val actualState = scenario.getState(scenario.startFrom)
      val expectedImages = Seq("image1", "image2").map(_.taggedWith[Tags.Url]).map(Image)
      val expectedState = BotScenario.State(
        id = scenario.startFrom,
        action = Action.Reply("{greeting}, {user-id}! Я - {usability} бот. {greeting}!", expectedImages),
        availableCommands = Seq(
          "Скажи свое имя",
          "Позови человека",
          "Бот, ты мне надоел, пока!",
          "Скажи мое имя"
        ).map(_.taggedWith)
      )
      actualState shouldBe Some(expectedState)
    }

    "preserve initial edges order" in {
      def edges(json: Json): Seq[Edge] =
        json.findAllByKey("edges").head.as[Seq[Edge]].getOrElse(???)

      val initialJson = parse(readResourceFileAsString(s"/$scenarioFileName")).getOrElse(???)
      val jsonAfterDecoding = scenarioFromFile(scenarioFileName).asJson
      edges(initialJson) shouldBe edges(jsonAfterDecoding)
    }
  }

  "RichNode" should {
    import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario.RichNode

    val nodesSeq = generateSeq[Node](5)
    val n1 = nodesSeq.head
    val n2 = nodesSeq(1)
    val n3 = nodesSeq(2)
    val n4 = nodesSeq(3)
    val n5 = nodesSeq(4)
    val name = "label".taggedWith[Tags.BotCommand]
    val payload = Message.Payload(name, Seq.empty, Seq.empty, None)
    val labelWithCommand = EdgeLabel(1, ExpectedInput(name))

    def withPlatform(edgeLabel: EdgeLabel, platform: Platform) =
      edgeLabel.copy(condition = Condition(Use(platform), OptInterval[Version](None, None)))
    def withMinVersion(edgeLabel: EdgeLabel, platform: Platform, minVersion: String) =
      edgeLabel.copy(condition = Condition(Use(platform), OptInterval(Some(minVersion.taggedWith[Tags.Version]), None)))
    def withMaxVersion(edgeLabel: EdgeLabel, platform: Platform, maxVersion: String) =
      edgeLabel.copy(condition = Condition(Use(platform), OptInterval(None, Some(maxVersion.taggedWith[Tags.Version]))))

    "check platform conditions while getting edge by command" in {
      val iosEdge = LDiEdge(n1, n2)(withPlatform(labelWithCommand, Platform.Ios))
      val androidEdge = LDiEdge(n1, n3)(withPlatform(labelWithCommand, Platform.Android))
      val desktopEdge = LDiEdge(n1, n4)(withPlatform(labelWithCommand, Platform.Desktop))
      val commonEdge = LDiEdge(n1, n5)(labelWithCommand)

      val graph1: BotGraph = Graph[Node, LDiEdge](iosEdge, commonEdge)
      val graph2: BotGraph = Graph[Node, LDiEdge](androidEdge, commonEdge)
      val graph3: BotGraph = Graph[Node, LDiEdge](iosEdge, androidEdge, desktopEdge)

      implicit val ctx: ExternalContext = new ExternalContext {
        override def os: Option[Platform] = Some(Platform.Ios)
        override def appVersion: Option[Version] = None
        override def replacements: Replacements = Map.empty
      }

      graph1.get(n1).edgeByInput(payload).get shouldBe iosEdge
      graph2.get(n1).edgeByInput(payload).get shouldBe commonEdge
      graph3.get(n1).edgeByInput(payload).get shouldBe iosEdge
    }
    "check version conditions while getting edge by command" in {
      val iosEdge = LDiEdge(n1, n2)(withMinVersion(labelWithCommand, Platform.Ios, "1.2.3"))
      val iosEdge1 = LDiEdge(n1, n2)(withMaxVersion(labelWithCommand, Platform.Ios, "1.2.3"))
      val androidEdge = LDiEdge(n1, n3)(withPlatform(labelWithCommand, Platform.Android))
      val desktopEdge = LDiEdge(n1, n4)(withPlatform(labelWithCommand, Platform.Desktop))
      val commonEdge = LDiEdge(n1, n5)(labelWithCommand)

      val graph1: BotGraph = Graph[Node, LDiEdge](iosEdge, commonEdge)
      val graph2: BotGraph = Graph[Node, LDiEdge](iosEdge1, commonEdge)
      val graph3: BotGraph = Graph[Node, LDiEdge](androidEdge, commonEdge)
      val graph4: BotGraph = Graph[Node, LDiEdge](iosEdge, androidEdge, desktopEdge)
      val graph1WithDifferentOrder: BotGraph = Graph[Node, LDiEdge](commonEdge, iosEdge)

      val ctx1: ExternalContext = new ExternalContext {
        override def os: Option[Platform] = Some(Platform.Ios)
        override def appVersion: Option[Version] = Some("1.2.2".taggedWith[Tags.Version])
        override def replacements: Replacements = Map.empty
      }
      val ctx2: ExternalContext = new ExternalContext {
        override def os: Option[Platform] = Some(Platform.Ios)
        override def appVersion: Option[Version] = Some("1.2.3".taggedWith[Tags.Version])
        override def replacements: Replacements = Map.empty
      }
      val ctx3: ExternalContext = new ExternalContext {
        override def os: Option[Platform] = Some(Platform.Ios)
        override def appVersion: Option[Version] = Some("1.2.4".taggedWith[Tags.Version])
        override def replacements: Replacements = Map.empty
      }

      graph1.get(n1).edgeByInput(payload)(ctx1).get shouldBe commonEdge
      graph1.get(n1).edgeByInput(payload)(ctx2).get shouldBe iosEdge
      graph2.get(n1).edgeByInput(payload)(ctx1).get shouldBe iosEdge1
      graph2.get(n1).edgeByInput(payload)(ctx3).get shouldBe commonEdge
      graph3.get(n1).edgeByInput(payload)(ctx1).get shouldBe commonEdge
      graph3.get(n1).edgeByInput(payload)(ctx2).get shouldBe commonEdge
      graph4.get(n1).edgeByInput(payload)(ctx1) shouldBe None
      graph4.get(n1).edgeByInput(payload)(ctx2).get shouldBe iosEdge
      graph1WithDifferentOrder.get(n1).edgeByInput(payload)(ctx1).get shouldBe commonEdge
      graph1WithDifferentOrder.get(n1).edgeByInput(payload)(ctx2).get shouldBe iosEdge
    }

    "check hasPhoto edge can be found" in {
      val textPayload = Message.Payload("label", Seq.empty, Seq.empty, None)
      val imgPayload = Message.Payload("", Seq(Image("url".taggedWith[Tags.Url])), Seq.empty, None)
      val hasPhotoEdgeLabel = EdgeLabel(1, ExpectedInput(Ignore, Use(true), Ignore, Ignore))
      val ctx: ExternalContext = ExternalContext(None, None, Map.empty)
      val hasPhotoEdge = LDiEdge(n1, n5)(hasPhotoEdgeLabel)

      val graph1: BotGraph = Graph[Node, LDiEdge](hasPhotoEdge)

      graph1.get(n1).edgeByInput(textPayload)(ctx) shouldBe None
      graph1.get(n1).edgeByInput(imgPayload)(ctx).get shouldBe hasPhotoEdge
    }

    "correctly find edge with hasPhotoFromCamera expected input" in {
      val textPayload = Message.Payload("text", Seq.empty, Seq.empty, None)
      val imageFromCameraPayload = Message.Payload("", Seq(Image("vs-techsupp".taggedWith[Tags.Url])), Seq.empty, None)
      val anotherImagePayload = Message.Payload("", Seq(Image("url".taggedWith[Tags.Url])), Seq.empty, None)
      val hasPhotoFromCameraEdgeLabel = EdgeLabel(1, ExpectedInput(Ignore, Ignore, Use(true), Ignore))
      implicit val ctx: ExternalContext = ExternalContext(None, None, Map.empty)
      val hasPhotoFromCameraEdge = LDiEdge(n1, n5)(hasPhotoFromCameraEdgeLabel)
      val graph: BotGraph = Graph[Node, LDiEdge](hasPhotoFromCameraEdge)
      graph.get(n1).edgeByInput(imageFromCameraPayload) shouldBe Some(hasPhotoFromCameraEdge)
      graph.get(n1).edgeByInput(anotherImagePayload) shouldBe None
      graph.get(n1).edgeByInput(textPayload) shouldBe None
    }

    "check hasAnything edge can be found" in {
      val textPayload = Message.Payload("label", Seq.empty, Seq.empty, None)
      val imgPayload = Message.Payload("", Seq(Image("url".taggedWith[Tags.Url])), Seq.empty, None)
      val hasAnythingEdgeLabel = EdgeLabel(1, ExpectedInput(Ignore, Ignore, Ignore, Use(true)))
      val ctx: ExternalContext = ExternalContext(None, None, Map.empty)
      val hasAnythingEdge = LDiEdge(n1, n5)(hasAnythingEdgeLabel)

      val graph1: BotGraph = Graph[Node, LDiEdge](hasAnythingEdge)

      graph1.get(n1).edgeByInput(textPayload)(ctx).get shouldBe hasAnythingEdge
      graph1.get(n1).edgeByInput(imgPayload)(ctx).get shouldBe hasAnythingEdge
    }
  }

}

object ExternalGraphScenarioSpec {

  case class TestCase(
      description: String,
      scenario: ExternalScenario,
      stateId: String,
      command: String,
      expectedNextStateId: Option[String],
      ctx: ExternalContext = Monoid[ExternalContext].empty)

}
