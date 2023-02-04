package ru.yandex.vertis.billing.dao

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{LoneElement, TryValues}
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CallFactDaoSpec.patch
import ru.yandex.vertis.billing.model_core.Resolution.{Automatic, Manually}
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core.gens.{teleponyCallFactGen, CallComplaintGen, Producer, ResolutionsVectorGen}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.CallsResolutionService._
import ru.yandex.vertis.billing.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.DateTimeAreOrdered
import ru.yandex.vertis.hobo.proto.Model.{PaidCallResolution, Resolution => HoboResolution, SuspiciousCallResolution}

import scala.util.Success

/**
  * Spec on [[CallFactDao]]
  *
  * @author ruslansd
  */
trait CallFactDaoSpec extends AnyWordSpec with Matchers with TryValues with LoneElement with AsyncSpecBase {

  protected def callFactDao: CallFactDao

  private val Calls = teleponyCallFactGen().next(10).toList
  private val Epoch = DateTimeInterval.currentDay.from.getMillis
  private val Head = Calls.head
  private val Header = CallFactHeader(Head)
  private val ManuallyResolution = Manually(Resolution.Statuses.Pass)
  private val AutomaticResolution = Automatic(Resolution.Statuses.Fail)
  private val Vector = ResolutionsVector(Some(ManuallyResolution), Some(AutomaticResolution))
  private val CallsCount = 20

  private val Interval = {
    val start = Calls.minBy(_.timestamp.getMillis).timestamp
    val end = Calls.maxBy(_.timestamp.getMillis).timestamp
    DateTimeInterval(start, end)
  }

  private val SuspiciousCallResolutionGen = for {
    value <- Gen.oneOf(SuspiciousCallResolution.Value.values().toSeq)
  } yield HoboResolution
    .newBuilder()
    .setVersion(1)
    .setSuspiciousCall(
      SuspiciousCallResolution
        .newBuilder()
        .setVersion(1)
        .setComment("Super Suspicious call")
        .setValue(value)
    )
    .build()

  private val PaidCallResolutionGen = for {
    value <- Gen.oneOf(PaidCallResolution.Value.values().toSeq)
  } yield HoboResolution
    .newBuilder()
    .setVersion(1)
    .setPaidCallResolution(
      PaidCallResolution
        .newBuilder()
        .setComment("Super Suspicious call")
        .setValue(value)
    )
    .build()

  private val HoboResolutionGen = {
    Gen.oneOf(SuspiciousCallResolutionGen, PaidCallResolutionGen)
  }

  "CallFactDao" should {
    "upsert calls and get calls" in {
      callFactDao.upsert(Calls) match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }

      callFactDao.getT(Since(Epoch)) match {
        case Success(calls) =>
          calls.map(_.call) should contain theSameElementsAs Calls
          calls.find(_.epoch.getMillis < Epoch) should be(None)
        case other => fail(s"Unexpected $other")
      }
    }

    "get all calls" in {
      callFactDao.getT(All) match {
        case Success(callFacts) =>
          callFacts.map(_.call) should contain theSameElementsAs Calls
        case other => fail(s"Unexpected $other")
      }
    }

    "get last call" in {
      val lastCall = Calls.maxBy(_.timestamp)

      val fact = callFactDao.getT(LastCall).success.value.loneElement
      fact.call shouldBe lastCall
    }

    "get calls by timestamp interval" in {
      val result = callFactDao.getT(InInterval(Interval))

      result.get.map(_.call) should contain theSameElementsAs Calls
      result.get.foreach(call => Interval.contains(call.call.timestamp) should be(true))

    }

    "get calls by id" in {
      val fact = callFactDao.getT(ByIds(Header.identity)).success.value.loneElement
      CallFactHeader(fact.call) should be(Header)
    }

    "get calls by id with Future function" in {
      val fact = callFactDao.get(ByIds(Header.identity)).futureValue.loneElement
      CallFactHeader(fact.call) should be(Header)
    }

