package ru.yandex.vertis.promocoder.service

//import org.scalacheck.ShrinkLowPriority
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.promocoder.WordSpecBase
import ru.yandex.vertis.promocoder.model.FeatureInstance
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.{Filter, Mode}
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.util.Random

/** @author alex-kovalenko
  */
trait FeatureInstanceArchiveServiceSpec
  extends WordSpecBase
  with ModelGenerators
  with ScalaCheckPropertyChecks /*
  with ShrinkLowPriority */ {

  implicit val rc: RequestContext = AutomatedContext("FeatureInstanceArchiveServiceSpec")

  def services(
      features: Iterable[FeatureInstance],
      archived: Iterable[FeatureInstance]
    ): (FeatureInstanceService, FeatureInstanceArchiveService)

  "FeatureInstanceArchiveService" should {
    "archive expired features" in {
      forAll(uniqueFeatureInstanceGen(50)) { features =>
        val deadline = features.map(_.deadline).apply(Random.nextInt(features.size))
        val (service, archive) = services(features, Iterable.empty)

        service.get(Filter.All, Mode.Default).futureValue should contain theSameElementsAs features
        service.get(Filter.All, Mode.Archive).futureValue shouldBe empty

        archive.archiveExpired(deadline).futureValue

        val (expired, active) = features.partition(_.deadline.isBefore(deadline))
        service.get(Filter.All, Mode.Default).futureValue should contain theSameElementsAs active
        service.get(Filter.All, Mode.Archive).futureValue should contain theSameElementsAs expired
      }
    }

    "archive used features" in {
      forAll(uniqueFeatureInstanceGen(100)) { featureSources =>
        val (active, used) = {
          val (active, expired) = featureSources.splitAt(featureSources.size / 2)
          (active, expired.map(_.copy(count = 0)))
        }
        val (service, archive) = services(active ++ used, Iterable.empty)

        service.get(Filter.All, Mode.Default).futureValue should contain theSameElementsAs (active ++ used)
        service.get(Filter.All, Mode.Archive).futureValue shouldBe empty

        archive.archiveUsed(DateTimeUtil.now().plus(100)).futureValue

        service.get(Filter.All, Mode.Default).futureValue should contain theSameElementsAs active
        service.get(Filter.All, Mode.Archive).futureValue should contain theSameElementsAs used
      }
    }

    "upsert in archive" in {
      forAll(uniqueFeatureInstanceGen(10)) { features =>
        val deadline = features.map(_.deadline).max.plus(1)

        val (service, archive) = services(features, features)
        service.get(Filter.All, Mode.Default).futureValue should contain theSameElementsAs features
        service.get(Filter.All, Mode.Archive).futureValue should contain theSameElementsAs features

        archive.archiveExpired(deadline).futureValue

        service.get(Filter.All, Mode.Default).futureValue shouldBe empty
        service.get(Filter.All, Mode.Archive).futureValue should contain theSameElementsAs features
      }
    }
  }

}
