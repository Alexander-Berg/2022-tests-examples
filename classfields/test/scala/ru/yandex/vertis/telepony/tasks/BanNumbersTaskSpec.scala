package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mockito.{ArgumentCaptor, Mockito}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.backend.ControllerComponent.DbAvailable
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.hydra.HydraClient
import ru.yandex.vertis.telepony.hydra.HydraClient.Counter
import ru.yandex.vertis.telepony.model.BlockInfo.Origins
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, BlockInfo, BlockReasons}
import ru.yandex.vertis.telepony.properties.DomainDynamicPropertiesReader
import ru.yandex.vertis.telepony.service.CallService.Filter
import ru.yandex.vertis.telepony.service.{ActualCallService, BlacklistService, BlockedCallService, DateTimeStorage, SourceTargetCallService}
import ru.yandex.vertis.telepony.settings.{BanNumbersSettings, DomainBanNumbersSettings}
import ru.yandex.vertis.telepony.settings.DomainBanNumbersSettings.{BanNumbersByOneTargetRule, BanNumbersByUniqueTargetsRule}
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.util.{Range, RequestContext, Slice, TestPrometheusComponent}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._

class BanNumbersTaskSpec extends SpecBase with MockitoSupport with TestPrometheusComponent {

  val actorSystem: ActorSystem = ActorSystem("test", ConfigFactory.empty())
  val actorMaterializer: ActorMaterializer = ActorMaterializer()(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = actorMaterializer.executionContext

  private val banSettings =
    BanNumbersSettings(limitCalls = 35, banPeriod = 1.day, limitCallsOneTarget = 50, windowByOneTarget = 60.minutes)

  private val banNumbersByUniqueTargetsSettings = BanNumbersByUniqueTargetsRule(
    limit = 5,
    window = 10.minutes,
    banInterval = 10.minutes,
    reason = BlockReasons.ManyCallsIn10min,
    antiFraudOption = AntiFraudOptions.CallsCounter
  )

  private val banNumbersByOneTargetSettings = BanNumbersByOneTargetRule(
    limit = 10,
    window = 10.minutes,
    banInterval = 10.minutes,
    reason = BlockReasons.ManyCallsIn10min,
    antiFraudOption = AntiFraudOptions.CallsCounter
  )

  private def mockActualCallsList: ActualCallService = {
    val m = mock[ActualCallService]
    val call = CallV2Gen.next.copy(source = Some(RefinedSourceGen.next), time = DateTime.now)
    stub(m.list(_: Filter, _: Slice)(_: RequestContext)) {
      case _ =>
        Future.successful(SlicedResult(Seq(call), 1, Range(0, 1)))
    }
    m
  }

  private def mockBlockedCallService = {
    val m = mock[BlockedCallService]
    stub(m.list(_: Filter, _: Slice)(_: RequestContext)) {
      case _ =>
        Future.successful(SlicedResult.empty(Range(0, 1)))
    }
    m
  }

  private def mockDateTime: DateTimeStorage = {
    val m = mock[DateTimeStorage]
    when(m.get).thenReturn(Future.successful(Some(DateTime.now.minusHours(1))))
    when(m.set(?)).thenReturn(Future.unit)
    m
  }

  private def mockBlacklistService: BlacklistService = {
    val m = mock[BlacklistService]
    stub(m.addOrUpdate(_: BlockInfo)) { case _ => Future.successful(true) }
    m
  }

  private def mockDomainDynamicPropertiesReader: DomainDynamicPropertiesReader = {
    val m = mock[DomainDynamicPropertiesReader]
    when(m.getValue(?)).thenReturn(true)
    m
  }

  private def mockHydraClient(count: Int): HydraClient = {
    val cm = mock[Counter]
    stub(cm.incrementAndGet(_: String, _: String)) { case _ => Future.successful(count) }

    val m = mock[HydraClient]
    when(m.counter(?)).thenReturn(cm)
    m
  }

  private def runTask(
      sourceTargetCallService: SourceTargetCallService,
      domainBanNumbersSettings: DomainBanNumbersSettings): Seq[BlockInfo] = {
    val blacklistService = mockBlacklistService

    val task = new BanNumbersTask(
      mockActualCallsList,
      mockBlockedCallService,
      mockDateTime,
      mockDateTime,
      DbAvailable(() => true),
      blacklistService,
      mockHydraClient(0),
      sourceTargetCallService,
      banSettings,
      domainBanNumbersSettings,
      mockDomainDynamicPropertiesReader
    )(actorMaterializer, prometheusRegistry)

    task.payload().futureValue

    val captor: ArgumentCaptor[BlockInfo] = ArgumentCaptor.forClass(classOf[BlockInfo])
    Mockito.verify(blacklistService).addOrUpdate(captor.capture())
    captor.getAllValues.asScala.toSeq
  }

  private def mockSourceTargetCallService(count: Int, countUnique: Int): SourceTargetCallService = {
    val m = mock[SourceTargetCallService]
    when(m.upsert(?, ?, ?, ?)).thenReturn(Future.unit)
    when(m.count(?, ?, ?)).thenReturn(Future.successful(count))
    when(m.countUniqueTargetCalls(?, ?)).thenReturn(Future.successful(countUnique))
    m
  }

  "BanNumbersTask" should {
    "ban" when {
      "BanNumbersByUniqueTargetsSettings limits exceeded" in {
        val sourceTargetCallService = mockSourceTargetCallService(0, banNumbersByUniqueTargetsSettings.limit)
        val bans = runTask(sourceTargetCallService, DomainBanNumbersSettings(Seq(banNumbersByUniqueTargetsSettings)))
        bans.headOption match {
          case Some(
              BlockInfo(_, BlockReasons.ManyCallsIn10min, _, Origins.CallsCounter, AntiFraudOptions.CallsCounter, _, _)
              ) =>
            ()
          case other =>
            fail(s"Unexpected $other")
        }
      }
      "BanNumbersByOneTargetSettings limits exceeded" in {
        val sourceTargetCallService = mockSourceTargetCallService(banNumbersByOneTargetSettings.limit, 0)
        val bans = runTask(sourceTargetCallService, DomainBanNumbersSettings(Seq(banNumbersByOneTargetSettings)))
        bans.headOption match {
          case Some(
              BlockInfo(_, BlockReasons.ManyCallsIn10min, _, Origins.CallsCounter, AntiFraudOptions.CallsCounter, _, _)
              ) =>
            ()
          case other =>
            fail(s"Unexpected $other")
        }
      }
    }
  }

}