    "set status null by default" in {
      val calls = callFactDao.getT(Since(Epoch)).success.value
      calls.find(_.resolutions != ResolutionsVector()) should be(None)
    }

    "set status null by default with Future function" in {
      val calls = callFactDao.get(Since(Epoch)).futureValue
      calls.find(_.resolutions != ResolutionsVector()) should be(None)
    }

    "set status correctly simple test" in {
      callFactDao.update(Header.identity, patch(ManuallyResolution)) should be(Success(()))
      callFactDao.update(Header.identity, patch(AutomaticResolution)) should be(Success(()))

      val result = callFactDao.getT(Since(Epoch))
      result.isSuccess should be(true)

      val facts = result.get.filter(_.call == Head)
      facts.size should be(1)

      facts.head.resolutions should be(Vector)
    }

    "set status correctly" in {
      val factToVector: Map[CallFact, ResolutionsVector] =
        teleponyCallFactGen().next(CallsCount).toList.map(c => c -> ResolutionsVectorGen.next).toMap

      callFactDao.upsert(factToVector.keys) should be(Success(()))

      factToVector.foreach { case (fact, ResolutionsVector(manual, automatic)) =>
        Seq(manual, automatic).flatten.foreach(r => callFactDao.update(fact.id, patch(r)) should be(Success(())))
      }

      callFactDao.getT(Since(Epoch)) match {
        case Success(records) =>
          records.foreach { r =>
            factToVector.get(r.call).foreach { vector =>
              vector should be(r.resolutions)
            }
          }
        case other =>
          fail(s"Unexpected $other")
      }
    }
    "set hobo_resolution correctly" in {
      val factToVector: Map[CallFact, (ResolutionsVector, HoboResolution)] =
        teleponyCallFactGen()
          .next(CallsCount)
          .toList
          .map(c => c -> (ResolutionsVectorGen.next -> HoboResolutionGen.next))
          .toMap

      callFactDao.upsert(factToVector.keys) should be(Success(()))

      factToVector.foreach { case (fact, (ResolutionsVector(manual, automatic), hoboResolution)) =>
        Seq(manual, automatic).flatten.foreach {
          case r @ Manually(_, _) =>
            callFactDao.update(fact.id, patch(r, hoboResolution)) should be(Success(()))
          case r @ Automatic(_) =>
            callFactDao.update(fact.id, patch(r)) should be(Success(()))
        }
      }

      callFactDao.getT(Since(Epoch)) match {
        case Success(records) =>
          records.foreach { r =>
            factToVector.get(r.call).foreach { case (vector, actualHoboResolution) =>
              vector shouldBe r.resolutions
              if (r.resolutions.manually.isDefined) {
                r.hoboResolution shouldBe Some(actualHoboResolution)
              }
            }
          }
        case other =>
          fail(s"Unexpected $other")
      }

    }

