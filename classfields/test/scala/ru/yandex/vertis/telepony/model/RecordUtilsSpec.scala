package ru.yandex.vertis.telepony.model

import java.io.ByteArrayInputStream

import javax.sound.sampled.{AudioFormat, AudioSystem}
import org.apache.commons.io.IOUtils
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.util.time.DateTimeUtil

/**
  * @author ruslansd
  */
class RecordUtilsSpec extends SpecBase {

  private val expectedFormat = new AudioFormat(
    AudioFormat.Encoding.ALAW,
    8000f,
    8,
    1,
    1,
    8000f,
    false
  )

  // 37,92 sec, Audio: pcm_alaw ([6][0][0][0] / 0x0006), 8000 Hz, 1 channels, s16, 64 kb/s
  private def record: Record = {
    val bytes = IOUtils.toByteArray(getClass.getResourceAsStream("/records/mts_new_--JICtHKasQ.wav"))
    val meta =
      RecordMeta("test", OperatorAccounts.BillingRealty, "test", None, None, DateTimeUtil.now(), customS3Prefix = None)
    Record(meta, bytes)
  }

  "RecordUtils" should {

    "correctly get first n seconds" in {
      val reducedRecord = RecordUtils.getFirstNSeconds(record.bytes, 10).get
      val audioStream = reducedRecord.stream

      audioStream.getFormat.matches(expectedFormat) shouldBe true
      audioStream.getFrameLength shouldBe expectedFormat.getFrameRate * 10
    }

    "correctly get reduced record" in {
      val reducedRecord = RecordUtils.getFirstNSeconds(record, 10).get

      val audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(reducedRecord.bytes))

      audioStream.getFormat.matches(expectedFormat) shouldBe true
      audioStream.getFrameLength shouldBe expectedFormat.getFrameRate * 10
    }

    "if record shorted return record" in {
      val reducedRecord = RecordUtils.getFirstNSeconds(record, 60).get

      val audioStream = RecordUtils.getFirstNSeconds(record.bytes, 60).get.stream
      audioStream.getFormat.matches(expectedFormat) shouldBe true
      audioStream.getFrameLength shouldBe (expectedFormat.getFrameRate * 37.92).toLong

      val recordBytes = reducedRecord.bytes
      val expectedBytes = record.bytes

      recordBytes.length shouldBe expectedBytes.length
      recordBytes shouldBe expectedBytes
    }

  }

}
