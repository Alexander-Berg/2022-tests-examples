package ru.yandex.vertis.billing.banker.tasks

import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3PaymentRequestExternalDao.Record
import ru.yandex.vertis.billing.banker.dao.gens.YV3ExternalRecordGen
import ru.yandex.vertis.billing.banker.model.gens.{readableString, Producer}
import ru.yandex.vertis.billing.banker.service.EpochService
import ru.yandex.vertis.billing.banker.tasks.YandexKassaV3DuplicatePaymentChecker.DuplicatePaymentException
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * Spec on [[YandexKassaV3DuplicatePaymentChecker]]
  *
  * @author ruslansd
  */
class YandexKassaV3DuplicatePaymentCheckerSpec
  extends Matchers
  with AnyWordSpecLike
  with AsyncSpecBase
  with MockitoSupport
  with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    Mockito.clearInvocations(epochServiceMock)
    super.beforeEach()
  }

  private def withExternalIdGen(externalId: String): Gen[Record] =
    YV3ExternalRecordGen.map(_.copy(externalId = externalId))

  private def externalDaoMock(records: Iterable[Record]) = {
    val m = mock[YandexKassaV3PaymentRequestExternalDao]

    when(m.get(?))
      .thenReturn(Future.successful(records))

    m
  }

  private val epochServiceMock = {
    val m = mock[EpochService]

    when(m.get(?))
      .thenReturn(Future.successful(0L))

    when(m.set(?, ?))
      .thenReturn(Future.successful(()))
    m
  }

  private def task(records: Iterable[Record]) =
    new YandexKassaV3DuplicatePaymentChecker(externalDaoMock(records), epochServiceMock)

  "YandexKassaV3DuplicatePaymentChecker" should {

    "correctly work on empty set of records" in {
      task(Iterable.empty).execute().futureValue

      Mockito.verify(epochServiceMock).get(?)
      Mockito.verify(epochServiceMock, Mockito.times(0)).set(?, ?)
    }

    "correctly work without duplicates" in {
      val clearRecords = YV3ExternalRecordGen.next(10)
      task(clearRecords).execute().futureValue

      Mockito.verify(epochServiceMock).get(?)
      Mockito.verify(epochServiceMock).set(?, ?)
    }

    "correctly work with duplicates" in {
      val externalId = readableString().next
      val withDuplicates = withExternalIdGen(externalId).next(10)
      intercept[DuplicatePaymentException] {
        task(withDuplicates).execute().await
      }

      Mockito.verify(epochServiceMock).get(?)
    }
  }
}