    "set complaint correctly" in {
      val factToCallComplaint: Map[CallFact, CallComplaint] =
        teleponyCallFactGen()
          .next(CallsCount)
          .toList
          .map(c => c -> CallComplaintGen.next)
          .toMap

      callFactDao.upsert(factToCallComplaint.keys) should be(Success(()))

      factToCallComplaint.foreach { case (fact, complaint) =>
        callFactDao.update(fact.id, patch(complaint)) should be(Success(()))
      }

      callFactDao.getT(Since(Epoch)) match {
        case Success(records) =>
          records.foreach { r =>
            factToCallComplaint.get(r.call).foreach(complaint => complaint should be(r.complaint.get))
          }
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "set complaint and remove passed manual resolution correctly" in {
      val factToCallComplaint: Map[CallFact, CallComplaint] =
        teleponyCallFactGen()
          .next(CallsCount)
          .toList
          .map(c => c -> CallComplaintGen.next)
          .toMap

      callFactDao.upsert(factToCallComplaint.keys) should be(Success(()))

      factToCallComplaint.foreach { case (fact, _) =>
        callFactDao.update(fact.id, patch(ManuallyResolution)) should be(Success(()))
      }

      factToCallComplaint.foreach { case (fact, complaint) =>
        callFactDao.update(
          fact.id,
          patch(complaint, removeManualResolution = true)
        ) should be(Success(()))
      }

      callFactDao.getT(Since(Epoch)) match {
        case Success(records) =>
          records.foreach { r =>
            factToCallComplaint.get(r.call).foreach { complaint =>
              complaint should be(r.complaint.get)
              r.resolutions match {
                case ResolutionsVector(None, _) =>
                  ()
                case other =>
                  fail(s"Unexpected $other")
              }
            }
          }
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "set call center call id correctly" in {
      val expectedCall = teleponyCallFactGen().next
      val callId = CallFactHeader(expectedCall).identity

      callFactDao.upsert(Seq(expectedCall)) shouldBe Success(())

      val callBeforeUpdate = callFactDao.getT(ByIds(callId)).get
      callBeforeUpdate.head.call shouldBe expectedCall

      val callCenterCallId = "beeper-0"
      val patch = Patch(callCenterCallId = Some(callCenterCallId))
      callFactDao.update(CallFactHeader(expectedCall).identity, patch) shouldBe Success(())

      val calls = callFactDao.getT(ByIds(callId)).get
      calls.size shouldBe 1
      val callAfterUpdate = calls.head
      callAfterUpdate.call shouldBe expectedCall
      callAfterUpdate.callCenterCallId shouldBe Some(callCenterCallId)
    }

    "correctly get calls with filter since with limit" in {
      val exactlyOneCall = callFactDao.getT(SinceWithLimit(Epoch, 1)).get
      exactlyOneCall.size shouldBe 1

      val twoCalls = callFactDao.getT(SinceWithLimit(Epoch, 2)).get
      twoCalls.size shouldBe 2
    }

    "rewrite call with record" in {
      val withoutRecord = teleponyCallFactGen().next.copy(recordId = None)

      callFactDao.upsert(Iterable(withoutRecord)).success.value

      val withoutRecordEC = callFactDao.getT(ByIds(withoutRecord.id)).get.exactlyOne

      val withRecord = withoutRecord.copy(recordId = Some("record_test"))

      callFactDao.upsert(Iterable(withRecord)).success.value

      val withRecordEC = callFactDao.getT(ByIds(withoutRecord.id)).get.exactlyOne

      withRecordEC.epoch should be > withoutRecordEC.epoch

      withRecordEC.call.asInstanceOf[TeleponyCallFact].recordId shouldBe withRecord.recordId
    }

    "rewrite call status" in {
      val unknownStatus = teleponyCallFactGen().next.copy(result = CallResults.Unknown)
      val updatedStatus = teleponyCallFactGen().next.copy(result = CallResults.NoAnswer)

      callFactDao.upsert(Iterable(unknownStatus)).success.value

      val unknownStatusEC = callFactDao.getT(ByIds(unknownStatus.id)).get.exactlyOne

      callFactDao.upsert(Iterable(updatedStatus)).success.value

      val updatedStatusEC = callFactDao.getT(ByIds(updatedStatus.id)).get.exactlyOne

      updatedStatusEC.epoch should be > unknownStatusEC.epoch

      updatedStatusEC.call.asInstanceOf[TeleponyCallFact].result shouldBe updatedStatus.result
    }

  }
}

object CallFactDaoSpec {

  private def patch(resolution: Resolution) = {
    Patch(resolution = Some(resolution))
  }

  private def patch(resolution: Resolution, hoboResolution: HoboResolution) = {
    Patch(resolution = Some(resolution), hoboResolution = Some(hoboResolution))
  }

  private def patch(complaint: CallComplaint, removeManualResolution: Boolean = false) = {
    Patch(callComplaint = Some(complaint), removePassedManualResolution = removeManualResolution)
  }
}
