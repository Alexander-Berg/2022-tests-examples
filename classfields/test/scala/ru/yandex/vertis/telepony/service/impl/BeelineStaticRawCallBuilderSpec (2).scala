package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.BeelineGenerator._
import ru.yandex.vertis.telepony.model.CallResults
import ru.yandex.vertis.telepony.service.impl.beeline.BeelineStaticRawCallBuilderImpl

import concurrent.duration._

class BeelineStaticRawCallBuilderSpec extends SpecBase with ScalaCheckPropertyChecks {

  private val callBuilder = new BeelineStaticRawCallBuilderImpl(isCallRecording = true)
  private val callBuilderNoRecords = new BeelineStaticRawCallBuilderImpl(isCallRecording = false)

  private val AllBuilders = Gen.oneOf(callBuilder, callBuilderNoRecords)

  "BeelineStaticRawCallBuilder" should {
    "build call with record" in {
      forAll(
        CallHistoryEntryGen
          .suchThat(e =>
            e.talkDuration > 0.seconds &&
              e.proxyNumber.isDefined &&
              e.recordUrl.isDefined
          )
      ) { entry =>
        val callOpt = callBuilder.build(entry)
        callOpt shouldBe defined
        val call = callOpt.get
        call.recUrl shouldBe defined
        call.callResult should ===(CallResults.Success)
      }
    }
    "build call without record" in {
      forAll(CallHistoryEntryGen.suchThat(e => e.proxyNumber.isDefined), AllBuilders) { (e, cb) =>
        val emptyTalkEntry = e.copy(talkDuration = 0.seconds)
        val callOpt = cb.build(emptyTalkEntry)
        callOpt shouldBe defined
        val call = callOpt.get
        call.recUrl should not be defined
        call.callResult should !==(CallResults.Success)
      }
    }
    "build call with no answer by code 19" in {
      forAll(CallHistoryEntryGen.suchThat(e => e.proxyNumber.isDefined), AllBuilders) { (e, cb) =>
        val entry = e.copy(talkDuration = 0.seconds, calleeReleaseCode = 19)
        val callOpt = cb.build(entry)
        callOpt shouldBe defined
        val call = callOpt.get
        call.recUrl should not be defined
        call.callResult should ===(CallResults.NoAnswer)
      }
    }
    "build call with no answer by codes 0" ignore {
      //Not actual. See https://st.yandex-team.ru/TELEPONY-1582#5f9bcd31c9b8b244791ba9fe
      forAll(CallHistoryEntryGen.suchThat(e => e.proxyNumber.isDefined), AllBuilders) { (e, cb) =>
        val entry = e.copy(
          talkDuration = 0.seconds,
          calleeReleaseCode = 0,
          callerReleaseCode = 0,
          calleeExtendedReleaseCode = 0,
          callerExtendedReleaseCode = 0
        )
        val callOpt = cb.build(entry)
        callOpt shouldBe defined
        val call = callOpt.get
        call.recUrl should not be defined
        call.callResult should ===(CallResults.NoAnswer)
      }
    }
  }

}
