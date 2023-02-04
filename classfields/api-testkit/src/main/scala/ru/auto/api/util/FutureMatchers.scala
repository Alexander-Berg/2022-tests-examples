package ru.auto.api.util

import org.scalactic.source.Position
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.concurrent.Future
import scala.reflect._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 10.04.18
  */
trait FutureMatchers extends ScalaFutures with IntegrationPatience {

  def failWith[E <: Throwable](implicit pc: PatienceConfig, ct: ClassTag[E], pos: Position): Matcher[Future[_]] = {
    Matcher[Future[_]] { f =>
      val res = f.failed.futureValue(pc, pos)
      val eClass = classTag[E].runtimeClass
      MatchResult(
        eClass.isAssignableFrom(res.getClass),
        "{} should be an instance of {} bot got {}",
        "{} is an instance of {}",
        Vector(res, eClass.getName, res.getClass.getName),
        Vector(res, eClass.getName)
      )
    }
  }

  def beSuccessful(implicit pc: PatienceConfig, pos: Position): Matcher[Future[_]] = Matcher[Future[_]] { f =>
    f.futureValue(pc, pos)
    MatchResult(
      matches = true,
      "Future should complete without errors",
      "Future should complete with error"
    )
  }

  def completeWith[R](expected: R)(implicit pc: PatienceConfig, pos: Position): Matcher[Future[_]] =
    Matcher[Future[_]] { f =>
      val res = f.futureValue(pc, pos)
      MatchResult(
        res == expected,
        "Future completed with {} but expected {}",
        "Future did complete with {}",
        Vector(res, expected),
        Vector(expected)
      )
    }
}

object FutureMatchers extends FutureMatchers
