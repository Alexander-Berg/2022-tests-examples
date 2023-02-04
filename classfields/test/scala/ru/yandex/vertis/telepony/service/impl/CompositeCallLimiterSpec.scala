package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.logging.LoggingCompositeCallLimiter
import ru.yandex.vertis.telepony.service.{AntiFraudService, ResolutionService, WhitelistHistoryService, WhitelistService}

import scala.concurrent.Future

/**
  * @author @logab
  */
class CompositeCallLimiterSpec extends SpecBase with MockitoSupport {

  val phone = Phone("+79123456789")
  val source = RefinedSource.from("1")

  val defaultDomain = TypedDomains.autoru_def

  val resolutionServices =
    for {
      callsCounterResoluton <- Seq(None, Some(AntiFraudOptions.CallsCounter))
      blacklistResoluton <- Seq(None, Some(AntiFraudOptions.Blacklist))
      aonBlacklistResoluton <- Seq(None, Some(AntiFraudOptions.AonBlacklist))
    } yield {
      val resolutionService = mock[ResolutionService]
      val resolutions = Set(callsCounterResoluton, blacklistResoluton, aonBlacklistResoluton).flatten
      when(resolutionService.getBlockResolutions(?)).thenReturn(Future.successful(resolutions))
      resolutionService
    }

  val Seq(passAllRS, blockAonRS, blockBlRS, blockAonBlRS, blockCCRS, blockAonCCRS, blockBlCCRS, blockAllRs) =
    resolutionServices

  val whitelistServices = Seq(true, false).map { res =>
    val whitelistService = mock[WhitelistService]
    when(whitelistService.exists(?)).thenReturn(Future.successful(res))
    whitelistService
  }

  val whitelistHistoryServices = Seq(true, false).map { res =>
    val whitelistHistoryService = mock[WhitelistHistoryService]
    when(whitelistHistoryService.exists(?, ?)).thenReturn(Future.successful(res))
    whitelistHistoryService
  }

  val Seq(existWls, notExistWls) = whitelistServices

  val Seq(existWH, notExistWH) = whitelistHistoryServices

  val antifraudServices =
    Seq(AntiFraud.Disabled, AntiFraud.AonAndBl, AntiFraud.AonAndBlAndCounter, AntiFraud.All).map { antifraud =>
      new AntiFraudService {

        override def antiFraud(source: Phone): Future[Set[AntiFraudOption]] =
          Future.successful(antifraud)
      }
    }

  val Seq(disableAntifraud, aonBlAntifraud, aonBlCCAntifraud, allAntifraud) = antifraudServices

  "CompositeCallLimiter" should {

    "pass when antiFraud disabled" in {
      for {
        resolutionService <- resolutionServices
        whitelistService <- whitelistServices
        whitelistHistoryService <- whitelistHistoryServices
      } yield {
        val compositeLimiter = new CompositeCallLimiterImpl(
          resolutionService,
          whitelistService,
          whitelistHistoryService,
          disableAntifraud,
          defaultDomain
        ) with LoggingCompositeCallLimiter
        compositeLimiter.blockReasons(source, phone).futureValue shouldBe empty
      }
    }

    "pass when no limit" in {
      val compositeLimiter = new CompositeCallLimiterImpl(
        passAllRS,
        notExistWls,
        notExistWH,
        allAntifraud,
        defaultDomain
      ) with LoggingCompositeCallLimiter
      compositeLimiter.blockReasons(source, phone).futureValue shouldBe empty
    }

    "pass when whitelist limiter pass" in {
      for {
        resolutionService <- resolutionServices
        whitelist <- Seq(existWls -> existWH, existWls -> notExistWH, notExistWls -> existWH)
      } yield {
        val compositeLimiter = new CompositeCallLimiterImpl(
          resolutionService,
          whitelist._1,
          whitelist._2,
          aonBlCCAntifraud,
          defaultDomain
        ) with LoggingCompositeCallLimiter
        compositeLimiter.blockReasons(source, phone).futureValue shouldBe empty
      }
    }

    "block when antifraud is enabled" in {
      for {
        resolutionService <- Seq(blockAonRS, blockBlRS, blockAonBlRS, blockCCRS, blockAonCCRS, blockBlCCRS, blockAllRs)
        whitelistService <- whitelistServices
        whitelistHistoryService <- whitelistHistoryServices
      } yield {
        val compositeLimiter = new CompositeCallLimiterImpl(
          resolutionService,
          whitelistService,
          whitelistHistoryService,
          allAntifraud,
          defaultDomain
        ) with LoggingCompositeCallLimiter
        compositeLimiter.blockReasons(source, phone).futureValue should not be empty
      }
    }

    "pass when number in whitelist" in {
      for {
        resolutionService <- Seq(blockAonRS, blockBlRS, blockAonBlRS, blockCCRS, blockAonCCRS, blockBlCCRS, blockAllRs)
        (whitelistService, whitelistHistoryService) <- Seq(
          existWls -> notExistWH,
          notExistWls -> existWH,
          existWls -> existWH
        )
      } yield {
        val compositeLimiter = new CompositeCallLimiterImpl(
          resolutionService,
          whitelistService,
          whitelistHistoryService,
          aonBlCCAntifraud,
          defaultDomain
        ) with LoggingCompositeCallLimiter
        compositeLimiter.blockReasons(source, phone).futureValue shouldBe empty
      }
    }

    "block when antifraud is enabled and not in whitelist" in {
      for {
        resolutionService <- resolutionServices
      } yield {
        val compositeLimiter = new CompositeCallLimiterImpl(
          resolutionService,
          notExistWls,
          notExistWH,
          allAntifraud,
          defaultDomain
        ) with LoggingCompositeCallLimiter
        val ukrainianSource = "+380664324213"
        compositeLimiter.blockReasons(RefinedSource(ukrainianSource), phone).futureValue should not be empty
      }
    }
  }
}
