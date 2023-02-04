package ru.yandex.vertis.telepony.tasks

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}
import ru.yandex.vertis.telepony.backend.ControllerComponent.SharedDbAvailable
import ru.yandex.vertis.telepony.dao.AonBlacklistUpdatesDao.AonBlacklistTableName
import ru.yandex.vertis.telepony.service.{AonBlacklistUpdatesService, DateTimeStorage}
import ru.yandex.vertis.telepony.service.impl.AonBlacklistServiceImpl
import ru.yandex.vertis.telepony.tasks.shared.AonBlacklistSyncTask
import ru.yandex.vertis.telepony.model.BlockInfo.Origins
import AonBlacklistSyncTaskIntSpec.AonBlockVerdicts
import ru.yandex.vertis.telepony.model.{AonAddAction, AonBlockInfo, AonChangeAction, AonDeleteSourceAction, AonUpdateVerdictAction, BlockReasons, RefinedSource}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * @author tolmach
  */
class AonBlacklistSyncTaskIntSpec extends SpecBase with MockitoSupport with IntegrationSpecTemplate {

  implicit val actorMaterializer: Materializer = materializer
  implicit val executionContext: ExecutionContextExecutor = actorMaterializer.executionContext

  val TimePoint = DateTime.now()

  object Sources {
    val Source11 = RefinedSource("+79991111111")
    val Source12 = RefinedSource("+79991111112")
    val Source13 = RefinedSource("+79991111113")
    val Source14 = RefinedSource("+79991111114")
    val Source15 = RefinedSource("+79991111115")
    val Source16 = RefinedSource("+79991111116")
    val Source17 = RefinedSource("+79991111117")
    val Source18 = RefinedSource("+79991111118")
    val Source19 = RefinedSource("+79991111119")
    val Source20 = RefinedSource("+79991111120")
    val Source21 = RefinedSource("+79991111121")
  }

  val currentBlackList = Seq(
    AonBlockInfo(Sources.Source11, verdict = "lol", Origins.BlacklistTasks),
    AonBlockInfo(Sources.Source12, verdict = "kek", Origins.BlacklistTasks),
    AonBlockInfo(
      Sources.Source13,
      verdict = AonBlockVerdicts.BankDebt,
      Origins.BlacklistTasks
    ),
    AonBlockInfo(
      Sources.Source14,
      verdict = AonBlockVerdicts.Scam,
      Origins.BlacklistTasks
    ),
    AonBlockInfo(
      Sources.Source15,
      verdict = AonBlockVerdicts.Debt,
      Origins.BlacklistTasks
    ),
    AonBlockInfo(
      Sources.Source16,
      verdict = AonBlockVerdicts.Silent,
      Origins.BlacklistTasks
    )
  )

  val aonBlacklistService = {
    val s = new AonBlacklistServiceImpl(aonBlacklistDao)
    val fillActions = currentBlackList.map(s.add)
    Future.sequence(fillActions).futureValue
    s
  }

  val aonUpdates = Seq(
    AonDeleteSourceAction(Sources.Source11),
    AonDeleteSourceAction(Sources.Source12),
    AonDeleteSourceAction(Sources.Source13),
    AonUpdateVerdictAction(Sources.Source14, AonBlockVerdicts.BankNotification),
    AonUpdateVerdictAction(Sources.Source15, AonBlockVerdicts.DebtCollector),
    AonUpdateVerdictAction(Sources.Source16, AonBlockVerdicts.Survey),
    AonAddAction(Sources.Source17, AonBlockVerdicts.Finance),
    AonAddAction(Sources.Source18, AonBlockVerdicts.DebtCollectorDebt),
    AonAddAction(Sources.Source19, AonBlockVerdicts.Telemarketing)
  )

  val expectedBlacklist = Seq(
    AonBlockInfo(
      Sources.Source14,
      verdict = AonBlockVerdicts.BankNotification,
      Origins.BlacklistTasks,
      updateTime = TimePoint
    ),
    AonBlockInfo(
      Sources.Source15,
      verdict = AonBlockVerdicts.DebtCollector,
      Origins.BlacklistTasks,
      updateTime = TimePoint
    ),
    AonBlockInfo(
      Sources.Source16,
      verdict = AonBlockVerdicts.Survey,
      Origins.BlacklistTasks,
      updateTime = TimePoint
    ),
    AonBlockInfo(
      Sources.Source17,
      verdict = AonBlockVerdicts.Finance,
      Origins.BlacklistTasks,
      updateTime = TimePoint
    ),
    AonBlockInfo(
      Sources.Source18,
      verdict = AonBlockVerdicts.DebtCollectorDebt,
      Origins.BlacklistTasks,
      updateTime = TimePoint
    ),
    AonBlockInfo(
      Sources.Source19,
      verdict = AonBlockVerdicts.Telemarketing,
      Origins.BlacklistTasks,
      updateTime = TimePoint
    )
  )

