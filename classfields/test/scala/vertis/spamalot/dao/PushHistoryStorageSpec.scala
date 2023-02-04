package vertis.spamalot.dao

import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.dao.model.PushHistoryEntry
import vertis.zio.test.ZioSpecBase
import zio.ZIO

import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

class PushHistoryStorageSpec extends ZioSpecBase with SpamalotYdbTest {
  private lazy val storage = storages.pushHistoryStorage

  private def truncatedInstant = Instant
    .now()
    .truncatedTo(
      ChronoUnit.SECONDS
    ) // this truncation is needed because sometimes ydb looses precision on instants, it does not affect how we work with resource, because we don't need super precise ts

  "PushStorage" should {
    "save operations" in ydbTest {
      val deviceId = "device_id"
      val topic = "topic"
      val ts = truncatedInstant
      val row = PushHistoryEntry(deviceId, topic, ts)
      for {
        _ <- txAutoCommit(storage.save(row))
        savedRows <- txAutoCommit(storage.slice(deviceId, ts.minusSeconds(10)))
        _ <- check("Should contain exact row")(savedRows should be(Seq(row)))
        _ <- txAutoCommit(storage.clean)
      } yield ()
    }
    "filter by topic" in ydbTest {
      val deviceId = "device_id"
      val topic1 = "topic1"
      val topic2 = "topic2"
      val ts = truncatedInstant
      val topic1Entries =
        Seq.iterate(PushHistoryEntry(deviceId, topic1, ts), 5)(entry => entry.copy(ts = entry.ts.plusSeconds(1)))
      val topic2Entries =
        Seq.iterate(PushHistoryEntry(deviceId, topic2, ts), 5)(entry => entry.copy(ts = entry.ts.plusSeconds(1)))
      for {
        _ <- ZIO.foreachPar(topic1Entries ++ topic2Entries)(entry => txAutoCommit(storage.save(entry)))
        slicedEntries <- txAutoCommit(storage.slice(deviceId, ts.minusSeconds(10), Some(topic1)))
        _ <- check("Should only contain topic1 entries")(slicedEntries should contain theSameElementsAs topic1Entries)
        _ <- txAutoCommit(storage.clean)
      } yield ()
    }
    "filter by ts" in ydbTest {
      val deviceId = "device_id"
      val topic = "topic"
      val initTs = truncatedInstant
      val oldEntries = Seq.iterate(PushHistoryEntry(deviceId, topic, initTs.minusSeconds(1)), 5)(entry =>
        entry.copy(ts = entry.ts.minusSeconds(1))
      )
      val newEntries =
        Seq.iterate(PushHistoryEntry(deviceId, topic, initTs), 5)(entry => entry.copy(ts = entry.ts.plusSeconds(1)))
      for {
        _ <- ZIO.foreachPar(oldEntries ++ newEntries)(entry => txAutoCommit(storage.save(entry)))
        slicedEntries <- txAutoCommit(storage.slice(deviceId, initTs))
        _ <- check("Should only contain new entries")(slicedEntries should contain theSameElementsAs newEntries)
        _ <- txAutoCommit(storage.clean)
      } yield ()
    }
    "filter by device_id" in ydbTest {
      val device1Id = "device1"
      val device2Id = "device2"
      val topic = "topic"
      val ts = truncatedInstant
      val device1Entries =
        Seq.iterate(PushHistoryEntry(device1Id, topic, ts), 5)(entry => entry.copy(ts = entry.ts.plusSeconds(1)))
      val device2Entries =
        Seq.iterate(PushHistoryEntry(device2Id, topic, ts), 5)(entry => entry.copy(ts = entry.ts.plusSeconds(1)))
      for {
        _ <- ZIO.foreachPar(device1Entries ++ device2Entries)(entry => txAutoCommit(storage.save(entry)))
        slicedEntries <- txAutoCommit(storage.slice(device1Id, ts.minusSeconds(1), Some(topic)))
        _ <- check("Should only contain device1 entries")(slicedEntries should contain theSameElementsAs device1Entries)
        _ <- txAutoCommit(storage.clean)
      } yield ()
    }
  }
}
