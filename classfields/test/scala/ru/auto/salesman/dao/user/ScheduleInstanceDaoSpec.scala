package ru.auto.salesman.dao.user

import org.scalacheck.Gen
import org.scalatest._
import ru.auto.salesman.dao.ScheduleInstanceDao
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceFilter.{
  ForIds,
  ForScheduleIds,
  ForStatuses
}
import ru.auto.salesman.dao.ScheduleInstanceDao.Patch.StatusPatch
import ru.auto.salesman.dao.ScheduleInstanceDao.{
  InstanceFilter,
  InstanceLimit,
  InstanceOrder
}
import ru.auto.salesman.dao.exceptions.ProcessScheduleInstanceException
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.ScheduleInstance
import ru.auto.salesman.model.ScheduleInstance.{Status, Statuses}
import ru.auto.salesman.model.schedule.RescheduleStrategy
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators
import ru.auto.salesman.test.model.gens.user.ScheduleServiceModelGenerators
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.util.Random

trait ScheduleInstanceDaoSpec
    extends BaseSpec
    with BasicGenerators
    with ScheduleServiceModelGenerators
    with ScheduleInstanceGenerators {

  def newDao(instances: Iterable[ScheduleInstance]): ScheduleInstanceDao

  def newInstances: List[ScheduleInstance] =
    listNUnique[ScheduleInstance, Long](10, ScheduleInstanceGen)(_.id).next

  "ScheduleInstanceDao" should {
    "get schedule instances" in {
      val instances = newInstances
      val dao = newDao(instances)

      dao.get()().get should contain theSameElementsAs instances

      val firstPart = instances.take(20)
      dao.get(ForIds(firstPart.map(_.id).toSet))().get should
        contain theSameElementsAs instances.take(20)

      val byFireTimeAsc = instances.sortBy(_.fireTime)
      (dao.get()(InstanceOrder.ByFireTime(asc = true)).get.toList should
        contain).theSameElementsInOrderAs(byFireTimeAsc)
      (dao.get()(InstanceOrder.ByFireTime(asc = false)).get.toList should
        contain).theSameElementsInOrderAs(byFireTimeAsc.reverse)

      (dao
        .get()(InstanceOrder.ByFireTime(asc = true), InstanceLimit.Count(10))
        .get
        .toList should
        contain).theSameElementsInOrderAs(byFireTimeAsc.take(10))

      val withScheduleIds = instances
        .groupBy(_.scheduleId)
        .values
        .toSeq
        .sortBy(_.size)
        .reverse
        .flatten
        .take(30)
      dao
        .get(
          InstanceFilter.ForScheduleIds(withScheduleIds.map(_.scheduleId).toSet)
        )()
        .get should
        contain theSameElementsAs withScheduleIds

      val byStatus = instances.groupBy(_.status)

      dao
        .get(
          InstanceFilter
            .ForStatuses(Statuses.Failed, Statuses.Acquired, Statuses.Pending)
        )()
        .get should
        contain theSameElementsAs (
          byStatus.get(Statuses.Failed) ++
          byStatus.get(Statuses.Acquired) ++
          byStatus.get(Statuses.Pending)
        ).flatten

      val (oneStatus, withOneStatus) = byStatus.toSeq.maxBy(_._2.size)
      val withOneStatusPart1 = {
        val grouped = withOneStatus.groupBy(_.scheduleId)
        grouped.take(math.max(grouped.size / 2, 1))
      }

      dao
        .get(
          InstanceFilter.ForStatuses(oneStatus),
          InstanceFilter.ForScheduleIds(withOneStatusPart1.keySet)
        )()
        .get should contain theSameElementsAs withOneStatusPart1.values.flatten

      val withOneStatusPart2 =
        withOneStatus.take(math.max(withOneStatus.size / 2, 1))
      dao
        .get(
          ForIds(withOneStatusPart2.map(_.id).toSet),
          InstanceFilter.ForStatuses(oneStatus)
        )()
        .get should contain theSameElementsAs withOneStatusPart2
    }

    "insert schedule instances" in {
      val dao = newDao(Iterable.empty)

      val size1 = 20
      val sources1 = Gen.listOfN(size1, ScheduleInstanceSourceGen).next

      dao.insert(sources1).get
      val instances1 = dao.get()().get
      instances1.size shouldBe size1

      instances1.map(_.scheduleId) should
        contain theSameElementsAs sources1.map(_.scheduleId)
      instances1.map(_.fireTime) should
        contain theSameElementsAs sources1.map(_.fireTime)
      instances1.map(_.scheduleUpdateTime) should
        contain theSameElementsAs sources1.map(_.scheduleUpdateTime)
      instances1.foreach(_.status shouldBe Statuses.Pending)

      val size2 = 10
      val sources2 = Gen.listOfN(size2, ScheduleInstanceSourceGen).next

      dao.insert(sources2).get

      val allInstances = dao.get()().get
      allInstances.size shouldBe (size1 + size2)
      val instances2 = allInstances.toList.sortBy(_.id).drop(size1)

      instances2.map(_.scheduleId) should
        contain theSameElementsAs sources2.map(_.scheduleId)
      instances2.map(_.fireTime) should
        contain theSameElementsAs sources2.map(_.fireTime)
      instances2.map(_.scheduleUpdateTime) should
        contain theSameElementsAs sources2.map(_.scheduleUpdateTime)
      instances2.foreach(_.status shouldBe Statuses.Pending)
    }

    "update instances" in {
      def nextStatus(s: Status): Status =
        Statuses((s.id + 1) % Statuses.values.size + 1)

      val size = 50
      val instances =
        listNUnique[ScheduleInstance, Long](size, ScheduleInstanceGen)(
          _.id
        ).next
      val dao = newDao(instances)

      val byStatus = instances.groupBy(_.status)

      byStatus.foreach { case (status, instancesWithStatus) =>
        val newStatus = nextStatus(status)
        val toUpdate =
          instancesWithStatus.take(math.max(instancesWithStatus.size / 2, 1))
        val ids = toUpdate.map(_.id).toSet

        dao
          .update(StatusPatch(newStatus), ForIds(ids))
          .get shouldBe toUpdate.size

        val updated = dao.get(ForIds(ids))().get
        updated.foreach(_.status shouldBe newStatus)
        updated.map(_.id) should contain theSameElementsAs ids
      }

      val allUpdated = dao.get()().get
      val single = allUpdated.toList(Random.nextInt(allUpdated.size))

      val newSingleStatus = nextStatus(single.status)

      dao
        .update(
          StatusPatch(newSingleStatus),
          ForIds(single.id),
          ForStatuses(newSingleStatus)
        )
        .get shouldBe 0

      dao
        .update(
          StatusPatch(newSingleStatus),
          ForIds(single.id),
          ForStatuses(single.status)
        )
        .get shouldBe 1
      dao.get(ForIds(single.id))().get.head.status shouldBe newSingleStatus

      val allUpdated2 = dao.get()().get

      val (scheduleIds, schedulesInstances) = {
        val sorted = allUpdated2.groupBy(_.scheduleId).toSeq.sortBy(_._2.size)
        val (scheduleIds, multiValues) = sorted.drop(sorted.size / 2).unzip
        (scheduleIds.toSet, multiValues.flatten)
      }
      val withScheduleIdsNewStatus = Statuses.Skipped
      val updateCnt = dao
        .update(
          StatusPatch(withScheduleIdsNewStatus),
          ForScheduleIds(scheduleIds)
        )
        .get
      updateCnt shouldBe schedulesInstances.size
      dao.get(ForScheduleIds(scheduleIds))().get.map(_.status).toSet should
        (have size 1 and contain(withScheduleIdsNewStatus))
    }

    "update and reschedule" in {
      val initFireTime = now()
      val forReschedule = newInstances.map(_.copy(fireTime = initFireTime))
      // type can't be infered by compiler due to some reason
      val expectedRescheduled: List[ScheduleInstance] = forReschedule match {
        case head :: tail =>
          head.copy(status = Statuses.Acquired) ::
            tail.map(
              _.copy(scheduleId = head.scheduleId, status = Statuses.Pending)
            )
        case Nil => fail("List for reschedule shouldn't be empty")
      }
      val scheduleId = expectedRescheduled.head.scheduleId
      val wontBeRescheduled = newInstances.filter { instance =>
        instance.scheduleId != scheduleId &&
        !expectedRescheduled.exists(_.id == instance.id)
      }
      val dao = newDao(expectedRescheduled ++ wontBeRescheduled)
      // plusMinutes(5) to avoid some random clashes with current time
      val newFireTime = now().plusMinutes(5)
      val rescheduleStrategy: RescheduleStrategy = List.fill(_)(newFireTime)
      dao
        .updateAndReschedule(
          expectedRescheduled.head,
          StatusPatch(Statuses.Cancelled),
          rescheduleStrategy
        )
        .success
        .value shouldBe expectedRescheduled.size
      val cancelled = dao
        .get(
          ForScheduleIds(scheduleId),
          ForStatuses(Statuses.Cancelled)
        )()
        .success
        .value
        .headOption
        .value
      cancelled shouldBe expectedRescheduled.head
        .copy(
          fireTime = cancelled.fireTime,
          epoch = cancelled.epoch,
          status = Statuses.Cancelled
        )
      val rescheduled = dao
        .get(
          ForScheduleIds(scheduleId),
          ForStatuses(Statuses.Pending)
        )()
        .success
        .value
      rescheduled.size shouldBe expectedRescheduled.size
      Inspectors.forEvery(rescheduled) { instance =>
        instance.fireTime shouldBe newFireTime
      }
      Inspectors.forEvery(wontBeRescheduled) { instance =>
        dao
          .get(ForIds(instance.id))()
          .success
          .value
          .headOption
          .value shouldBe instance
      }
    }

    "only cancel, but not reschedule anything, if rescheduled strategy returned Nil" in {
      val initFireTime = now()
      val instances: List[ScheduleInstance] =
        newInstances.map(_.copy(fireTime = initFireTime)) match {
          case head :: tail =>
            head.copy(status = Statuses.Acquired) ::
              tail.map(
                _.copy(scheduleId = head.scheduleId, status = Statuses.Pending)
              )
          case Nil => fail("List for reschedule shouldn't be empty")
        }
      val scheduleId = instances.head.scheduleId
      val dao = newDao(instances)
      val rescheduleStrategy: RescheduleStrategy = _ => Nil
      dao
        .updateAndReschedule(
          instances.head,
          StatusPatch(Statuses.Cancelled),
          rescheduleStrategy
        )
        .success
        .value shouldBe 0
      val cancelled = dao
        .get(
          ForScheduleIds(scheduleId),
          ForStatuses(Statuses.Cancelled)
        )()
        .success
        .value
        .headOption
        .value
      cancelled shouldBe instances.head
        .copy(
          fireTime = cancelled.fireTime,
          epoch = cancelled.epoch,
          status = Statuses.Cancelled
        )
      dao
        .get(
          ForScheduleIds(scheduleId),
          ForStatuses(Statuses.Pending)
        )()
        .success
        .value should contain theSameElementsAs instances.tail
    }

    "throw exception, if got unexisting schedule for rescheduling" in {
      val dao = newDao(Nil)
      val rescheduleStrategy: RescheduleStrategy = _ => Nil
      dao
        .updateAndReschedule(
          ScheduleInstanceGen.next,
          StatusPatch(Statuses.Cancelled),
          rescheduleStrategy
        )
        .failure
        .exception shouldBe a[ProcessScheduleInstanceException]
    }
  }
}
