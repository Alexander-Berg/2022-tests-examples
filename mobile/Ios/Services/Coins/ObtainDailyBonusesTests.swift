import OHHTTPStubs
import XCTest

@testable import BeruServices

final class ObtainDailyBonusesTests: CoinsServiceTest {

    func test_shouldSendProperRequestAndReturnBonuses() {
        // given
        let expectedData: [Bonus] = [
            Bonus(
                id: "QjiRsPqhvYg8iNv68eIrbQ",
                title: "",
                subtitle: "",
                bindingStatus: CoinBindingStatus(
                    isAvailable: true,
                    reason: .undefined,
                    activationCode: "ygbhjc-23"
                ),
                nominal: 20,
                isHiddenUntilBound: false,
                isForPlus: false
            ),
            Bonus(
                id: "yr5o4IZSJBZzf--TppHD3g",
                title: "",
                subtitle: "",
                bindingStatus: CoinBindingStatus(
                    isAvailable: false,
                    reason: .promoExpired,
                    activationCode: "ixdmldj-12"
                ),
                nominal: 20,
                isHiddenUntilBound: false,
                isForPlus: false
            )
        ]

        stub(
            requestPartName: "resolveDailyBonuses",
            responseFileName: "obtain_daily_bonuses",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveDailyBonuses"])
        )

        // when
        let result = coinsService
            .obtainDailyBonuses()
            .expect(in: self)

        // then
        guard case let .success(response) = result else {
            XCTFail("Wrong response for `resolveDailyBonuses` resolver. Model cannot be nil")
            return
        }

        XCTAssertEqual(response.coins.count, expectedData.count)
        zip(response.coins, expectedData).forEach { responseBonus, expectedBonus in
            XCTAssertEqual(responseBonus.coinId, expectedBonus.id)
            XCTAssertEqual(responseBonus.title, expectedBonus.title)
            XCTAssertEqual(responseBonus.subtitle, expectedBonus.subtitle)
            XCTAssertEqual(responseBonus.bindingStatus, expectedBonus.bindingStatus)
            XCTAssertEqual(responseBonus.nominal, expectedBonus.nominal)
            XCTAssertEqual(responseBonus.isHiddenUntilBound, expectedBonus.isHiddenUntilBound)
            XCTAssertEqual(responseBonus.isForPlus, expectedBonus.isForPlus)
        }
    }
}
