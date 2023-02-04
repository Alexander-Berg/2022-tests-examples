package ru.yandex.vertis.vsquality.hobo.sender

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, MessageEntity}
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.concurrent.ScalaFutures
import org.specs2.mock.Mockito.{one, spy, there}
import ru.yandex.vertis.hobo.proto.Model.Response.ContentType
import ru.yandex.vertis.vsquality.hobo.kafka.taskresults.TaskResultSerializerDeserializer
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.{HttpResponseGen, NotificationInfoGen, TaskGen}
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer._
import ru.yandex.vertis.vsquality.hobo.model.{KafkaResponse, Task}
import ru.yandex.vertis.vsquality.hobo.sender.impl.TaskSenderImpl
import ru.yandex.vertis.vsquality.hobo.util.QueuedAkkaHttpClient
import ru.yandex.vertis.vsquality.hobo.view.json.TaskView
import ru.yandex.vertis.vsquality.hobo.view.protobuf.ProtobufView
import ru.yandex.vertis.vsquality.utils.test_utils.KafkaSpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

class TaskSenderImplSpec extends KafkaSpecBase with ScalaFutures {

  implicit val materializer = Materializer.matFromSystem
  implicit val taskResultsSerDeser = new TaskResultSerializerDeserializer

  lazy val settings: ProducerSettings[String, Task] =
    ProducerSettings(actorSystem, new StringSerializer, taskResultsSerDeser)
      .withBootstrapServers(connectionString)

  lazy val taskResultsProducer = spy(new StubTaskResultsProducer(settings))
  lazy val queuedAkkaHttpClient = spy(new QueuedAkkaHttpClient)
  lazy val taskSender = new TaskSenderImpl(queuedAkkaHttpClient, taskResultsProducer)

  "TaskSender" should {

    "call TaskResultsProducer in case of KafkaResponse" in {
      val task: Task =
        TaskGen.next.copy(
          notificationInfo = NotificationInfoGen.next.copy(response = Some(KafkaResponse))
        )

      taskSender.send(task)
      there.was(
        one(taskResultsProducer)
          .append(task)
      )
    }

    "call QueuedAkkaHttpClient in case of HttpResponse" in {
      val httpResponse = HttpResponseGen.next
      val httpRespNotificationInfo = NotificationInfoGen.next.copy(response = Some(httpResponse))
      val task: Task =
        TaskGen.next.copy(
          notificationInfo = httpRespNotificationInfo
        )
      implicit val marshaller =
        httpResponse.contentType match {
          case ContentType.APPLICATION_JSON     => TaskView.modelMarshaller
          case ContentType.APPLICATION_PROTOBUF => ProtobufView.Task.marshaller
        }
      val body = Marshal(task).to[MessageEntity].futureValue
      val request =
        HttpRequest(
          method = HttpMethods.POST,
          uri = httpResponse.url.toString,
          entity = body
        )

      taskSender.send(task)
      there.was(
        one(queuedAkkaHttpClient)
          .singleRequest(request)
      )
    }

    "throw an exception in case of None" in {
      val noneRespNotificationInfo = NotificationInfoGen.next.copy(response = None)
      val task: Task = TaskGen.next.copy(notificationInfo = noneRespNotificationInfo)

      taskSender.send(task)
      there.was(Failure(new IllegalArgumentException(s"Response must be set for task: $task")))
    }
  }
}
