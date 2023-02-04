package ru.auto.salesman.tasks

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, Suite}
import ru.auto.salesman.dao.QuotaDao
import ru.auto.salesman.dao.QuotaDao.{
  AllActiveChangedSince,
  LastActivationsToOffersNotDeactivated
}
import ru.auto.salesman.environment.{now, timeAt}
import ru.auto.salesman.model.QuotaEntities.Dealer
import ru.auto.salesman.model._
import ru.auto.salesman.service.quota_offers.QuotaOffersActualizer
import ru.auto.salesman.service.{EpochService, QuotaService}
import ru.auto.salesman.tasks.QuotaOffersActualizationTaskSpec._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => argEq}
import ru.yandex.vertis.mockito.util.RichTryOngoingStub

import scala.util.{Failure, Success, Try}

class QuotaOffersActualizationTaskSpec extends BaseSpec with Mocking {

  "QuotaOffersActualizationTask" should {

    "successful execute for sync size" in new Captors {
      val quotas = storedQuotaGen
        .next(5)
      val maxEpoch = quotas.map(_.epoch).max

      whenEpochServiceGet(testSyncSizeMarker, Success(testEpoch))
      whenEpochServiceSet(testSyncSizeMarker, maxEpoch, Success(()))
      whenQuotasGet(testEpoch)(Success(quotas))

      syncSizeTask().execute().success.value shouldBe (())

      verify(quotaService).get(quotaFilterCaptor.capture())(any())
      verifyNoMoreInteractions(quotaService)
      verify(quotaOffersService, times(quotas.size))
        .actualize(any(), any())
      verify(epochService).set(any(), any())

      quotaFilterCaptor.getValue should matchPattern {
        case AllActiveChangedSince(_, _, _) =>
      }
    }

    "successful execute for cars quotas sync size" in new Captors {
      val quotas = storedQuotaGen
        .next(5)
        .map(_.copy(quotaType = ProductId.QuotaPlacementCarsNew))
      val allQuotas = quotas ++ storedQuotaGen
        .next(1)
        .map(_.copy(quotaType = ProductId.QuotaPlacementCommercial))

      val maxEpoch = quotas.map(_.epoch).max

      whenEpochServiceGet(testSyncSizeMarker, Success(testEpoch))
      whenEpochServiceSet(testSyncSizeMarker, maxEpoch, Success(()))
      whenQuotasGet(testEpoch)(Success(allQuotas))

      syncSizeTask(QuotaOffersActualizeTask.isCarsQuota)
        .execute()
        .success
        .value shouldBe (())

      verify(quotaService).get(quotaFilterCaptor.capture())(any())
      verify(quotaOffersService, times(quotas.size))
        .actualize(any(), any())
      verify(epochService).set(any(), any())

      quotaFilterCaptor.getValue should matchPattern {
        case AllActiveChangedSince(_, _, _) =>
      }
    }

    "successful execute for not cars quotas sync size" in new Captors {
      val quotas = storedQuotaGen
        .next(5)
        .map(_.copy(quotaType = ProductId.QuotaPlacementMoto))
      val allQuotas = quotas ++ storedQuotaGen
        .next(1)
        .map(_.copy(quotaType = ProductId.QuotaPlacementCarsUsedPremium))

      val maxEpoch = quotas.map(_.epoch).max

      whenEpochServiceGet(testSyncSizeMarker, Success(testEpoch))
      whenEpochServiceSet(testSyncSizeMarker, maxEpoch, Success(()))
      whenQuotasGet(testEpoch)(Success(allQuotas))

      syncSizeTask(QuotaOffersActualizeTask.isCategorizedQuota)
        .execute()
        .success
        .value shouldBe (())

      verify(quotaService).get(quotaFilterCaptor.capture())(any())
      verify(quotaOffersService, times(quotas.size))
        .actualize(any(), any())
      verify(epochService).set(any(), any())

      quotaFilterCaptor.getValue should matchPattern {
        case AllActiveChangedSince(_, _, _) =>
      }
    }

    "successful execute for deactivate cars quotas" in new Captors {
      val activeQuotas = storedQuotaGen
        .next(5)
        .map(
          _.copy(
            to = timeAt(testQuotaDateToEpoch).plusHours(2),
            quotaType = ProductId.QuotaPlacementCarsNewPremium
          )
        )
      val inactiveQuotas = storedQuotaGen.next(5).map { q =>
        val shift = Gen.choose(1, 30).next
        q.copy(
          to = now().minusMinutes(shift),
          quotaType = ProductId.QuotaPlacementCarsUsed
        )
      }
      val quotas = activeQuotas ++ inactiveQuotas ++ storedQuotaGen
        .next(1)
        .map(_.copy(quotaType = ProductId.QuotaPlacementCommercial))

      val maxEpoch = inactiveQuotas.map(_.to.getMillis).max

      whenEpochServiceGet(testDeactivateMarker, Success(testQuotaDateToEpoch))
      whenEpochServiceSet(testDeactivateMarker, maxEpoch, Success(()))
      whenQuotasGet(testQuotaDateToEpoch)(Success(quotas))
      when(quotaService.markDeactivated(?, ?)(?)).thenReturnT(())

      deactivateInactiveTask(QuotaOffersActualizeTask.isCarsQuota)
        .execute()
        .success
        .value shouldBe (())

      verify(quotaService).get(quotaFilterCaptor.capture())(any())
      verify(quotaOffersService, times(inactiveQuotas.size))
        .actualize(any(), any())
      verify(epochService).set(any(), any())
      verify(quotaService, times(5)).markDeactivated(argEq(Dealer), ?)(?)
      verifyNoMoreInteractions(quotaService)

      quotaFilterCaptor.getValue should matchPattern {
        case LastActivationsToOffersNotDeactivated(_, _) =>
      }
    }

    "successful execute for deactivate" in new Captors {
      val activeQuotas = storedQuotaGen
        .next(5)
        .map(_.copy(to = timeAt(testQuotaDateToEpoch).plusHours(2)))
      val inactiveQuotas = storedQuotaGen
        .next(5)
        .map { q =>
          val shift = Gen.choose(1, 30).next
          q.copy(to = now().minusMinutes(shift))
        }
      val quotas = activeQuotas ++ inactiveQuotas
      val maxEpoch = inactiveQuotas.map(_.to.getMillis).max

      whenEpochServiceGet(testDeactivateMarker, Success(testQuotaDateToEpoch))
      whenEpochServiceSet(testDeactivateMarker, maxEpoch, Success(()))
      whenQuotasGet(testQuotaDateToEpoch)(Success(quotas))
      when(quotaService.markDeactivated(?, ?)(?)).thenReturnT(())

      deactivateInactiveTask().execute().success.value shouldBe (())

      verify(quotaService).get(quotaFilterCaptor.capture())(any())
      verify(quotaOffersService, times(inactiveQuotas.size))
        .actualize(any(), any())
      verify(epochService).set(any(), any())
      verify(quotaService, times(5)).markDeactivated(argEq(Dealer), ?)(?)
      verifyNoMoreInteractions(quotaService)

      quotaFilterCaptor.getValue should matchPattern {
        case LastActivationsToOffersNotDeactivated(_, _) =>
      }
    }

    "mark proper quota as deactivated" in {
      val quota =
        storedQuotaGen.next.copy(
          to = timeAt(testQuotaDateToEpoch).minusHours(2),
          quotaType = ProductId.QuotaPlacementCarsUsed
        )

      whenEpochServiceGet(testDeactivateMarker, Success(testQuotaDateToEpoch))
      whenEpochServiceSet(testDeactivateMarker, quota.to.getMillis, Success(()))
      whenQuotasGet(testQuotaDateToEpoch)(Success(List(quota)))
      when(quotaService.markDeactivated(?, ?)(?)).thenReturnT(())

      deactivateInactiveTask().execute().success.value shouldBe (())

      verify(quotaService).get(any())(any())
      verify(quotaService).markDeactivated(argEq(Dealer), argEq(quota.id))(?)
      verifyNoMoreInteractions(quotaService)
    }

    "successful execute sync size for empty quotas" in {
      val quotas = Iterable.empty[StoredQuota]

      whenEpochServiceGet(testSyncSizeMarker, Success(testEpoch))
      whenQuotasGet(testEpoch)(Success(quotas))

      syncSizeTask().execute().success.value shouldBe (())
      verify(epochService, times(1)).get(any())
      verifyNoMoreInteractions(epochService)
    }

    "successful execute deactivate for empty quotas" in {
      val quotas = Iterable.empty[StoredQuota]

      whenEpochServiceGet(testDeactivateMarker, Success(testQuotaDateToEpoch))
      whenQuotasGet(testQuotaDateToEpoch)(Success(quotas))

      deactivateInactiveTask().execute().success.value shouldBe (())
      verify(epochService, times(1)).get(any())
      verifyNoMoreInteractions(epochService)
    }

    "failed for get epoch" in {
      whenEpochServiceGet(testSyncSizeMarker, Failure(new TestException))
      syncSizeTask().execute().failure.exception shouldBe a[TestException]
      verify(epochService, times(1)).get(any())
      verifyNoMoreInteractions(epochService)

      reset(epochService)

      whenEpochServiceGet(testDeactivateMarker, Failure(new TestException))
      deactivateInactiveTask()
        .execute()
        .failure
        .exception shouldBe a[TestException]
      verify(epochService, times(1)).get(any())
      verifyNoMoreInteractions(epochService)
    }

    "failed for get quotas" in {
      whenEpochServiceGet(testSyncSizeMarker, Success(testEpoch))
      whenQuotasGet(testEpoch)(Failure(new TestException))
      syncSizeTask().execute().failure.exception shouldBe a[TestException]

      verify(epochService, times(1)).get(any())
      verifyNoMoreInteractions(epochService)
      verifyZeroInteractions(quotaOffersService)

      reset(epochService)

      whenEpochServiceGet(testDeactivateMarker, Success(testQuotaDateToEpoch))
      whenQuotasGet(testQuotaDateToEpoch)(Failure(new TestException))
      deactivateInactiveTask()
        .execute()
        .failure
        .exception shouldBe a[TestException]

      verify(epochService, times(1)).get(any())
      verifyNoMoreInteractions(epochService)
      verifyZeroInteractions(quotaOffersService)
    }

    "do not set epoch for some failed actualize quotas" in {
      val quotas = storedQuotaGen
        .next(4) ++
        Iterable(
          storedQuotaGen.next.copy(
            clientId = testFailedQuotaClientId,
            quotaType = ProductId.QuotaPlacementCarsUsed
          )
        )

      whenEpochServiceGet(testSyncSizeMarker, Success(testEpoch))
      whenQuotasGet(testEpoch)(Success(quotas))
      whenQuotaOffersActualizeFailed()

      syncSizeTask().execute().success.value shouldBe (())

      verify(epochService, times(1)).get(any())
      verify(quotaOffersService, times(quotas.size)).actualize(any(), any())
      verifyNoMoreInteractions(epochService)
    }
  }

