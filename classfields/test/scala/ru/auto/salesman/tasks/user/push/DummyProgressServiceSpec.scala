package ru.auto.salesman.tasks.user.push

class DummyProgressServiceSpec extends ProgressServiceSpec[Long, Long] {

  override protected val progressService: ProgressService[Long, Long] =
    new DummyProgressService
  override protected val trackingId: Long = 1
  override protected val anotherTrackingId: Long = 2
  override protected val lastHandled: Long = 3
  override protected val laterLastHandled: Long = 4
}
