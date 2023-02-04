package ru.auto.salesman.dao.impl.jdbc.user.push

import cats.implicits._
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.dao.impl.jdbc._
import ru.auto.salesman.dao.impl.jdbc.user.push.TestJdbcBroadcastPushingScheduleDao._
import ru.auto.salesman.dao.slick.invariant.StaticQuery.interpolation
import ru.yandex.vertis.generators.BasicGenerators.{bool, readableString}
import ru.yandex.vertis.generators.DateTimeGenerators.dateTime
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.model.push.{PushBody, PushName, PushTitle}

import scala.slick.jdbc.SetParameter

class TestJdbcBroadcastPushingScheduleDao(database: Database)
    extends JdbcBroadcastPushingScheduleDao(
      database = database,
      slaveDatabase = database
    ) {

  def insert(schedule: NewBroadcastPushingSchedule): Unit =
    database.withSession { implicit session =>
      insertQuery(schedule).execute
    }
}

object TestJdbcBroadcastPushingScheduleDao {

  final case class NewBroadcastPushingSchedule(
      start: DateTime,
      deadline: DateTime,
      finished: Boolean,
      title: PushTitle,
      body: PushBody,
      name: PushName,
      sourceId: Int,
      sourceType: String
  )

  private val pushTitleGen = readableString.map(PushTitle)
  private val pushBodyGen = readableString.map(PushBody)
  private val pushNameGen = readableString.map(PushName)

  def newBroadcastPushingScheduleGen(
      startGen: Gen[DateTime] = dateTime(),
      deadlineGen: Gen[DateTime] = dateTime(),
      finishedGen: Gen[Boolean] = bool
  ): Gen[NewBroadcastPushingSchedule] =
    for {
      start <- startGen
      deadline <- deadlineGen
      finished <- finishedGen
      title <- pushTitleGen
      body <- pushBodyGen
      name <- pushNameGen
      sourceId <- Gen.posNum[Int]
      sourceType = "periodical_discount"
    } yield
      NewBroadcastPushingSchedule(
        // datetime fields in MySQL table are truncated to seconds
        start.withMillisOfSecond(0),
        deadline.withMillisOfSecond(0),
        finished,
        title,
        body,
        name,
        sourceId,
        sourceType
      )

  implicit private val pushTitleSetParameter: SetParameter[PushTitle] =
    implicitly[SetParameter[String]].contramap(_.asString)

  implicit private val pushBodySetParameter: SetParameter[PushBody] =
    implicitly[SetParameter[String]].contramap(_.asString)

  implicit private val pushNameSetParameter: SetParameter[PushName] =
    implicitly[SetParameter[String]].contramap(_.asString)

  private def insertQuery(schedule: NewBroadcastPushingSchedule) = {
    import schedule._
    sqlu"""
      INSERT INTO `broadcast_pushing_schedule`
      (`start`, `deadline`, `finished`, `push_title`, `push_body`, `push_name`, `source_id`, `source_type`)
      VALUES ($start, $deadline, $finished, $title, $body, $name, $sourceId, $sourceType)
    """
  }
}
