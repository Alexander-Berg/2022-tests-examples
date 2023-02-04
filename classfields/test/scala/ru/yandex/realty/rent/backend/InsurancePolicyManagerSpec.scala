package ru.yandex.realty.rent.backend

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.backend.InsurancePolicyManager.InsuranceType
import ru.yandex.realty.rent.clients.mango.MangoClient
import ru.yandex.realty.rent.clients.mango.model.{FlatInsurancePolicyRequest, FlatInsurancePolicySubscription}
import ru.yandex.realty.rent.dao.FlatDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TestDataSettings

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class InsurancePolicyManagerSpec extends AsyncSpecBase with RentPaymentsData with RentModelsGen {

  implicit private val traced: Traced = Traced.empty

  private val RentStartDate = TodayDate
  private val Contract = createContract(rentStartDate = RentStartDate, paymentDayOfMonth = RentStartDate.getDayOfMonth)
  private val PolicySubscriptions =
    FlatInsurancePolicySubscription(landlordSubscriptionId = "ownerId", tenantSubscriptionId = "tenantId")

  "InsurancePolicyManager" should {
    "create current insurance policy by mango" in {
      val insurancePolicyManager = handleMocks("mango")
      val resultContract = insurancePolicyManager.createInsurancePolicy(Contract, InsuranceType.Current).futureValue

      resultContract.data.getInsurance.getTenantSubscriptionId should (not equal "")
      resultContract.data.getInsurance.getOwnerSubscriptionId should (not equal "")
      resultContract.data.getInsurance.hasPolicyDate shouldBe false
    }

    "create temporary policy by mango" in {
      val insurancePolicyManager = handleMocks("mango")
      val resultContract = insurancePolicyManager.createInsurancePolicy(Contract, InsuranceType.Temporary).futureValue

      resultContract.data.getTemporaryAmounts.getInsurance.getTenantSubscriptionId should (not equal "")
      resultContract.data.getTemporaryAmounts.getInsurance.getOwnerSubscriptionId should (not equal "")
      resultContract.data.getTemporaryAmounts.getInsurance.hasPolicyDate shouldBe false
    }

    "enrich contract with insurance data" in {
      val contractData = Contract.data
      val contractRentStartDate = contractData.getRentStartDate
      val insurancePolicyManager = handleMocks("enrich")
      val resultContractData =
        insurancePolicyManager.supplementContractByInsuranceData(
          contractData,
          contractRentStartDate
        )

      resultContractData.getInsurance.getPolicyDate shouldBe contractRentStartDate
    }
  }

  private def handleMocks(testCase: String) = {
    val mockFlatDao = mock[FlatDao]
    val mockMangoClient = mock[MangoClient]
    val mockTestDataSettings = mock[TestDataSettings]

    testCase match {
      case "mango" => handleMangoMocks(mockFlatDao, mockMangoClient, mockTestDataSettings)
      case "enrich" => createDefaultInsuranceManager(mockFlatDao, mockMangoClient, mockTestDataSettings)
    }
  }

  private def createDefaultInsuranceManager(
    mockFlatDao: FlatDao,
    mockMangoClient: MangoClient,
    mockTestDataSettings: TestDataSettings
  ): InsurancePolicyManager =
    new InsurancePolicyManager(mockFlatDao, mockMangoClient, mockTestDataSettings)

  private def handleMangoMocks(
    mockFlatDao: FlatDao,
    mockMangoClient: MangoClient,
    mockTestDataSettings: TestDataSettings
  ): InsurancePolicyManager = {
    (mockFlatDao
      .findById(_: String)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.successful(flatGen().next))
    (mockMangoClient
      .createFlatInsurancePolicy(_: FlatInsurancePolicyRequest)(_: Traced))
      .expects(*, Traced.empty)
      .once()
      .returning(Future.successful(PolicySubscriptions))
    (mockTestDataSettings
      .canDoSoftlyExpensiveThing(_: String))
      .expects(*)
      .once()
      .returning(true)

    new InsurancePolicyManager(mockFlatDao, mockMangoClient, mockTestDataSettings)
  }
}
