package ru.yandex.vertis.telepony.util.records.impl

import org.scalatest.Ignore
import ru.yandex.vertis.telepony.client.records.RecordsBaseSpec
import ru.yandex.vertis.telepony.model.CallRecord
import ru.yandex.vertis.telepony.util.Threads

@Ignore
class AudioConverterImplSpec extends RecordsBaseSpec {

  val fileSystemExecutor = new FileSystemExecutorImpl(Threads.blockingEc)
  val audioConverter = new AudioConverterImpl(fileSystemExecutor)

  "FfmpegConverter" should {
    "convert to ogg opus" in {
      val convertedBytes = audioConverter.convertToOggOpus(beeline_new_record, "beeline_record").futureValue
      convertedBytes should not be empty
    }

    "merge call records" in {
      val leftRecord = CallRecord("left.mp3", mts_old_record)
      val rightRecord = CallRecord("right.mp3", mts_old_record)
      val mergedCallRecord = audioConverter.mergeCallRecords(leftRecord, rightRecord).futureValue
      mergedCallRecord.bytes should not be empty
    }

    "unify record format" in {
      val callRecord = CallRecord("beeline_record", beeline_new_record)
      val convertedBytes = audioConverter.unifyRecord(callRecord).futureValue
      convertedBytes.bytes should not be empty
    }
  }

}
