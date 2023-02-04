package ru.yandex.vertis.parsing.workers.holocron

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.FunSuite
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.components.zookeeper.node.ZookeeperNode
import ru.yandex.vertis.parsing.dao.CommonParsedRow
import ru.yandex.vertis.parsing.dao.holocron.HolocronDao
import ru.yandex.vertis.parsing.features.AlwaysEnabledFeature
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.workmoments.WorkMoments
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.duration._

class OldDataRemoverTest extends FunSuite with MockitoSupport {
  implicit private val trace: Traced = TracedUtils.empty

  test("correct keepDuration handle") {
    val holocronDao = mock[HolocronDao[CommonParsedRow]]
    implicit val timeService: TimeService = mock[TimeService]
    val keepDuration = 30.days
    val workMoments = WorkMoments.every(2.minutes)
    val node = mock[ZookeeperNode[Long]]
    val worker = new OldDataRemover(
      holocronDao,
      node,
      workMoments,
      timeService,
      keepDuration,
      AlwaysEnabledFeature("main"),
      AlwaysEnabledFeature("confirm")
    ) {
      override def shouldWork: Boolean = true

      override def start(): Unit = ???

      override def stop(): Unit = ???
    }
    val now = DateTime.now().withMillisOfDay(0)
    when(timeService.getNow).thenReturn(now)
    when(holocronDao.getOldProcessedIds(eq(100), eq(now.minusDays(30)))(?)).thenReturn(Seq())
    when(node.get(?)).thenReturn(-1L)
    worker.removeOld()
    verify(holocronDao).getOldProcessedIds(eq(100), eq(now.minusDays(30)))(?)
  }
}
