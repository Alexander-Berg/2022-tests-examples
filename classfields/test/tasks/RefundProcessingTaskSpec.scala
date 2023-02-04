package ru.yandex.vertis.billing.banker.tasks

import java.util.regex.Pattern
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.service.{EpochService, PaymentSystemService}
import ru.yandex.vertis.mockito.MockitoSupport
import RefundProcessingTaskSpec.{
  GenPaymentSystems,
  GenPaymentSystemsWithError,
  MessagePattern,
  TestSource,
  ValidationException
}
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.util.LogFetchingProvider
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.RequestFilter
import ru.yandex.vertis.billing.banker.model.{
  Epoch,
  PaymentRequestId,
  PaymentSystemId,
  PaymentSystemIds,
  RefundPaymentRequest,
  RefundPaymentRequestId
}
import ru.yandex.vertis.billing.banker.model.gens.{refundRequestGen, Producer}
import ru.yandex.vertis.billing.banker.tasks.RefundProcessingTask.WrappedRefundProcessingException
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.concurrent.Future
import scala.util.Random

class RefundProcessingTaskSpec
  extends Matchers
  with LogFetchingProvider
  with AnyWordSpecLike
  with MockitoSupport
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  private def checkLoggingEvent(event: ILoggingEvent): Unit = {
    event.getLevel shouldBe Level.ERROR

    val wrapper = event.getThrowableProxy
    wrapper.getClassName shouldBe classOf[WrappedRefundProcessingException].getName
    val matcher = MessagePattern.matcher(wrapper.getMessage)
    matcher.matches() shouldBe true

    val cause = wrapper.getCause
    cause.getClassName shouldBe classOf[IllegalArgumentException].getName
    cause.getMessage shouldBe ValidationException.getMessage: Unit
  }

  "RefundProcessingTask" should {
    "process all" in {
      forAll(GenPaymentSystems) { case TestSource(wrappers, epochService) =>
        val pss = wrappers.map(_.pss)
        val events = withLogFetching[RefundProcessingTask, Unit] {
          val task = new RefundProcessingTask(pss, epochService)
          task.execute().futureValue
        }
        events.logEvents.isEmpty shouldBe true
      }
    }
    "fail and process rest" in {
      forAll(GenPaymentSystemsWithError) { case TestSource(wrappers, epochService) =>
        val pss = wrappers.map(_.pss)
        val errorsCount = wrappers.count(_.withError)
        val events = withLogFetching[RefundProcessingTask, Unit] {
          val task = new RefundProcessingTask(pss, epochService)
          intercept[WrappedRefundProcessingException] {
            task.execute().await
          }
          ()
        }

        events.logEvents.size shouldBe errorsCount
        events.logEvents.foreach(checkLoggingEvent)
      }
    }
  }

}

object RefundProcessingTaskSpec extends MockFactory {

  private val ValidationException = new IllegalArgumentException("OOPS")

  private val MessagePattern = Pattern.compile(
    s"Processing of refund request with id \\[.*\\] was failed"
  )

  private def mockPaymentSystem(
      psId: PaymentSystemId,
      requests: Iterable[RefundPaymentRequest],
      withError: Boolean): PaymentSystemServiceWrapper = {
    val paymentSystemService = mock[PaymentSystemService]

    (() => paymentSystemService.psId).expects().returns(psId)

    (paymentSystemService.getRefundPaymentRequests(_: Seq[RequestFilter])(_: RequestContext)).expects(*, *).returning {
      Future.successful(requests)
    }

    val errorIndex =
      if (withError) {
        Gen.chooseNum(0, requests.size - 1).next
      } else {
        requests.size
      }

    requests.zipWithIndex.map {
      case (spoiledRequest, `errorIndex`) =>
        (paymentSystemService
          .processRefundRequest(_: RefundPaymentRequestId, _: PaymentRequestId)(_: RequestContext))
          .expects(spoiledRequest.id, spoiledRequest.source.refundFor, *)
          .returns(Future.failed(ValidationException))
      case (request, _) =>
        (paymentSystemService
          .processRefundRequest(_: RefundPaymentRequestId, _: PaymentRequestId)(_: RequestContext))
          .expects(request.id, request.source.refundFor, *)
          .returns(Future.unit)
    }

    PaymentSystemServiceWrapper(paymentSystemService, withError)
  }

  private val refundsGen: Gen[List[RefundPaymentRequest]] = {
    for {
      count <- Gen.chooseNum(15, 25)
      refundRequest <- Gen.listOfN(count, refundRequestGen())
    } yield refundRequest
  }

  private def genPaymentSystem(psId: PaymentSystemId, withError: Boolean = false): PaymentSystemServiceWrapper = {
    mockPaymentSystem(psId, refundsGen.next, withError)
  }

  private def mockEpochService(systems: Seq[(PaymentSystemId, Boolean)]): EpochService = {
    val epochService = mock[EpochService]

    systems.foreach { case (psId, hasError) =>
      val marker = s"${psId}_refund_processing"
      (epochService.get(_: String)).expects(marker).returns(Future.successful(0L))
      if (!hasError) {
        (epochService.set(_: String, _: Epoch)).expects(marker, *).returns(Future.unit)
      }
    }

    epochService
  }

  private val NonEmptyPaymentSystemIdSeqGen: Gen[Seq[PaymentSystemId]] = {
    Gen.chooseNum(1, PaymentSystemIds.values.size).map { count =>
      val ids = PaymentSystemIds.values.take(count).toSeq
      Random.shuffle(ids)
    }
  }

  private val GenPaymentSystems: Gen[TestSource] = {
    NonEmptyPaymentSystemIdSeqGen.map { psIds =>
      val mocks = psIds.map { psId =>
        genPaymentSystem(psId)
      }

      val source = psIds.map { psId =>
        (psId, false)
      }
      val epochService = mockEpochService(source)
      TestSource(mocks, epochService)
    }
  }

  private def genNonZeroMask(size: Int): Gen[Seq[Boolean]] = {
    for {
      nonZeroCount <- Gen.chooseNum(1, size)
      nonZero = (1 to nonZeroCount).map(_ => true)
      zeros = (1 to (size - nonZeroCount)).map(_ => false)
      all = nonZero ++ zeros
      mask = Random.shuffle(all)
    } yield mask
  }

  case class PaymentSystemServiceWrapper(pss: PaymentSystemService, withError: Boolean)

  case class TestSource(pssWrappers: Seq[PaymentSystemServiceWrapper], epochService: EpochService)

  private val GenPaymentSystemsWithError: Gen[TestSource] =
    for {
      psIds <- NonEmptyPaymentSystemIdSeqGen
      mask <- genNonZeroMask(psIds.size)
      psIdWithStatus = psIds.zip(mask)
      systemMocks = psIdWithStatus.map { t =>
        genPaymentSystem(t._1, t._2)
      }
      epochServiceMock = mockEpochService(psIdWithStatus)
    } yield TestSource(systemMocks, epochServiceMock)

}
