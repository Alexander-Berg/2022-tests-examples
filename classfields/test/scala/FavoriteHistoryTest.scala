import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.auto.incite.api_model.ApiModel.FavoriteHistory
import ru.auto.incite.api_model.ApiModel.FavoriteHistory.{FavoriteHistoryRecord, StatusHistoryRecord}
import ru.yandex.vertis.incite.utils.Protobuf.{RichDateTime, RichTimestamp}

import scala.collection.JavaConverters._

class FavoriteHistoryTest extends WordSpec {

  "test" when {
    "get latest tag" in {
      val favoriteHistory = FavoriteHistory.newBuilder()

      val date2 = new DateTime().minusDays(1)
      val statusHistoryRecord2 = StatusHistoryRecord.newBuilder().setTimestamp(date2.toProtobufTimestamp)
      val record2 = FavoriteHistoryRecord
        .newBuilder()
        .setStatusHistoryRecord(statusHistoryRecord2)
        .build()
      favoriteHistory.addFavoriteHistory(record2)

      val date1 = new DateTime().minusDays(2)
      val statusHistoryRecord = StatusHistoryRecord.newBuilder().setTimestamp(date1.toProtobufTimestamp)
      val record = FavoriteHistoryRecord
        .newBuilder()
        .setStatusHistoryRecord(statusHistoryRecord)
        .build()
      favoriteHistory.addFavoriteHistory(record)

      val timestamp = favoriteHistory.getFavoriteHistoryList.asScala
        .filter(_.hasStatusHistoryRecord)
        .map(_.getStatusHistoryRecord.getTimestamp.toDateTime)
        .sortBy(_.getMillis)
        .lastOption

      val res = favoriteHistory.getFavoriteHistoryList.asScala.find(_.hasStatusHistoryRecord) match {
        case Some(history) =>
          Some(history.getStatusHistoryRecord.getTimestamp.toDateTime)
        case _ =>
          None
      }

      assert(timestamp.get == date2)

    }
  }

}
