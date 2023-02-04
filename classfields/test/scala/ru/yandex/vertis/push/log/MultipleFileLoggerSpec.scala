package ru.yandex.vertis.push.log

import java.io.File
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.testkit.TestKit
import org.apache.commons.io.FileUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.push.model.{LogMessage, ModelGenerators}

import com.typesafe.config.ConfigFactory
import sun.misc.Signal

import scala.concurrent.Future
import scala.io
import scala.util.Try
import scala.util.control.NonFatal

/**
  * @author @logab
  */
class MultipleFileLoggerSpec
  extends TestKit(ActorSystem("MultipleFileLoggerSpec", ConfigFactory.empty()))
    with WordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterEach {
  val dir = new File("/tmp/kafka-push-server-spec")

  val mfl = new MultipleFileLogger(dir, immediateFlush = true)

  import system.dispatcher


  override protected def beforeEach(): Unit = reload()


  override protected def afterEach(): Unit = reload()


  private def reload() = {
    FileUtils.deleteDirectory(dir)
    mfl.clear()
  }

  implicit val am = ActorMaterializer()


  "file logger" should {
    "not fail with too many open files" in {
      val elements = ModelGenerators.LogMessageGen.values.take(10000)
      val iter = (1 to 10)
        .iterator
        .flatMap { _ => elements }
      val (kill, f) = Source.fromIterator(() => iter)
        .map { case LogMessage(service, instance, tag, message) =>
          mfl
            .logger(service, instance, tag)
            .info(message)
          mfl
            .logger(service, tag)
            .info(message)
        }
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.ignore)(Keep.both)
        .run()
      val r = Future.fromTry(Try {
        f.futureValue
      })
        .recoverWith { case NonFatal(th) =>
          kill.abort(th)
          f
        }

      r.futureValue
    }

    "reload" in {
      val graph = Source.single(ModelGenerators.LogMessageGen.next)
        .map { case LogMessage(service, instance, tag, message) =>
          mfl.logger(service, tag).info(message)
        }
      graph.runWith(Sink.ignore).futureValue
      reload()
      graph.runWith(Sink.ignore).futureValue
      firstFile(dir).length() > 0 shouldEqual true
    }

    def firstFile(dir: File): File = {
      if (dir.isFile) {
        dir
      } else {
        firstFile(dir.listFiles().head)
      }
    }

    "handle sighup" ignore {
      val msg = ModelGenerators.LogMessageGen.next

      mfl.logger(msg.service, msg.tag).info("data")
      io.Source.fromFile(firstFile(dir))
        .getLines()
        .size shouldEqual 1

      Signal.raise(new Signal("HUP"))
      Thread.sleep(1000)

      firstFile(dir).delete()

      mfl.logger(msg.service, msg.tag).info("data")
      Thread.sleep(1000)
      io.Source.fromFile(firstFile(dir))
        .getLines()
        .size shouldEqual 1
    }
  }

  import scala.concurrent.duration.DurationInt

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(500.seconds, 100.millis)
}
