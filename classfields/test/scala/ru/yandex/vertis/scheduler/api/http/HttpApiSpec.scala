package ru.yandex.vertis.scheduler.api.http

import java.io.IOException
import java.util.UUID

import com.google.common.io.Closer
import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.util.EntityUtils
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthChecks}
import ru.yandex.vertis.scheduler.CloseableScheduler
import ru.yandex.vertis.scheduler.api.http.HttpApiSpec.CheckResult
import ru.yandex.vertis.scheduler.api.http.HttpApiSpec.CheckResult.{CanNotConnect, NotFound, Ok}
import ru.yandex.vertis.scheduler.builder.Builders
import ru.yandex.vertis.scheduler.impl.jvm.JvmLockManager
import ru.yandex.vertis.scheduler.model.Payload.Sync
import ru.yandex.vertis.scheduler.model.Schedule.EverySeconds
import ru.yandex.vertis.scheduler.model.{Payload, Schedule, Task, TaskDescriptor}

import scala.concurrent.duration._

/**
  * Specs on scheduler HTTP API.
  *
  * @author logab
  */
class HttpApiSpec
  extends WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  val log = LoggerFactory.getLogger(classOf[HttpApiSpec])

  val ApiPort = 36100
  val SchedulerName = "service-name"
  val serviceName = "service-name"

  val closer = Closer.create()

  "Scheduler with its API" should {
    "close http api" in {
      val client = HttpClientBuilder.create().build()
      val scheduler = createInternalHttpApiScheduler(serviceName)
      scheduler.start()
      assureAvailability(client, serviceName) match {
        case Ok(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      scheduler.shutdown(100.millis)
      assureAvailability(client, serviceName) match {
        case CanNotConnect => info("Done")
        case other => fail(s"Unexpected $other")
      }
      val secondScheduler = createInternalHttpApiScheduler(serviceName)
      secondScheduler.start()
      assureAvailability(client, serviceName) match {
        case Ok(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      secondScheduler.shutdown(100.millis)
      assureAvailability(client, serviceName) shouldEqual CanNotConnect
    }

    "not close external http api" in {
      val client = HttpClientBuilder.create().build()
      withHttpServer(new SchedulerHttpServer(ApiPort)) {
        httpService =>
          val scheduler = createExternalHttpApiScheduler(httpService, serviceName)
          scheduler.start()
          assureAvailability(client, serviceName) match {
            case Ok(_) => info("Done")
            case other => fail(s"Unexpected $other")
          }
          scheduler.shutdown(100.millis)
          assureAvailability(client, serviceName) shouldEqual NotFound
      }
      assureAvailability(client, serviceName) shouldEqual CanNotConnect
    }

    "register several schedulers to one external http api" in {
      val secondServiceName = "second-service-name"
      val client = HttpClientBuilder.create().build()
      withHttpServer(new SchedulerHttpServer(ApiPort)) {
        httpService =>
          val scheduler = createExternalHttpApiScheduler(httpService, serviceName)
          scheduler.start()
          val secondScheduler = createExternalHttpApiScheduler(httpService, secondServiceName)
          secondScheduler.start()
          assureAvailability(client, serviceName) match {
            case Ok(_) => info("Done")
            case other => fail(s"Unexpected $other")
          }
          assureAvailability(client, secondServiceName) match {
            case Ok(_) => info("Done")
            case other => fail(s"Unexpected $other")
          }
          scheduler.shutdown(100.millis)
      }
    }

    "fail to register several schedulers with same name" in {
      val serviceName = "service-name"
      withHttpServer(new SchedulerHttpServer(ApiPort)) {
        httpService =>
          val scheduler = createExternalHttpApiScheduler(httpService, serviceName)
          scheduler.start()
          intercept[IllegalStateException] {
            val secondScheduler = createExternalHttpApiScheduler(httpService, serviceName)
            secondScheduler.start()
          }
          scheduler.shutdown(100.millis)
      }
    }

    "answer pong" in {
      val client = HttpClientBuilder.create().build()
      val scheduler = createInternalHttpApiScheduler(serviceName)
      scheduler.start()
      checkPing(client) match {
        case Ok(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      scheduler.shutdown(100.millis)
      checkPing(client) match {
        case CanNotConnect => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }

//    //TODO: fix test stability
//    "pass prettyPrint'ed long message" in {
//      val client = HttpClientBuilder.create().build()
//      withHttpServer(new SchedulerHttpServer(ApiPort)) {
//        httpService => val ex = () => new IOException("Communications link failure\n\nThe last packet successfully received from the server was 30 027 milliseconds ago.  The last packet sent successfully to the server was 30 027 milliseconds ago.\ncom.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure\n\nThe last packet successfully received from the server was 30 027 milliseconds ago.  The last packet sent successfully to the server was 30 027 milliseconds ago.")
//          val task = Task(
//            TaskDescriptor("test-fail", Schedule.Manually),
//            Sync(() => throw ex())
//          )
//          val scheduler =
//            createExternalHttpApiScheduler(httpService, serviceName, Iterable(task))
//          scheduler.start()
//          scheduler.getApi.offer(task.descriptor.id, None).get
//          Thread.sleep(500)
//          assureAvailability(client, serviceName) match {
//            case Ok(r) =>
//              r should include(Util.prettyPrint(ex().getMessage))
//            case other => fail(s"Unexpected $other")
//          }
//          scheduler.shutdown(100.millis)
//      }
//    }
  }

  private def withHttpServer[A](server: SchedulerHttpServer)
                               (action: SchedulerHttpServer => A): A =
    try {
      action(server)
    } finally {
      server.close()
    }

  override protected def afterAll(): Unit = {
    closer.close()
    super.afterAll()
  }

  def createScheduler(serviceName: String) = {
    val instanceId = UUID.randomUUID().toString.take(5)
    val descriptor = TaskDescriptor("id", EverySeconds(100))
    val task = Task(descriptor, Payload.Sync(() => ()))
    Builders.newJvmSchedulerBuilder().
      setMaxConcurrentTasks(1).
      setSchedulerInstanceId(instanceId).
      setSchedulerName(serviceName).
      setLockManager(new JvmLockManager).
      setHealthChecks(new CompoundHealthCheckRegistry()).
      register(task)
  }

  def createInternalHttpApiScheduler(serviceName: String) = {
    val scheduler = createScheduler(serviceName).
      setApiPort(ApiPort).
      build()
    closer.register(CloseableScheduler(scheduler))
    scheduler
  }

  def createExternalHttpApiScheduler(schedulerHttpServer: SchedulerHttpServer,
                                     serviceName: String,
                                     tasks: Iterable[Task] = Iterable.empty) = {
    val scheduler = createScheduler(serviceName).
      setHttpServer(schedulerHttpServer).
      register(tasks).
      build()
    closer.register(CloseableScheduler(scheduler))
    scheduler
  }

  def assureAvailability(client: CloseableHttpClient,
                         serviceName: String): CheckResult = try {
    val response: CloseableHttpResponse = client
      .execute(new HttpGet(s"http://localhost:$ApiPort/$serviceName/status"))
    val entity: HttpEntity = response.getEntity
    val bytes = new Array[Byte](entity.getContentLength.toInt)
    entity.getContent.read(bytes)
    entity.getContent.close()
    EntityUtils.consume(entity)
    val resultString = new String(bytes)
    if (resultString == "<h1>404 Not Found</h1>No context found for request")
      NotFound
    else
      Ok(resultString)
  } catch {
    case e: Exception => CanNotConnect
  }

  def checkPing(client: CloseableHttpClient): CheckResult = try {
    val response: CloseableHttpResponse = client
      .execute(new HttpGet(s"http://localhost:$ApiPort/ping"))
    val entity: HttpEntity = response.getEntity
    val bytes = new Array[Byte](entity.getContentLength.toInt)
    entity.getContent.read(bytes)
    entity.getContent.close()
    EntityUtils.consume(entity)
    val resultString = new String(bytes)
    if (resultString == "<h1>404 Not Found</h1>No context found for request") {
      NotFound
    } else Ok(resultString)
  } catch {
    case _: Exception => CanNotConnect
  }
}

object HttpApiSpec {

  trait CheckResult

  object CheckResult {

    case object NotFound extends CheckResult

    case object CanNotConnect extends CheckResult

    case class Ok(result: String) extends CheckResult

  }

}
