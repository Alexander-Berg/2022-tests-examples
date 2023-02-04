package ru.yandex.vertis.billing.tasks

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.model_core.callcenter.{CallCenterCall, CallCenterCallId}
import ru.yandex.vertis.billing.dao.CallCenterCallDao.{MarkAsMatched, NotMatched, Patch => CallCenterCallPatch}
import ru.yandex.vertis.billing.dao.{CallCenterCallDao, CallFactDao}
import ru.yandex.vertis.billing.model_core.{
  CallFact,
  CallFactHeader,
  CallFactId,
  EvaluatedCallFact,
  Phone,
  TeleponyCallFact
}
import ru.yandex.vertis.billing.service.CallsResolutionService.Patch
import ru.yandex.vertis.billing.util.DateTimeUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.model_core.gens.{
  evaluatedCallFactGen,
  randomPrintableString,
  CallCenterCallGen,
  EvaluatedCallFactGenParams,
  Producer,
  TeleponyCallFactGenCallTypes,
  TeleponyCallFactGenParams
}
import ru.yandex.vertis.billing.util.DateTimeUtils.DateTimeWithDuration

import scala.concurrent.duration.DurationInt
import CallCenterCallMatcherTaskSpec.{findMatches, MatchResult, TimestampMaxDiff}
import billing.common.testkit.zio.ZIOSpecBase
import org.joda.time.DateTime
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.wordspec.AnyWordSpec
import zio.Task

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}

