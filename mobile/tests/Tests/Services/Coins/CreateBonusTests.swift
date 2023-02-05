import OHHTTPStubs
import XCTest

@testable import BeruServices

class CreateBonusTests: CoinsServiceTest {

    func test_shouldReceiveProperCoin() {
        // given
        let promoId = 10_611
        let idempotencyKey = UUID().uuidString
        let reason = YBMCoinCreationReason.forUserAction

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]
            let promoId = params?.first?["promoId"] as? Int
            let isEmulator = params?.first?["isEmulator"] as? Bool
            let reason = params?.first?["reason"] as? String
            return promoId == 10_611 && isEmulator == true && reason == "FOR_USER_ACTION"
        }

        stub(
            requestPartName: "resolveCreateBonusByPromoId",
            responseFileName: "simple_create_bonus",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveCreateBonusByPromoId"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService
            .makeBonus(
                promoId: promoId,
                idempotencyKey: idempotencyKey,
                reason: reason
            )
            .expect(in: self)

        // then
        switch result {
        case let .success(responseObject):
            XCTAssertEqual(responseObject.count, 1)
        default:
            XCTFail("Wrong create coin result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let promoId = 10_611
        let idempotencyKey = UUID().uuidString
        let reason = YBMCoinCreationReason.forUserAction
        stubError(requestPartName: "resolveCreateBonusByPromoId", code: 500)

        // when
        let result = coinsService
            .makeBonus(
                promoId: promoId,
                idempotencyKey: idempotencyKey,
                reason: reason
            )
            .expect(in: self)

        // then
        guard case let .failure(error as ApiClientError) = result else {
            XCTFail("Can't be successfull with 500 response")
            return
        }

        guard case let .network(response: response, _, _, _) = error else {
            XCTFail("Wrong type of error")
            return
        }

        XCTAssertEqual(response?.statusCode, 500)
    }

    func test_shouldReturnProperError_whenReceiveEmulatorError() {
        // given
        let promoId = 10_611
        let idempotencyKey = UUID().uuidString
        let reason = YBMCoinCreationReason.forUserAction
        stub(
            requestPartName: "resolveCreateBonusByPromoId",
            responseFileName: "emulator_error"
        )

        // when
        let result = coinsService
            .makeBonus(
                promoId: promoId,
                idempotencyKey: idempotencyKey,
                reason: reason
            )
            .expect(in: self)

        // then
        guard case let .failure(error as BonusCreationError) = result else {
            XCTFail("Can't be successfull with emulator error")
            return
        }

        XCTAssertEqual(error, BonusCreationError.emulator)
    }

    func test_shouldReturnProperError_whenReceiveLoyaltyError() {
        // given
        let promoId = 10_611
        let idempotencyKey = UUID().uuidString
        let reason = YBMCoinCreationReason.forUserAction
        stub(
            requestPartName: "resolveCreateBonusByPromoId",
            responseFileName: "loyalty_error"
        )

        // when
        let result = coinsService
            .makeBonus(
                promoId: promoId,
                idempotencyKey: idempotencyKey,
                reason: reason
            )
            .expect(in: self)

        // then
        guard case let .failure(error as BonusCreationError) = result else {
            XCTFail("Can't be successfull with emulator error")
            return
        }

        XCTAssertEqual(error, BonusCreationError.promoNotFound)
    }
}