  private def aonServiceMockForRehandle(updatesForRehandle: Seq[AonChangeAction]): AonBlacklistUpdatesService = {
    val m = mock[AonBlacklistUpdatesService]
    stub(m.rehandleUpdates(_: AonBlacklistTableName, _: Int)(_: Seq[AonChangeAction] => Future[Unit])) {
      case (_, _, action) =>
        action(updatesForRehandle)
      case _ =>
        Future.failed(new UnsupportedOperationException("Unexpected call"))
    }
    m
  }

  private def aonServiceMockForHandle(
      updatesForHandle: Seq[AonChangeAction],
      processed: DateTime): AonBlacklistUpdatesService = {
    val m = mock[AonBlacklistUpdatesService]
    stub(m.handleUpdatesIfNeeded(_: Option[AonBlacklistTableName], _: Int)(_: Seq[AonChangeAction] => Future[Unit])) {
      case (_, _, action) =>
        action(updatesForHandle).map(_ => Some(AonBlacklistTableName(processed)))
      case _ =>
        Future.failed(new UnsupportedOperationException("Unexpected call"))
    }
    m
  }

  private def dateTimeStorageMock(time: Option[DateTime] = None): DateTimeStorage = {
    val timeStorage = mock[DateTimeStorage]
    when(timeStorage.get()).thenReturn(Future.successful(time))
    when(timeStorage.set(?)).thenReturn(Future.unit)
    timeStorage
  }

  private val TimePointSample = DateTime.now()

  @nowarn
  private def checkHandleUpdates(lastProcessed: Option[DateTime], readyToProcess: DateTime): Unit = {
    val aonService = aonServiceMockForHandle(aonUpdates, readyToProcess)
    val dateTimeStorage = dateTimeStorageMock(lastProcessed)
    val task =
      new AonBlacklistSyncTask(aonService, aonBlacklistService, dateTimeStorage, SharedDbAvailable(() => true))
    task.payload(ConfigFactory.empty()).futureValue
    val actual = aonBlacklistService.all().futureValue.map(_.copy(updateTime = TimePoint))
    actual should contain theSameElementsAs expectedBlacklist
    Mockito.verify(dateTimeStorage).get()
    Mockito.verify(dateTimeStorage).set(eq(readyToProcess))
  }

  "AonBlacklistSyncTask" should {
    "handle updates" when {
      "aon table hasn't been processed yet" in {
        checkHandleUpdates(None, TimePointSample)
      }
      "the last processed table is older than the current one" in {
        checkHandleUpdates(Some(TimePointSample.minusDays(1)), TimePointSample)
      }
    }
    "rehandle updates" in {
      val aonService = aonServiceMockForRehandle(aonUpdates)
      val dateTimeStorage = dateTimeStorageMock(Some(TimePointSample))
      val task = {
        new AonBlacklistSyncTask(aonService, aonBlacklistService, dateTimeStorage, SharedDbAvailable(() => true))
      }
      val config = ConfigFactory.parseMap(Map("strategy" -> "reprocess-last").asJava)
      task.payload(config).futureValue
      val actual = aonBlacklistService.all().futureValue.map(_.copy(updateTime = TimePoint))
      actual should contain theSameElementsAs expectedBlacklist
      Mockito.verify(dateTimeStorage).get()
      Mockito.verify(dateTimeStorage, Mockito.never()).set(?)
    }
  }

}

object AonBlacklistSyncTaskIntSpec {

  object AonBlockVerdicts {

    val BankDebt = "bank.debt"
    val BankNotification = "bank.notification"
    val BankTelemarketing = "bank.telemarketing"
    val Debt = "debt"
    val DebtNeutral = "debt.neutral"
    val DebtCollector = "debt_collector"
    val DebtCollectorDebt = "debt_collector.debt"
    val Finance = "finance"
    val FinanceNegative = "finance.negative"
    val FinanceTelemarketing = "finance.telemarketing"
    val Scam = "scam"
    val Silent = "silent"
    val Survey = "survey"
    val Telemarketing = "telemarketing"

    val All: Set[String] =
      Set(
        BankDebt,
        BankNotification,
        BankTelemarketing,
        Debt,
        DebtNeutral,
        DebtCollector,
        DebtCollectorDebt,
        Finance,
        FinanceNegative,
        FinanceTelemarketing,
        Scam,
        Silent,
        Survey,
        Telemarketing
      )
  }

}
