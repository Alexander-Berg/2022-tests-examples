package vertis.yt.hooch.testkit

import vertis.yt.zio.hooch.input.DayInput.DayInput
import vertis.yt.zio.hooch.input.Input
import vertis.yt.zio.hooch.jobs.DayPartition
import zio.{RIO, UIO}

import java.time.{Instant, LocalDate}

/**
  */
object TestInput {

  def of[T](v: T*): Input[Any, Any, T] = new Input[Any, Any, T] {
    override def changes(state: Any): RIO[Any, Iterable[T]] = UIO(v)
  }

  val empty: Input[Any, Any, Nothing] = of()

  def ofDays(ds: LocalDate*): DayInput[Any, Any] = of(ds.map(DayPartition(_, Instant.now())): _*)

}
