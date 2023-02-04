package vertis.yt.hooch.testkit

import vertis.yt.zio.hooch.output.Output
import zio.{RIO, UIO}

/**
  */
object TestOutput {

  def passToState[T]: Output[Any, Iterable[T], T] = new Output[Any, Iterable[T], T] {
    override def consume(state: Iterable[T], jobs: Iterable[T]): RIO[Any, Iterable[T]] = UIO(jobs)
  }

  def acceptOnly[T](xs: T*): Output[Any, Iterable[T], T] = new Output[Any, Iterable[T], T] {
    override def consume(state: Iterable[T], jobs: Iterable[T]): RIO[Any, Iterable[T]] = UIO(jobs)

    override def shouldProcess(job: T): RIO[Any, Boolean] = UIO(xs.contains(job))
  }

}
