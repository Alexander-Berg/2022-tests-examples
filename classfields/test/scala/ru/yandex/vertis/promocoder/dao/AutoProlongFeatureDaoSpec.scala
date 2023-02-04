package ru.yandex.vertis.promocoder.dao

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.promocoder.AsyncSpecBase
import ru.yandex.vertis.promocoder.model.AutoProlongFeature
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.AutoProlongFeatureService.{All, ForUser, WithNextProlongation}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

/** @author ruslansd
  */
trait AutoProlongFeatureDaoSpec extends DaoSpecBase with BeforeAndAfterEach with ModelGenerators with AsyncSpecBase {

  protected def dao: AutoProlongFeatureDao with CleanableDao

  override def beforeEach(): Unit = {
    super.beforeEach()
    dao.clean.futureValue
  }

  "AutoProlongFeatureDao" should {
    "create once autoProlongFeature" in {
      val feature = AutoProlongFeatureGen.next
      dao.create(feature).futureValue

      dao.read(All).futureValue shouldBe Seq(feature)

      intercept[IllegalArgumentException] {
        ExtendedFutureConcept(dao.create(feature)).await
      }
    }

    "upsert features" in {
      val feature = AutoProlongFeatureGen.next

      dao.upsert(feature).futureValue

      dao.read(All).futureValue shouldBe Seq(feature)

      val updatedFeature = feature.copy(startFrom = feature.startFrom.plusDays(1))
      dao.upsert(updatedFeature).futureValue

      dao.read(All).futureValue shouldBe Seq(updatedFeature)
    }

    "disable feature" in {
      val feature = AutoProlongFeatureGen.next

      dao.upsert(feature).futureValue

      dao.read(All).futureValue shouldBe Seq(feature)
      dao.disable(feature.id)

      dao.read(All).futureValue shouldBe Seq.empty
    }

    "read next prolongation" in {
      val tomorrow = DateTimeUtil.now().withTimeAtStartOfDay().plusDays(1)
      val features = AutoProlongFeatureGen
        .next(5)
        .map(_.copy(nextProlong = tomorrow))

      features.foreach(f => dao.create(f).futureValue)

      dao.read(WithNextProlongation(tomorrow.minus(1))).futureValue shouldBe Seq.empty

      dao.read(WithNextProlongation(tomorrow)).futureValue should contain theSameElementsAs features

      Future.sequence(features.map(_.id).map(dao.disable)).futureValue

      dao.read(WithNextProlongation(tomorrow)).futureValue shouldBe Seq.empty

    }

    "read by user" in {
      val features = AutoProlongFeatureGen.next(5)
      Future.sequence(features.map(dao.create)).futureValue

      val user = features.head.user
      val expected = features.filter(_.user == user)

      val actual = dao.read(ForUser(user)).futureValue

      actual should contain theSameElementsAs expected
    }
  }

}
