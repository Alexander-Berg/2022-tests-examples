package ru.yandex.realty.takeout.managers

import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.takeout.SecurityTaskStatus.SecurityCategory
import ru.yandex.realty.takeout.dao.SecurityTaskDao
import ru.yandex.realty.takeout.model.security.SecurityTask
import ru.yandex.realty.takeout.processor.security.impl.SecurityTaskS3CsvExporter
import ru.yandex.realty.takeout.{SecurityTaskResponse, SecurityTaskStatus}
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class SecurityTaskManagerSpec extends AsyncSpecBase {

  "SecurityTaskManager" should {
    "getTasks " in {
      val dao = mock[SecurityTaskDao]
      val s3Exporter = mock[SecurityTaskS3CsvExporter]
      val manager = new SecurityTaskManager(dao, s3Exporter)

      (dao
        .select(_: Long)(_: Traced))
        .expects(1L, *)
        .returning(Future.successful(Seq()))

      val value: SecurityTaskResponse = manager.getTasks(1L)(Traced.empty).futureValue
      value.getStatusList shouldBe empty
    }
    "submit tasks " in {
      val dao = mock[SecurityTaskDao]
      val s3Exporter = mock[SecurityTaskS3CsvExporter]
      val manager = new SecurityTaskManager(dao, s3Exporter)

      val dateTime = DateTime.parse("2022-07-07T14:00:01")
      DateTimeUtils.setCurrentMillisFixed(dateTime.getMillis)

      (dao
        .select(_: Long)(_: Traced))
        .expects(1L, *)
        .returning(Future.successful(Seq()))

      (dao
        .insert(_: SecurityTask)(_: Traced))
        .expects(
          SecurityTask(0, 1L, SecurityCategory.CHAT.getNumber, Math.abs(1L.toInt), dateTime, Some(dateTime), None),
          *
        )
        .returning(Future.successful(Unit))
      (dao
        .insert(_: SecurityTask)(_: Traced))
        .expects(
          SecurityTask(0, 1L, SecurityCategory.EVENT_LOG.getNumber, Math.abs(1L.toInt), dateTime, Some(dateTime), None),
          *
        )
        .returning(Future.successful(Unit))
      (dao
        .insert(_: SecurityTask)(_: Traced))
        .expects(
          SecurityTask(0, 1L, SecurityCategory.OFFER.getNumber, Math.abs(1L.toInt), dateTime, Some(dateTime), None),
          *
        )
        .returning(Future.successful(Unit))

      val value: SecurityTaskResponse = manager.submitTask(1L)(Traced.empty).futureValue

      val expected = SecurityCategory.values.toSeq
        .filter(c => c != SecurityCategory.CATEGORY_UNKNOWN && c != SecurityCategory.UNRECOGNIZED)
        .map(
          category =>
            SecurityTaskStatus
              .newBuilder()
              .setId(s"1_${category.getNumber}")
              .setState(SecurityTaskStatus.SecurityTaskState.IN_PROGRESS)
              .setCategory(category)
              .build()
        )
        .toSet

      value.getStatusList.asScala.toSet shouldBe expected
    }
  }

}
