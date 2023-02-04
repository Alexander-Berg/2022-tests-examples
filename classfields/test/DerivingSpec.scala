package ru.yandex.vertis.vsquality.utils.cats_utils

import cats.implicits._
import cats.laws.discipline.MonadTests
import cats.{~>, Monad}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

/**
  * @author potseluev
  */
class DerivingSpec extends AnyFunSuite with FunSuiteDiscipline with Configuration {

  private type F[A] = Either[Unit, A]
  private type G[A] = Option[A]

  implicit private val fg: F ~> G = λ[F ~> G](_.toOption)

  implicit private val gf: G ~> F =
    λ[G ~> F] {
      case Some(value) => Right(value)
      case None        => Left(())
    }

  implicit private val derivedMonadInstance: Monad[G] = Deriving.deriveMonad[F, G]

  private val monadTests = MonadTests(derivedMonadInstance).monad[Int, Double, String]

  checkAll("Deriving.deriveMonad.monadTests", monadTests)
}
