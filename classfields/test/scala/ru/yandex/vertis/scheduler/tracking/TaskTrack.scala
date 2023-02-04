package ru.yandex.vertis.scheduler.tracking

import ru.yandex.vertis.scheduler._
import ru.yandex.vertis.scheduler.journal.FinishRecord
import ru.yandex.vertis.scheduler.journal.StartRecord
import ru.yandex.vertis.scheduler.journal.{JournalRecord, Journal}
import scala.annotation.tailrec
import scala.util.Try

/**
 * Sequence of task events (starts/stops/etc)
 * to be analyzed for test scheduler correctness.
 *
 * @author dimas
 */
case class TaskTrack(id: TaskId,
                     points: Seq[TrackPoint]) {
  require(id != null, "Null task ID")
  require(points != null, "Null track points")

  def checkValid(): Unit = {
    checkTrackIsValid(id, points.toList)
  }

  @tailrec
  private def checkTrackIsValid(task: TaskId,
                                pointsList: List[TrackPoint]): Unit =
    pointsList match {
      case TrackPoint(startTime, startInstance, TaskEvent.Start, true) ::
        TrackPoint(finishTime, finishInstance, TaskEvent.Finish, _) :: rest =>
        require(startTime.isBefore(finishTime),
          "Start time is not before stop time")
        require(startInstance == finishInstance,
          s"Task started on $startInstance but finished on $finishInstance")
        checkTrackIsValid(task, rest)
      case TrackPoint(startTime, startInstance, TaskEvent.Start, false):: rest =>
        checkTrackIsValid(task, rest)
      case _ :: Nil =>
      case Nil =>
      case someInvalidTrackPart =>
        throw new IllegalArgumentException(
          s"Invalid track part for task $task starts from: $someInvalidTrackPart")
    }
}

object TaskTrack {
  def apply(journal: Journal): Try[Iterable[TaskTrack]] = Try {
    val records = journal.list().get
    val tasks = records.map(_.taskId).toSet
    for {
      task <- tasks
      track = watchTrack(task, records)
    } yield track
  }

  def apply(taskId: TaskId, journal: Journal): Try[TaskTrack] = Try {
    val records = journal.list().get
    watchTrack(taskId, records)
  }

  def watchTrack(taskId: TaskId,
                 records: Seq[JournalRecord]) = {
    val points = for {
      record <- records
      point <- record match {
        case StartRecord(`taskId`, instance, _, time, success) =>
          Some(TrackPoint(time, instance, TaskEvent.Start, success))
        case FinishRecord(`taskId`, instance, jobResult, success) =>
          Some(TrackPoint(jobResult.finishTime, instance, TaskEvent.Finish, success))
        case _ =>
          None
      }
    } yield point
    TaskTrack(taskId, points)
  }
}
