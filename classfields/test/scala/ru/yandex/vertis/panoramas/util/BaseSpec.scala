package ru.yandex.vertis.panoramas.util

import com.google.common.util.concurrent.MoreExecutors
import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait BaseSpec
  extends WordSpecLike
  with Matchers
  with ScalaFutures
  with Eventually
  with BeforeAndAfter
  with BeforeAndAfterAll
  with GeneratorUtils {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  implicit protected val directExecutor: ExecutionContext =
    ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  implicit class AwaitableFuture[T](f: Future[T]) {
    //scalastyle:off awaitresult
    def await: T = Await.result(f, Duration.Inf)
    //scalastyle:on awaitresult
  }

  implicit class RichOngoingStub[T](stub: OngoingStubbing[Future[T]]) {

    def thenReturnF(result: T): OngoingStubbing[Future[T]] =
      stub.thenReturn(Future.successful(result))

    def thenThrowF(t: Throwable): OngoingStubbing[Future[T]] =
      stub.thenReturn(Future.failed(t))
  }

}
