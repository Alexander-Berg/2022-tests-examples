package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.util

import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import org.apache.commons.io.IOUtils
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.{
  FailureMessage,
  OfferMessage,
  OutgoingMessage,
  StreamEndMessage
}
import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOffer, ExternalOfferError, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.parser.Parser
import ru.yandex.vertis.feedprocessor.util.StreamTestBase

import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * @author pnaydenov
  */
class ParserStageSpec extends StreamTestBase {
  private type Input = (InputStream, TaskContext)
  private type Output = OutgoingMessage[ExternalOffer]

  case class TestExternalOfferImpl(mark: String, price: Int, position: Int, taskContext: TaskContext)
    extends ExternalOffer

  case class ExternalOfferErrorImpl(position: Int, error: Throwable, taskContext: TaskContext)
    extends ExternalOfferError

  /**
    * parse TSV formatted feeds in format:
    * <header>
    * <mark>\t<price>
    * ...
    */
  private val parser = new Parser[TestExternalOfferImpl] {

    override def parse(
        feed: InputStream
      )(implicit tc: TaskContext): Iterator[Either[ExternalOfferError, TestExternalOfferImpl]] = {

      new Iterator[Either[ExternalOfferError, TestExternalOfferImpl]] {
        private var position = 0

        private def readLine(): String = {
          val sb = new StringBuilder()
          var byte: Int = feed.read()
          var eol: Boolean = false
          while (!eol && byte != -1) {
            if (byte == '\n'.toInt) {
              eol = true
            } else {
              sb.append(byte.toChar)
              byte = feed.read()
            }
          }
          sb.result()
        }

        override def hasNext: Boolean = {
          feed.available() > 0
        }

        override def next(): Either[ExternalOfferError, TestExternalOfferImpl] = {
          val currentPosition = position
          position += 1
          val line = readLine() // handle IO outside of try-catch: ParserStage should handle such cases itself
          try {
            val columns = line.split("\t")
            val mark = columns(0)
            val price = columns(1).toInt
            Right(TestExternalOfferImpl(mark, price, currentPosition, tc))
          } catch {
            case NonFatal(ex) =>
              Left(ExternalOfferErrorImpl(currentPosition, ex, tc))
          }
        }

        // read required header: can generate IO error just in Iterator constructor
        if (readLine().isEmpty) {
          throw new RuntimeException("Wrong format: header required")
        }
      }
    }
  }

  private val emptyStream = IOUtils.toInputStream("", UTF_8)

  private val ioErrorStream1 = new InputStream {
    override def read(): Int = throw new IOException()

    override def available(): Int = 1
  }

  private val ioErrorStream2 = new InputStream {
    val a = new AtomicInteger('a'.toInt) // correctly return "header" but throw exception on available()
    override def read(): Int = a.getAndSet('\n'.toInt)

    override def available(): Int = throw new IOException()
  }
  private val emptyContent = IOUtils.toInputStream("mark\tprice", UTF_8)
  private val wrongContent = IOUtils.toInputStream("mark\tprice\nfoobar\nbaz\tFFF", UTF_8)
  private val nonEmptyContent = IOUtils.toInputStream("mark\tprice\nBMW\t150\nFord\t100", UTF_8)
  private val taskContext = TaskContext(newTasksGen.next, 12345L)

  private def buildGraph(): (TestPublisher.Probe[Input], TestSubscriber.Probe[Output]) = {
    val parallelism = 2
    val source = TestSource.probe[Input]
    val sink = TestSink.probe[Output]
    val (pub, sub) = source
      .flatMapMerge(parallelism, { case (is, tc) => new ParserStage(parser, is)(global, tc) })
      .toMat(sink)(Keep.both)
      .run()
    pub -> sub
  }

  "ParserStage" should {
    "properly handle empty file" in {
      val (pub, sub) = buildGraph()
      sub.request(1)
      pub.sendNext(emptyStream -> taskContext)
      sub.expectNextPF {
        case StreamEndMessage(_, Some(ex), _) =>
          ex.getMessage should include("Wrong format: header required")
      }
      sub.expectNoMessage(100.millis)
    }

    "properly handle IO exception in parser initialization" in {
      val (pub, sub) = buildGraph()
      sub.request(1)
      pub.sendNext(ioErrorStream1 -> taskContext)
      sub.expectNextPF {
        case StreamEndMessage(_, Some(error), _) =>
      }
      sub.expectNoMessage(100.millis)
    }
    "properly handle IO exception in parser traverse" in {
      val (pub, sub) = buildGraph()
      sub.request(1)
      pub.sendNext(ioErrorStream2 -> taskContext)
      sub.expectNextPF {
        case StreamEndMessage(_, Some(error), _) =>
      }
      sub.expectNoMessage(100.millis)
    }

    "fail on well formatted empty file" in {
      val (pub, sub) = buildGraph()
      sub.request(1)
      pub.sendNext(emptyContent -> taskContext)
      sub.expectNextPF {
        case StreamEndMessage(_, Some(ex), _) =>
          ex.getMessage should include("Empty file")
      }
      sub.expectNoMessage(100.millis)
    }

    "properly handle well formatted non-empty file" in {
      val (pub, sub) = buildGraph()
      sub.request(1)
      pub.sendNext(nonEmptyContent -> taskContext)
      sub.expectNextPF {
        case OfferMessage(TestExternalOfferImpl("BMW", 150, 0, _)) =>
      }
      sub.request(1)
      sub.expectNextPF {
        case OfferMessage(TestExternalOfferImpl("Ford", 100, 1, _)) =>
      }
      sub.request(1)
      sub.expectNextPF {
        case StreamEndMessage(_, None, _) =>
      }
      sub.expectNoMessage(100.millis)
    }

    "properly handle wrong formatted non-empty file" in {
      val (pub, sub) = buildGraph()
      sub.request(1)
      pub.sendNext(wrongContent -> taskContext)
      sub.expectNextPF {
        case FailureMessage(Left(ExternalOfferErrorImpl(0, ex, _)), ex2, _) =>
          ex shouldBe a[ArrayIndexOutOfBoundsException]
          ex2 shouldBe a[ArrayIndexOutOfBoundsException]
      }
      sub.request(1)
      sub.expectNextPF {
        case FailureMessage(Left(ExternalOfferErrorImpl(1, ex, _)), ex2, _) =>
          ex shouldBe a[NumberFormatException]
          ex2 shouldBe a[NumberFormatException]
      }
      sub.request(1)
      sub.expectNextPF {
        case StreamEndMessage(_, None, _) =>
      }
      sub.expectNoMessage(100.millis)
    }
  }
}
