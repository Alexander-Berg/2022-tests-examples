package ru.yandex.realty.logging

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LogUtilsSpec extends WordSpec with Matchers {

  "LogUtils.format" should {
    "make the requested amount of placeholders" in {
      assert(LogUtils.format(1) == "{}")
      assert(LogUtils.format(5) == "{} {} {} {} {}")
    }
  }

  private val that = this

  "LogUtilsSpec" should {

    "Return correctly class name for class without mixin" in {
      (new SomeFoo).getName shouldEqual "SomeFoo"
    }

    "Return correctly class name for class with mixin" in {
      (new SomeFoo with MeteredFoo).getName shouldEqual "SomeFoo"
    }

    "Return correctly class name for trait" in {
      new Foo[Int]() {
        def getName: String = LogUtils.findClassName(that).getSimpleName
      }.getName shouldEqual getClass.getSimpleName
    }
  }
}

trait Foo[T] {
  def getName: String
}

trait AbstractFoo[T] extends Foo[T] {
  override def getName: String = LogUtils.findClassName(getClass).getSimpleName
}

trait MeteredFoo extends AbstractFoo[Int]

class SomeFoo extends AbstractFoo[Int]
