package ru.yandex.vertis.promocoder.tasks

import org.joda.time.Seconds
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.promocoder.AsyncSpecBase
import ru.yandex.vertis.promocoder.dao.impl.jdbc.config.dualdb.PlainDualDatabase
import ru.yandex.vertis.promocoder.dao.impl.jdbc.{
  JdbcContainerSpecTemplate,
  OrmAutoProlongFeatureDao,
  OrmFeatureInstanceArchiveDao,
  OrmFeatureInstanceDao
}
import ru.yandex.vertis.promocoder.model.FeatureInstance.AutoProlongOrigin
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.AutoProlongFeatureService.All
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Filter.ByOrigin
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Mode
import ru.yandex.vertis.promocoder.service.impl.{AutoProlongFeatureServiceImpl, FeatureInstanceServiceImpl}
import ru.yandex.vertis.promocoder.util.{AutomatedContext, TimeService}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.annotation.nowarn
import scala.concurrent.Future

/** @author ruslansd
  */
class AutoProlongFeatureTaskIntSpec
  extends AnyWordSpecLike
  with Matchers
  with AsyncSpecBase
  with BeforeAndAfterEach
  with JdbcContainerSpecTemplate
  with ModelGenerators {

  implicit private val ac = AutomatedContext("AutoProlongFeatureTaskIntSpec")

  @nowarn("msg=discarded non-Unit value")
  override def beforeEach(): Unit = {
    super.beforeEach()
    prolongService
      .read(All)
      .flatMap { afs =>
        Future.sequence(afs.map(_.id).map(prolongService.disable))
      }
      .futureValue
  }

  private val prolongService = {
    val dao = new OrmAutoProlongFeatureDao(database)
    new AutoProlongFeatureServiceImpl(dao)
  }

  private val featureService = {
    val dualDatabase = PlainDualDatabase(database, database)
    val dao = new OrmFeatureInstanceDao(dualDatabase, new TimeService())
    val archive = new OrmFeatureInstanceArchiveDao(dualDatabase)
    new FeatureInstanceServiceImpl(dao, archive)
  }

  private val task = new AutoProlongFeatureTask(prolongService, featureService)

  "AutoProlongFeatureTask" should {
    "do nothing on empty prolong features" in {
      task.execute().futureValue
    }

    "prolong feature if need" in {
      val prolongFeature = AutoProlongFeatureGen.next.copy(nextProlong = DateTimeUtil.now().withTimeAtStartOfDay())

      prolongService.create(prolongFeature).futureValue
      task.execute().futureValue

      val features = featureService.get(ByOrigin(AutoProlongOrigin(prolongFeature.id)), Mode.Default).futureValue

      features.size shouldBe prolongFeature.featureContents.size
      features.foreach { f =>
        f.startTime.isDefined shouldBe true
        f.startTime.get shouldBe prolongFeature.nextProlong
        Seconds.secondsBetween(f.startTime.get, f.deadline).getSeconds shouldBe prolongFeature.prolongPeriod.toSeconds
      }
    }

    "prolong features" in {
      val prolongFeatures = AutoProlongFeatureGen.next(5)

      Future.sequence(prolongFeatures.map(prolongService.create)).futureValue
      task.execute().futureValue

      prolongFeatures.foreach { af =>
        val features = featureService.get(ByOrigin(AutoProlongOrigin(af.id)), Mode.Default).futureValue
        features.foreach { f =>
          f.startTime.isDefined shouldBe true
          Seconds.secondsBetween(f.startTime.get, f.deadline).getSeconds shouldBe af.prolongPeriod.toSeconds
          f.deadline.isAfter(DateTimeUtil.now()) shouldBe true
          f.startTime.get.isAfter(DateTimeUtil.now()) shouldBe false
        }
        features.size shouldBe af.featureContents.size
      }
    }

    "not prolong future feature" in {
      val tomorrow = DateTimeUtil.now().plusDays(1).withTimeAtStartOfDay()
      val prolongFeature = AutoProlongFeatureGen.next.copy(startFrom = tomorrow, nextProlong = tomorrow)

      prolongService.create(prolongFeature).futureValue
      task.execute().futureValue

      val features = featureService.get(ByOrigin(AutoProlongOrigin(prolongFeature.id)), Mode.Default).futureValue

      features shouldBe Seq.empty
    }

    "not prolong disabled feature" in {
      val prolongFeature = AutoProlongFeatureGen.next

      prolongService.create(prolongFeature).futureValue
      prolongService.disable(prolongFeature.id).futureValue
      task.execute().futureValue

      val features = featureService.get(ByOrigin(AutoProlongOrigin(prolongFeature.id)), Mode.Default).futureValue

      features shouldBe Seq.empty
    }

  }
}
