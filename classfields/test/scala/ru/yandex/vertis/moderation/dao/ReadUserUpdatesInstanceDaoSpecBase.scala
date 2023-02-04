package ru.yandex.vertis.moderation.dao

import cats.effect.{Async, ContextShift, IO}
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.impl.ReadUserUpdatesInstanceDao
import ru.yandex.vertis.moderation.dao.impl.inmemory.{InMemoryInstanceDao, InMemoryStorageImpl}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, Instance}
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.model.signal.{SignalSet, Tombstone}
import ru.yandex.vertis.moderation.proto.Model.Service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author akhazhoyan 06/2019
  */
@RunWith(classOf[JUnitRunner])
trait ReadUserUpdatesInstanceDaoSpecBase extends SpecBase {

  def cache: UserUpdatesCache

  val InstanceGen: Gen[Instance] =
    CoreGenerators.InstanceGen.map(
      _.copy(
        metadata = MetadataSet.Empty
      )
    )

  private val memoryStorageImpl = new InMemoryStorageImpl

  lazy val instanceDaoImpl: InstanceDao[Future] =
    new InMemoryInstanceDao(
      service = Service.REALTY,
      storage = memoryStorageImpl
    )

  lazy val instanceDao: InstanceDao[Future] =
    new DelegateInstanceDao(instanceDaoImpl) with ReadUserUpdatesInstanceDao {
      override protected def userUpdatesCache: UserUpdatesCache = cache

      implicit override protected def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      implicit override val contextShift: ContextShift[IO] = IO.contextShift(ec)
    }

  before {
    memoryStorageImpl.clear()
  }

  private case class TestCase(description: String, modify: Instance => Instance)

  private val testCases =
    Seq(
      TestCase(
        "append signals",
        { instance =>
          val signalToAppend =
            signalGen(instance.service).withoutSwitchOff.next.withTimestamp(
              instance.updateTime.plus(100)
            )
          instance.copy(signals = instance.signals ++ SignalSet(signalToAppend))
        }
      ),
      TestCase(
        "add switchOff",
        { instance =>
          val signalKey = SignalGen.map(_.key).next
          val signalToAppend = SignalSwitchOffGen.next.copy(timestamp = instance.updateTime.plus(100))
          instance.copy(signals = instance.signals.withSwitchOffs(Map(signalKey -> signalToAppend)))
        }
      ),
      TestCase(
        "delete signals",
        { instance =>
          val timestamp = instance.updateTime.plus(100)
          val signalKeys =
            SignalGen
              .map(_.key)
              .next(2)
              .map(key => key -> Tombstone(timestamp, None))
              .toMap
          instance.copy(signals = instance.signals.withSignalTombstones(signalKeys))
        }
      ),
      TestCase(
        "delete switchOff",
        { instance =>
          val timestamp = instance.updateTime.plus(100)
          val signalKeysMap =
            SignalGen
              .map(_.key)
              .next(2)
              .map(key => key -> Tombstone(timestamp, None))
              .toMap
          instance.copy(signals = instance.signals.withSwitchOffTombstones(signalKeysMap))
        }
      )
    )

  "ReadUserUpdatesInstanceDao" should {

    testCases.foreach { case TestCase(description, modify) =>
      s"correctly merge values if cache is obsolete on $description" in {
        val oldInstance = InstanceGen.next
        val externalId = oldInstance.externalId
        val newInstance = modify(oldInstance)

        updateInstanceDao(newInstance)
        cache.set(externalId, oldInstance, 20.minutes).futureValue

        val actualResult = instanceDao.getOpt(externalId, allowExpired = false).futureValue
        actualResult shouldBe Some(newInstance)
      }

      s"correctly merge values if storage is obsolete on $description" in {
        val oldInstance = InstanceGen.next
        val externalId = oldInstance.externalId
        val newInstance = modify(oldInstance)

        updateInstanceDao(newInstance)
        cache.set(externalId, newInstance, 20.minutes).futureValue

        val actualResult = instanceDao.getOpt(externalId, allowExpired = false).futureValue
        actualResult shouldBe Some(newInstance)
      }
    }
  }

  private def updateInstanceDao(newInstance: Instance): Unit = {
    instanceDao.upsert(EssentialsPatch.fromInstance(newInstance)).futureValue
    instanceDao.updateContext(newInstance.id, newInstance.context).futureValue
    instanceDao
      .changeSignalsAndSwitchOffs(
        newInstance.id,
        newInstance.signals.signalMap,
        newInstance.signals.switchOffMap,
        SignalSet.Empty
      )
      .futureValue
  }
}
