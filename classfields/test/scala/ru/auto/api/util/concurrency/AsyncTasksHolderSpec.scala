package ru.auto.api.util.concurrency

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.model.StatusCodes
import io.prometheus.client.Counter
import ru.auto.api.auth.Application
import ru.auto.api.model.{RequestParams, Version}
import ru.auto.api.util.RequestImpl
import ru.auto.api.{AsyncTasksSupport, BaseSpec}

import scala.concurrent.Future

class AsyncTasksHolderSpec extends BaseSpec with AsyncTasksSupport {

  implicit val request: RequestImpl = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setVersion(Version.V1_0)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.named("test")
    r
  }

  "AsyncTasksHolder" should {
    "execute some tasks asynchronously" in {
      val customCounter = Counter.build("custom", "help").labelNames("name", "error").create()
      val testInt = new AtomicInteger(0)
      val tasks = new AsyncTasksHolder()
      tasks.add(() => Future.unit)
      tasks.add(() =>
        Future {
          testInt.set(3)
        }
      )
      Thread.sleep(100)
      testInt.get() shouldBe 0
      val futures = tasks.start(StatusCodes.OK)(directExecutor, customCounter, request)
      futures.size shouldBe 2
      futures.foreach(_.await)
      testInt.get() shouldBe 3
      customCounter.labels("test", "false").get() shouldBe 2
    }

    "fail on double start" in {
      val tasks = new AsyncTasksHolder()
      tasks.add(() => Future.unit)
      tasks.add(() => Future.unit)
      tasks.start(StatusCodes.OK)
      an[IllegalStateException] should be thrownBy {
        tasks.start(StatusCodes.OK)
      }
    }

    "fail on add task after start" in {
      val tasks = new AsyncTasksHolder()
      tasks.add(() => Future.unit)
      tasks.add(() => Future.unit)
      tasks.start(StatusCodes.OK)
      an[IllegalStateException] should be thrownBy {
        tasks.add(() => Future.unit)
      }
    }

    "catch exception in future creation function" in {
      val tasks = new AsyncTasksHolder()
      tasks.add(() => Future.unit)
      tasks.add(() => throw new IllegalAccessException())
      val result = tasks.start(StatusCodes.OK)
      an[IllegalAccessException] should be thrownBy {
        result.last.await
      }
    }
  }
}
