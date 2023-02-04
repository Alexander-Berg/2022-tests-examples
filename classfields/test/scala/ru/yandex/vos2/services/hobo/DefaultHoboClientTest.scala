package ru.yandex.vos2.services.hobo

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.hobo.proto.ModelFactory
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vos2.util.http.MockHttpClientHelper

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.12.16
  */
@RunWith(classOf[JUnitRunner])
class DefaultHoboClientTest extends FunSuite with MockHttpClientHelper with Matchers {

  private def createClient(status: Int, body: Array[Byte]): DefaultHoboClient = {
    new DefaultHoboClient("localhost", 90, TestOperationalSupport) {
      override protected def doRequest[T <: HttpRequestBase, R](name: String, request: T)
                                                               (f: (HttpResponse) => R): R = {
        val response = mockBinaryResponse(status, body)
        f(response)
      }
    }
  }

  test("create task") {
    val queue = QueueId.AUTO_RU_TELEPONY
    val taskSource = ModelFactory.newTaskSourceBuilder().build()

    val task = ModelFactory.newTaskBuilder().build()

    val client = createClient(200, task.toByteArray)

    client.createTask(queue, taskSource) shouldBe task
  }

  test("throw exception on unexpected response") {
    val queue = QueueId.AUTO_RU_TELEPONY
    val taskSource = ModelFactory.newTaskSourceBuilder().build()

    val task = ModelFactory.newTaskBuilder().build()

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
