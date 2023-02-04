package ru.yandex.verba.core.util

import akka.util.Timeout
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.core.application._

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

/**
  * User: Vladislav Dolbilov (darl@yandex-team.ru)
  * Date: 20.02.13 2:59
  */
class VerbaUtilsTest extends AnyFreeSpec with VerbaUtils with Logging {
  implicit val timeout: FiniteDuration = 10.hours
  implicit val tout = Timeout(timeout)
  implicit val ec = system.dispatcher

  "Measure future work" ignore {
    //should log > 2000 ms
    Future {
      Thread.sleep(2000); 1
    }.measuredF("woow").await
  }

  "Lazy view" - {
    val elems = Seq(() => 1, () => 2, () => 3, () => throw new IllegalStateException())
    "Simple lazy map" in {
      val Seq(1, 2) = elems.view.map(_.apply()).take(2).toSeq
    }
    "Iterable grouped method" in {
      val List(Seq(1), Seq(2)) = elems.view.map(_.apply()).grouped(1).take(2).map(_.toSeq).toList
    }
    //todo not works
    "Traversable grouped method" ignore {
      val List(List(1), List(2)) = elems.view.map(_.apply()).grouped(1).take(2).toList.map(_.toList)
      elems.view.map(_.apply()).grouped(1).take(2).toList

      intercept[IllegalStateException] {
        //unfortunately, it shouldn't work due nature of traversable (access to next elem)
        elems.view.map(_.apply()).grouped(1).take(3).toList
      }
      intercept[IllegalStateException] {
        elems.view.map(_.apply()).grouped(1).take(4).toList
      }
    }
    "Par map" in {
//      val Seq(1, 2) = view.map(x => Future.successful(x()).await).take(2).toSeq
//       view.parMap(Future.successful, 1).map(_.apply()).take(2).toSeq

      //      val Seq(1, 2) = view.map(_.apply()).take(2).toSeq

//      intercept[IllegalStateException] {
//        view.parMap(Future.successful).map(_.apply()).take(3).toSeq
//      }
    }
  }
}