  "QuotaOffersActualizationTask Object" should {
    "got true for cars quota filter" in {
      val carsNewQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementCarsNew)
      val carsUsedQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementCarsUsedPremium)

      QuotaOffersActualizeTask.isCarsQuota(carsNewQuota) should be(true)
      QuotaOffersActualizeTask.isCarsQuota(carsUsedQuota) should be(true)
    }

    "got false for cars quota filter" in {
      val trucksQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementCommercial)
      val motoQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementMoto)

      QuotaOffersActualizeTask.isCarsQuota(trucksQuota) should be(false)
      QuotaOffersActualizeTask.isCarsQuota(motoQuota) should be(false)
    }
    "got false for others quotas filter" in {
      val carsNewQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementCarsNew)
      val carsUsedQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementCarsUsedPremium)

      QuotaOffersActualizeTask.isCategorizedQuota(carsNewQuota) should be(false)
      QuotaOffersActualizeTask.isCategorizedQuota(carsUsedQuota) should be(
        false
      )
    }

    "got true for others quotas filter" in {
      val trucksQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementCommercial)
      val motoQuota =
        QuotaGen.next.copy(quotaType = ProductId.QuotaPlacementMoto)

      QuotaOffersActualizeTask.isCategorizedQuota(trucksQuota) should be(true)
      QuotaOffersActualizeTask.isCategorizedQuota(motoQuota) should be(true)
    }
  }
}

