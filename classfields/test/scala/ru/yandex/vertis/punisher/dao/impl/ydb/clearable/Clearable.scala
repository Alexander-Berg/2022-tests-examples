package ru.yandex.vertis.punisher.dao.impl.ydb.clearable

trait Clearable[T] {
  def clear(): Unit
}

object Clearable {

  def apply[T: Clearable]: Clearable[T] = implicitly[Clearable[T]]

  implicit class ClearableOps[T](val t: T) extends AnyVal {

    def clear()(implicit clearable: Clearable[T]): Unit = clearable.clear()
  }

}
