package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.enablers.Sortable
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.CallService.{Filter, Update}
import ru.yandex.vertis.telepony.time._
import ru.yandex.vertis.telepony.util.{FutureUtil, Page}
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author evans
  */
trait CallDaoV2Spec extends SpecBase with BeforeAndAfterEach with DatabaseSpec {

  def callDao: CallDaoV2

  def numberDao: OperatorNumberDaoV2

  def redirectDao: RedirectDaoV2

  private val Moscow: GeoId = 213

  private def forNumber(phone: Phone) = {
    OperatorNumber(phone, OperatorAccounts.MtsShared, Operators.Mts, Moscow, PhoneTypes.Local, Status.New(None), None)
  }

  override protected def beforeEach(): Unit = {
    callDao.clear().databaseValue.futureValue
    redirectDao.clear().databaseValue.futureValue
    numberDao.clear().databaseValue.futureValue
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    callDao.clear().databaseValue.futureValue
    redirectDao.clear().databaseValue.futureValue
    numberDao.clear().databaseValue.futureValue
    super.afterEach()
  }

  private val phone: OperatorNumber = forNumber(PhoneGen.next)
  private val target = PhoneGen.next

  private def sampleFor(id: String, from: RefinedSource, proxy: ActualRedirect) =
    sampleWithTimeFor(id, from, proxy, DateTime.now())

  private def sampleWithTimeFor(id: String, from: RefinedSource, proxy: ActualRedirect, dateTime: DateTime) =
    CallV2(
      id,
      dateTime,
      dateTime,
      id,
      Some(from),
      proxy.asHistoryRedirect,
      dateTime,
      20.seconds,
      15.seconds,
      hasRecord = true,
      CallResults.Unknown,
      fallbackCall = Gen.option(FallbackCallGen).next,
      whitelistOwnerId = Gen.option(ShortStr).next
    )

  object DayOrdering extends Ordering[CallsCountByDay] {

    override def compare(first: CallsCountByDay, second: CallsCountByDay): Int =
      first.day.compareTo(second.day)
  }

  implicit val sortable: Sortable[Array[CallsCountByDay]] =
    Sortable.sortableNatureOfArray(DayOrdering)

  "CallDao" should {
    "create call" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
    }

