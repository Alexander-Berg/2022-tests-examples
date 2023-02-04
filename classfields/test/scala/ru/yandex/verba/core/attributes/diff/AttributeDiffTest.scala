package ru.yandex.verba.core.attributes.diff

import akka.util.Timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.application._
import ru.yandex.verba.core.attributes._
import ru.yandex.verba.core.manager.TermManager
import ru.yandex.verba.core.model.Entity
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.VerbaUtils

import scala.concurrent.duration.{FiniteDuration, _}

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 22.07.14
  */
/**
  * Important test. All tests should work!
  */
class AttributeDiffTest extends AnyFlatSpec with Matchers with VerbaUtils {
  DBInitializer

  val termManager = TermManager.ref
  implicit val timeout: FiniteDuration = 10.hours
  implicit val tout = Timeout(timeout)
  implicit val ec = system.dispatcher

  "" should "be empty" in {
    val attrs1 = Link(Entity("a", "A", Path("/a")))
    Thread.sleep(1000)
    val attrs2 = Link(Entity("a", "A", Path("/a")))
    AttributeDiff.apply(attrs1, attrs2) match {
      case NotChanged =>
      case x => throw new IllegalStateException(s"attr should be equals $x")
    }
  }

  /*

  "" should "be same" in {
    val paths = Seq(
      "/auto/authorized-dealers/20134925",
      "/auto/marks/ALFA_ROMEO/models/145/super-gen/6015736/configurations/6015737/tech-params/6015742"
    )
    paths.foreach {
      path =>
      val t1 = termManager.getFullTerm(Path(path)).await
      val t2 = termManager.getFullTerm(Path(path)).await
      val attrPair: Seq[(Attribute, Attribute)] = t1.entity.attributes.values zip t2.entity.attributes.values
      attrPair.toList map { case (x, y) =>
        val diff = AttributeDiff(x, y)
        diff match {
          case NotChanged =>
          case x => throw new IllegalStateException(s"attr should be equals $x")
        }
      }
    }

  }
 */
}
