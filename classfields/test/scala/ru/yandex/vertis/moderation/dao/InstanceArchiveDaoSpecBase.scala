package ru.yandex.vertis.moderation.dao

import org.joda.time.{DateTime, Interval}
import org.scalacheck.Gen
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{InstanceGen, _}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.signal.SignalSet

/**
  * Base specs on [[InstanceArchiveDao]]
  *
  * @author azakharov
  */
trait InstanceArchiveDaoSpecBase extends SpecBase {

  def instanceArchiveDao: InstanceArchiveDao

  "upsert" should {

    "correctly insert instance" in {
      val instance = InstanceGen.next
      instanceArchiveDao.upsert(instance).futureValue

      val actualResult = instanceArchiveDao.get(instance.externalId, getInterval(instance)).futureValue
      val expectedResult = Seq(instance)

      actualResult should be(expectedResult)
    }

    "correctly update essentials" in {
      val instance = InstanceGen.next
      val updatedInstance = instance.copy(essentials = RealtyEssentialsGen.next)

      instanceArchiveDao.upsert(instance).futureValue
      instanceArchiveDao.upsert(updatedInstance).futureValue

      val actualResult = instanceArchiveDao.get(instance.externalId, getInterval(instance)).futureValue
      val expectedResult = Seq(updatedInstance)

      actualResult should be(expectedResult)
    }

    "correctly update signals" in {
      val service = ServiceGen.next
      val oldSignal = banSignalGen(service).next
      val instance =
        instanceGen(service).next.copy(
          signals = SignalSet(oldSignal)
        )
      val newSignal =
        unbanSignalGen(service).next.copy(
          timestamp = instance.updateTime.plusSeconds(1),
          switchOff = None
        )
      val updatedInstance =
        instance.copy(
          signals = SignalSet(newSignal)
        )
      instanceArchiveDao.upsert(instance).futureValue
      instanceArchiveDao.upsert(updatedInstance).futureValue

      val actualResult = instanceArchiveDao.get(instance.externalId, getInterval(instance)).futureValue
      val expectedResult = Seq(updatedInstance, instance)

      actualResult should be(expectedResult)
    }

    "correctly update context" in {
      val oldContext = ContextGen.next
      val instance = InstanceGen.next.copy(context = oldContext)
      val newContext =
        ContextGen
          .suchThat(c => c != oldContext)
          .next
          .copy(
            updateTime = Some(instance.updateTime.plusSeconds(1))
          )
      val updatedInstance = instance.copy(context = newContext)
      instanceArchiveDao.upsert(instance).futureValue
      instanceArchiveDao.upsert(updatedInstance).futureValue

      val actualResult = instanceArchiveDao.get(instance.externalId, getInterval(instance)).futureValue
      val expectedResult = Seq(updatedInstance, instance)

      actualResult should be(expectedResult)
    }

    "correctly saves history" in {
      val instance = InstanceGen.next
      val updatedInstance = instance.copy(essentialsUpdateTime = instance.updateTime.plusMinutes(5))

      instanceArchiveDao.upsert(instance).futureValue
      instanceArchiveDao.upsert(updatedInstance).futureValue

      val coveringInterval = new Interval(getInterval(instance).getStart, getInterval(updatedInstance).getEnd)

      val actualResult = instanceArchiveDao.get(instance.externalId, coveringInterval).futureValue
      val expectedResult = Seq(updatedInstance, instance)

      actualResult should be(expectedResult)
    }
  }

  "get" should {

    "return existent instances" in {
      val instance = InstanceGen.next

      instanceArchiveDao.upsert(instance).futureValue

      val actualResult = instanceArchiveDao.get(instance.externalId, getInterval(instance)).futureValue
      val expectedResult = Seq(instance)

      actualResult should be(expectedResult)
    }

    "correctly filter history" in {
      val instance = InstanceGen.next
      val updatedInstance =
        instance.copy(
          essentialsUpdateTime = instance.essentialsUpdateTime.plusMinutes(5),
          signals = SignalSet.Empty,
          context = ContextGen.next.copy(updateTime = None)
        )

      instanceArchiveDao.upsert(instance).futureValue
      instanceArchiveDao.upsert(updatedInstance).futureValue

      val actualResult = instanceArchiveDao.get(instance.externalId, getInterval(instance)).futureValue
      val expectedResult = Seq(instance)

      actualResult should be(expectedResult)
    }

    "not return nonexistent instance" in {
      val nonexistentId = ExternalIdGen.next
      val startTime = DateTimeGen.next
      val endTime =
        DateTimeGen.next match {
          case et if et.isAfter(startTime) => et
          case _                           => startTime
        }
      val interval = new Interval(startTime, endTime)
      val none = instanceArchiveDao.get(nonexistentId, interval).futureValue

      none should be(Seq.empty)
    }
    "return instances with limit" in {
      val externalId = ExternalIdGen.next
      val countInstances = 3
      val hugeInterval =
        new Interval(
          new DateTime(0),
          DateTime.now().plusYears(10)
        )
      val impossibleInterval =
        new Interval(
          new DateTime(0),
          new DateTime(Long.MaxValue)
        )
      val instances =
        Gen
          .listOfN(countInstances, instanceGen(externalId))
          .suchThat(_.map(_.updateTime).toSet.size == countInstances)
          .next

      instances.foreach(instanceArchiveDao.upsert(_).futureValue)

      instanceArchiveDao.get(externalId, impossibleInterval, -1).shouldCompleteWithException[RuntimeException]

      (0 to countInstances).foreach { l =>
        instanceArchiveDao.get(externalId, hugeInterval, l).futureValue.size should be(l)
      }
    }
  }

  private def getInterval(instance: Instance): Interval =
    new Interval(
      instance.updateTime.minusMinutes(1),
      instance.updateTime.plusMinutes(1)
    )
}
