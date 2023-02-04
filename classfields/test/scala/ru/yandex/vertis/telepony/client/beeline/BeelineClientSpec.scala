package ru.yandex.vertis.telepony.client.beeline

import akka.actor.Scheduler
import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.exception.BeelineException
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.Phone
import ru.yandex.vertis.telepony.model.beeline.{AudioFile, CDRRequestId, CDRRequestStatus, NumberSet, PreMediaSetting, Redirection, UpdatePreMediaCommand}
import ru.yandex.vertis.telepony.service.BeelineClient

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author neron
  */
trait BeelineClientSpec extends SpecBase with SimpleLogging with ScalaCheckPropertyChecks {

  def client: BeelineClient

  def scheduler: Scheduler

  private val audioFile = AudioFile(
    bytes = IOUtils.toByteArray(
      getClass.getClassLoader.getResourceAsStream("beeline/beeline_autoru_fomenko.wav")
    ),
    name = "beeline_autoru_fomenko.wav",
    comment = "Здравствуйте, вам звонят по объявлению на auto.ru"
  )

  private val testAudioFile = AudioFile(
    bytes = IOUtils.toByteArray(
      getClass.getClassLoader.getResourceAsStream("beeline/beeline_autoru_fomenko.wav")
    ),
    name = "unit_test.wav",
    comment = "Файл используется в юнит тестах"
  )

  private val beelineNumber = Phone("+79647624408")
  private val oneNumberSet = NumberSet.Few(Iterable(beelineNumber))

  "BeelineClient" should {

    "return redirects" in {
      client.getRedirect(beelineNumber).futureValue
    }

    "set redirect" in {
      val target = Phone("+78004445555")
      val setRedirection = Redirection.default(beelineNumber, target)
      client.setRedirects(Iterable(setRedirection)).futureValue
      val redirection = client.getRedirect(beelineNumber).futureValue
      redirection should ===(Some(setRedirection))
    }

    "cancel redirect" in {
      val target = Phone("+78004445555")
      val setRedirection = Redirection.default(beelineNumber, target)
      client.setRedirects(Iterable(setRedirection)).futureValue
      client.cancelRedirects(oneNumberSet).futureValue
      val redirection = client.getRedirect(beelineNumber).futureValue
      redirection should ===(None)
    }

    "list pre media" in {
      client.getPreMedia(beelineNumber).futureValue
    }

    "update pre media" in {
      import UpdatePreMediaCommand._
      def createCommand(fileName: String): Command = if (fileName.isEmpty) Delete else Set(fileName)
      val preMediaFilesGen = Gen.oneOf("", audioFile.name)
      val atLeastOneFileGen = for {
        f1 <- preMediaFilesGen
        f2 <- preMediaFilesGen.suchThat(f => f.nonEmpty || f1.nonEmpty)
      } yield (f1, f2)
      forAll(atLeastOneFileGen) {
        case (fileA, fileB) =>
          val settings = PreMediaSetting(fileA, fileB)
          val command = Map(
            beelineNumber -> UpdatePreMediaCommand(
              caller = createCommand(fileA),
              callee = createCommand(fileB)
            )
          )
          client.updatePreMedia(command).futureValue
          val actual = client.getPreMedia(beelineNumber).futureValue
          actual should ===(settings)
      }
    }

    "remove pre media" in {
      import UpdatePreMediaCommand._
      val command = UpdatePreMediaCommand(caller = Delete, callee = Delete)
      client.updatePreMedia(Map(beelineNumber -> command)).futureValue
      val settings = client.getPreMedia(beelineNumber).futureValue
      settings shouldBe PreMediaSetting.empty
    }

    "list call recording" in {
      client.listCallRecording().futureValue
    }

    // TODO: neron. Ask beeline
    "enable call recording on all numbers" ignore {
      client.enableCallRecording(NumberSet.All).futureValue
      val numberSet = client.listCallRecording().futureValue
      numberSet should ===(NumberSet.All)
    }

    // TODO: neron. Ask beeline
    "enable call recording" ignore {
      client.disableCallRecording(NumberSet.All).futureValue
      client.enableCallRecording(oneNumberSet).futureValue
      val numberSet = client.listCallRecording().futureValue
      numberSet should ===(oneNumberSet)
    }

    // TODO: neron. Ask beeline
    "disable call recording on all numbers" ignore {
      client.disableCallRecording(NumberSet.All).futureValue
      val numberSet = client.listCallRecording().futureValue
      numberSet should ===(NumberSet.Few(Iterable.empty))
    }

    // TODO: neron. Ask beeline
    "disable call recording" ignore {
      client.disableCallRecording(oneNumberSet).futureValue
      val numberSet = client.listCallRecording().futureValue
      numberSet match {
        case NumberSet.Few(numbers) => numbers should not contain beelineNumber
        case NumberSet.All => fail("Got AllNumbers, expected FewNumbers")
      }
    }

    "request CDR" in {
      val from = DateTime.now().minusYears(1)
      val to = DateTime.now()
      client.requestCallDetailRecords(from, to).futureValue
    }

    "get CDR request status" in {
      val from = DateTime.now().minusYears(1)
      val to = DateTime.now()
      val cdrRequestId = client.requestCallDetailRecords(from, to).futureValue
      val status = client.getCDRRequestStatus(cdrRequestId).futureValue
      status should ===(
        CDRRequestStatus(cdrRequestId, CDRRequestStatus.Statuses.NotReady, None)
      )
    }

    "long get call history" ignore {
      val from = DateTime.now().minusYears(1)
      val to = DateTime.now()
      val entries = Await.result(client.getCallHistory(from, to)(scheduler), 5.minutes)
      log.info(s"Loads ${entries.size} entries")
      log.info(s"\n${entries.mkString("\n")}")
    }

    "get missing call history" in {
      val prevRequestId = CDRRequestId("1")
      val th = client.getCDRRequestStatus(prevRequestId).failed.futureValue
      th shouldBe a[BeelineException]
    }

    "get call record" in {
      val url = "https://cloudcc.beeline.ru/api/download/r/119_186770949540979.wav"
      val r = client.getCallRecord(url).futureValue.get
      r.name should !==("")
      r.bytes should not be empty
    }

    "list audio files" in {
      client.listAudioFiles().futureValue
    }

    "load audio file" in {
      client.loadAudioFile(testAudioFile).futureValue
      val files = client.listAudioFiles().futureValue
      files should not be empty
      val preMediaFile = files.find(_.filename == testAudioFile.name)
      preMediaFile shouldBe defined
    }

    "delete audio files" in {
      val files = client.listAudioFiles().futureValue
      files.find(f => !f.isPending && f.filename == testAudioFile.name).foreach { preMediaFile =>
        client.deleteAudioFiles(Set(testAudioFile.name)).futureValue
        val preMediaFile = files.find(_.filename == testAudioFile.name)
        preMediaFile shouldBe empty
      }
    }

    "fail to get history" in {
      val url = "https://cloudcc.beeline.ru/api/download/s/6185a99_20190326225713"
      val th = client.getCallHistory(url).failed.futureValue
      th shouldBe a[BeelineException]
    }

    "fail to get record" in {
      val url = "https://cloudcc.beeline.ru/api/download/r/118_182475993000000"
      client.getCallRecord(url).failed.futureValue
    }

  }

}
