package ru.yandex.verba.core

import akka.util.Timeout
import org.scalatest.Ignore
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.time.{Seconds, Span}
import ru.yandex.verba.core.application._
import ru.yandex.verba.core.manager.TermManager
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.pipeline._
import ru.yandex.verba.core.storage.entity
import ru.yandex.verba.core.util.VerbaUtils

import java.io.PrintWriter
import scala.annotation.nowarn
import scala.concurrent.duration._

/**
 * Author: Evgeny Vanslov (evans@yandex-team.ru)
 * Created: 07.08.14
 */
//-Xmx50m
@Ignore
@nowarn("cat=w-flag-value-discard")
class StreamManagerTest extends AnyFreeSpec with VerbaUtils {
  DBInitializer
  val termManager = TermManager.ref
  implicit val timeout: FiniteDuration = 10.hours
  implicit val tout = Timeout(timeout)
  implicit val ec = system.dispatcher
  val pipeline =
    requestResponse(entity.ref) ~> loadAttributes(storage.attributes.ref) ~> resolveLinks(entity.ref)

  "Entity lazy view " in {
    failAfter(Span(5, Seconds)) {
      val data = StreamManager.ref.getEntityStream(Path("/auto/authorized-dealers")).await
      data.toSeq.take(10).toSeq
    }
  }

  "Term lazy view " ignore {
    failAfter(Span(5, Seconds)) {
      val data = StreamManager.ref.getEntityStream(Path("/auto/authorized-dealers")).await.asTermStream
      data.toSeq.take(10).toSeq
    }
  }

  "Term stack lazy view " in {
    failAfter(Span(1, Seconds)) {
      val data = StreamManager.ref.getEntityStream(Path("/auto/marks")).await.asTermStream.asStackStream
      using(new PrintWriter("temp.txt")) { pw =>
        data.foreach {
          case (term, stack) => pw.println(term.entity.name + " Stack: " + stack.map(_.entity.code).mkString(" "))
        }
      }
    }
  }

  "Term stack filter view " ignore {
    val data = StreamManager.ref.getEntityStream(Path("/auto/marks/ANADOL")).await.asTermStream.asStackStream
      .filterStream(
        e => Path("/auto/marks/ANADOL/models/BOCEK").startsWith(e.entity.path)
      )
    using(new PrintWriter("temp.txt")) { pw =>
      data.foreach {
        case (term, stack) => pw.println(term.entity.name + " Stack: " + stack.map(_.entity.code).mkString(" "))
      }
    }

  }

  "Test" ignore {
      StreamManager.fullRef.getEntityStream(Path("/auto/marks")).await
        .asTermStream
        .foreach {
        t =>
          logger.info(t.entity.path.toString)
          println(t.entity.path)
      }
  }
}
