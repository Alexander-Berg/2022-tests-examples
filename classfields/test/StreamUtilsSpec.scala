package vsquality.utils.cats_utils.test.scala.ru.yandex.vertis.quality.cats_utils

import cats.effect.{ContextShift, IO}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.vsquality.utils.cats_utils.{FutureUtil, StreamUtils}

import scala.concurrent.ExecutionContext

class StreamUtilsSpec extends AnyWordSpec with Matchers with ScalaFutures {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "StreamUtilsSpec" should {
    "create and read async stream" in {
      val stream = StreamUtils.fs2StreamFromAsyncCallback[IO, String] { cb =>
        new Thread(() => {
          cb(IO.pure(Some("test")))
          Thread.sleep(50)
          cb(IO.none)
        }).start()
      }

      val result = FutureUtil.fromF(stream.compile.toList).futureValue

      result should contain theSameElementsAs List("test")
    }
  }
}
