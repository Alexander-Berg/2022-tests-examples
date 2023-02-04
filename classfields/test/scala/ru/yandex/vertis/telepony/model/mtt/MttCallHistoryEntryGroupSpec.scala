package ru.yandex.vertis.telepony.model.mtt

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.H323ConfIdGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.RawCall.Origins
import ru.yandex.vertis.telepony.model.{CallResults, MttGenerator, Operators, RefinedSource}
import ru.yandex.vertis.telepony.service.MttPhoneClient.CallTypes

import scala.concurrent.duration._

class MttCallHistoryEntryGroupSpec extends SpecBase with ScalaCheckDrivenPropertyChecks {

  val Mtt1GroupGen: Gen[CallHistoryEntryGroup] = for {
    d <- MttGenerator.CallHistoryEntryGen
    f <- MttGenerator.CallHistoryEntryGen
  } yield CallHistoryEntryGroup(
    main = d.copy(callType = CallTypes.Dialed),
    linkedEntries = f.copy(callType = CallTypes.Forwarded, h323IncomingConfId = Some(d.h323ConfId)) :: Nil
  )

  val Mtt2GroupForwardedGen: Gen[Mtt2Group] = for {
    d <- MttGenerator.CallHistoryEntryGen
    f1 <- MttGenerator.CallHistoryEntryGen
    f2 <- MttGenerator.CallHistoryEntryGen
  } yield {
    val forwarded1 = f1.copy(callType = CallTypes.Forwarded, h323IncomingConfId = Some(d.h323ConfId))
    val forwarded2 = f2.copy(callType = CallTypes.Forwarded, h323IncomingConfId = Some(f1.h323ConfId))
    CallHistoryEntryGroup(
      main = d.copy(callType = CallTypes.Dialed),
      linkedEntries = forwarded1 :: forwarded2 :: Nil
    ).toMtt2Group.head
  }

  val Mtt2GroupForwardedWithoutDialedGen: Gen[Mtt2Group] = for {
    h323IncomingConfId <- H323ConfIdGen
    f1 <- MttGenerator.CallHistoryEntryGen
    f2 <- MttGenerator.CallHistoryEntryGen
  } yield {
    val forwarded1 = f1.copy(callType = CallTypes.Forwarded, h323IncomingConfId = Some(h323IncomingConfId))
    val forwarded2 = f2.copy(callType = CallTypes.Forwarded, h323IncomingConfId = Some(f1.h323ConfId))
    CallHistoryEntryGroup(
      main = forwarded1,
      linkedEntries = forwarded2 :: Nil
    ).toMtt2Group.head
  }

  val Mtt2GroupInterruptedGen: Gen[Mtt2Group] = for {
    d <- MttGenerator.CallHistoryEntryGen
    f1 <- MttGenerator.CallHistoryEntryGen
  } yield {
    val forwarded1 =
      f1.copy(callType = CallTypes.Forwarded, h323IncomingConfId = Some(d.h323ConfId), setupTimeMs = 0.seconds)
    CallHistoryEntryGroup(
      main = d.copy(callType = CallTypes.Dialed),
      linkedEntries = forwarded1 :: Nil
    ).toMtt2Group.head
  }

  val Mtt2GroupGen: Gen[Mtt2Group] =
    Gen.oneOf(Mtt2GroupForwardedGen, Mtt2GroupForwardedWithoutDialedGen, Mtt2GroupInterruptedGen)

