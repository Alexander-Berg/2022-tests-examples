package ru.yandex.vertis.vsquality.techsupport.util

/**
  * @author potseluev
  */
trait Clearable[T] {
  def clear(): Unit
}

object Clearable {

  implicit class Ops[T](val t: T) extends AnyVal {

    def clear()(implicit clearable: Clearable[T]): Unit =
      clearable.clear()
  }

}
