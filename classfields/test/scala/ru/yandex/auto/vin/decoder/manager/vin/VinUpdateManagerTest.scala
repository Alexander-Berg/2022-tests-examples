package ru.yandex.auto.vin.decoder.manager.vin

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.spy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.cache.AudatexDealersCache
import ru.yandex.auto.vin.decoder.extdata.region.Tree
import ru.yandex.auto.vin.decoder.model._
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.model.state.StateUtils
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.{CheckburoReportType, CheckburoUpdateManager}
import ru.yandex.auto.vin.decoder.partners.megaparser.model.MegaParserGibddReportType
import ru.yandex.auto.vin.decoder.partners.{
  CheckburoGibddUpdateManager,
  MegaParserGibddUpdateManager,
  MegaParserRsaUpdateManager
}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.StandardState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.StateUpdateHistory.StateUpdateSource
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.service.vin.VinUpdateService
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.storage.ShardedMySql
import ru.yandex.auto.vin.decoder.storage.orders.InternalReportDefinitions
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.Threads
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.ListHasAsScala

class VinUpdateManagerTest
  extends AnyWordSpecLike
  with Matchers
  with MockitoSupport
  with MockedFeatures
  with BeforeAndAfterAll {

  import VinMainDataUpdateManager._

  implicit val t: Traced = Traced.empty

  implicit val partnerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger(StateUpdateSource.OFFER_ACTIVE, None, None, None, None)

  implicit val ec: ExecutionContext = Threads.lightweightEc

  val updateDao = mock[VinWatchingDao]
  val mysql: ShardedMySql = mock[ShardedMySql]
  val megaParserRsaUpdateManager = new MegaParserRsaUpdateManager
  val mpGibddUpdateManager = new MegaParserGibddUpdateManager
  val checkburoUpdateManager = new CheckburoUpdateManager
  val partnerStateUpdateManager = mock[PartnerStateUpdateManager]
  val checkburoGibddUpdateManager = new CheckburoGibddUpdateManager(features, mpGibddUpdateManager)
  val finesUpdateManager = mock[FinesUpdateManager]
  val tree: Tree = mock[Tree]
  val vinMainDataUpdateManager: VinMainDataUpdateManager = mock[VinMainDataUpdateManager]
  val techInspectionUpdateManager: TechInspectionUpdateManager = mock[TechInspectionUpdateManager]
  val vinUpdateService: VinUpdateService = mock[VinUpdateService]
  val reportManager: ReportManager = mock[ReportManager]
  val audatexDealersCache: AudatexDealersCache = mock[AudatexDealersCache]

  when(audatexDealersCache.get).thenReturn(AudatexDealers(List.empty[AudatexPartner]))

  when(features.AudatexAudaHistory).thenReturn(enabledFeature)
  when(features.AudatexFraudCheck).thenReturn(enabledFeature)
  when(features.BMW).thenReturn(enabledFeature)
  when(features.Avtonomer).thenReturn(enabledFeature)
  when(features.Migalki).thenReturn(enabledFeature)
  when(features.SuzukiVehicle).thenReturn(enabledFeature)
  when(features.Nissan).thenReturn(enabledFeature)
  when(features.Mitsubishi).thenReturn(enabledFeature)
  when(features.Mazda).thenReturn(enabledFeature)
  when(features.JLR).thenReturn(enabledFeature)
  when(features.Suzuki).thenReturn(enabledFeature)
  when(features.Carprice).thenReturn(enabledFeature)
  when(features.Filter).thenReturn(enabledFeature)
  when(features.RusAuto).thenReturn(enabledFeature)
  when(features.MosAutocodeWorker).thenReturn(enabledFeature)
  when(features.Avilon).thenReturn(enabledFeature)
  when(features.Wilgood).thenReturn(enabledFeature)
  when(features.FitService).thenReturn(enabledFeature)
  when(features.Migtorg).thenReturn(enabledFeature)
  when(features.UremontMisc).thenReturn(enabledFeature)
  when(features.EnableVinRsaTrigger).thenReturn(enabledFeature)
  when(features.ProgressReportNotification).thenReturn(enabledFeature)
  when(features.ReadyReportNotification).thenReturn(enabledFeature)
  when(features.EnableReportPurchaseSummaryForOwnerNotification).thenReturn(enabledFeature)
  when(features.Tradesoft).thenReturn(enabledFeature)
  when(features.Acat).thenReturn(enabledFeature)
  when(features.Infiniti).thenReturn(enabledFeature)
  when(features.AutocodeTaxi).thenReturn(enabledFeature)
  when(features.AutocodeIdentifiersByVIN).thenReturn(enabledFeature)
  when(features.AutocodeTechInspections).thenReturn(enabledFeature)
  when(features.FinesStage).thenReturn(enabledFeature)
  when(features.AutocodeFines).thenReturn(enabledFeature)
  when(features.AutocodeFullSetOfDocuments).thenReturn(enabledFeature)
  when(features.BuyPaidSourcesForResellersOffers).thenReturn(enabledFeature)
  when(features.EnablePromocodeForUselessReport).thenReturn(enabledFeature)
  when(features.NbkiPledges).thenReturn(enabledFeature)
  when(features.InterfaxCheckVin).thenReturn(enabledFeature)

  private val vinUpdateManager =
    new VinUpdateManager(
      megaParserRsaUpdateManager,
      vinMainDataUpdateManager,
      techInspectionUpdateManager,
      mpGibddUpdateManager,
      checkburoGibddUpdateManager,
      reportManager,
      vinUpdateService,
      finesUpdateManager,
      checkburoUpdateManager,
      partnerStateUpdateManager,
      audatexDealersCache,
      features
    )

  val MOSCOW: Long = 213
  val LUBERCI: Long = 10738
  val MOSCOW_AND_MO: Long = 1
  val KOSTROMA: Long = 513

  val vin = VinCode("ABCDEFGHJKLMNPRST")
  val holder = WatchingStateHolder(vin, StateUtils.getNewState, System.currentTimeMillis)

  override def beforeAll() = {
    when(finesUpdateManager.update(?, ?)(?, ?)).thenReturn(None)
    ()
  }

  "forceUpdateAutocodeByReport" should {

    "update state with report" in {
      val state = StateUtils.getNewStateUpdate
      val updatedState = AutocodeUpdateManager.forceUpdate(state, AutocodeReportType.OldTaxiByVin)
      updatedState.state.getAutocodeState.getAutocodeReportsCount shouldBe 1
      updatedState.state.getAutocodeState.getAutocodeReportsList.asScala.head.getReportType shouldBe AutocodeReportType.OldTaxiByVin.id
      updatedState.state.getAutocodeState.getAutocodeReportsList.asScala.head.getForceUpdate shouldBe true
    }

    "update avilon state by purchase" in {
      when(vinMainDataUpdateManager.forceUpdate(?, ?, ?, ?)(?, ?)).thenReturn(None)
      when(techInspectionUpdateManager.update(?, ?)(?, ?)).thenReturn(None)
      val updatedState = vinUpdateManager.prepareUpdateByPurchase(vin, None, holder)

      updatedState.state.hasAvilonState shouldBe true
      updatedState.state.getAvilonState.getShouldProcess shouldBe true
    }

    "do not update avilon state by purchase when it was already updated in last 30 days" in {
      when(vinMainDataUpdateManager.forceUpdate(?, ?, ?, ?)(?, ?)).thenReturn(None)
      when(techInspectionUpdateManager.update(?, ?)(?, ?)).thenReturn(None)
      val avilonState = StandardState.newBuilder().setLastCheck(System.currentTimeMillis() - 29.days.toMillis).build
      val existingState = StateUtils.getNewState.toBuilder.setAvilonState(avilonState).build
      val holder = WatchingStateHolder(vin, existingState, System.currentTimeMillis)

      val updatedState = vinUpdateManager.prepareUpdateByPurchase(vin, None, holder)

      updatedState.state.hasAvilonState shouldBe true
      updatedState.state.getAvilonState.getShouldProcess shouldBe false
    }

    "updateByDefinition" should {

      "not update GIBDD states (Autocode, Adaperio, MP, CB) for removed offer" in {
        val updatedState = vinUpdateManager
          .prepareUpdateByReportDefinition(
            holder,
            vin,
            InternalReportDefinitions.OfferRemoveReport,
            ContextForRequestTrigger(None, None, None, None, None),
            None
          )
        updatedState.get.state.getAutocodeState.getShouldProcess shouldBe false
        updatedState.get.state.getAdaperio.getShouldProcess shouldBe false
        updatedState.get.state.getMegaparserGibddState
          .findReport(MegaParserGibddReportType.Registration.toString)
          .exists(_.getShouldProcess) shouldBe false
        updatedState.get.state.getCheckburoState
          .findReport(CheckburoReportType.RegActions.toString)
          .exists(_.getShouldProcess) shouldBe false
        updatedState.get.state.getAutocodeState
          .findReport(AutocodeReportType.Main.toString)
          .exists(_.getShouldProcess) shouldBe false
        updatedState.get.state.getAdaperio
          .findReport(AdaperioReportType.Main.toString)
          .exists(_.getShouldProcess) shouldBe false
      }
    }

    "update mazda state when Mazda" in {
      when(vinMainDataUpdateManager.update(?, ?, ?, ?)(?, ?)).thenReturn(None)
      val updatedState = vinUpdateManager
        .prepareUpdateByReportDefinition(
          holder,
          vin,
          InternalReportDefinitions.OfferRemoveReport,
          ContextForRequestTrigger(Some("MAZDA"), None, None, None, None),
          Some("MAZDA")
        )
      updatedState.get.state.getMazdaState.getShouldProcess shouldBe true
    }

    "update Autocode Mos state when handle offer from Moscow" in {
      val vinUpdateManagerSpy = spy(vinUpdateManager)
      when(partnerStateUpdateManager.updateForOrder(?, ?, ?, ?)(?, ?)).thenReturn(Some(holder.toUpdate))
      val updatedState = vinUpdateManagerSpy
        .prepareUpdateByReportDefinition(
          holder,
          CommonVinCode("vin"),
          InternalReportDefinitions.OfferActiveReportWithMo,
          ContextForRequestTrigger(Some("MAZDA"), None, None, None, None),
          Some("MAZDA")
        )
      updatedState.get.state.getMosAutocodeState.getShouldProcess shouldBe true
    }

    "update Autocode Mos state when handle offer from city of Moscow" in {
      val vinUpdateManagerSpy = spy(vinUpdateManager)
      when(partnerStateUpdateManager.updateForOrder(?, ?, ?, ?)(?, ?)).thenReturn(Some(holder.toUpdate))
      val updatedState = vinUpdateManagerSpy
        .prepareUpdateByReportDefinition(
          holder,
          CommonVinCode("vin"),
          InternalReportDefinitions.OfferActiveReportWithMo,
          ContextForRequestTrigger(Some("MAZDA"), None, None, None, None),
          Some("MAZDA")
        )
      updatedState.get.state.getMosAutocodeState.getShouldProcess shouldBe true
    }

    "do not update Autocode Mos state when handle offer from Kostroma" in {
      val vinUpdateManagerSpy = spy(vinUpdateManager)
      when(partnerStateUpdateManager.updateForOrder(?, ?, ?, ?)(?, ?)).thenReturn(Some(holder.toUpdate))
      val updatedState = vinUpdateManagerSpy
        .prepareUpdateByReportDefinition(
          holder,
          CommonVinCode("vin"),
          InternalReportDefinitions.OfferActiveReport,
          ContextForRequestTrigger(Some("MAZDA"), None, None, None, None),
          Some("MAZDA")
        )
      updatedState.get.state.getMosAutocodeState.getShouldProcess shouldBe false
    }
  }
}
