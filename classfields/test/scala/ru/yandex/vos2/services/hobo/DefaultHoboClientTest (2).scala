package ru.yandex.vos2.services.hobo

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.hobo.proto.Model.{QueueId, Task, TaskSource}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.util.http.MockHttpClientHelper

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.12.16
  */
@RunWith(classOf[JUnitRunner])
class DefaultHoboClientTest extends AnyFunSuite with MockHttpClientHelper with Matchers {
  implicit val trace = Traced.empty

  private def createClient(status: Int, body: Array[Byte]): DefaultHoboClient = {
    new DefaultHoboClient("localhost", 90, TestOperationalSupport) {
      override protected def doRequest[T <: HttpRequestBase, R](name: String, request: T)(
          f: (HttpResponse) => R
      )(implicit trace: Traced): R = {
        val response = mockBinaryResponse(status, body)
        f(response)
      }
    }
  }

  test("create task") {
    val queue = QueueId.AUTO_RU_TELEPONY
    val taskSource = TaskSource.newBuilder().setVersion(1).build()

    val task = Task.newBuilder().setVersion(1).build()

    val client = createClient(200, task.toByteArray)

    client.createTask(queue, taskSource) shouldBe task
  }

  test("throw exception on unexpected response") {
    val queue = QueueId.AUTO_RU_TELEPONY
    val taskSource = TaskSource.newBuilder().setVersion(1).build()

    val task = Task.newBuilder().setVersion(1).build()

    val client = createClient(409, task.toByteArray)

    intercept[Exception] {
      client.createTask(queue, taskSource)
    }
  }

  test("cancel task") {
    val queue = QueueId.AUTO_RU_TELEPONY
    val key = "1231"

    val client = createClient(200, Array.empty)

    client.cancelTask(queue, key) shouldBe true
  }

  test("cancel non existing task") {
    val queue = QueueId.AUTO_RU_TELEPONY
    val key = "1231"

    val client = createClient(404, Array.empty)

    client.cancelTask(queue, key) shouldBe false
  }
}
