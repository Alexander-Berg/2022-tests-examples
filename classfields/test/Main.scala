package ru.yandex.vertis.billing

import java.io.{BufferedWriter, File, FileWriter}
import ru.yandex.vertis.billing.dao.CallCenterCallDao.NotMatched
import ru.yandex.vertis.billing.dao.impl.jdbc.{DualDatabase, JdbcCallCenterCallDao, JdbcCallFactDao, JdbcConnector}
import ru.yandex.vertis.billing.model_core.{CallFact, Phone}
import ru.yandex.vertis.billing.model_core.callcenter.CallCenterCall
import ru.yandex.vertis.billing.service.CallsResolutionService.{All, Since}

import scala.concurrent.duration
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

/**
  * @author tolmach
  */
object Main extends App with JdbcConnector {

  override protected def port = 3317

  override protected def databaseName = "vs_bill_realty"

  val callCenterCallDao = new JdbcCallCenterCallDao(database)

  val callFactDao = new JdbcCallFactDao(DualDatabase(database, database))

  private case class CallIdentifier(calleeNumber: String, callerNumber: Phone)

  private object CallIdentifier {

    def apply(callCenterCall: CallCenterCall): CallIdentifier = {
      new CallIdentifier(callCenterCall.calleeNumber, callCenterCall.callerNumber)
    }

    def apply(callFact: CallFact): CallIdentifier = {
      new CallIdentifier(callFact.incoming, callFact.getRedirectOrThrow())
    }

  }

  sealed trait MatchResult {

    def callCenterCall: CallCenterCall

  }

  case class MatchMiss(callCenterCall: CallCenterCall) extends MatchResult

  case class PossibleMatchHitWithDiff(callCenterCall: CallCenterCall, candidates: Seq[CallFact], diff: Long)
    extends MatchResult

  private val TimestampMaxDiff = 30.minutes

  case class GroupedMatchResults(
      notMatched: Seq[MatchMiss],
      haveCandidates: Seq[PossibleMatchHitWithDiff],
      matched: Seq[PossibleMatchHitWithDiff])

  object GroupedMatchResults {

    val Empty = GroupedMatchResults(Seq.empty, Seq.empty, Seq.empty)

  }

  def asCallCenterCallLine(callCenterCall: CallCenterCall): Seq[String] = {
    import callCenterCall._
    val calleePhoneStr = s"+${callerNumber.country}${callerNumber.code}${callerNumber.phone}"
    Seq(calleeNumber, calleePhoneStr, timestamp.toString)
  }

  val units = List(
    (duration.DAYS, "days"),
    (duration.HOURS, "hours"),
    (duration.MINUTES, "minutes"),
    (duration.SECONDS, "seconds")
  )

  def humanReadable(timediff: Long): String = {
    val init = ("", timediff)
    val (human, _) = units.foldLeft(init) { case ((human, rest), (unit, name)) =>
      val res = unit.convert(rest, duration.MILLISECONDS)
      if (res > 0) {
        val newHuman = human + " " + res + " " + name
        val newRest = rest - duration.MILLISECONDS.convert(res, unit)
        (newHuman, newRest)
      } else {
        (human, rest)
      }

    }
    human.trim
  }

  def asCsv(seq: Seq[String]): String = {
    seq.mkString(",")
  }

  case class GrouppedTextMatchResults(notMatched: Seq[String], haveCandidates: Seq[String], matched: Seq[String])

