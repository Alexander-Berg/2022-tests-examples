package ru.yandex.vertis.incite.dao

import org.joda.time.DateTime
import org.mockito.Mockito.{doNothing, times, verify, verifyNoMoreInteractions}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, Suite, WordSpec}
import ru.auto.incite.api_model.ApiModel.{FavoriteCounters, FavoriteHistory}
import ru.yandex.vertis.incite.YdbIncite
import ru.yandex.vertis.incite.models.FavoriteTableRow
import ru.yandex.vertis.incite.utils.Protobuf.RichDateTime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.ydb.skypper.request.RequestContext
import ru.yandex.vertis.ydb.skypper.settings.TransactionSettings
import ru.yandex.vertis.ydb.skypper.{YdbQueryExecutor, YdbWrapper}

class InciteDaoImplTest extends WordSpec with Matchers with Suite with ScalaFutures with MockitoSupport {

  implicit val ex = Threads.SameThreadEc
  implicit val trace = Traced.empty

  abstract private class Fixture {
    val ydbMocked = mock[YdbWrapper]
    val mockedExecutor = mock[YdbQueryExecutor]
    val dao = new InciteDaoImpl(ydbMocked)
    val offerId = "testOffer"
    val userId = "testUser"
    val ownerId = "testDealer"

    val dateTime = new DateTime()
    val currentTimeProto = dateTime.toProtobufTimestamp

    stub(
      ydbMocked.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
    ) { case (_, _, executor, _, _) => executor(mockedExecutor) }
  }

  "InciteDao" should {
    "update favorite row on add, with existing row, skipping" in new Fixture {
      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator(
            FavoriteTableRow(
              offerId = offerId,
              ownerId = ownerId,
              userId = userId,
              canSend = true,
              timeSent = None,
              history = FavoriteHistory.newBuilder().build(),
              sentCount = 0,
              deleted = false
            )
          )
        )
      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = false, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

    "update favorite row on delete, with existing row, skipping" in new Fixture {
      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator(
            FavoriteTableRow(
              offerId = offerId,
              ownerId = ownerId,
              userId = userId,
              canSend = false,
              timeSent = None,
              history = FavoriteHistory.newBuilder().build(),
              sentCount = 0,
              deleted = true
            )
          )
        )
      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = true, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

    "update favorite row on delete, without row, with counters unchanged" in new Fixture {
      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator()
        )
      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = true, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

    "update favorite row on delete, with row, with counters updated, with messages was not sent" in new Fixture {
      val counters = FavoriteCounters.newBuilder().setCountCanSend(1).setCountFavorites(1).setSentCount(0).build()
      val updatedCounters = FavoriteCounters
        .newBuilder()
        .setCountCanSend(0)
        .setCountFavorites(0)
        .setSentCount(0)
        .build()

      val query = YdbIncite.getCountersUpsertQuery(offerId, updatedCounters)

      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)

      when(mockedExecutor.queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?))
        .thenReturn(
          Iterator(counters)
        )

      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator(
            FavoriteTableRow(
              offerId = offerId,
              ownerId = ownerId,
              userId = userId,
              canSend = true,
              timeSent = None,
              history = FavoriteHistory.newBuilder().build(),
              sentCount = 0,
              deleted = false
            )
          )
        )

      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = true, dateTime)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)
      verifyNoMoreInteractions(mockedExecutor)

    }
    "update favorite row on delete, with row, with counters updated, with messages was sent and canSend=false" in new Fixture {
      val counters = FavoriteCounters.newBuilder().setCountCanSend(1).setCountFavorites(2).setSentCount(1).build()
      val updatedCounters = FavoriteCounters
        .newBuilder()
        .setCountCanSend(1)
        .setCountFavorites(1)
        .setSentCount(1)
        .build()
      val query = YdbIncite.getCountersUpsertQuery(offerId, updatedCounters)

      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)

      when(mockedExecutor.queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?))
        .thenReturn(
          Iterator(counters)
        )

      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator(
            FavoriteTableRow(
              offerId = offerId,
              ownerId = ownerId,
              userId = userId,
              canSend = false,
              timeSent = None,
              history = FavoriteHistory.newBuilder().build(),
              sentCount = 1,
              deleted = false
            )
          )
        )

      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = true, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)

    }

    "update favorite row on add, without row, with counters updated" in new Fixture {
      val counters = FavoriteCounters.newBuilder().setCountCanSend(1).setCountFavorites(1).setSentCount(0).build()
      val updatedCounters = FavoriteCounters
        .newBuilder()
        .setCountCanSend(2)
        .setCountFavorites(2)
        .setSentCount(0)
        .build()
      val query = YdbIncite.getCountersUpsertQuery(offerId, updatedCounters)

      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)

      when(mockedExecutor.queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?))
        .thenReturn(
          Iterator(counters)
        )

      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator()
        )

      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = false, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

    "update favorite row on add, with row, with counters updated, with messages was not sent" in new Fixture {
      val counters = FavoriteCounters.newBuilder().setCountCanSend(1).setCountFavorites(1).setSentCount(0).build()
      val updatedCounters = FavoriteCounters
        .newBuilder()
        .setCountCanSend(1)
        .setCountFavorites(2)
        .setSentCount(0)
        .build()
      val query = YdbIncite.getCountersUpsertQuery(offerId, updatedCounters)

      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)

      when(mockedExecutor.queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?))
        .thenReturn(
          Iterator(counters)
        )

      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator(
            FavoriteTableRow(
              offerId = offerId,
              ownerId = ownerId,
              userId = userId,
              canSend = false,
              timeSent = None,
              history = FavoriteHistory.newBuilder().build(),
              sentCount = 0,
              deleted = true
            )
          )
        )

      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = false, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

    "update favorite row on delete, without row, without counters update" in new Fixture {
      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator()
        )
      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = true, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

    "update favorite row on delete, without row, with counters create" in new Fixture {
      val updatedCounters = FavoriteCounters
        .newBuilder()
        .setCountCanSend(1)
        .setCountFavorites(1)
        .setSentCount(0)
        .build()
      val query = YdbIncite.getCountersUpsertQuery(offerId, updatedCounters)

      doNothing().when(mockedExecutor).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      doNothing().when(mockedExecutor).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)

      when(mockedExecutor.queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?))
        .thenReturn(
          Iterator()
        )

      when(mockedExecutor.queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?))
        .thenReturn(
          Iterator()
        )

      dao.updateFavoriteTableRow(offerId, userId, ownerId, deleting = false, dateTime)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("update-favorite-table-row"))(?, ?)
      verify(mockedExecutor, times(1)).updatePrepared(eqq("upsert-counters"))(eqq(query), ?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteTableRow](eqq("get-favorite-table-row"))(?, ?)(?)
      verify(mockedExecutor, times(1)).queryPrepared[FavoriteCounters](eqq("get-current-counters"))(?, ?)(?)
      verifyNoMoreInteractions(mockedExecutor)
    }

  }

}
