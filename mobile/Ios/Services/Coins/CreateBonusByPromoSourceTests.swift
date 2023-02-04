import OHHTTPStubs
import XCTest

@testable import BeruServices

final class CreateBonusByPromoSourceTests: CoinsServiceTest {

    func test_shouldSendProperRequestAndReturnCreatedBonus_whenRequestSucceeded() {
        // given
        let promoSource = "chaynichek-09-04"
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]

            let isEmulator = params?.first?["isEmulator"] as? Bool
            let deviceId = params?.first?["deviceId"] as? UIDevice.CompositeDeviceId
            let source = params?.first?["source"] as? String
            let idempotencyKey = params?.first?["idempotencyKey"] as? String

            return isEmulator == true
                && deviceId == UIDevice.deviceId
                && source == promoSource
                && idempotencyKey != nil
        }
        let expectedData: [Bonus] = [
            Bonus(
                id: "3857487",
                title: "Скидка 1 ₽",
                subtitle: "на любой заказ",
                bindingStatus: nil,
                nominal: 1
            )
        ]

        stub(
            requestPartName: "resolveCreateBonusByPromoSource",
            responseFileName: "create_new_bonus",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveCreateBonusByPromoSource"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService
            .makeBonus(promoSource: promoSource)
            .expect(in: self)

        // then
        guard case let .success(bonuses) = result else {
            XCTFail("Wrong response for `resolveCreateBonusByPromoSource` resolver. Model cannot be nil")
            return
        }

        XCTAssertEqual(bonuses.count, expectedData.count)
        zip(bonuses, expectedData).forEach { responseBonus, expectedBonus in
            XCTAssertEqual(responseBonus.coinId, expectedBonus.id)
            XCTAssertEqual(responseBonus.title, expectedBonus.title)
            XCTAssertEqual(responseBonus.subtitle, expectedBonus.subtitle)
            XCTAssertEqual(responseBonus.bindingStatus, expectedBonus.bindingStatus)
            XCTAssertEqual(responseBonus.nominal, expectedBonus.nominal)
        }
    }

    func test_shouldReturnProperError_whenReceiveEmulatorError() {
        // given
        let promoSource = "chaynichek-09-04"
        stub(
            requestPartName: "resolveCreateBonusByPromoSource",
            responseFileName: "emulator_error"
        )

        // when
        let result = coinsService
            .makeBonus(promoSource: promoSource)
            .expect(in: self)

        // then
        guard case let .failure(error as BonusCreationError) = result else {
            XCTFail("Can't be successfull with emulator error")
            return
        }

        XCTAssertEqual(error, BonusCreationError.emulator)
    }

    func test_shouldReturnProperError_whenBudgetExceeded() {
        // given
        let promoSource = "chaynichek-09-04"
        stub(
            requestPartName: "resolveCreateBonusByPromoSource",
            responseFileName: "budget_exceeded_error"
        )

        // when
        let result = coinsService
            .makeBonus(promoSource: promoSource)
            .expect(in: self)

        // then
        guard case let .failure(error as BonusCreationError) = result else {
            XCTFail("Can't be successfull with budget exceeded error")
            return
        }

        XCTAssertEqual(error, BonusCreationError.budgetExceeded)
    }
}
