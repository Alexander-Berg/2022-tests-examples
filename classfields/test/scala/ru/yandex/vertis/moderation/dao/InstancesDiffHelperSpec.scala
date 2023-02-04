package ru.yandex.vertis.moderation.dao

import cats.effect.IO
import org.joda.time.Interval
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.impl.empty.EmptyInstanceDao
import ru.yandex.vertis.moderation.model.MaybeExpiredInstance
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  instanceGen,
  EssentialsGenerators,
  ExternalIdGen,
  InstanceGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{ExternalId, Instance, InstanceUpdateResult}
import ru.yandex.vertis.moderation.util.DateTimeUtil

import java.util.concurrent.Executors
import scala.collection.immutable.Iterable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * Spec of [[InstancesDiffHelper]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class InstancesDiffHelperSpec extends SpecBase {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool())

  val externalId = ExternalIdGen.next
  val essentialsGen = Gen.oneOf(EssentialsGenerators).next
  val from = DateTimeUtil.now()
  val to = from.plusDays(1)
  val timeInterval = new Interval(from, to)
  val current = instanceGen(ExternalIdGen.next, essentialsGen).next

  val mockInstanceArchiveDao: InstanceArchiveDao = mock[InstanceArchiveDao]
  val mockInstanceDao: InstanceDao[Future] =
    new EmptyInstanceDao() {
      override def getOpt(externalId: ExternalId, allowExpired: Boolean): Future[Option[Instance]] =
        Future.successful(Some(current))
    }

  val instanceArchiveDaoHelper = new InstancesDiffHelper(mockInstanceArchiveDao, mockInstanceDao)

  "InstanceArchiveDaoHelper diff" should {

    implicit def pairToDiff(newAndOld: (Instance, Instance)): InstanceUpdateResult =
      InstanceUpdateResult(newAndOld._1, newAndOld._2)

    implicit def snapshot(instance: Instance): InstanceUpdateResult = InstanceUpdateResult(instance)

    "return correct diff with last instance and snapshot" in {
      val archivedInstances = instanceGen(externalId, essentialsGen).next(3)
      doReturn(Future.successful(archivedInstances))
        .when(mockInstanceArchiveDao)
        .get(meq(externalId), meq(timeInterval), anyInt)
      val List(instance1, instance2, instance3) = archivedInstances.toList
      val diff =
        instanceArchiveDaoHelper.diff(externalId, timeInterval, addCurrent = true, addSnapshot = true).futureValue
      val expectedDiff: Iterable[InstanceUpdateResult] =
        Iterable(current -> instance1, instance1 -> instance2, instance2 -> instance3, instance3)
      diff shouldBe expectedDiff
    }

    "return correct diff with last instance and without snapshot" in {
      val archivedInstances = instanceGen(externalId, essentialsGen).next(3)
      doReturn(Future.successful(archivedInstances))
        .when(mockInstanceArchiveDao)
        .get(meq(externalId), meq(timeInterval), anyInt)
      val List(instance1, instance2, instance3) = archivedInstances.toList
      val diff =
        instanceArchiveDaoHelper.diff(externalId, timeInterval, addCurrent = true, addSnapshot = false).futureValue
      val expectedDiff: Iterable[InstanceUpdateResult] =
        Iterable(current -> instance1, instance1 -> instance2, instance2 -> instance3)
      diff shouldBe expectedDiff
    }

    "return correct diff without last instance and with snapshot" in {
      val archivedInstances = instanceGen(externalId, essentialsGen).next(3)
      doReturn(Future.successful(archivedInstances))
        .when(mockInstanceArchiveDao)
        .get(meq(externalId), meq(timeInterval), anyInt)
      val List(instance1, instance2, instance3) = archivedInstances.toList
      val expectedDiff: Iterable[InstanceUpdateResult] =
        Iterable(instance1 -> instance2, instance2 -> instance3, instance3)
      val diff =
        instanceArchiveDaoHelper.diff(externalId, timeInterval, addCurrent = false, addSnapshot = true).futureValue
      diff shouldBe expectedDiff
    }

    "return only snapshot when there is one instance in the archive storage and no current instance" in {
      val archivedInstance = InstanceGen.next
      doReturn(Future.successful(Seq(archivedInstance)))
        .when(mockInstanceArchiveDao)
        .get(meq(externalId), meq(timeInterval), anyInt)
      val expectedDiff: Iterable[InstanceUpdateResult] = Iterable(archivedInstance)
      val diff =
        instanceArchiveDaoHelper.diff(externalId, timeInterval, addCurrent = false, addSnapshot = true).futureValue
      diff shouldBe expectedDiff
    }

    "return snapshot of current instance when there are no instances in the archive storage and current instance is provided" in {
      doReturn(Future.successful(Seq.empty[Instance]))
        .when(mockInstanceArchiveDao)
        .get(meq(externalId), meq(timeInterval), anyInt)
      val expectedDiff: Iterable[InstanceUpdateResult] = Iterable(current)
      val diff =
        instanceArchiveDaoHelper.diff(externalId, timeInterval, addCurrent = true, addSnapshot = true).futureValue
      diff shouldBe expectedDiff
    }

    "return empty result when there are no instances in the archive storage and no current instance provided" in {
      doReturn(Future.successful(Seq.empty[Instance]))
        .when(mockInstanceArchiveDao)
        .get(meq(externalId), meq(timeInterval), anyInt)
      val expectedDiff = Iterable.empty[InstanceUpdateResult]
      val diff =
        instanceArchiveDaoHelper.diff(externalId, timeInterval, addCurrent = false, addSnapshot = true).futureValue
      diff shouldBe expectedDiff
    }
  }
}
