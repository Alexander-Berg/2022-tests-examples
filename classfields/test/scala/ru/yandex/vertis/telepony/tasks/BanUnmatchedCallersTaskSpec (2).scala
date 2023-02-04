package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.joda.time.Days
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentMatchers, Mockito}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.?
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.BlockInfo.Origins
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.{BlacklistService, UnmatchedCallService}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

class BanUnmatchedCallersTaskSpec extends SpecBase {

  import BanUnmatchedCallersTask._
  import BanUnmatchedCallersTaskSpec._

  implicit val actorSystem: ActorSystem = ActorSystem("test", ConfigFactory.empty())
  implicit val actorMaterializer: Materializer = Materializer(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = actorMaterializer.executionContext

  private val blacklistService: BlacklistService = Mockito.mock(classOf[BlacklistService])
  private val unmatchedService: UnmatchedCallService = Mockito.spy(classOf[UnmatchedCallService])
  private val defaultConfig = Map(proxyThresholdKey -> 30, periodKey -> 180, banDurationKey -> 1)

  private val task = new BanUnmatchedCallersTask(
    unmatchedService,
    blacklistService,
    ConfigFactory.parseMap(defaultConfig.asJava)
  )

  @nowarn
  override def beforeEach(): Unit = {
    Mockito.reset(blacklistService, unmatchedService)
    stubUnmatchedCalls(Iterable.empty)
    stubBlackListGet(Set.empty, Set.empty)
    stubBlackListAdd(Set.empty)
  }

  @nowarn
  private def stubBlackListAdd(errorNumbers: Set[RefinedSource]): Unit = {
    MockitoSupport.stub(blacklistService.add _) {
      case info if info != null && errorNumbers.contains(info.source) => Future.failed(new IgnoredException)
      case _ => Future.successful(())
    }
  }

  @nowarn
  private def stubBlackListGet(blockedNumbers: Set[RefinedSource], errorNumbers: Set[RefinedSource]): Unit = {
    MockitoSupport.stub(blacklistService.get _) {
      case (refinedSource, AntiFraudOptions.Blacklist) if blockedNumbers.contains(refinedSource) =>
        Future.successful(
          Some(
            BlockInfo(refinedSource, BlockReasons.Other, None, Origins.BlacklistTasks, AntiFraudOptions.Blacklist, None)
          )
        )
      case (refinedSource, AntiFraudOptions.Blacklist) if errorNumbers.contains(refinedSource) =>
        Future.failed(new IgnoredException)
      case _ =>
        Future.successful(None)
    }
  }

  @nowarn
  private def stubUnmatchedCalls(calls: Iterable[RefinedSource]): Unit = {
    MockitoSupport.stub(unmatchedService.listUniqueCallers(_, _)) {
      case _ => Future.successful(calls)
    }
  }

  private def processTask(configMap: Map[String, _]): Unit = {
    val future = task.payload(ConfigFactory.parseMap(configMap.asJava))
    future.recover {
      case _: IgnoredException => ()
    }.futureValue
  }

  "BanUnmatchedCallersTask" should {
    "specify correct threshold" in {
      processTask(Map(proxyThresholdKey -> 387))
      verify(unmatchedService, times(1)).listUniqueCallers(?, ArgumentMatchers.eq(387))
    }

    "specify correct period of time" in {
      processTask(Map(periodKey -> 283))
      val period = Days.days(283)
      verify(unmatchedService, times(1)).listUniqueCallers(ArgumentMatchers.eq(period), ?)
    }

    "set correct ban period" in {
      stubUnmatchedCalls(List(number1, number2, number3))
      processTask(Map(banDurationKey -> 345))
      verify(unmatchedService, times(1)).listUniqueCallers(?, ?)

      verify(blacklistService, times(1)).add(
        ArgumentMatchers.argThat { block: BlockInfo =>
          block.source == number1 && block.deadline.get.minusDays(346).isBeforeNow &&
          block.deadline.get.minusDays(344).isAfterNow
        }
      )

      verify(blacklistService, times(1)).add(
        ArgumentMatchers.argThat { block: BlockInfo =>
          block.source == number2 && block.deadline.get.minusDays(346).isBeforeNow &&
          block.deadline.get.minusDays(344).isAfterNow
        }
      )

      verify(blacklistService, times(1)).add(
        ArgumentMatchers.argThat { block: BlockInfo =>
          block.source == number3 && block.deadline.get.minusDays(346).isBeforeNow &&
          block.deadline.get.minusDays(344).isAfterNow
        }
      )
    }

    "not ban already banned numbers" in {
      stubUnmatchedCalls(List(number1, number2, number3, number4))
      stubBlackListGet(Set(number1, number3), Set.empty)
      processTask(Map.empty)

      verify(blacklistService, times(2)).add(?)
      verify(blacklistService, times(1)).add(ArgumentMatchers.argThat { info: BlockInfo =>
        info.source == number2
      })
      verify(blacklistService, times(1)).add(ArgumentMatchers.argThat { info: BlockInfo =>
        info.source == number4
      })
    }

    "process all numbers even if exception occurs" in {
      stubUnmatchedCalls(List(number1, number2, number3, number4))
      stubBlackListGet(Set.empty, Set(number1))
      stubBlackListAdd(Set(number3))
      processTask(Map.empty)

      verify(blacklistService, times(3)).add(?)
      verify(blacklistService, times(1)).add(ArgumentMatchers.argThat { info: BlockInfo =>
        info.source == number2
      })
      verify(blacklistService, times(1)).add(ArgumentMatchers.argThat { info: BlockInfo =>
        info.source == number3
      })
      verify(blacklistService, times(1)).add(ArgumentMatchers.argThat { info: BlockInfo =>
        info.source == number4
      })
    }
  }
}

object BanUnmatchedCallersTaskSpec {
  private val number1 = RefinedSource("+70000000001")
  private val number2 = RefinedSource("+70000000002")
  private val number3 = RefinedSource("+70000000003")
  private val number4 = RefinedSource("+70000000004")

  private class IgnoredException extends Throwable
}
