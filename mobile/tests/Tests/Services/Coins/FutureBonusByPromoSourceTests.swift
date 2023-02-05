import OHHTTPStubs
import XCTest

@testable import BeruServices

final class FutureBonusByPromoSourceTests: CoinsServiceTest {

    func test_shouldSendProperRequestAndReturnBonus_whenRequestSucceeded() {
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
                id: "296be6bb93893c35ad5884f9f9eb2e0d",
                title: "Скидка 700 ₽",
                subtitle: "на чайничек",
                bindingStatus: CoinBindingStatus(
                    isAvailable: true,
                    reason: .undefined,
                    activationCode: promoSource
                ),
                nominal: 700
            )
        ]

        stub(
            requestPartName: "resolveFutureBonusByPromoSource",
            responseFileName: "get_future_bonus",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveFutureBonusByPromoSource"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService
            .checkFutureBonus(promoSource: promoSource)
            .expect(in: self)

        // then
        guard case let .success(bonuses) = result else {
            XCTFail("Wrong response for `resolveFutureBonusByPromoSource` resolver. Model cannot be nil")
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

    func test_shouldReturnUnavailableBonus_whenBonusIsOutOfBudget() {
        // given
        let promoSource = "efim-test-004"
        let expectedData: [Bonus] = [
            Bonus(
                id: "afe83abd2beb0c12fd3bce34941d1041",
                title: "Бесплатная доставка",
                subtitle: "на туалетную бумагу",
                bindingStatus: CoinBindingStatus(
                    isAvailable: false,
                    reason: .outOfBudget,
                    activationCode: promoSource
                ),
                nominal: nil
            )
        ]

        stub(
            requestPartName: "resolveFutureBonusByPromoSource",
            responseFileName: "bonus_is_over"
        )

        // when
        let result = coinsService
            .checkFutureBonus(promoSource: promoSource)
            .expect(in: self)

        // then
        guard case let .success(bonuses) = result else {
            XCTFail("Wrong response for `resolveFutureBonusByPromoSource` resolver. Model cannot be nil")
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
}
