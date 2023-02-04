package ru.yandex.vertis.telepony.client.mts

import org.apache.commons.io.IOUtils
import org.joda.time.LocalTime
import org.scalacheck.Gen
import org.scalactic.anyvals.{PosInt, PosZInt}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.mts._
import ru.yandex.vertis.telepony.model.{Credentials, Phone}
import ru.yandex.vertis.telepony.service.MtsClient
import ru.yandex.vertis.telepony.service.MtsClient._
import ru.yandex.vertis.telepony.time.now
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.service.impl.mts.DualMtsClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

/**
  * Base specs on [[MtsClient]].
  *
  * @author dimas
  */
trait MtsClientSpec extends SpecBase with SimpleLogging with ScalaCheckPropertyChecks {

  implicit def propertyCheckConfiguration: PropertyCheckConfiguration = PropertyCheckConfiguration(
    sizeRange = PosZInt(10),
    minSuccessful = PosInt(20)
  )

  private val snd =
    SoundValue(
      "autotel_2_sox.wav",
      IOUtils.toByteArray(
        getClass.getClassLoader.getResourceAsStream("mts/realty/autotel_2_sox.wav")
      )
    )

  private def menuWithName(name: String) =
    MenuValue(s"""<?xml version="1.0" encoding="utf-8"?>
          |<menu_list>
          |    <menu_item>
          |        <id>bba2af46-79af-4846-ba62-8734cb6dda71</id>
          |        <name>$name</name>
          |        <active>0</active>
          |        <type>1</type>
          |        <begin_time>00:00:00</begin_time>
          |        <end_time>23:59:59</end_time>
          |        <days>127</days>
          |        <vxml application="BeginMenu" version="2.0">
          |            <menu id="M_0" dtmf="false">
          |                <prompt timeout="3s">
          |                    <audio src="autotel_2_sox.wav"/>
          |                </prompt>
          |                <choice dtmf="0" next="#M_0_exit_0"/>
          |                <noinput>
          |                    <assign name="exit_reason" expr="continue"/>
          |                    <exit/>
          |                </noinput>
          |            </menu>
          |            <form id="M_0_exit_0">
          |                <block>
          |                    <assign name="exit_reason" expr="exit"/>
          |                    <exit/>
          |                </block>
          |            </form>
          |        </vxml>
          |    </menu_item>
          |</menu_list>""")

  //  private val TestPhone = Phone("+74994290944")
  private lazy val TestUniversalNumber = fetchNumber()

  def client: DualMtsClient

  val CrmGen: Gen[Crm] = for {
    url <- ShortStr
    login <- ShortStr
    password <- ShortStr
    escort <- BooleanGen
  } yield Crm(
    url,
    Credentials(login, password),
    escort,
    callRoutingEnabled = false
  )

  val ConnectionParamsGen: Gen[ConnectionParams] = for {
    runLlo <- BooleanGen
    needAnswerBase <- BooleanGen
  } yield ConnectionParams(runLlo, needAnswerBase && runLlo)

  val TimingParamsGen: Gen[TimingParams] = for {
    makeCallTime <- Gen.chooseNum(10, 50).map(_.seconds)
    maxQueueTime <- Gen.chooseNum(10, 50).map(_.seconds).suchThat(_.gt(makeCallTime))
  } yield TimingParams(makeCallTime, maxQueueTime)

  def dnGen: Gen[DestinationNumber] =
    for {
      phone <- PhoneGen
      begin = LocalTime.MIDNIGHT
      minusMinutes <- Gen.choose(1, 1000)
      end = begin.minusMinutes(minusMinutes)
      day = 0
      priority = 0
      locked = false
      record <- BooleanGen
      stereo <- BooleanGen
      channels <- Gen.oneOf(1, 10)
    } yield DestinationNumber(phone, begin, end, day, priority, locked, record, stereo, channels)

  def updateUniversalNumberGen(phone: Phone): Gen[UpdateRequest] =
    for {
      description <- Gen.option(ShortStr)
      enabled <- Gen.option(BooleanGen)
      timeZone <- Gen.option(Gen.chooseNum(-1, 8))
      needRecord <- Gen.option(BooleanGen)
      sayAni <- Gen.option(BooleanGen)
      needBeginIvr <- Gen.option(BooleanGen)
      dnList <- Gen.option(Gen.oneOf(Gen.listOfN(0, dnGen), Gen.listOfN(1, dnGen)))
      crm <- Gen.option(CrmGen)
      menuAllowed <- Gen.option(BooleanGen)
      needAdvert <- Gen.option(BooleanGen)
      timingParams <- Gen.option(TimingParamsGen)
      connectionParams <- Gen.option(ConnectionParamsGen)
      callerIdMode <- Gen.option(BooleanGen)
    } yield UpdateRequest(
      phone = phone,
      description = None,
//    description,
      enabled = None,
//    enabled,
      timeZone = None,
//    timeZone,
      needRecord = None,
//    needRecord,
      sayAni = None,
//    sayAni,
      needBeginIvr = None,
//    needBeginIvr,
//    None,
      dnList = dnList,
      crm = None,
      // crm, VSINFR-1780
      menuAllowed = None,
//    menuAllowed,
      needAdvert = None,
//    needAdvert, VSINFR-1660
//    None,
      timingParams = timingParams,
      connectionParams = None,
//    connectionParams
      callerIdMode = callerIdMode
    )

