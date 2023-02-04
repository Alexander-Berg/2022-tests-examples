package ru.auto.catalog

import com.google.common.util.concurrent.MoreExecutors
import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait BaseSpec
  extends AnyWordSpecLike
  with Matchers
  with ScalaFutures
  with Eventually
  with BeforeAndAfter
  with BeforeAndAfterAll
  with EitherValues
  with OptionValues
  with TryValues
  with MockitoSupport {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  implicit protected val sameThreadExecutor: ExecutionContext =
    ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  implicit class AwaitableFuture[T](f: Future[T]) {
    def await: T = Await.result(f, Duration.Inf)
  }

  implicit class RichOngoingStub[T](stub: org.mockito.stubbing.OngoingStubbing[Future[T]]) {
    def thenReturnF(result: T): OngoingStubbing[Future[T]] = stub.thenReturn(Future.successful(result))

    def thenThrowF(t: Throwable): OngoingStubbing[Future[T]] = stub.thenReturn(Future.failed(t))
  }
}
