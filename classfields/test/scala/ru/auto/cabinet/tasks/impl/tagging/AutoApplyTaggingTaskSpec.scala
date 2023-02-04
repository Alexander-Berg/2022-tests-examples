package ru.auto.cabinet.tasks.impl.tagging

import java.time.OffsetDateTime
import org.mockito.ArgumentMatchers.{eq => argEq, _}
import org.mockito.Mockito._
import ru.auto.cabinet.dao.jdbc.{JdbcAutoApplyScheduleDao, JdbcKeyValueDao}
import ru.auto.cabinet.model.AutoApplySchedule
import ru.auto.cabinet.model.offer.OfferId
import ru.auto.cabinet.service.vos.VosClient
import ru.auto.cabinet.test.TestUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

class AutoApplyTaggingTaskSpec extends TaggingTaskSpec {

  private val autoApplyScheduleDao = mock[JdbcAutoApplyScheduleDao]
  private val keyValueDao = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient]

  private val task =
    new AutoApplyTaggingTask(
      autoApplyScheduleDao,
      keyValueDao,
      vosClient,
      includeInvisible = false) with TestInstrumented
  import task._

  private val DtStrInPast = "2017-01-01T00:00:00.000+03:00"
  private val DtInPast = OffsetDateTime.parse(DtStrInPast)
  private val LaterDtStrInPast = "2017-02-01T00:00:00.000+03:00"
  private val LaterDtInPast = OffsetDateTime.parse(LaterDtStrInPast)
  private val LatestDtStrInPast = "2017-02-02T00:00:00.000+03:00"
  private val LatestDtInPast = OffsetDateTime.parse(LatestDtStrInPast)

  private val scheduleGen =
    AutoApplySchedule(
      _: OfferId,
      deleted = false,
      visible = true,
      epoch = LaterDtInPast)

  private def schedules(offerIds: OfferId*) = offerIds.map(scheduleGen)

  "AutoApplyTaggingTask" should "tag offer even tagged with other tags" in {
    when(keyValueDao.valueByKey(lastTaggingKey)).thenReturnF(Some(DtStrInPast))
    when(
      autoApplyScheduleDao
        .find(updatedSince = Some(DtInPast), includeInvisible = false))
      .thenReturnF(schedules(OfferId1))
    when(vosClient.putTag(OfferId1, tag)).thenReturnF(())
    when(keyValueDao.upsert(lastTaggingKey, LaterDtStrInPast))
      .thenReturnF(1)
    whenReady(task.execute) { _ =>
      verify(vosClient).putTag(OfferId1, tag)
      verify(keyValueDao).valueByKey(lastTaggingKey)
      verify(keyValueDao).upsert(lastTaggingKey, LaterDtStrInPast)
      verifyNoMoreInteractions(vosClient, keyValueDao)
    }
  }

  it should "not update lastTaggingKey if no schedules found" in {
    when(keyValueDao.valueByKey(lastTaggingKey)).thenReturnF(None)
    when(
      autoApplyScheduleDao.find(updatedSince = None, includeInvisible = false))
      .thenReturnF(schedules())
    when(
      keyValueDao.upsert(argEq(lastTaggingKey), argThat(strAfterTestStart))(
        any()))
      .thenReturnF(1)
    whenReady(task.execute) { _ =>
      verify(keyValueDao).valueByKey(lastTaggingKey)
      verify(autoApplyScheduleDao)
        .find(updatedSince = None, includeInvisible = false)
      verifyNoMoreInteractions(keyValueDao)
      verifyNoMoreInteractions(autoApplyScheduleDao)
      verifyNoInteractions(vosClient)
    }
  }

  it should "put tags and update tagging time properly when dealing with multiple schedules" in {
    when(keyValueDao.valueByKey(lastTaggingKey)).thenReturnF(Some(DtStrInPast))
    when(
      autoApplyScheduleDao
        .find(updatedSince = Some(DtInPast), includeInvisible = false))
      .thenReturnF(
        schedules(OfferId1, OfferId2, OfferId3, OfferId4)
          .map { sched =>
            if (sched.offerId == OfferId4) sched.copy(epoch = LatestDtInPast)
            else sched
          }
      )
    when(vosClient.putTag(any(), argEq(tag))(any())).thenReturnF(())
    when(
      keyValueDao.upsert(argEq(lastTaggingKey), argThat(strAfterTestStart))(
        any()))
      .thenReturnF(1)
    when(keyValueDao.upsert(lastTaggingKey, LatestDtStrInPast)).thenReturnF(1)
    whenReady(task.execute(rc)) { _ =>
      verify(vosClient).putTag(OfferId1, tag)
      verify(vosClient).putTag(OfferId2, tag)
      verify(vosClient).putTag(OfferId3, tag)
      verify(vosClient).putTag(OfferId4, tag)
      verify(keyValueDao).valueByKey(lastTaggingKey)
      verify(keyValueDao).upsert(lastTaggingKey, LatestDtStrInPast)
      verifyNoMoreInteractions(vosClient, keyValueDao)
    }
  }

  it should "not update tagging time if couldn't get at least one of offers" in {
    when(keyValueDao.valueByKey(lastTaggingKey))
      .thenReturnF(Some(DtStrInPast))
    when(
      autoApplyScheduleDao
        .find(updatedSince = Some(DtInPast), includeInvisible = false))
      .thenReturnF(schedules(OfferId1, OfferId2))
    try {
      Await.result(task.execute, 1.second)
      throw new RuntimeException
    } catch {
      case _: Exception =>
    }
    verify(keyValueDao).valueByKey(lastTaggingKey)
    verifyNoMoreInteractions(keyValueDao)
  }

  it should "not update tagging time if couldn't put tag for at least one of offers" in {
    when(keyValueDao.valueByKey(lastTaggingKey))
      .thenReturnF(Some(DtStrInPast))
    when(
      autoApplyScheduleDao
        .find(updatedSince = Some(DtInPast), includeInvisible = false))
      .thenReturnF(schedules(OfferId1, OfferId2))
    when(vosClient.putTag(OfferId1, tag)).thenReturnF(())
    when(vosClient.putTag(OfferId2, tag)).thenThrowF(new Exception)
    try {
      Await.result(task.execute(rc), 1.second)
      throw new RuntimeException
    } catch {
      case _: Exception =>
    }
    verify(keyValueDao).valueByKey(lastTaggingKey)
    verifyNoMoreInteractions(keyValueDao)
  }

  it should "not put tag for offer with old format id" in {
    when(
      autoApplyScheduleDao.find(updatedSince = None, includeInvisible = false))
      .thenReturnF(schedules("badofferid"))
    when(vosClient.putTag(any(), any())(any())).thenReturnF(())
    whenReady(task.doTagging(None)) { _ =>
      verifyNoInteractions(vosClient)
    }
  }

  it should "delete tag if deleted schedule found" in {
    when(
      autoApplyScheduleDao
        .find(updatedSince = None, includeInvisible = false)).thenReturnF(Seq(
      AutoApplySchedule(OfferId1, deleted = true, visible = true, LaterDtInPast)
    ))
    when(vosClient.putTag(any(), any())(any())).thenReturnF(())
    when(vosClient.deleteTag(any(), any())(any())).thenReturnF(())
    whenReady(task.doTagging(None)) { _ =>
      verify(vosClient).deleteTag(OfferId1, tag)
      verifyNoMoreInteractions(vosClient)
    }
  }

  it should "put tag if last schedule is not deleted" in {
    when(
      autoApplyScheduleDao.find(updatedSince = None, includeInvisible = false))
      .thenReturnF(
        Seq(
          AutoApplySchedule(
            OfferId1,
            deleted = true,
            visible = true,
            LaterDtInPast),
          AutoApplySchedule(
            OfferId1,
            deleted = false,
            visible = true,
            LaterDtInPast.plusSeconds(1))
        ))
    when(vosClient.putTag(any(), any())(any())).thenReturnF(())
    when(vosClient.deleteTag(any(), any())(any())).thenReturnF(())
    whenReady(task.doTagging(None)) { _ =>
      verify(vosClient).putTag(OfferId1, tag)
      verifyNoMoreInteractions(vosClient)
    }
  }

  it should "put tag if last schedule is deleted, but active schedule exists nevertheless" in {
    when(
      autoApplyScheduleDao.find(updatedSince = None, includeInvisible = false))
      .thenReturnF(
        Seq(
          AutoApplySchedule(
            OfferId1,
            deleted = false,
            visible = true,
            LaterDtInPast),
          AutoApplySchedule(
            OfferId1,
            deleted = true,
            visible = true,
            LaterDtInPast.plusSeconds(1))
        ))
    when(vosClient.putTag(any(), any())(any())).thenReturnF(())
    when(vosClient.deleteTag(any(), any())(any())).thenReturnF(())
    whenReady(task.doTagging(None)) { _ =>
      verify(vosClient).putTag(OfferId1, tag)
      verifyNoMoreInteractions(vosClient)
    }
  }

  it should "put tag only for active schedule" in {
    when(
      autoApplyScheduleDao.find(updatedSince = None, includeInvisible = false))
      .thenReturnF(
        Seq(
          AutoApplySchedule(
            OfferId1,
            deleted = false,
            visible = true,
            LaterDtInPast),
          AutoApplySchedule(
            OfferId2,
            deleted = true,
            visible = true,
            LaterDtInPast.plusSeconds(1))
        ))
    when(vosClient.putTag(any(), any())(any())).thenReturnF(())
    when(vosClient.deleteTag(any(), any())(any())).thenReturnF(())
    whenReady(task.doTagging(None)) { _ =>
      verify(vosClient).putTag(OfferId1, tag)
      verify(vosClient).deleteTag(OfferId2, tag)
      verifyNoMoreInteractions(vosClient)
    }
  }

  private val OfferId1 = "1043045004-977b1"
  private val OfferId2 = "1043045004-977b2"
  private val OfferId3 = "1043045004-977b3"
  private val OfferId4 = "1043045004-977b4"
}