  private val MenuValueGen = ShortStr.map(menuWithName)

  def localUpdate(un: UniversalNumber, request: UpdateRequest): UniversalNumber = {
    require(un.phone == request.phone)
    UniversalNumber(
      un.phone,
      request.description.getOrElse(un.description),
      request.enabled.orElse(un.enabled),
      request.timeZone.getOrElse(un.timeZone),
      None,
      request.needRecord.getOrElse(un.needRecord),
      request.sayAni.getOrElse(un.sayAni),
      request.needBeginIvr.getOrElse(un.needBeginIvr),
      request.dnList.getOrElse(un.dnList),
      request.crm.filter(_.callEscortEnabled).orElse(un.crm),
      request.menuAllowed.getOrElse(un.menuAllowed),
      request.needAdvert.getOrElse(un.needAdvert),
      request.timingParams.getOrElse(un.timingParams),
      request.connectionParams.getOrElse(un.connectionParams),
      request.callerIdMode.orElse(un.callerIdMode)
    )
  }

  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(300, Seconds), interval = Span(2000, Milliseconds))

  "MtsClient" should {
    "not provide unknown number info" in {
      client.clientV4.getUniversalNumber(PhoneGen.next).failed.futureValue
    }
    "provide universal numbers" in {
      val phones = client.clientV4.getUniversalNumbers().futureValue
      phones should not be empty
    }

    "provide info about exist universal number" in {
      fetchNumber()
    }

    "update universal number description" in {
      var un = fetchNumber()
      forAll(Gen.const(un), updateUniversalNumberGen(un.phone)) { (_, request) =>
        client.clientV4.updateUniversalNumber(request).futureValue
        val actual = client.clientV4.getUniversalNumber(un.phone).futureValue
//          ignore enabled, updatedOn, timingParams
        val expected = localUpdate(un, request)
          .copy(enabled = un.enabled, updatedOn = actual.updatedOn, timingParams = actual.timingParams)
        Thread.sleep(1000)
        actual shouldEqual expected
        un = actual
      }
    }

//        mts - slowpoke
    "provide call detail records" ignore {
      val un = TestUniversalNumber
      val request = CdrRequest.ByTime(un.phone, now().minusDays(356), now(), 100)
      val records = client.clientV4.getCallDetailRecords(request).futureValue
      records should not be empty
    }

//        mts stores records only for 30 days, so you should provide actual url
    "load record" ignore {
      val url = "https://aa.mts.ru/api/v4/record?phone=8123836301&id=33140689502"
      val recordOpt = client.clientV4.getCallRecord(url).futureValue
      inside(recordOpt) { case Some(record) =>
        record.name shouldEqual "33140689502.mp3"
      }
    }

    "get empty sounds" in {
      val phone = TestUniversalNumber.phone
      clear(phone).futureValue
      client.clientV4.getSounds(phone).futureValue shouldEqual Seq.empty
    }

    "put sound" in {
      val phone = TestUniversalNumber.phone
      val sound = Sound(phone, snd.name)
      clear(phone).futureValue
      client.clientV4.putSound(Sound.UpdateRequest(phone, snd)).futureValue
      val sounds = client.clientV4.getSounds(phone).futureValue
      sounds shouldEqual Seq(sound)
    }

    "delete sound" in {
      val phone = TestUniversalNumber.phone
      val sound = Sound(phone, snd.name)
      clear(phone).futureValue
      client.clientV4.putSound(Sound.UpdateRequest(phone, snd)).futureValue
      client.clientV4.deleteSound(sound).futureValue
      Thread.sleep(1000)
      client.clientV4.getSounds(phone).futureValue shouldEqual Seq.empty
    }

    // doesn't work now
    "menu" ignore {
      forAll(MenuValueGen) { menuValue =>
        val phone = TestUniversalNumber.phone
        clear(phone).futureValue
        val menu = Menu(phone, menuValue)
        client.clientV4.updateMenu(menu).futureValue
        val actual = client.clientV4.getMenu(phone).futureValue
        actual shouldEqual menu
      }
    }

    "set and delete master" in {
      val request = SetMasterRequest(
        phone = TestUniversalNumber.phone,
        master = PhoneGen.next
      )

      client.clientV4.setMaster(request).futureValue
      client.clientV4.deleteMaster(request.phone).futureValue
    }
  }

  private def clear(phone: Phone): Future[Unit] =
    for {
      sounds <- client.clientV4.getSounds(phone)
      _ <- Future.sequence(sounds.map(client.clientV4.deleteSound))
      _ <- client.clientV4.updateMenu(Menu(phone, menuWithName("empty")))
    } yield ()

  private val badNumbers = Set(Phone("+79184349254"))

  private def fetchNumber() = {
    val numberF = for {
      numbers <- client.clientV4.getUniversalNumbers()
      candidate = Random.shuffle(numbers.diff(badNumbers).toList).head
      number <- client.clientV4.getUniversalNumber(candidate)
    } yield number
    numberF.futureValue
  }

}
