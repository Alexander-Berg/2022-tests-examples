package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline

import java.io.InputStream
import java.time.temporal.ChronoUnit
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.config.Config
import org.apache.commons.io.IOUtils
import org.mockito.internal.stubbing.defaultanswers.ReturnsMocks
import org.scalacheck.Gen
import ru.yandex.common.tokenization._
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.app.{CoreComponents, UtilizatorService}
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages._
import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOfferError, OffersSendStatus, Task, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.loader.Loader
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.MapperFlow
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.parser.Parser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.picker.TaskPicker
import ru.yandex.vertis.feedprocessor.autoru.utils.TestExternalOffer
import ru.yandex.vertis.feedprocessor.util._
import ru.yandex.vertis.mockito.MockitoSupport

import java.nio.charset.StandardCharsets.UTF_8
import scala.annotation.nowarn
import scala.concurrent.Future
import scala.util.Success
import scala.xml.XML

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class FeedProcessingPipelineSpec extends StreamTestBase with MockitoSupport {

  trait Fixture extends TestApplication with PipelineComponents[TestExternalOffer] with Logging with DummyOpsSupport {
    implicit val self = testActor

    implicit val actorSystem: ActorSystem = system
    implicit val actorMaterializer: ActorMaterializer = materalizer
  }

  case class OfferImpl(pos: String)

  "FeedProcessingPipeline" should {
    "handle simple task" in {
      val app = new FeedProcessingPipeline[TestExternalOffer, OfferImpl] with Fixture {
        override def parallelism: Int = 2

        override val taskDistribution: TokensDistribution = new DummyTokensDistribution(2)

        override def utilizationDistribution: TokensDistribution = new DummyTokensDistribution(1)

        override val taskPicker: TaskPicker = new TaskPicker {
          override def pick(credentials: TaskPicker.Credentials): Future[Seq[Task]] =
            Future {
              Array.fill(credentials.limit)(newTasksGen.next)
            }
        }

        override val loader: Loader = new Loader {
          override def loadFeed(task: Task): Future[InputStream] =
            Future {
              val elements = (0 until Gen.choose(1, 5).next).map(i => "<element></element>").mkString("\n")
              IOUtils.toInputStream(s"<xml>$elements</xml>", UTF_8)
            }
        }

        override val parser: Parser[TestExternalOffer] = new Parser[TestExternalOffer] {
          override def parse(
              feed: InputStream
            )(implicit tc: TaskContext): Iterator[Either[ExternalOfferError, TestExternalOffer]] = {
            val elements = XML.load(feed) \ "element"
            elements.zipWithIndex.map {
              case (node, position) =>
                Right(TestExternalOffer(position, tc))
            }.toIterator
          }
        }

        override def mapper =
          new MapperFlow[TestExternalOffer] {
            override def flow() = Flow[OutgoingMessage[TestExternalOffer]]
          }

        override def senderFlow(
          ): Flow[SendingMessage[TestExternalOffer, OfferImpl], OffersSendStatus[TestExternalOffer], NotUsed] = {
          Flow[SendingMessage[TestExternalOffer, OfferImpl]].map { message =>
            model.OffersSendStatus(Nil, message.taskContext, Success(1))
          }
        }

        override def components: CoreComponents = {
          val mockCoreComponent = mock[CoreComponents](new ReturnsMocks)
          val mockConfig: Config = mock[Config](new ReturnsMocks)

          when(mockConfig.getInt(?)).thenReturn(1)
          when(mockConfig.getDuration(?)).thenReturn(java.time.Duration.of(1, ChronoUnit.HOURS))
          when(mockCoreComponent.appConf).thenReturn(mockConfig)
          mockCoreComponent
        }

        override def utilizator: UtilizatorService = mock[UtilizatorService](new ReturnsMocks)
      }
      val (pub, sub) = createPubSub(app.taskProcessingPipeline())
      sub.request(1)
      pub.sendNext(())
      sub.expectNextPF {
        case OffersSendStatus(_, _, Success(_)) =>
      }
      pub.sendError(new Exception("Test"))
      sub.expectError().getMessage shouldBe "Test"
    }
  }
}
