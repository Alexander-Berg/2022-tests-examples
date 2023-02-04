package ru.yandex.vertis.tvm.impl

import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Second, Span}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.tvm.TvmClient.{GrantTypes, TicketRequest, TicketResponse}
import ru.yandex.vertis.tvm.{RequestSigner, ServiceId, Sign, TvmClient, TvmServerException}
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.util.time.DateTimeUtil

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.{DurationInt, TimeUnit}
import scala.util.{Success, Try}

/**
  * Runnable spec on [[TvmTicketProviderImpl]]
  *
  * @author alex-kovalenko
  */
class TvmTicketProviderImplSpec
  extends AnyWordSpec
  with Matchers
  with MockFactory
  with OneInstancePerTest
  with ScalaFutures {

  implicit val ec: ExecutionContext = Threads.SameThreadEc

  val dstIds = List(2, 3)

  val ticketsResponse = TicketResponse(dstIds.map(id => id -> s"ticket$id").toMap)

  val sign = "signature"

  val client = mock[TvmClient]
  val signer = mock[RequestSigner]

  val defaultSettings = TvmTicketProviderImpl.Settings(1, "secret", dstIds)

  val defaultExecutor = mock[ScheduledExecutorService]

  private def defaultSignerSign =
    (signer.sign _).expects(*, *).returning(Success(sign))

  private def defaultExecutorScheduleAtFixedRate =
    (defaultExecutor.scheduleAtFixedRate _)
      .expects(*, *, *, *)
      .onCall { (runnable, _, _, _) =>
        runnable.run()
        mock[ScheduledFuture[Unit]]
      }

  private def defaultTicketsResponse =
    (client.getTickets _).expects(*).returning(Future.successful(ticketsResponse))

  private def newProvider(
      client: TvmClient = client,
      signer: RequestSigner = signer,
      settings: TvmTicketProviderImpl.Settings = defaultSettings,
      executor: ScheduledExecutorService = defaultExecutor) =
    new TvmTicketProviderImpl(client, signer, settings, executor)

  "TvmTicketProvider" should {
    "get tickets on init" in {
      val settings = defaultSettings
      val startTime = DateTimeUtil.now()
      var signTs: DateTime = None.orNull
      inSequence {
        (defaultExecutor.scheduleAtFixedRate _)
          .expects(*, 0, settings.ttl.length, settings.ttl.unit)
          .onCall { (runnable, _, _, _) =>
            runnable.run()
            mock[ScheduledFuture[Unit]]
          }
        (signer.sign _)
          .expects(where { (ts, clientIds) =>
            ts.getMillis should be > startTime.getMillis
            clientIds should contain theSameElementsAs settings.dstIds
            signTs = ts
            true
          })
          .returning(Success(sign))
        (client.getTickets _)
          .expects(where { (request: TicketRequest) =>
            request.sign shouldBe sign
            request.ts shouldBe signTs
            request.src shouldBe settings.selfClientId
            request.dst should contain theSameElementsAs settings.dstIds
            request.grantType shouldBe GrantTypes.ClientCredentials
            true
          })
          .returning(Future.successful(ticketsResponse))
      }
      val provider = newProvider(settings = settings)
      dstIds.foreach { dstId =>
        provider.provide(dstId, updateIfNotExist = false).futureValue shouldBe s"ticket$dstId"
      }
    }

    "renew tickets in ttl" in {
      val renewingFinished = Promise[Unit]()
      val signer = new RequestSigner {
        private val initialRun = new AtomicBoolean(true)
        override def sign(ts: DateTime, dstClientIds: List[ServiceId]): Try[Sign] = {
          val result = if (initialRun.get()) "initial" else "renewing"
          initialRun.set(false)
          Success(result)
        }
      }
      val client: TvmClient = request => {
        if (request.sign == "renewing") renewingFinished.trySuccess(())
        Future.successful(ticketsResponse)
      }
      val settings = defaultSettings.copy(ttl = 100.millis)
      val executor = Executors.newSingleThreadScheduledExecutor()

      newProvider(client, signer, settings, executor)

      // Ждём секунду, чтобы тест не флапал при тормозах агентов. По факту он работает примерно за 100 мс, как и должен.
      implicit val patienceConfig: PatienceConfig =
        PatienceConfig(timeout = Span(1, Second), interval = Span(15, Millis))
      renewingFinished.future.futureValue
    }

    "retry get tickets" in {
      val retryPeriod = 10.seconds
      val settings = defaultSettings.copy(retryPeriod = retryPeriod)

      var callCount = 0

      inAnyOrder {
        defaultExecutorScheduleAtFixedRate.once()
        (defaultExecutor
          .schedule(_: Runnable, _: Long, _: TimeUnit))
          .expects(*, retryPeriod.length, retryPeriod.unit)
          .onCall { (runnable, _, _) =>
            runnable.run()
            mock[ScheduledFuture[Unit]]
          }
          .once()
        defaultSignerSign.anyNumberOfTimes()
        (client.getTickets _)
          .expects(*)
          .onCall { (_: TicketRequest) =>
            callCount += 1
            if (callCount == 1) {
              Future.failed(new TvmServerException("artificial"))
            } else {
              Future.successful(ticketsResponse)
            }
          }
          .anyNumberOfTimes()
      }
      newProvider(settings = settings)
    }

    "async provide ticket" in {
      val dstId = dstIds.head
      inSequence {
        defaultExecutorScheduleAtFixedRate.once()
        defaultSignerSign.once()
        defaultTicketsResponse.once()
      }
      newProvider().provide(dstId, updateIfNotExist = false).futureValue shouldBe s"ticket$dstId"
    }

    "async provide ticket with force update" in {
      val dstId = dstIds.head
      (defaultExecutor.scheduleAtFixedRate _)
        .expects(*, *, *, *)
        .returning(mock[ScheduledFuture[Unit]])
      defaultSignerSign.never()
      val provider = newProvider()
      inSequence {
        defaultSignerSign.once()
        defaultTicketsResponse.once()
      }
      provider.provide(dstId, updateIfNotExist = true).futureValue shouldBe s"ticket$dstId"
    }
  }
}
