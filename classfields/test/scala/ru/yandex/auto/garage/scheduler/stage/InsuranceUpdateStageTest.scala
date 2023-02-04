package ru.yandex.auto.garage.scheduler.stage

import auto.carfax.common.clients.carfax.CarfaxClient
import auto.carfax.common.clients.hydra.HydraClient
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.yandex.auto.garage.managers.insurance.{InsuranceClicker, InsuranceClickerType}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.InsuranceInfo.AutoUpdates
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, Insurance}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.DateTimeUtils._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class InsuranceUpdateStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with GarageCardStageSupport[InsuranceUpdateStage] {

  private val mainClickerHydra = mock[HydraClient]
  private val mainClicker = new InsuranceClicker(InsuranceClickerType.MAIN, mainClickerHydra, 10)
  private val secondaryClickerHydra = mock[HydraClient]
  private val secondaryClicker = new InsuranceClicker(InsuranceClickerType.SECONDARY, secondaryClickerHydra, 10)
  private val carfaxClient = mock[CarfaxClient]
  private val rateLimiter: RateLimiter = RateLimiter.create(100)

  implicit val t: Traced = Traced.empty

  private val stage = createProcessingStage()

  before {
    reset(mainClickerHydra)
    reset(secondaryClickerHydra)
    reset(carfaxClient)
  }

  private val TestVin = VinCode("SALGA2BE8LA405000")

  def buildCard(
      status: GarageCard.Status = GarageCard.Status.ACTIVE,
      cardType: CardType = CardType.CURRENT_CAR,
      optVin: Option[VinCode] = Some(TestVin),
      created: Long = System.currentTimeMillis()): GarageCard = {
    val builder = GarageCard.newBuilder()
    builder.getMetaBuilder.setStatus(status).setCreated(Timestamps.fromMillis(created))
    builder.getMetaBuilder.getCardTypeInfoBuilder.getCurrentStateBuilder.setCardType(cardType)
    optVin.foreach(vin => builder.getVehicleInfoBuilder.getDocumentsBuilder.setVin(vin.toString))
    builder.build()
  }

  "shouldProcess" should {
    "return true" when {
      "suitable card" in {
        checkShouldProcess(buildCard())
      }
    }
    "return false" when {
      "card deleted" in {
        checkIgnored(buildCard(status = GarageCard.Status.DELETED))
      }
      "card type not current" in {
        checkIgnored(buildCard(cardType = CardType.EX_CAR))
      }
      "card without vin" in {
        checkIgnored(buildCard(optVin = None))
      }
    }
  }

  "process" should {
    "update update and reschedule" when {
      "card recently created and dont has insurances yet" in {
        when(mainClickerHydra.getClicker(?, ?)(?)).thenReturn(Future.successful(1))
        when(mainClickerHydra.incClicker(?, ?)(?)).thenReturn(Future.unit)
        when(carfaxClient.adminUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val card = buildCard()

        val res = stage.processWithAsync(0, createProcessingState(card))
        val info = res.state.getInsuranceInfo.getAutoUpdatesInfo

        info.getRetryCount shouldBe 1
        (System.currentTimeMillis() - info.getLastUpdateRequest.getMillis) <= 10000 shouldBe true
      }
      "last osago policy finish recently, 1 retry" in {
        when(mainClickerHydra.getClicker(?, ?)(?)).thenReturn(Future.successful(1))
        when(mainClickerHydra.incClicker(?, ?)(?)).thenReturn(Future.unit)
        when(carfaxClient.adminUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val card = {
          val builder = buildCard().toBuilder
          builder.getInsuranceInfoBuilder.addInsurances(
            buildPolicy(policyTo = System.currentTimeMillis() - 15.days.toMillis)
          )
          builder.getInsuranceInfoBuilder.getAutoUpdatesInfoBuilder
            .setRetryCount(1)
            .setLastUpdateRequest(Timestamps.fromMillis(System.currentTimeMillis() - 15.days.toMillis))
          builder.build()
        }

        val res = stage.processWithAsync(0, createProcessingState(card))
        val info = res.state.getInsuranceInfo.getAutoUpdatesInfo

        info.getRetryCount shouldBe 2
        (System.currentTimeMillis() - info.getLastUpdateRequest.getMillis) <= 10000 shouldBe true
      }
    }
    "reschedule" when {
      "there are actual osage policy" in {
        when(mainClickerHydra.getClicker(?, ?)(?)).thenReturn(Future.successful(1))
        when(mainClickerHydra.incClicker(?, ?)(?)).thenReturn(Future.unit)
        when(carfaxClient.adminUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val card = {
          val builder = buildCard().toBuilder
          builder.getInsuranceInfoBuilder.addInsurances(
            buildPolicy(policyTo = System.currentTimeMillis() + 15.days.toMillis)
          )
          builder.getInsuranceInfoBuilder.getAutoUpdatesInfoBuilder
            .setLastUpdateRequest(Timestamps.fromMillis(System.currentTimeMillis() - 15.days.toMillis))
          builder.build()
        }

        val res = stage.processWithAsync(0, createProcessingState(card))
        val info = res.state.getInsuranceInfo.getAutoUpdatesInfo

        info shouldBe card.getInsuranceInfo.getAutoUpdatesInfo
      }
    }
  }

  "getLastOsagePolicy" should {
    "return None" when {
      "insurance list is empty" in {
        stage.getLastOsagePolicy(List.empty) shouldBe None
      }
      "all osage policies deleted" in {
        stage.getLastOsagePolicy(List(buildPolicy(isDeleted = true))) shouldBe None
      }
      "there are not osage policies" in {
        stage.getLastOsagePolicy(List(buildPolicy(insuranceType = InsuranceType.KASKO))) shouldBe None
      }
    }
    "return policy" in {
      val osago1 = buildPolicy(policyTo = 100)
      val osago2 = buildPolicy(policyTo = 200)

      stage.getLastOsagePolicy(List(osago1, osago2)) shouldBe Some(osago2)
    }
  }

  "requestInsuranceUpdate" should {
    val now = System.currentTimeMillis()
    def buildAutoUpdates(retryCount: Int, lastRequest: Long): AutoUpdates = {
      AutoUpdates
        .newBuilder()
        .setRetryCount(retryCount)
        .setLastUpdateRequest(Timestamps.fromMillis(lastRequest))
        .build()
    }
    val clicker = new InsuranceClicker(InsuranceClickerType.SECONDARY, mainClickerHydra, 10)

    "inc retry count and update request date" when {
      "there are clicker limit and request successful" in {
        when(mainClickerHydra.getClicker(?, ?)(?)).thenReturn(Future.successful(10))
        when(mainClickerHydra.incClicker(?, ?)(?)).thenReturn(Future.unit)
        when(carfaxClient.adminUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val res = stage
          .requestInsuranceUpdate(
            cardId = 0L,
            info = buildAutoUpdates(1, now - 100),
            vin = TestVin,
            clicker = clicker,
            now = now
          )
          .await

        res._1.getRetryCount shouldBe 2
        res._1.getLastUpdateRequest.getMillis shouldBe now
        res._2 shouldBe Some(14.days)

        verify(carfaxClient, times(1)).adminUpdate(?, ?, ?)(?)
      }
    }
    "don't inc retry count and last request date" when {
      "carfax throw error" in {
        when(mainClickerHydra.getClicker(?, ?)(?)).thenReturn(Future.successful(2))
        when(mainClickerHydra.incClicker(?, ?)(?)).thenReturn(Future.unit)
        when(carfaxClient.adminUpdate(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException("")))

        val res = stage
          .requestInsuranceUpdate(
            cardId = 0L,
            info = buildAutoUpdates(1, now - 100),
            vin = TestVin,
            clicker = clicker,
            now = now
          )
          .await

        res._1.getRetryCount shouldBe 1
        res._1.getLastUpdateRequest.getMillis shouldBe now - 100

        res._2.nonEmpty shouldBe true
        res._2.get.toMillis <= 10.minutes.toMillis shouldBe true

        verify(carfaxClient, times(1)).adminUpdate(?, ?, ?)(?)
      }
      "rate limit exceeded" in {
        when(mainClickerHydra.getClicker(?, ?)(?)).thenReturn(Future.successful(20))
        when(mainClickerHydra.incClicker(?, ?)(?)).thenReturn(Future.unit)
        when(carfaxClient.adminUpdate(?, ?, ?)(?)).thenReturn(Future.unit)

        val res = stage
          .requestInsuranceUpdate(
            cardId = 0L,
            info = buildAutoUpdates(1, now - 100),
            vin = TestVin,
            clicker = clicker,
            now = now
          )
          .await

        res._1.getRetryCount shouldBe 1
        res._1.getLastUpdateRequest.getMillis shouldBe now - 100

        res._2.get.toMillis <= 180.days.toMillis shouldBe true
        res._2.get.toMillis >= 1.days.toMillis shouldBe true

        verify(carfaxClient, never()).adminUpdate(?, ?, ?)(?)
      }
    }
  }

  "choose clicker" should {
    val now = System.currentTimeMillis()
    "choose main clicker" when {
      "card created recently" in {
        stage.chooseClicker(now - 1.day.toMillis, lastPolicyFinish = 0, now) shouldBe mainClicker
      }
      "last policy finish recently" in {
        stage.chooseClicker(now - 50.day.toMillis, lastPolicyFinish = now - 3.days.toMillis, now) shouldBe mainClicker
      }
    }
    "choose secondary clicker" when {
      "old card created with old policy" in {
        stage.chooseClicker(
          now - 60.day.toMillis,
          lastPolicyFinish = now - 60.days.toMillis,
          now
        ) shouldBe secondaryClicker
      }
    }
  }

  private def buildPolicy(
      insuranceType: InsuranceType = InsuranceType.OSAGO,
      isDeleted: Boolean = false,
      policyTo: Long = 0): Insurance = {
    Insurance
      .newBuilder()
      .setInsuranceType(insuranceType)
      .setIsDeleted(isDeleted)
      .setTo(Timestamps.fromMillis(policyTo))
      .build()
  }

  override def createProcessingStage(): InsuranceUpdateStage = {
    new InsuranceUpdateStage(mainClicker, secondaryClicker, carfaxClient, rateLimiter)
  }

  implicit protected def createProcessingState(card: GarageCard): ProcessingState[GarageCard] = {
    ProcessingState(createWatchingState(card))
  }

  implicit protected def createWatchingState(card: GarageCard): WatchingStateUpdate[GarageCard] = {
    WatchingStateUpdate(card, DefaultDelay(48.hours))
  }
}
