package ru.yandex.vertis.promocoder

import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

/** Extension for [[ScalaFutures]]
  *
  * @author alex-kovalenko
  */
trait SpecWithFutures extends ScalaFutures {

  def shouldFailWith[E <: Exception: ClassTag](future: Future[_])(implicit timeout: Duration = 3.second): E = {
    Assertions.intercept[E] {
      Await.result(future, timeout)
    }
  }

}