  "CallHistoryEntryGroup" should {
    "not fail linkage on empty history" in {
      CallHistoryEntryGroup.linkEntries(Nil) shouldBe empty
    }

    "link entries" in {
      val dialed = MttGenerator.CallHistoryEntryGen.next.copy(
        callType = CallTypes.Dialed
      )
      val forwarded = MttGenerator.CallHistoryEntryGen.next.copy(
        callType = CallTypes.Forwarded,
        h323IncomingConfId = Some(dialed.h323ConfId)
      )
      val entries = dialed :: forwarded :: Nil
      val groups = CallHistoryEntryGroup.linkEntries(entries)
      groups should have size 1
    }

    "link forward-only entries" in {

      val someConfId = H323ConfIdGen.next

      val forwarded1 = MttGenerator.CallHistoryEntryGen.next.copy(
        callType = CallTypes.Forwarded,
        h323IncomingConfId = Some(someConfId)
      )

      val forwarded2 = forwarded1.copy(
        h323ConfId = H323ConfIdGen.next,
        connectTime = forwarded1.connectTime.plusSeconds(10),
        h323IncomingConfId = Some(forwarded1.h323ConfId)
      )

      val groups = CallHistoryEntryGroup.linkEntries(Seq(forwarded1, forwarded2))

      groups should have size 1

      val mtt2Group = groups.head.toMtt2Group
      mtt2Group.isDefined should ===(true)
      mtt2Group.get shouldBe a[Mtt2GroupForwarded]
      val mtt2GroupForwarded = mtt2Group.get.asInstanceOf[Mtt2GroupForwarded]
      mtt2GroupForwarded.dialed should ===(None)

      val call = mtt2Group.map(_.toRawCall)
      call.map(_.startTime) should contain(forwarded1.connectTime)
      call.map(_.externalId) should contain(forwarded1.h323ConfId.value)
    }

    // Fields that have the same logic for (Offline) and (Offline + Online) modes
    "build common fields of raw-call" in {
      val gen = Mtt2GroupGen.map(g => g.entries -> g.toRawCall)
      forAll(gen) {
        case (dialedOrForwarded :: _, call) =>
          call.source should ===(Some(RefinedSource(dialedOrForwarded.cli)))
          call.proxy should ===(SipId(dialedOrForwarded.accountId).toPhone)
          call.startTime should ===(dialedOrForwarded.connectTime)
          call.origin should ===(Origins.Offline)
          call.operator should ===(Operators.Mtt)
          call.debugInfo shouldBe empty
      }
    }

    // This test shows the difference between  in building raw call
    "build new mtt call (Online + Offline)" in {
      forAll(Mtt2GroupGen.map(g => g -> g.toRawCall)) {
        case (Mtt2GroupForwarded(Some(d), f1, f2), call) =>
          call.externalId should ===(f1.h323ConfId.value)
          call.source should contain(RefinedSource.from(d.cli))
          call.proxy should ===(SipId(d.accountId).toPhone)
          call.startTime should ===(d.connectTime)
          call.duration should ===(f2.setupTimeMs + f2.usedQuantity)
          call.talkDuration should ===(f2.usedQuantity)
          call.recUrl.isDefined should ===(f1.voiceRecordExist && f2.usedQuantity > 0.seconds)
          call.recUrl.foreach { url =>
            url should ===(f1.h323ConfId.value)
          }
          call.callResult should ===(CallHistoryEntryGroup.callResultBy(f2, f2.usedQuantity))
        case (Mtt2GroupForwarded(None, f1, f2), call) =>
          call.externalId should ===(f1.h323ConfId.value)
          call.source should contain(RefinedSource.from(f1.cli))
          call.proxy should ===(SipId(f1.accountId).toPhone)
          call.startTime should ===(f1.connectTime)
          call.duration should ===(f2.setupTimeMs + f2.usedQuantity)
          call.talkDuration should ===(f2.usedQuantity)
          call.recUrl.isDefined should ===(f1.voiceRecordExist && f2.usedQuantity > 0.seconds)
          call.recUrl.foreach { url =>
            url should ===(f1.h323ConfId.value)
          }
          call.callResult should ===(CallHistoryEntryGroup.callResultBy(f2, f2.usedQuantity))
        case (Mtt2GroupInterrupted(_, f1), call) =>
          call.externalId should ===(f1.h323ConfId.value)
          call.duration should ===(0.seconds)
          call.talkDuration should ===(0.seconds)
          call.recUrl should ===(None)
          call.callResult should ===(CallHistoryEntryGroup.callResultBy(f1, 0.seconds))
      }
    }
  }

}