@nowarn("msg=discarded non-Unit value")
class CallCenterCallMatcherTaskSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase
  with ZIOSpecBase {

  def mockCallCenterDao(unmatchedCalls: Iterable[CallCenterCall]): CallCenterCallDao = {
    val m = mock[CallCenterCallDao]

    when(m.get(NotMatched)).thenReturn(Task.succeed(unmatchedCalls))

    stub(m.update _) {
      case _: MarkAsMatched =>
        Success(())
      case other =>
        Failure(new IllegalArgumentException(s"Unexpected $other"))
    }

    m
  }

  def mockCallFactDao(values: Seq[EvaluatedCallFact]): CallFactDao = {
    val m = mock[CallFactDao]

    when(m.get(?)).thenReturn(Future.successful(values))

    stub(m.update(_: CallFactId, _: Patch)) {
      case (_, Patch(None, None, None, Some(_), false)) =>
        Success(())
      case other =>
        Failure(new IllegalArgumentException(s"Unexpected $other"))
    }

    m
  }

  def test(
      callCenterCalls: Iterable[CallCenterCall],
      callFacts: Seq[EvaluatedCallFact],
      expectedMatched: Iterable[MatchResult]): Task[Unit] = {

    val callCenterCallDao = mockCallCenterDao(callCenterCalls)

    val callFactDao = mockCallFactDao(callFacts)

    val task = new CallCenterCallMatcherTask(callCenterCallDao, callFactDao)

    val result = task.execute()

    result.as {
      Mockito.verify(callCenterCallDao).get(NotMatched)
      Mockito.verify(callFactDao, Mockito.times(1)).get(?)
      if (expectedMatched.nonEmpty) {
        val captor: ArgumentCaptor[CallCenterCallPatch] = ArgumentCaptor.forClass(classOf[CallCenterCallPatch])
        Mockito.verify(callCenterCallDao).update(captor.capture())
        captor.getValue match {
          case MarkAsMatched(ids) =>
            val expectedCallCenterCallIds = expectedMatched.map(_.callCenterCallId)
            ids.toSortedSet should contain theSameElementsAs expectedCallCenterCallIds
          case other =>
            throw new IllegalArgumentException(s"Unexpected $other")
        }

        val idsCaptor: ArgumentCaptor[CallFactId] = ArgumentCaptor.forClass(classOf[CallFactId])
        val patchesCaptor: ArgumentCaptor[Patch] = ArgumentCaptor.forClass(classOf[Patch])
        Mockito
          .verify(callFactDao, Mockito.times(expectedMatched.size))
          .update(
            idsCaptor.capture(),
            patchesCaptor.capture()
          )
        val actualCalls = idsCaptor.getAllValues.asScala.zip(patchesCaptor.getAllValues.asScala)
        val actualMatches = actualCalls.collect {
          case (callFactId, Patch(None, None, None, Some(callCenterCallId), false)) =>
            MatchResult(callFactId, callCenterCallId)
        }
        actualMatches should contain theSameElementsAs expectedMatched
      }
    }
  }

  val correctCallCenterCallsGen: Gen[CallCenterCall] = for {
    possibleInterval <- Gen.const(DateTimeUtils.wholeDay(DateTimeUtils.now()))
    timestamp <- Gen.choose(
      possibleInterval.from.getMillis + 3 * TimestampMaxDiff.toMillis,
      possibleInterval.to.getMillis - 3 * TimestampMaxDiff.toMillis
    )
    callCenterCall <- CallCenterCallGen
  } yield callCenterCall.copy(timestamp = DateTimeUtils.fromMillis(timestamp))

  def toPhone(str: String): Phone = {
    Phone(str.take(1), str.tail.take(3), str.drop(4))
  }

  def updateCallFact(
      evaluatedCallFact: EvaluatedCallFact
    )(updater: TeleponyCallFact => TeleponyCallFact): EvaluatedCallFact = {
    evaluatedCallFact.call match {
      case t: TeleponyCallFact =>
        evaluatedCallFact.copy(call = updater(t))
      case other =>
        throw new IllegalArgumentException(s"Unexpected $other")
    }
  }

  def redirectCallFactsGen(
      timestamp: DateTime,
      calleeNumber: String,
      callerNumber: Phone,
      minTimeDif: Long,
      maxTimeDif: Long): Gen[EvaluatedCallFact] =
    for {
      evaluatedCallFact <- evaluatedCallFactGen(
        EvaluatedCallFactGenParams(
          TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect)
        )
      )
      timestampDiffAbs <- Gen.choose(minTimeDif, maxTimeDif)
      timestampDiff <- Gen.oneOf(-timestampDiffAbs, timestampDiffAbs)
      changedTimestamp = timestamp.plus(timestampDiff)
      updatedEvaluatedCallFact = updateCallFact(evaluatedCallFact) { t =>
        t.copy(
          incoming = calleeNumber,
          redirect = Some(callerNumber),
          timestamp = changedTimestamp
        )
      }
    } yield updatedEvaluatedCallFact.copy(callCenterCallId = None)

  def matchGen(
      minTimeDif: Long,
      maxTimeDif: Long,
      count: Int): Gen[(CallCenterCall, List[EvaluatedCallFact])] = {
    correctCallCenterCallsGen.flatMap { callCenterCall =>
      val gen = redirectCallFactsGen(
        callCenterCall.timestamp,
        callCenterCall.calleeNumber,
        callCenterCall.callerNumber,
        minTimeDif,
        maxTimeDif
      )
      Gen.listOfN(count, gen).map { callFacts =>
        callCenterCall -> callFacts
      }
    }
  }

  def matchGen(
      minTimeDif: Long,
      maxTimeDif: Long): Gen[(CallCenterCall, EvaluatedCallFact)] = {
    matchGen(minTimeDif, maxTimeDif, 1).map { case (e, c) =>
      e -> c.head
    }
  }

  val EvaluatedDefaultRedirectCallFactGenParams = EvaluatedCallFactGenParams(
    TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect)
  )

  val MatchableRedirectCallsGen = matchGen(0, TimestampMaxDiff.toMillis)

  val UnMatchableRedirectCallsGen = matchGen(TimestampMaxDiff.toMillis + 1, TimestampMaxDiff.toMillis * 2)

  val UnMatchableCallbackCallsGen = MatchableRedirectCallsGen.map { case (callCenterCall, evaluatedCallFact) =>
    val changedEvaluatedCallFact = updateCallFact(evaluatedCallFact) { call =>
      call.copy(redirect = None, callbackOrderId = Some(randomPrintableString(16)))
    }
    (callCenterCall, changedEvaluatedCallFact)
  }

  "CallCenterCallMatcherTask" should {
    "match" when {
      "redirect and call center calls timestamp diff less or equal then needed" in {
        val data = MatchableRedirectCallsGen.next(100).toSeq
        val callCenterCalls = data.map(_._1)
        val callFacts = data.map(_._2)
        val matches = data.map { case (e, c) =>
          MatchResult(CallFactHeader(c.call).identity, e.id)
        }
        test(callCenterCalls, callFacts, matches).unsafeRun()
      }
      "one call center with many call facts" in {
        val data = matchGen(0, TimestampMaxDiff.toMillis, 10).next
        val callCenterCalls = Seq(data._1)
        val callFacts = data._2
        val matches = findMatches(callCenterCalls, callFacts)
        test(callCenterCalls, callFacts, matches).unsafeRun()
      }
      "one call center with two call facts with same absolute diff" in {
        val data = MatchableRedirectCallsGen.next
        val callCenterCall = data._1
        val callCenterCalls = Seq(callCenterCall)
        val callFact = data._2
        val firstCallFact = updateCallFact(callFact) {
          _.copy(timestamp = callCenterCall.timestamp.minus(TimestampMaxDiff))
        }
        val secondCallFact = updateCallFact(callFact) {
          _.copy(timestamp = callCenterCall.timestamp.plus(TimestampMaxDiff))
        }
        val callFacts = Seq(firstCallFact, secondCallFact)
        val matches = Seq(MatchResult(firstCallFact, callCenterCall))
        test(callCenterCalls, callFacts, matches).unsafeRun()
      }
      "two call center calls with one call fact" in {
        val data = MatchableRedirectCallsGen.next
        val firstCallCenterCall = data._1
        val secondCallCenterCall = firstCallCenterCall.copy(
          timestamp = firstCallCenterCall.timestamp.minus(TimestampMaxDiff)
        )
        val callCenterCalls = Seq(firstCallCenterCall, secondCallCenterCall)
        val callFact = data._2
        val updatedCallFact = updateCallFact(callFact) {
          _.copy(timestamp = firstCallCenterCall.timestamp)
        }
        val callFacts = Seq(updatedCallFact)
        val matches = Seq(MatchResult(updatedCallFact, firstCallCenterCall))
        test(callCenterCalls, callFacts, matches).unsafeRun()
      }
      "many call center calls with many call facts" in {
        val data = matchGen(0, 2 * TimestampMaxDiff.toMillis, 20).next(10)
        val callCenterCalls = data.map(_._1).toSeq
        val callFacts = data.flatMap(_._2).toSeq
        val matches = findMatches(callCenterCalls, callFacts)
        test(callCenterCalls, callFacts, matches).unsafeRun()
      }
    }
    "not match" when {
      "redirect and call center calls timestamp diff more then needed" in {
        val data = UnMatchableRedirectCallsGen.next(100).toSeq
        val callCenterCalls = data.map(_._1)
        val callFacts = data.map(_._2)
        test(callCenterCalls, callFacts, Seq.empty).unsafeRun()
      }

      "call center call to callback call" in {
        val data = UnMatchableCallbackCallsGen.next(100).toSeq
        val callCenterCalls = data.map(_._1)
        val callFacts = data.map(_._2)
        test(callCenterCalls, callFacts, Seq.empty).unsafeRun()
      }
    }
  }
}

