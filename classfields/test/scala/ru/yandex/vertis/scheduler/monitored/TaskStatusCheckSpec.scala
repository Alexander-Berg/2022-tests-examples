package ru.yandex.vertis.scheduler.monitored

import com.typesafe.config.ConfigFactory
import org.joda.time.Duration
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.scheduler.TestData
import ru.yandex.vertis.scheduler.model.{Job, JobCompleted, JobFailed, TaskContext}
import ru.yandex.vertis.scheduler.now

import scala.util.Try

/**
 * Specs on [[TaskStatusCheck]]
 *
 * @author dimas
 */
class TaskStatusCheckSpec
  extends Matchers
  with WordSpecLike {
  
  private var isFailed: Boolean = false

  def contexts() = {
    val descriptor = TestData.descriptor1
    val result = if (isFailed)
      JobFailed(now(), Duration.ZERO, "foo")
    else
      JobCompleted(now(), Duration.ZERO)
    val job = Job(
      now().minusHours(1),
      TestData.instance1,
      config = ConfigFactory.empty(),
      Some(result))
    val context = TaskContext(descriptor, Some(job))
    Try(Iterable(context))
  }

  private val healthCheck =
    new TaskStatusCheck(contexts())
  
  "TaskStatusCheck" should {
    "be healthy" in {
      isFailed = false 
      healthCheck.check().isHealthy should be(true)
    }
    "be unhealthy" in {
      isFailed = true
      healthCheck.check().isHealthy should be(false)
    }
  }
}
