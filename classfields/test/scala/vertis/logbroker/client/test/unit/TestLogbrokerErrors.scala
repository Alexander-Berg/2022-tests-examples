package vertis.logbroker.client.test.unit

import com.yandex.ydb.persqueue.PersqueueErrorCodes.EErrorCode
import ru.yandex.kikimr.persqueue.consumer.ConsumerException
import vertis.logbroker.client.LbTask
import vertis.logbroker.client.model.LogbrokerError
import zio.{IO, Ref, UIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
object TestLogbrokerErrors {

  val nonRetryable: LogbrokerError =
    LogbrokerError.translateLbException(new ConsumerException(EErrorCode.ACCESS_DENIED, "you shall not pass"))

  val retryable: LogbrokerError =
    LogbrokerError.translateLbException(new IllegalStateException("I have paws"))

  def failNTimes(n: Int): UIO[LbTask[Unit]] = Ref.make(0).map {
    _.getAndUpdate(_ + 1).flatMap { i =>
      if (i < n) IO.fail(retryable)
      else UIO.unit
    }
  }
}