object QuotaOffersActualizationTaskSpec {

  val testSyncSizeMarker = Markers.QuotaCarsOffersSyncSizeEpoch
  val testDeactivateMarker = Markers.QuotaCarsOffersDeactivateEpoch
  val testEpoch = 1234L
  val testQuotaDateToEpoch = now().getMillis
  val testFailedQuotaClientId = 111111L
  val defaultQuotasFilter = (quota: Quota) => true

  trait Mocking extends BeforeAndAfter with MockitoSupport {
    this: Suite =>

    val quotaService = mock[QuotaService]
    val quotaOffersService = mock[QuotaOffersActualizer]
    val epochService = mock[EpochService]

    def syncSizeTask(filter: Quota => Boolean = defaultQuotasFilter) =
      new QuotaOffersSyncSizeTask(
        quotaService,
        quotaOffersService,
        epochService,
        testSyncSizeMarker
      )(filter)

    def deactivateInactiveTask(filter: Quota => Boolean = defaultQuotasFilter) =
      new QuotaOffersDeactivateTask(
        quotaService,
        quotaOffersService,
        epochService,
        testDeactivateMarker
      )(filter)

    before {
      reset(quotaService, quotaOffersService, epochService)
      when(epochService.set(any(), any()))
        .thenReturn(Success(()))
      when(quotaOffersService.actualize(any(), any()))
        .thenReturn(Success(()))
    }

    def whenEpochServiceGet(marker: Marker, epoch: Try[Epoch]): Unit =
      stub(epochService.get _) {
        case m if m == marker.toString => epoch
      }

    def whenEpochServiceSet(
        marker: Marker,
        epoch: Epoch,
        result: Try[Unit]
    ): Unit =
      stub(epochService.set _) {
        case (m, `epoch`) if m == marker.toString => result
      }

    def whenQuotasGet(epoch: Epoch)(quotas: Try[Iterable[StoredQuota]]): Unit =
      stub(quotaService.get(_: QuotaDao.Filter)(_: RequestContext)) {
        case (LastActivationsToOffersNotDeactivated(to, _), _) if to == epoch =>
          quotas
        case (AllActiveChangedSince(e, _, _), _) if e == epoch => quotas
        case _ => Success(Iterable.empty[StoredQuota])
      }

    def whenQuotaOffersActualizeFailed(): Unit =
      stub(quotaOffersService.actualize _) {
        case (_, `testFailedQuotaClientId`) =>
          Failure(new Exception("arfiticial"))
        case _ => Success(())
      }
  }

  trait Captors {

    val quotaFilterCaptor: ArgumentCaptor[QuotaDao.Filter] =
      ArgumentCaptor.forClass(classOf[QuotaDao.Filter])
  }
}
