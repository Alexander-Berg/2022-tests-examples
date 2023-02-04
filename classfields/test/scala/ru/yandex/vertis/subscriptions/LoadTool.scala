package ru.yandex.vertis.subscriptions

import java.util.concurrent.{ArrayBlockingQueue, Executors, RejectedExecutionHandler, ThreadPoolExecutor}

import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.subscriptions.Model._

import scala.util.{Failure, Success}

/**
  * VSSUBS-337: Generate number of subscriptions for matcher stress testing
  *
  * @author dimas
  */
class LoadTool extends Matchers with WordSpecLike {

  private val log = LoggerFactory.getLogger(getClass)

  private val client = new ApiClientImpl(
    "http://csbo1ft.yandex.ru:36134/api/1.x/subscriptions/service/realtyaccount/user",
    maxConcurrentRequests = 8
  )

  val rejectionHandler: RejectedExecutionHandler = new RejectedExecutionHandler {

    override def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor): Unit = {
      executor.getQueue.put(r)
    }
  }

  val pool = new ThreadPoolExecutor(
    8,
    8,
    0L,
    java.util.concurrent.TimeUnit.MINUTES,
    new ArrayBlockingQueue[Runnable](1024),
    rejectionHandler
  )

  private var count = 0

  "Api client" should {
    "create 10M subscriptions" in {
      for {
        uid <- 500000 to 1000000
        eventType <- Set("type1", "type2", "type3", "type4")
      } {
        val user = User.newBuilder().setUid(uid.toString).build
        val source = buildSubscriptionSource(uid, eventType)
        val task = new Runnable {
          override def run(): Unit =
            client.create(user, source) match {
              case Success(_) => log.info("Successfully created sub")
              case Failure(e) => log.error("Error while create sub", e)
            }
        }
        pool.submit(task)
        count += 1
        if (count % 100 == 0) {
          log.info(s"$count tasks submitted")
        }
      }

    }
  }

  private def buildSubscriptionSource(userId: Int, eventType: String) = {
    val delivery = OuterDelivery
      .newBuilder()
      .setEmail(
        OuterDelivery.Email
          .newBuilder()
          .setAddress("foo")
          .setPeriod(10)
      )
    val query = DSL.and(
      DSL.term(DSL.point("user-id", userId)),
      DSL.term(DSL.point("type", eventType))
    )
    val requestSource = RequestSource.newBuilder().setQuery(query)
    val state = ModelDSL.createState(Model.State.Value.ACTIVE)
    val view = OuterView.newBuilder().setTitle(userId.toString).setBody(eventType)
    OuterSubscriptionSource
      .newBuilder()
      .setDelivery(delivery)
      .setRequest(requestSource)
      .setState(state)
      .setView(view)
      .build()
  }

}
