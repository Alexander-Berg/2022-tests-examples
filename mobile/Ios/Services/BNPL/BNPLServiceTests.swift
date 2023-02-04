import BeruLegacyNetworking
import BeruMapping
import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

final class BNPLServiceTests: NetworkingTestCase {

    private var bnplService: BNPLServiceImpl!

    private let amount = NSDecimalNumber(value: 14_150)
    private let currency: YMTPriceCurrency = .RUB
    private let orderId = 5_746_488

    override func setUp() {
        super.setUp()

        bnplService = BNPLServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    func test_shouldObtainPlanDetails_whenParametersAndResponseValid() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)

            guard
                let params = json["params"].array?.first,
                let amount = params["amount"].numberValue as? NSDecimalNumber,
                let currencyString = params["currency"].string,
                let currency = YMTPriceCurrency(string: currencyString)
            else { return false }

            return amount == self.amount && currency.rawValue == self.currency.rawValue
        }

        stub(
            requestPartName: "resolveBnplPlan",
            responseFileName: "obtain_bnpl_plan_details_results",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveBnplPlan"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = bnplService.obtainPlanDetails(
            forAmount: amount,
            currency: currency,
            isPaidSplitEnabled: false
        ).expect(in: self)

        // then
        switch result {
        case let .success(details):
            XCTAssertEqual(details.deposit, NSDecimalNumber(decimal: 254.9))
            XCTAssertEqual(details.payments.count, 4)
        default:
            XCTFail("Wrong order payment id \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenObtainPlanDetailsAndReceivedInvalidResponse() {
        // given
        stub(
            requestPartName: "resolveBnplPlan",
            responseFileName: "obtain_bnpl_plan_details_wrong_format"
        )

        // when
        let result = bnplService.obtainPlanDetails(
            forAmount: amount,
            currency: currency,
            isPaidSplitEnabled: false
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, case ServiceError.invalidResponseClass = error else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenObtainPlanDetailsAndErrorReceived() throws {
        // given
        stub(
            requestPartName: "resolveBnplPlan",
            responseFileName: "obtain_bnpl_plan_details_error"
        )

        // when
        let result = bnplService.obtainPlanDetails(
            forAmount: amount,
            currency: currency,
            isPaidSplitEnabled: false
        ).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }

    func test_shouldObtainPlanDetailsByOrderId_whenParametersAndResponseValid() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)

            guard
                let params = json["params"].array?.first,
                let orderId = params["orderId"].int
            else { return false }

            return orderId == self.orderId
        }

        stub(
            requestPartName: "resolveBnplPlanDetailsByOrderId",
            responseFileName: "obtain_bnpl_plan_details_by_order_id_results",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveBnplPlanDetailsByOrderId"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = bnplService.obtainPlanDetails(
            by: orderId,
            isPaidSplitEnabled: false
        ).expect(in: self)

        // then
        switch result {
        case let .success(details):
            XCTAssertEqual(details.deposit, NSDecimalNumber(decimal: 254.9))
            XCTAssertEqual(details.payments.count, 4)
        default:
            XCTFail("Wrong order payment id \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenObtainPlanDetailsByOrderIdAndReceivedInvalidResponse() {
        // given
        stub(
            requestPartName: "resolveBnplPlanDetailsByOrderId",
            responseFileName: "obtain_bnpl_plan_details_by_order_id_wrong_format"
        )

        // when
        let result = bnplService.obtainPlanDetails(
            by: orderId,
            isPaidSplitEnabled: false
        ).expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, case ServiceError.invalidResponseClass = error else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenObtainPlanDetailsByOrderIdAndErrorReceived() throws {
        // given
        stub(
            requestPartName: "resolveBnplPlanDetailsByOrderId",
            responseFileName: "obtain_bnpl_plan_details_by_order_id_error"
        )

        // when
        let result = bnplService.obtainPlanDetails(
            by: orderId,
            isPaidSplitEnabled: false
        ).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }

    func test_shouldObtainUserStatistics_whenParametersAndResponseValid() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)

            guard
                let params = json["params"].array?.first,
                params.isEmpty
            else { return false }

            return true
        }

        stub(
            requestPartName: "resolveBnplUserStat",
            responseFileName: "obtain_user_statistics_results",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveBnplUserStat"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = bnplService.obtainUserStatistics().expect(in: self)

        // then
        switch result {
        case let .success(userStatistics):
            XCTAssertEqual(userStatistics.paymentsLeft, 3)
            XCTAssertEqual(userStatistics.accountUrl.absoluteString, "https://ya.ru")
            XCTAssertTrue(userStatistics.hasOverduePayment)
        default:
            XCTFail("Wrong data \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenObtainUserStatisticsAndReceivedInvalidResponse() {
        // given
        stub(
            requestPartName: "resolveBnplUserStat",
            responseFileName: "obtain_user_statistics_wrong_format"
        )

        // when
        let result = bnplService.obtainUserStatistics().expect(in: self)

        // then
        guard case let .failure(error as ServiceError) = result, error == ServiceError.invalidResponseClass() else {
            XCTFail("Incorrect error class")
            return
        }
    }

    func test_shouldReturnError_whenObtainUserStatisticsAndErrorReceived() throws {
        // given
        stub(
            requestPartName: "resolveBnplUserStat",
            responseFileName: "obtain_user_statistics_error"
        )

        // when
        let result = bnplService.obtainUserStatistics().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }
}
