import OHHTTPStubs
import XCTest

@testable import BeruServices

final class FutureBonusesByPromoGroupTokenTests: CoinsServiceTest {

    func test_shouldSendProperRequestAndReturnBonuses_whenRequestSucceeded() {
        // given
        let token = "EFIM-1"
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]

            let promoGroupToken = params?.first?["promoGroupToken"] as? String

            return promoGroupToken == token
        }
        let expectedData: [Bonus] = [
            Bonus(
                id: "fd1993b12f37419188508120f4e0eeb7",
                title: "Купон на 100 ₽",
                subtitle: "на ботинки",
                bindingStatus: CoinBindingStatus(isAvailable: true, reason: .undefined, activationCode: "efim0102"),
                nominal: 100
            ),
            Bonus(
                id: "b0d28f1d5ac15261c22b13bc42eb3854",
                title: "Купон на 200 ₽",
                subtitle: "на стиральные порошки",
                bindingStatus: CoinBindingStatus(isAvailable: true, reason: .undefined, activationCode: "efim0103"),
                nominal: 200
            ),
            Bonus(
                id: "6cbe860d15931e036444c1cb9c8fa806",
                title: "Купон на 150 ₽",
                subtitle: "на зубные щетки",
                bindingStatus: CoinBindingStatus(isAvailable: true, reason: .undefined, activationCode: "efim0105"),
                nominal: 150
            )
        ]

        stub(
            requestPartName: "resolveFutureBonusesByPromoGroupToken",
            responseFileName: "obtain_future_bonuses",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveFutureBonusesByPromoGroupToken"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService.futureBonusesByPromoGroupToken(token: token).expect(in: self)

        // then
        guard case let .success(bonuses) = result else {
            XCTFail("Wrong response for `resolveFutureBonusesByPromoGroupToken` resolver. Model cannot be nil")
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
