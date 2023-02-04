package ru.yandex.vertis.ydb.skypper

import java.util.concurrent.CompletableFuture._
import java.util.concurrent.atomic.AtomicInteger

import com.yandex.ydb.ValueProtos.ResultSet
import com.yandex.ydb.core.Result._
import com.yandex.ydb.core.StatusCode._
import com.yandex.ydb.core.{Result, UnexpectedResultException}
import com.yandex.ydb.table.query.{DataQueryResult, Params}
import com.yandex.ydb.table.settings.ExecuteDataQuerySettings
import com.yandex.ydb.table.{Session, TableClient}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.ydb.skypper.exceptions.TooManyRetriesException
import ru.yandex.vertis.ydb.skypper.tcl.TclService

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class UpdateRetriesTest extends AnyFunSuite with Matchers with MockitoSupport {
  private val tableClient = mock[TableClient]
  private val tclService = mock[TclService]
  private val session = mock[Session]
  private val SameThread = Threads.SameThreadEc

  private val ydb = new YdbWrapperImpl("test", tableClient, tclService, "")(SameThread)
  implicit private val trace: Traced = Traced.empty
  implicit private val reads: YdbReads[String] = YdbReads(_.getColumn(0).getUtf8)

  test("generic error") {
    when(tableClient.getOrCreateSession(?)).thenReturn(completedFuture(success[Session](session)))
    when(session.executeDataQuery(?, ?, ?, ?)).thenReturn(completedFuture(Result.fail[DataQueryResult](GENERIC_ERROR)))
    when(session.release()).thenReturn(true)

    intercept[UnexpectedResultException] {
      ydb.update("")("select * from series")
    }
  }

  test("not_found") {
    when(tableClient.getOrCreateSession(?)).thenReturn(completedFuture(success[Session](session)))
    val count = new AtomicInteger(0)
    stub(session.executeDataQuery(_: String, _: TxControlType, _: Params, _: ExecuteDataQuerySettings)) {
      case (_, _, _, _) =>
        if (count.getAndIncrement() == 0) completedFuture(Result.fail[DataQueryResult](NOT_FOUND))
        else {
          val resultSet = ResultSet.getDefaultInstance
          val result = new DataQueryResult("", List(resultSet).asJava)
          completedFuture(Result.success[DataQueryResult](result))
        }
    }
    when(session.release()).thenReturn(true)

    ydb.update("")("select * from series")

    count.get() shouldBe 2
  }

  test("retries") {
    when(tableClient.getOrCreateSession(?)).thenReturn(completedFuture(success[Session](session)))
    val count = new AtomicInteger(0)
    stub(session.executeDataQuery(_: String, _: TxControlType, _: Params, _: ExecuteDataQuerySettings)) {
      case (_, _, _, _) =>
        count.getAndIncrement()
        completedFuture(Result.fail[DataQueryResult](NOT_FOUND))
    }
    when(session.release()).thenReturn(true)

    intercept[TooManyRetriesException] {
      ydb.update("")("select * from series")
    }

    count.get() shouldBe 11
  }
}