  def readableResults(results: Seq[MatchResult]): GrouppedTextMatchResults = {
    val grouped = results.foldLeft(GroupedMatchResults.Empty) {
      case (acc, m: MatchMiss) =>
        acc.copy(notMatched = acc.notMatched :+ m)
      case (acc, p: PossibleMatchHitWithDiff) if p.diff > TimestampMaxDiff.toMillis =>
        acc.copy(haveCandidates = acc.haveCandidates :+ p)
      case (acc, p: PossibleMatchHitWithDiff) =>
        acc.copy(matched = acc.matched :+ p)
    }

    val noMatchedFirstLine = asCsv(Seq("Входящий", "Подменник", "Время звонка"))
    val notMatchedLines = grouped.notMatched.sortBy(-_.callCenterCall.timestamp.getMillis).map { n =>
      asCsv(asCallCenterCallLine(n.callCenterCall))
    }
    val notMatchedText = noMatchedFirstLine +: notMatchedLines

    val candidatesFirstLine = asCsv(
      Seq("Входящий", "Подменник", "Время звонка", "Id звонка кандидата на стороне биллинга", "Разница времени звонка")
    )

    val haveCandidatesLines = grouped.haveCandidates.flatMap { h =>
      h.candidates.map { candidate =>
        asCsv {
          asCallCenterCallLine(h.callCenterCall) ++ Seq(candidate.id, humanReadable(h.diff))
        }
      }
    }
    val haveCandidatesText = candidatesFirstLine +: haveCandidatesLines

    val matchedLines = grouped.matched.flatMap { m =>
      m.candidates.map { candidate =>
        asCsv {
          asCallCenterCallLine(m.callCenterCall) ++ Seq(candidate.id, humanReadable(m.diff))
        }
      }
    }
    val matchedText = candidatesFirstLine +: matchedLines

    GrouppedTextMatchResults(notMatchedText, haveCandidatesText, matchedText)
  }

  def eval(callCenterCalls: Seq[CallCenterCall], callFacts: Seq[CallFact]): Seq[MatchResult] = {
    val callFactMap = callFacts.groupBy(CallIdentifier.apply)
    val callCenterCallMap = callCenterCalls.groupBy(CallIdentifier.apply)

    callCenterCallMap.flatMap { case (id, callCenterCalls) =>
      callFactMap.get(id) match {
        case Some(callFacts) =>
          findMatches(callCenterCalls, callFacts)
        case None =>
          callCenterCalls.map(MatchMiss.apply)
      }
    }.toSeq
  }

  private def diff(callCenterCall: CallCenterCall, callFact: CallFact): Long = {
    callFact.timestamp.getMillis - callCenterCall.timestamp.getMillis
  }

  private def absDiff(callCenterCall: CallCenterCall, callFact: CallFact): Long = {
    math.abs(diff(callCenterCall, callFact))
  }

  private def findMatches(
      callCenterCalls: Seq[CallCenterCall],
      callFacts: Seq[CallFact]): Seq[PossibleMatchHitWithDiff] = {
    if (callCenterCalls.isEmpty || callFacts.isEmpty) {
      Seq.empty
    } else {
      val callCenterCall = callCenterCalls.head
      val sorted = callFacts.sortWith { case (a, b) =>
        val aDiff = absDiff(callCenterCall, a)
        val bDiff = absDiff(callCenterCall, b)
        aDiff < bDiff || (aDiff == bDiff && a.timestamp.getMillis < b.timestamp.getMillis)
      }
      val firstCandidate = sorted.head
      val aimAbsDiff = absDiff(callCenterCall, firstCandidate)
      val candidates = sorted.takeWhile { callFact =>
        absDiff(callCenterCall, callFact) == aimAbsDiff
      }
      val result = PossibleMatchHitWithDiff(callCenterCall, candidates, aimAbsDiff)
      result +: findMatches(callCenterCalls.tail, callFacts)
    }
  }

  def writeFile(filename: String, lines: Seq[String]): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- lines) {
      bw.write(line)
      bw.newLine()
    }
    bw.close()
  }

  val r = for {
    notMatchedCallCenterCalls <- callCenterCallDao.getTry(NotMatched)
    evaluatedCallFacts <- callFactDao.getT(All)
    callFacts = evaluatedCallFacts.map(_.call)
    matchResult = eval(notMatchedCallCenterCalls.toSeq, callFacts)
    grouppedTexts = readableResults(matchResult)
    _ = writeFile("not_matched.csv", grouppedTexts.notMatched)
    _ = writeFile("have_candidates.csv", grouppedTexts.haveCandidates)
    _ = writeFile("matched.csv", grouppedTexts.matched)
  } yield ()

  r.get

}
