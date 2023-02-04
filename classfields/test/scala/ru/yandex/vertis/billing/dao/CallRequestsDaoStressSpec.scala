package ru.yandex.vertis.billing.dao

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import java.util.concurrent.{ArrayBlockingQueue, ThreadPoolExecutor, TimeUnit}

import ru.yandex.vertis.billing.dao.CallsSearchDao.ForCallFactFilter
import ru.yandex.vertis.billing.model_core.gens.{campaignCallGen, Producer}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import ru.yandex.vertis.util.concurrent.future.RichFuture
import scala.util.Success

/**
  * Specs on [[CampaignCallDao]]
  */
trait CallRequestsDaoStressSpec extends CallsSearchDaoBaseSpec {

  protected def callRequestsDaoFactory: CallsSearchDaoFactory

  def operationsCount = 500
  def factsCount = 500

  private val executor = new ThreadPoolExecutor(
    5,
    5,
    1,
    TimeUnit.MINUTES,
    new ArrayBlockingQueue[Runnable](10),
    new CallerRunsPolicy
  )

  implicit protected val executorContext =
    ExecutionContext.fromExecutorService(executor)

  @volatile
  private var error: Option[Throwable] = None

  private val window = 2.hours

  private val facts = campaignCallGen().next(factsCount)
  private val requests = facts.map(asRecord)

  "Stress test" should {
    "work" in {
      (1 to operationsCount).foreach { _ =>
        val result = callRequestsDaoFactory.instance().map(operate)
        result.onFailureEffect { e =>
          error = Some(e)
        }
      }

      executor.shutdown()
      executor.awaitTermination(10, TimeUnit.MINUTES)

      error.foreach { e =>
        fail(e)
      }
    }
  }

  private def operate(dao: CallsSearchDao) = {
    dao.write(requests).get
    val s = facts
      .map(f =>
        dao.get(ForCallFactFilter(f.fact, window)) match {
          case Success(r) =>
            r.map(_ => 1).getOrElse(0)
          case _ => 0
        }
      )
      .sum
    assert(s > 0)
    dao.cleanup().get
  }

}