    "find call" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      callDao.exists(call.id).databaseValue.futureValue shouldEqual true
    }

    "not find call" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.exists(call.id).databaseValue.futureValue shouldEqual false
    }

    "list calls" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      callDao.list(Filter.Empty, Page(0, 10)).databaseValue.futureValue.size shouldEqual 1
    }

    "list by redirect id" in {
      numberDao.create(phone).databaseValue.futureValue

      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue

      val call1 = sampleFor("id1", RefinedSourceGen.next, redirect)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleFor("id2", RefinedSourceGen.next, redirect)
      callDao.create(call2).databaseValue.futureValue

      val filter =
        Filter.ByRedirectId(redirect.id, DateTime.now.minusYears(1), DateTime.now)
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.size shouldEqual 2
    }

    "list by redirect id and call_results" in {
      numberDao.create(phone).databaseValue.futureValue

      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue

      val call1 = sampleFor("id1", RefinedSourceGen.next, redirect).copy(callResult = CallResults.Success)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleFor("id2", RefinedSourceGen.next, redirect).copy(callResult = CallResults.NoAnswer)
      callDao.create(call2).databaseValue.futureValue

      val call3 = sampleFor("id3", RefinedSourceGen.next, redirect).copy(callResult = CallResults.Error)
      callDao.create(call3).databaseValue.futureValue

      val filter =
        Filter.ByRedirectId(
          redirect.id,
          DateTime.now.minusYears(1),
          DateTime.now,
          callResultOpt = Some(Set(CallResults.Success, CallResults.NoAnswer))
        )
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.size shouldEqual 2
    }

    "list by redirect-id and min-talk-duration" in {
      numberDao.create(phone).databaseValue.futureValue

      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue

      val call1 = sampleFor("id1", RefinedSourceGen.next, redirect).copy(talkDuration = 1.second)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleFor("id2", RefinedSourceGen.next, redirect).copy(talkDuration = 2.seconds)
      callDao.create(call2).databaseValue.futureValue

      val call3 = sampleFor("id3", RefinedSourceGen.next, redirect).copy(talkDuration = 3.seconds)
      callDao.create(call3).databaseValue.futureValue

      val filter =
        Filter.ByRedirectId(redirect.id, DateTime.now.minusYears(1), DateTime.now, minTalkDurationOpt = Some(2.seconds))
      val actualCalls = callDao.list(filter, Page(0, 10)).databaseValue.futureValue.values
      actualCalls.toSet should ===(Set(call2, call3))
    }

    "list by redirect-id and min-duration" in {
      numberDao.create(phone).databaseValue.futureValue

      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue

      val call1 = sampleFor("id1", RefinedSourceGen.next, redirect).copy(duration = 31.second)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleFor("id2", RefinedSourceGen.next, redirect).copy(duration = 32.seconds)
      callDao.create(call2).databaseValue.futureValue

      val call3 = sampleFor("id3", RefinedSourceGen.next, redirect).copy(duration = 33.seconds)
      callDao.create(call3).databaseValue.futureValue

      val filter =
        Filter.ByRedirectId(redirect.id, DateTime.now.minusYears(1), DateTime.now, minDurationOpt = Some(32.seconds))
      val actualCalls = callDao.list(filter, Page(0, 10)).databaseValue.futureValue.values
      actualCalls.toSet should ===(Set(call2, call3))
    }

    "list by qualifier" in {
      val phone1 = makeSomePhone()
      val phone2 = makeSomePhone()

      val redirect1 = generateRedirectV2(phone1, target).next
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleFor("id1", RefinedSourceGen.next, redirect1)
      callDao.create(call1).databaseValue.futureValue

      val next: ActualRedirect = generateRedirectV2(phone2, target).next
      val objectId: ObjectId = ObjectId("objectId")
      val redirect2 = next.copy(key = next.key.copy(objectId = objectId))
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleFor("id2", RefinedSourceGen.next, redirect2)
      callDao.create(call2).databaseValue.futureValue

      val filter = Filter.ByObjectId(objectId, DateTime.now.minusYears(1), DateTime.now)
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.toList shouldEqual List(call2)
    }

    "list by source" in {
      val Seq(source1, source2) = RefinedSourceGen.nextUnique(2).toSeq
      val phone1 = makeSomePhone()
      val phone2 = makeSomePhone()

      val redirect1 = generateRedirectV2(phone1, target).next
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleFor("id1", source1, redirect1)
      callDao.create(call1).databaseValue.futureValue

      val next: ActualRedirect = generateRedirectV2(phone2, target).next
      val objectId: ObjectId = ObjectId("objectId")
      val redirect2 = next.copy(key = next.key.copy(objectId = objectId))
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleFor("id2", source2, redirect2)
      callDao.create(call2).databaseValue.futureValue

      val filter = Filter.BySource(source1, DateTime.now.minusYears(1), DateTime.now)
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.toList shouldEqual List(call1)
    }

    "list by objectId and targets" in {

      val redirect1 = generateRedirectV2(makeSomePhone(), target).next
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleFor("id1", RefinedSourceGen.next, redirect1)
      callDao.create(call1).databaseValue.futureValue

      val redirect2 = redirect1.copy(
        id = RedirectIdGen.next,
        key = redirect1.key.copy(target = PhoneGen.next),
        source = makeSomePhone()
      )
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleFor("id2", RefinedSourceGen.next, redirect2)
      callDao.create(call2).databaseValue.futureValue

      val redirect3 = redirect1.copy(
        id = RedirectIdGen.next,
        key = redirect1.key.copy(target = PhoneGen.next),
        source = makeSomePhone()
      )
      redirectDao.create(redirect3).databaseValue.futureValue
      val call3 = sampleFor("id3", RefinedSourceGen.next, redirect3)
      callDao.create(call3).databaseValue.futureValue

      val filter = Filter.ByObjectId(
        redirect1.key.objectId,
        DateTime.now.minusYears(1),
        DateTime.now,
        targetOpt = Some(Set(redirect2.target, redirect3.target))
      )
      val actualCalls = callDao.list(filter, Page(0, 10)).databaseValue.futureValue
      actualCalls.toSet should ===(Set(call2, call3))
    }

    "list by objectId and tag" in {
      val objectId = QualifierGen.next
      val tag1 = TagGen.next
      val tag2 = TagGen.suchThat(_ != tag1).next

      val redirect1 = {
        val r = generateRedirectV2(makeSomePhone(), target).next
        r.copy(key = r.key.copy(objectId = objectId, tag = tag1))
      }
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleFor(ShortStr.next, RefinedSourceGen.next, redirect1)
      callDao.create(call1).databaseValue.futureValue

      val redirect2 = {
        val r = generateRedirectV2(makeSomePhone(), target).next
        r.copy(key = r.key.copy(objectId = objectId, tag = tag2))
      }
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleFor(ShortStr.next, RefinedSourceGen.next, redirect2)
      callDao.create(call2).databaseValue.futureValue

      val filter = Filter.ByTag(objectId, tag1, DateTime.now.minusYears(1), DateTime.now)
      callDao.list(filter, Page(0, 10)).databaseValue.futureValue.toList shouldEqual List(call1)
    }

    "list by objectId and tagPart" in {
      import scala.concurrent.ExecutionContext.Implicits.global
      val objectId = QualifierGen.next
      val tagEmpty = Tag.Empty
      val suffixValue = "offerid=3034178360921083392"
      val fullValue = s"flattype=secondary#owner=yes#$suffixValue"
      val tagFull = Tag.NonEmpty(fullValue)
      val tagPartFull = TagPart(fullValue)
      val tagSuffix = Tag.NonEmpty(suffixValue)
      val tagPartSuffix = TagPart(suffixValue)

      val Seq((_, _), (_, call2), (_, call3)) = FutureUtil
        .traverseSequential(Seq(tagEmpty, tagFull, tagSuffix)) { tag =>
          val redirect = {
            val r = generateRedirectV2(makeSomePhone(), target).next
            r.copy(key = r.key.copy(objectId = objectId, tag = tag))
          }
          val call = sampleFor(ShortStr.next, RefinedSourceGen.next, redirect)
          redirectDao
            .create(redirect)
            .databaseValue
            .flatMap { _ =>
              callDao.create(call).databaseValue
            }
            .map { _ =>
              (redirect, call)
            }
        }
        .futureValue

      Map(
        tagPartFull -> List(call2),
        tagPartSuffix -> List(call2, call3).sortBy(_.time.getMillis).reverse
      ).foreach {
        case (tagPart, calls) =>
          val filter = Filter.ByTagPart(objectId, tagPart, DateTime.now.minusYears(1), DateTime.now)
          callDao.list(filter, Page(0, 10)).databaseValue.futureValue.toList shouldEqual calls
      }
    }

    "skip creation if exists" in {
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      callDao.list(Filter.Empty, Page(0, 10)).databaseValue.futureValue.size shouldEqual 1
    }

    "ignore insert call if exists" in {
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue

      val newCall = call.copy(externalId = "1234")
      callDao.create(newCall).databaseValue.futureValue
      val actual = callDao.list(Filter.Empty, Page(0, 10)).databaseValue.futureValue
      actual.toList shouldEqual List(call)
    }

    "list limit call" in {
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue

      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      val call2 = sampleFor("id2", RefinedSourceGen.next, red)
      callDao.create(call2).databaseValue.futureValue
      val actual = callDao.list(Filter.Empty, 1).databaseValue
      actual.futureValue.size shouldEqual 1
    }

    "call result stats" in {
      numberDao.create(phone).databaseValue.futureValue
      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue
      val source = RefinedSourceGen.next
      val call1 = sampleFor(ShortStr.next, source, redirect).copy(callResult = CallResults.Unknown)
      val call2 = sampleFor(ShortStr.next, source, redirect).copy(callResult = CallResults.Success)
      val call3 = sampleFor(ShortStr.next, source, redirect).copy(callResult = CallResults.Success)
      val call4 = sampleFor(ShortStr.next, source, redirect).copy(callResult = CallResults.NoAnswer)
      val fs = Seq(call1, call2, call3, call4).map(callDao.create(_).databaseValue)
      Future.sequence(fs).futureValue
      val stats = callDao.callResultStats(source, redirect.objectId, redirect.target).databaseValue.futureValue
      stats should have size 3
      val expectedSet =
        Set((CallResults.Unknown, 1), (CallResults.Success, 2), (CallResults.NoAnswer, 1))
      stats.toSet should ===(expectedSet)
    }

    "count calls by days by redirect-id" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue

      val currentDate: DateTime = DateTime.now()
      val call = sampleWithTimeFor("id", RefinedSourceGen.next, red, currentDate)

      val filter: Filter =
        Filter.ByRedirectId(red.id, currentDate.minusDays(1), currentDate.plusDays(1))

      callDao.create(call).databaseValue.futureValue
      callDao.callsByDay(filter).databaseValue.futureValue should ===(
        Iterable(CallsCountByDay(currentDate.toLocalDate, 1))
      )
    }

    "count calls in several dates  by redirect-id" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue

      val today: DateTime = DateTime.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)

      val call1 = sampleWithTimeFor("id1", RefinedSourceGen.next, red, today)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleWithTimeFor("id2", RefinedSourceGen.next, red, yesterday)
      callDao.create(call2).databaseValue.futureValue

      val call3 = sampleWithTimeFor("id3", RefinedSourceGen.next, red, today)
      callDao.create(call3).databaseValue.futureValue

      val call4 = sampleWithTimeFor("id4", RefinedSourceGen.next, red, tomorrow)
      callDao.create(call4).databaseValue.futureValue

      val expectedResponse1: List[CallsCountByDay] = List(
        CallsCountByDay(yesterday.toLocalDate, 1),
        CallsCountByDay(today.toLocalDate, 2),
        CallsCountByDay(tomorrow.toLocalDate, 1)
      )

      val expectedResponse2: List[CallsCountByDay] =
        List(CallsCountByDay(yesterday.toLocalDate, 1), CallsCountByDay(today.toLocalDate, 2))

      val filter: Filter =
        Filter.ByRedirectId(red.id, today.minusDays(1), today.plusDays(2))

      val trimmedFilter: Filter =
        Filter.ByRedirectId(red.id, today.minusDays(1), today.plusHours(1))

      callDao.callsByDay(filter).databaseValue.futureValue.toList shouldEqual expectedResponse1
      callDao.callsByDay(trimmedFilter).databaseValue.futureValue.toList shouldEqual
        expectedResponse2
    }

    "count calls not sorted by date by redirect-id" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue

      val today: DateTime = DateTime.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val longTimeAgo = today.minusDays(239)
      val afterLongTime = today.plusDays(239)

      val call1 = sampleWithTimeFor("id1", RefinedSourceGen.next, red, tomorrow)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleWithTimeFor("id2", RefinedSourceGen.next, red, today)
      callDao.create(call2).databaseValue.futureValue

      val call3 = sampleWithTimeFor("id3", RefinedSourceGen.next, red, yesterday)
      callDao.create(call3).databaseValue.futureValue

      val call4 = sampleWithTimeFor("id4", RefinedSourceGen.next, red, today)
      callDao.create(call4).databaseValue.futureValue

      val call5 = sampleWithTimeFor("id5", RefinedSourceGen.next, red, afterLongTime)
      callDao.create(call5).databaseValue.futureValue

      val call6 = sampleWithTimeFor("id6", RefinedSourceGen.next, red, afterLongTime)
      callDao.create(call6).databaseValue.futureValue

      val call7 = sampleWithTimeFor("id7", RefinedSourceGen.next, red, longTimeAgo)
      callDao.create(call7).databaseValue.futureValue

      val call8 = sampleWithTimeFor("id8", RefinedSourceGen.next, red, today)
      callDao.create(call8).databaseValue.futureValue

      val filter: Filter =
        Filter.ByRedirectId(red.id, today.minusDays(1), today.plusDays(2))

      callDao.callsByDay(filter).databaseValue.futureValue.toArray shouldBe sorted
    }

    "count calls by day by redirect id and call_results" in {
      numberDao.create(phone).databaseValue.futureValue
      val today: DateTime = DateTime.now()
      val redirect = generateRedirectV2(phone, target).next
      redirectDao.create(redirect).databaseValue.futureValue

      val call1 = sampleWithTimeFor("id1", RefinedSourceGen.next, redirect, today)
        .copy(callResult = CallResults.Success)
      callDao.create(call1).databaseValue.futureValue

      val call2 = sampleWithTimeFor("id2", RefinedSourceGen.next, redirect, today)
        .copy(callResult = CallResults.NoAnswer)
      callDao.create(call2).databaseValue.futureValue

      val call3 = sampleWithTimeFor("id3", RefinedSourceGen.next, redirect, today)
        .copy(callResult = CallResults.Error)
      callDao.create(call3).databaseValue.futureValue

      val filter =
        Filter.ByRedirectId(
          redirect.id,
          DateTime.now.minusYears(1),
          today.plusHours(1),
          callResultOpt = Some(Set(CallResults.Success, CallResults.NoAnswer))
        )
      callDao.callsByDay(filter).databaseValue.futureValue.size shouldEqual 1
      callDao.callsByDay(filter).databaseValue.futureValue.head.count shouldEqual 2
    }

    "count calls by day by qualifier" in {
      val today: DateTime = DateTime.now()
      val redirect1 = generateRedirectV2(makeSomePhone(), target).next
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleWithTimeFor("id1", RefinedSourceGen.next, redirect1, today)
      callDao.create(call1).databaseValue.futureValue

      val next: ActualRedirect = generateRedirectV2(makeSomePhone(), target).next
      val objectId: ObjectId = ObjectId("objectId")
      val redirect2 = next.copy(key = next.key.copy(objectId = objectId))
      redirectDao.create(redirect2).databaseValue.futureValue

      val call2 = sampleWithTimeFor("id2", RefinedSourceGen.next, redirect2, today)
      callDao.create(call2).databaseValue.futureValue

      val filter = Filter.ByObjectId(objectId, today.minusYears(1), today.plusDays(1))
      callDao.callsByDay(filter).databaseValue.futureValue.toList shouldEqual
        List(CallsCountByDay(today.toLocalDate, 1))
    }

    "count calls by day by objectId and targets" in {
      val today: DateTime = DateTime.now()

      val redirect1 = generateRedirectV2(makeSomePhone(), target).next
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleWithTimeFor("id1", RefinedSourceGen.next, redirect1, today)
      callDao.create(call1).databaseValue.futureValue

      val redirect2 = redirect1.copy(
        id = RedirectIdGen.next,
        key = redirect1.key.copy(target = PhoneGen.next),
        source = makeSomePhone()
      )
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleWithTimeFor("id2", RefinedSourceGen.next, redirect2, today.plusDays(1))
      callDao.create(call2).databaseValue.futureValue

      val redirect3 = redirect1.copy(
        id = RedirectIdGen.next,
        key = redirect1.key.copy(target = PhoneGen.next),
        source = makeSomePhone()
      )
      redirectDao.create(redirect3).databaseValue.futureValue
      val call3 = sampleWithTimeFor("id3", RefinedSourceGen.next, redirect3, today)
      callDao.create(call3).databaseValue.futureValue

      val filter = Filter.ByObjectId(
        redirect1.key.objectId,
        DateTime.now.minusYears(1),
        DateTime.now.plusDays(2),
        targetOpt = Some(Set(redirect2.target, redirect3.target))
      )
      val actualCalls = callDao.callsByDay(filter).databaseValue.futureValue
      actualCalls shouldEqual List(
        CallsCountByDay(today.toLocalDate, 1),
        CallsCountByDay(today.plusDays(1).toLocalDate, 1)
      )
    }

    "count calls by day by objectId and tag" in {
      val today: DateTime = DateTime.now()
      val objectId = QualifierGen.next
      val tag1 = TagGen.next
      val tag2 = TagGen.suchThat(_ != tag1).next

      val redirect1 = {
        val r = generateRedirectV2(makeSomePhone(), target).next
        r.copy(key = r.key.copy(objectId = objectId, tag = tag1))
      }
      redirectDao.create(redirect1).databaseValue.futureValue
      val call1 = sampleWithTimeFor(ShortStr.next, RefinedSourceGen.next, redirect1, today)
      callDao.create(call1).databaseValue.futureValue

      val redirect2 = {
        val r = generateRedirectV2(makeSomePhone(), target).next
        r.copy(key = r.key.copy(objectId = objectId, tag = tag2))
      }
      redirectDao.create(redirect2).databaseValue.futureValue
      val call2 = sampleWithTimeFor(ShortStr.next, RefinedSourceGen.next, redirect2, today.plusDays(1))
      callDao.create(call2).databaseValue.futureValue

      val filter = Filter.ByTag(objectId, tag1, DateTime.now.minusYears(1), DateTime.now.plusDays(2))
      callDao.callsByDay(filter).databaseValue.futureValue.toList shouldEqual
        List(CallsCountByDay(today.toLocalDate, 1))
    }

    "count calls by day by objectId and tagPart" in {
      import scala.concurrent.ExecutionContext.Implicits.global
      val objectId = QualifierGen.next
      val tagEmpty = Tag.Empty
      val suffixValue = "offerid=3034178360921083392"
      val fullValue = s"flattype=secondary#owner=yes#$suffixValue"
      val tagFull = Tag.NonEmpty(fullValue)
      val tagPartFull = TagPart(fullValue)
      val tagSuffix = Tag.NonEmpty(suffixValue)
      val tagPartSuffix = TagPart(suffixValue)
      val today: DateTime = DateTime.now()

      val tagDateSeq = Seq(
        (tagEmpty, today),
        (tagFull, today.plusDays(1)),
        (tagSuffix, today.plusDays(2))
      )

      FutureUtil
        .traverseSequential(tagDateSeq) { v =>
          val redirect = {
            val r = generateRedirectV2(makeSomePhone(), target).next
            r.copy(key = r.key.copy(objectId = objectId, tag = v._1))
          }
          val call = sampleWithTimeFor(ShortStr.next, RefinedSourceGen.next, redirect, v._2)
          redirectDao.create(redirect).databaseValue.flatMap { _ =>
            callDao.create(call).databaseValue
          }
        }
        .futureValue

      Map(
        tagPartFull -> List(CallsCountByDay(today.plusDays(1).toLocalDate, 1)),
        tagPartSuffix -> List(
          CallsCountByDay(today.plusDays(1).toLocalDate, 1),
          CallsCountByDay(today.plusDays(2).toLocalDate, 1)
        )
      ).foreach {
        case (tagPart, calls) =>
          val filter = Filter.ByTagPart(objectId, tagPart, DateTime.now.minusYears(1), DateTime.now.plusDays(3))
          callDao.callsByDay(filter).databaseValue.futureValue.toList shouldEqual calls
      }
    }

    "get by id" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val call = sampleFor("id", RefinedSourceGen.next, red)
      callDao.create(call).databaseValue.futureValue
      callDao.get(call.id).databaseValue.futureValue should ===(Some(call))
    }

    "list updated" in {
      numberDao.create(phone).databaseValue.futureValue

      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue

      val call1 = sampleFor("id1", RefinedSourceGen.next, red)
      callDao.create(call1).databaseValue.futureValue
      val call2 = sampleFor("id2", RefinedSourceGen.next, red).copy(updateTime = call1.updateTime.plus(1.hour))
      callDao.create(call2).databaseValue.futureValue
      val call3 = sampleFor("id3", RefinedSourceGen.next, red).copy(updateTime = call2.updateTime)
      callDao.create(call3).databaseValue.futureValue

      val response1 =
        callDao.listUpdated(call1.updateTime.minus(1.hour), nextTo = "any", limit = 2).databaseValue.futureValue
      (response1 should contain).theSameElementsInOrderAs(Seq(call1, call2))

      val response2 = callDao.listUpdated(call2.updateTime, nextTo = call2.id, limit = 10).databaseValue.futureValue
      response2 should contain theSameElementsAs Seq(call3)
    }

    "update call" in {
      numberDao.create(phone).databaseValue.futureValue
      val red = generateRedirectV2(phone, target).next
      redirectDao.create(red).databaseValue.futureValue
      val someCall = sampleFor("foo", RefinedSourceGen.next, red)
      callDao.create(someCall).databaseValue.futureValue

      val update = Update.from(RawCallGen.next)
      callDao.update(someCall.id, update).databaseValue.futureValue

      val actual = callDao.get(someCall.id).databaseValue.futureValue.get
      actual.callResult shouldBe update.callResult
      actual.fallbackCall shouldBe update.fallbackCall
      actual.talkDuration shouldBe update.talkDuration
    }
  }

  "CallDao.findCallToNotFallback" should {
    "not fail when nothing to return" in {
      callDao
        .findCallToNotFallback(RefinedSourceGen.next, QualifierGen.next, DateTimeGen.next, DateTimeGen.next)
        .databaseValue
        .futureValue shouldBe None
    }

    "find success call" in {
      val redirect = makeSomeRedirect()
      val call = CallV2Gen
        .suchThat(_.source.isDefined)
        .next
        .copy(
          callResult = CallResults.Success,
          fallbackCall = None,
          redirect = redirect.asHistoryRedirect,
          time = DateTime.now().minusHours(1)
        )
      callDao.create(call).databaseValue.futureValue

      val result1 = callDao
        .findCallToNotFallback(call.source.get, redirect.objectId, DateTime.now().minusHours(2), DateTimeGen.next)
        .databaseValue
        .futureValue

      result1 shouldBe defined
      result1.get.id shouldBe call.id

      val result2 = callDao
        .findCallToNotFallback(call.source.get, redirect.objectId, DateTime.now().minusMinutes(2), DateTimeGen.next)
        .databaseValue
        .futureValue

      result2 should not be defined
    }

    "find fallback call" in {
      val redirect = makeSomeRedirect()
      val call = CallV2Gen
        .suchThat(_.source.isDefined)
        .next
        .copy(
          fallbackCall = Some(FallbackCallGen.next),
          redirect = redirect.asHistoryRedirect,
          time = DateTime.now().minusHours(1)
        )
      callDao.create(call).databaseValue.futureValue

      val result = callDao
        .findCallToNotFallback(call.source.get, redirect.objectId, DateTimeGen.next, DateTime.now().minusHours(2))
        .databaseValue
        .futureValue

      result shouldBe defined
      result.get.id shouldBe call.id
    }

  }

  def makeSomeRedirect(): ActualRedirect = {
    val someNumber = forNumber(PhoneGen.next)
    numberDao.create(phone).databaseValue.futureValue
    val redirect = generateRedirectV2(phone, PhoneGen.next).next
    redirectDao.create(redirect).databaseValue.futureValue
    redirect
  }

  def makeSomePhone(): OperatorNumber = {
    val phone = forNumber(PhoneGen.next)
    numberDao.create(phone).databaseValue.futureValue
    phone
  }
}
