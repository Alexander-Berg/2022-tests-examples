package vertis.yt.storage

import vertis.zio.BaseEnv
import zio.{RIO, RefM, UIO}

/**
  */
class AtomicStorageTestImpl[T](storage: RefM[T]) extends AtomicStorage[BaseEnv, T] {

  override def get: RIO[Any, T] = storage.get

  override def updateM[R1 <: Any, A](f: T => RIO[R1, (T, A)]): RIO[R1, A] = storage.modify { v =>
    f(v).map(_.swap)
  }
}

object AtomicStorageTestImpl {
  def create[T](initValue: T): UIO[AtomicStorageTestImpl[T]] = RefM.make(initValue).map(new AtomicStorageTestImpl(_))
}
