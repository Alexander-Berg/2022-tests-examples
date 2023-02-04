package ru.auto.api.managers.autostrategies

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.OfferID
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.util.OwnershipChecker
import ru.yandex.vertis.mockito.MockitoSupport

class AutostrategiesManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with TestRequest {

  private val salesmanClient = mock[SalesmanClient]
  private val ownershipChecker = mock[OwnershipChecker]
  private val autostrategiesManager = new AutostrategiesManager(salesmanClient, ownershipChecker)

  "Autostrategies manager" should {

    "put autostrategies" in {
      forAll(Gen.nonEmptyListOf(AutostrategyGen)) { autostrategies =>
        mockCheckOwnership(autostrategies.map(_.getOfferId).map(OfferID.parse))
        when(salesmanClient.putAutostrategies(eq(autostrategies))(?)).thenReturnF(())
        val result = autostrategiesManager.putAutostrategies(autostrategies).futureValue
        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }

    "delete autostrategies" in {
      forAll(Gen.nonEmptyListOf(AutostrategyIdGen)) { ids =>
        mockCheckOwnership(ids.map(_.getOfferId).map(OfferID.parse))
        when(salesmanClient.deleteAutostrategies(eq(ids))(?)).thenReturnF(())
        val result = autostrategiesManager.deleteAutostrategies(ids).futureValue
        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }

    "fail to put autostrategies" in {
      forAll(Gen.nonEmptyListOf(AutostrategyGen)) { autostrategies =>
        mockFailedCheckOwnership(autostrategies.map(_.getOfferId).map(OfferID.parse))
        val result = autostrategiesManager.putAutostrategies(autostrategies).failed.futureValue
        result shouldBe an[OfferNotFoundException]
      }
    }

    "fail to delete autostrategies" in {
      forAll(Gen.nonEmptyListOf(AutostrategyIdGen)) { ids =>
        mockFailedCheckOwnership(ids.map(_.getOfferId).map(OfferID.parse))
        val result = autostrategiesManager.deleteAutostrategies(ids).failed.futureValue
        result shouldBe an[OfferNotFoundException]
      }
    }
  }

  private def mockCheckOwnership(offerIds: List[OfferID]): Unit = {
    when(ownershipChecker.checkOwnership(eq(offerIds))(?)).thenReturnF(())
  }

  private def mockFailedCheckOwnership(offerIds: List[OfferID]): Unit = {
    when(ownershipChecker.checkOwnership(eq(offerIds))(?)).thenThrowF(new OfferNotFoundException)
  }
}
