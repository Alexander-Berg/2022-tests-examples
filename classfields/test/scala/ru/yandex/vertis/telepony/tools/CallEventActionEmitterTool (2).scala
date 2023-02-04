package ru.yandex.vertis.telepony.tools

import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.Config
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.telepony.component.impl.{AkkaComponentImpl, ApplicationComponentImpl, DomainKafkaComponentImpl, KafkaSupportComponentImpl}
import ru.yandex.vertis.telepony.component.{ComponentComponent, PrometheusComponent, TypedDomainComponent}
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals
import ru.yandex.vertis.telepony.model.Action.NoAction
import ru.yandex.vertis.telepony.model.{MtsCallEventAction, TypedDomains}
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.settings.KafkaSettings
import ru.yandex.vertis.telepony.util.Component

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * For testing purposes
  * @author neron
  */
object CallEventActionEmitterTool
  extends App
  with ApplicationComponentImpl
  with ComponentComponent
  with Operational
  with PrometheusComponent
  with TypedDomainComponent
  with KafkaSupportComponentImpl
  with AkkaComponentImpl
  with DomainKafkaComponentImpl {

  private val kafkaConnectionString: String = ???

  val runtime = ru.yandex.vertis.application.runtime.VertisRuntime
  val operational = Operational.default(runtime)

  override def kafkaSettings: KafkaSettings = KafkaSettings(kafkaConnectionString)

  override def config: Config = ConfigHelper.load(Seq("application-test.conf"))

  implicit override def prometheusRegistry: PrometheusRegistry = operational.prometheusRegistry

  override def domain: String = TypedDomains.autoru_def.toString

  override def component: Component = new Component {
    override def hostName: String = "localhost"

    override def formatName: String = "tskv-fromat"

    override def name: String = "some-name"
  }

  val eventJournal = writeJournal(KafkaJournals.MtsCallEventKafkaJournal)

  val f = Source
    .fromIterator(() => generateEvents().iterator)
    .mapAsync(5)(eventJournal.send)
    .runWith(Sink.ignore)(materializer)

  Await.result(f, 3.seconds)
  sys.exit(0)

  /**
    * generate events for call
    */
  private def generateEvents(): List[MtsCallEventAction] = {
    import ru.yandex.vertis.telepony.generator.Producer._
    import ru.yandex.vertis.telepony.model.EventModelGenerators._

    val now = DateTime.now()
    val eventsGen = for {
      externalId <- ExternalIdGen
      accepted <- CallAcceptedEventGen
      answered <- CallAnsweredEventGen
      recStartOpt <- Gen.option(RecordStartEventGen)
      recStopOpt <- Gen.option(RecordStopEventGen)
      goToTr <- Gen.option(GoToTransferEventGen)
      transfer <- TransferEventGen
      end <- CallEndEventGen
    } yield List(
      accepted.copy(externalId = externalId, eventTime = now.plusSeconds(1)),
      answered.copy(externalId = externalId, eventTime = now.plusSeconds(2)),
      transfer.copy(externalId = externalId, eventTime = now.plusSeconds(3)),
      end.copy(externalId = externalId, eventTime = now.plusSeconds(7))
    ) ++
      List(
        recStartOpt.map(_.copy(externalId = externalId, eventTime = now.plusSeconds(4))),
        recStopOpt.map(_.copy(externalId = externalId, eventTime = now.plusSeconds(5))),
        goToTr.map(_.copy(externalId = externalId, eventTime = now.plusSeconds(6)))
      ).flatMap(_.toList)

    eventsGen.next.map(e => MtsCallEventAction(e, NoAction(e.externalId)))
  }

}
