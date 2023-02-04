package ru.auto.salesman.service.impl

import org.scalacheck.Gen
import org.scalatest.Matchers
import ru.auto.salesman.dao.AutostrategiesDao
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._

import scala.util.Random

class AutostrategiesServiceImplSpec extends BaseSpec with Matchers {

  private val autostrategiesDao = mock[AutostrategiesDao]

  private val service = new AutostrategiesServiceImpl(autostrategiesDao)

  "Autostrategies service" should {

    "get autostrategies successfully" in {
      forAll(Gen.listOf(AutoruOfferIdGen), AutostrategyListGen) {
        (offerIds, autostrategies) =>
          (autostrategiesDao.get _).expects(offerIds).returningZ(autostrategies)
          val allOfferAutostrategies = service.get(offerIds).success.value
          for (autostrategy <- autostrategies) {
            val offerAutostrategies =
              allOfferAutostrategies.filter(_.offerId == autostrategy.offerId)
            offerAutostrategies should have size 1
            offerAutostrategies.head.autostrategies should contain(autostrategy)
          }
          allOfferAutostrategies.flatMap(
            _.autostrategies
          ) should contain theSameElementsAs autostrategies
      }
    }

    "get only autostrategies that didn't created before current date" in {
      forAll(
        Gen.listOf(AutoruOfferIdGen),
        AutostrategyListGen,
        OldAutostrategyListGen
      ) { (offerIds, autostrategies, oldAutostrategies) =>
        val oldFiltered = oldAutostrategies.filterNot(autostrategy =>
          autostrategies.exists(_.offerId == autostrategy.offerId)
        )
        val daoAutostrategies = Random.shuffle(autostrategies ++ oldFiltered)
        (autostrategiesDao.get _)
          .expects(offerIds)
          .returningZ(daoAutostrategies)
        val allOfferAutostrategies = service.get(offerIds).success.value
        for (autostrategy <- autostrategies) {
          val offerAutostrategies =
            allOfferAutostrategies.filter(_.offerId == autostrategy.offerId)
          offerAutostrategies should have size 1
          offerAutostrategies.head.autostrategies should contain(autostrategy)
        }
        for (autostrategy <- oldFiltered)
          allOfferAutostrategies.exists(
            _.offerId == autostrategy.offerId
          ) shouldBe false
        allOfferAutostrategies.flatMap(
          _.autostrategies
        ) should contain theSameElementsAs autostrategies
      }
    }

    "fail getting on dao fail" in {
      forAll(Gen.listOf(AutoruOfferIdGen)) { offerIds =>
        (autostrategiesDao.get _)
          .expects(offerIds)
          .throwingZ(new RuntimeException)
        service.get(offerIds).failure.exception shouldBe a[RuntimeException]
      }
    }

    "put autostrategies successfully" in {
      forAll(AutostrategyListGen) { autostrategies =>
        (autostrategiesDao.put _).expects(autostrategies).returningZ(())
        service.put(autostrategies).success.value shouldBe (())
      }
    }

    "fail putting on dao fail" in {
      forAll(AutostrategyListGen) { autostrategies =>
        (autostrategiesDao.put _)
          .expects(autostrategies)
          .throwingZ(new RuntimeException)
        service
          .put(autostrategies)
          .failure
          .exception shouldBe a[RuntimeException]
      }
    }

    "delete autostrategies successfully" in {
      forAll(AutostrategyIdListGen) { ids =>
        (autostrategiesDao.delete _).expects(ids).returningZ(())
        service.delete(ids).success.value shouldBe (())
      }
    }

    "fail deleting on dao fail" in {
      forAll(AutostrategyIdListGen) { ids =>
        (autostrategiesDao.delete _)
          .expects(ids)
          .throwingZ(new RuntimeException)
        service.delete(ids).failure.exception shouldBe a[RuntimeException]
      }
    }
  }
}
