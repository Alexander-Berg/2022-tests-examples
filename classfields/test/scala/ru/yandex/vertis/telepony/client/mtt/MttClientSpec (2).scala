package ru.yandex.vertis.telepony.client.mtt

import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.{Phone, RefinedSource}
import ru.yandex.vertis.telepony.model.mtt.FollowMeStruct.KeepOriginalCliValue
import ru.yandex.vertis.telepony.model.mtt.{CallHistoryByAgentResponse, CallHistoryEntryGroup, FilePrompt, FollowMeStruct, H323ConfId, SipId, TextToSpeechPrompt}
import ru.yandex.vertis.telepony.service.MttClient
import ru.yandex.vertis.telepony.service.MttPhoneClient.{ASC, CallTypes, TimeInterval}

import scala.concurrent.duration._

/**
  * @author neron
  */
trait MttClientSpec extends SpecBase with SimpleLogging with ScalaCheckPropertyChecks {

  def client: MttClient

  private val filePrompt = FilePrompt(
    name = "unit_test.mp3",
    bytes = IOUtils.toByteArray(
      getClass.getClassLoader.getResourceAsStream("mtt/mtt_autoru_fomenko.mp3")
    )
  )

  private val badSipId = SipId("74996490165")

  private lazy val allSipIds = Seq(SipId("74996481803"))

  private val followMeStructGen: Gen[FollowMeStruct] = for {
    accountId <- Gen.option(ShortStr)
    timeout <- Gen.choose(1, 60).map(_.seconds)
    redirectNumber <- PhoneGen
    name <- ShortStr
    active <- BooleanGen
    period <- Gen.const("always")
    periodDescription <- Gen.option(ShortStr)
    followOrder <- Gen.option(Gen.choose(1, 10))
    domain <- Gen.option(ShortStr)
    useTcp <- Gen.option(BooleanGen)
    keepOriginalCli <- Gen.option(Gen.oneOf(KeepOriginalCliValue.values.toSeq))
    keepOriginalCld <- Gen.option(BooleanGen)
  } yield FollowMeStruct(
    accountId,
    timeout,
    redirectNumber,
    name,
    active,
    period,
    periodDescription,
    followOrder,
    domain,
    useTcp,
    keepOriginalCli,
    keepOriginalCld
  )

  private lazy val existingSipIdGen: Gen[SipId] = Gen.oneOf(allSipIds)

  "MttClient" should {
    "not provide FollowMe structs for unknown sip id" in {
      client.getFollowMeStructs(badSipId).failed.futureValue
    }
    "provide FollowMe structs for exist sip id" in {
      existingSipIdGen.next
    }
    "update FollowMe structs" ignore {
      forAll(existingSipIdGen, followMeStructGen) { (sipId, followMeStruct) =>
        client.setFollowMeStructs(sipId, Seq(followMeStruct)).futureValue
        val actual = client.getFollowMeStructs(sipId).futureValue
        actual should have size 1
        actual.foreach { actualFollowMeStruct =>
          // some fields do not affect when set
          val actualFMS = actualFollowMeStruct.copy(
            accountId = followMeStruct.accountId,
            useTcp = followMeStruct.useTcp,
            keepOriginalCli = followMeStruct.keepOriginalCli,
            keepOriginalCld = followMeStruct.keepOriginalCld
          )
          // followOrder automatically sets to 1
          actualFMS shouldEqual followMeStruct.copy(followOrder = Some(1))
        }
      }
    }

    /**
      * evidence for MttOperatorClient.updateRedirect works
      */
    "update only redirect number" ignore {
      forAll(existingSipIdGen, PhoneGen, followMeStructGen) { (sourceSipId, targetPhone, followMeStruct) =>
        client.setFollowMeStructs(sourceSipId, Seq(followMeStruct)).futureValue
        val struct = client.getFollowMeStructs(sourceSipId).futureValue.head
        val structWithNewTarget = struct.copy(redirectNumber = targetPhone)
        client.setFollowMeStructs(sourceSipId, Seq(structWithNewTarget)).futureValue

        val actualStruct = client.getFollowMeStructs(sourceSipId).futureValue.head
        actualStruct shouldEqual structWithNewTarget
      }
    }

    /**
      * possible way to remove redirects:
      * always keep followMeStruct for each number, if active == false then removed.
      * Another way: update with Seq() some number,
      * but HttpResponse will be:
      * 404 Not Found: {"jsonrpc":"2.0","id":"XE_6LdV9ee4","error":{"code":-32001,"message":"Data not found","data":"No followMe on this account"}}
      */
    "remove FollowMe struct" ignore {
      val sipId = existingSipIdGen.next
      val followMeStruct = followMeStructGen.next
      client.setFollowMeStructs(sipId, Seq(followMeStruct)).futureValue
      client.setFollowMeStructs(sipId, Seq()).futureValue
      client.getFollowMeStructs(sipId).futureValue shouldBe empty
    }

    "get call record" ignore {
      val h323ConfId = H323ConfId("CFC321D3 6879755C 5B9F6E32 FA49F0B7")
      val recordOpt = client.getCallRecord(h323ConfId).futureValue
      inside(recordOpt) {
        case Some(record) =>
          println(record.bytes.length)
      }
    }

    "get all hist" in {
      val from = DateTime.parse("2020-05-12T19:25:00+03:00")
      val to = DateTime.parse("2020-05-12T19:40:00+03:00")
      val h = getAllHist(from, to)
      h.all.foreach(println)

      val groups = CallHistoryEntryGroup.linkEntries(h.all)
      groups.foreach(println)

      val rawCalls = groups.flatMap(_.toMtt2Group).map(_.toRawCall)
      rawCalls.foreach(println)
    }

    "add GBL rule" in {
      val source = RefinedSource.from("79817757575")
      client.addGBLRule(source).futureValue
    }

    "delete GBL rule" in {
      val source = RefinedSource.from("79817757575")
      client.deleteGBLRule(source).futureValue
    }

    "get GBL rules" in {
      val rules = client.getGBLRules.futureValue
      println(rules)
    }

    "get customer accounts" in {
      val sipIds = client.getCustomerAccountsShort.futureValue
      println(sipIds)
    }

    "update SIP control url" in {
      val sipId = existingSipIdGen.next
      client.setSipCallControlURL(sipId).futureValue
    }

    "setSipReserveNumber" in {
      val sipId = existingSipIdGen.next
      client.setSipReserveNumber(sipId, Some(Phone("+78004445555"))).futureValue
    }

    "createCustomerPrompt file" in {
      client.createCustomerPrompt(filePrompt).futureValue
    }

    "createCustomerPrompt text" in {
      val textToSpeechPrompt = TextToSpeechPrompt("unit_test_tts.mp3", "Привет")
      client.createCustomerPrompt(textToSpeechPrompt).futureValue
    }

    "getCustomerPrompts" in {
      client.getCustomerPrompts.futureValue
    }

    "deleteCustomerPrompt" in {
      client.deleteCustomerPrompt(filePrompt.name).futureValue
    }

  }

  private def getAllHist(from: DateTime, to: DateTime): CallHistoryByAgentResponse = {
    val timeInterval = TimeInterval(Some(from), Some(to))
    client
      .getServiceHistoryByCustomer(
        callType = CallTypes.All,
        timeInterval = timeInterval,
        order = Some(ASC)
      )
      .futureValue
  }

}
