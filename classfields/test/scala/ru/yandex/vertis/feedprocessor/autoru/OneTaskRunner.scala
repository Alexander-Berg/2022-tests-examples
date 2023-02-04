package ru.yandex.vertis.feedprocessor.autoru

import io.prometheus.client.Counter
import ru.auto.feedprocessor.FeedprocessorModel.OffersResponse
import ru.yandex.common.tokenization.TokensDistribution
import ru.yandex.vertis.feedprocessor.autoru.scheduler.app.{DefaultSchedulerComponents, SchedulerComponents}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.{AutoruExternalOffer, AutoruOfferResult}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.AutoruParser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.parser.Parser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.picker.TaskPicker
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.util.TokenDistributingUtils
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.{CombinedPipelines, _}

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.concurrent.Future

/**
  * Обрабатывает один раз одну таску из DEV базы, независимо от ее статуса (может брать в работу completed таски)
  * Параметры:
  * "--task 123" - таска на выполнение
  */
object OneTaskRunner
  extends CombinedPipelines[AutoruExternalOffer, AutoruOfferResult, OffersResponse]
  with KafkaProducerFlow
  with OpsFeedProcessorPipeline[AutoruExternalOffer, AutoruOfferResult]
  with ToyotaFlow[AutoruOfferResult]
  with AutoruSenderFlow
  with OpsAutoruAnswerPipeline
  with AutoruFailureSenderFlow {

  import actorSystem.dispatcher

  lazy val schedulerComponents: SchedulerComponents = new DefaultSchedulerComponents(components)

  override def parser: Parser[AutoruExternalOffer] = new AutoruParser

  override def mapper: AutoruMapperFlow = new AutoruMapperFlow(schedulerComponents)

  override protected def mapperFlowsCounter: Counter = mapper.mapperFlowsCounter

  private val args = new AtomicReference[Args]
  private val taskStarted = new AtomicBoolean(false)

  override lazy val taskPicker: TaskPicker = { _ =>
    if (!taskStarted.getAndSet(true)) {
      Future.successful(components.tasksDao.findById(args.get().task).toList)
    } else {
      Future.successful(Nil)
    }
  }

  override lazy val taskDistribution: TokensDistribution =
    TokenDistributingUtils.create(
      components.zkClient,
      components.env.hostName,
      components.appConf.getInt("task-buckets"),
      "one_task_runner"
    )

  onStart {
    //todo зачем тут вообще val?
    val (taskTicker, answersConsumer, done) = fullOffersProcessingGraph().run()
  }

  onStop(schedulerComponents.close())

  override def main(args: Array[String]): Unit = {
    this.args.set(parseArgs(args))
    super.main(args)
  }

  private def parseArgs(args: Array[String]): Args = {
    var task: Option[Long] = None
    args.sliding(2, 2).toList.collect {
      case Array("--task", taskStr: String) => task = Some(taskStr.toLong)
    }
    Args(task.get)
  }

  private case class Args(task: Long)
}