object CallCenterCallMatcherTaskSpec {

  private val TimestampMaxDiff = 30.minutes

  private def diff(callCenterCall: CallCenterCall, callFact: CallFact): Long = {
    callFact.timestamp.getMillis - callCenterCall.timestamp.getMillis
  }

  private def absDiff(callCenterCall: CallCenterCall, callFact: CallFact): Long = {
    math.abs(diff(callCenterCall, callFact))
  }

  case class MatchResult(callFactId: CallFactId, callCenterCallId: CallCenterCallId)

  object MatchResult {

    def apply(callFact: EvaluatedCallFact, callCenterCall: CallCenterCall): MatchResult = {
      new MatchResult(CallFactHeader(callFact.call).identity, callCenterCall.id)
    }

  }

  case class MatchResultWithDiff(callFactId: CallFactId, callCenterCallId: CallCenterCallId, diff: Long) {

    def simplify: MatchResult = {
      MatchResult(callFactId, callCenterCallId)
    }

  }

  object MatchResultWithDiff {

    def apply(callFact: CallFact, callCenterCall: CallCenterCall, diff: Long): MatchResultWithDiff = {
      new MatchResultWithDiff(CallFactHeader(callFact).identity, callCenterCall.id, diff)
    }

  }

  private case class CallIdentifier(calleeNumber: String, callerNumber: Phone)

  private object CallIdentifier {

    def apply(callCenterCall: CallCenterCall): CallIdentifier = {
      new CallIdentifier(callCenterCall.calleeNumber, callCenterCall.callerNumber)
    }

    def apply(callFact: CallFact): CallIdentifier = {
      new CallIdentifier(callFact.incoming, callFact.redirect.get)
    }

  }

  private def findMatches(callCenterCalls: Seq[CallCenterCall], callFacts: Seq[EvaluatedCallFact]): Seq[MatchResult] = {
    val callFactMap = callFacts.map(_.call).groupBy(CallIdentifier.apply)
    val callCenterCallMap = callCenterCalls.groupBy(CallIdentifier.apply)
    val matchesWithDiff = callFactMap.flatMap { case (key, callFacts) =>
      val callCenterCalls = callCenterCallMap(key)
      findMatchesWithDiff(callCenterCalls, callFacts)
    }

    val groupedByCallFactId = matchesWithDiff.groupBy(_.callFactId)
    groupedByCallFactId.map { case (_, results) =>
      if (results.size > 1) {
        val sorted = results.toSeq.sortBy(_.diff)
        val value = sorted.minBy(m => math.abs(m.diff))
        value.simplify
      } else {
        results.head.simplify
      }
    }.toSeq
  }

  private def findMatchesWithDiff(
      callCenterCalls: Seq[CallCenterCall],
      callFacts: Seq[CallFact]): Seq[MatchResultWithDiff] = {
    if (callCenterCalls.isEmpty || callFacts.isEmpty) {
      Seq.empty
    } else {
      val callCenterCall = callCenterCalls.head
      val sorted = callFacts.sortWith { case (a, b) =>
        val aDiff = absDiff(callCenterCall, a)
        val bDiff = absDiff(callCenterCall, b)
        aDiff < bDiff || (aDiff == bDiff && a.timestamp.getMillis < b.timestamp.getMillis)
      }
      sorted.headOption match {
        case Some(callFact) if absDiff(callCenterCall, callFact) <= TimestampMaxDiff.toMillis =>
          val result = MatchResultWithDiff(callFact, callCenterCall, diff(callCenterCall, callFact))
          result +: findMatchesWithDiff(callCenterCalls.tail, sorted.tail)
        case _ =>
          findMatchesWithDiff(callCenterCalls.tail, sorted)
      }
    }
  }

}
