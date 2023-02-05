import MarketProtocols
import OHHTTPStubs
import XCTest

@testable import BeruServices

class ObtainBonusAchievabilityTests: CoinsServiceTest {

    func test_shouldSendProperRequest() {
        // given
        let promoId = CoinPromoType.firstLogin.identifier(isProduction: true)
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]
            let promoId = params?.first?["promoId"] as? Int
            return promoId == 11_283
        }

        stub(
            requestPartName: "resolveWillCreateBonus",
            responseFileName: "obtain_bonus_achievability_true",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveWillCreateBonus"]) &&
                verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService.obtainBonusAchievability(promoId: promoId).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldSendProperRequest_whenTestingEndpoint() {
        // given
        let promoId = CoinPromoType.firstLogin.identifier(isProduction: false)
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]
            let promoId = params?.first?["promoId"] as? Int
            return promoId == 10_603
        }

        stub(
            requestPartName: "resolveWillCreateBonus",
            responseFileName: "obtain_bonus_achievability_true",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService.obtainBonusAchievability(promoId: promoId).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReceiveBonusAchievabilityTrue() {
        // given
        let promoId = CoinPromoType.firstLogin.identifier(isProduction: true)
        stub(
            requestPartName: "resolveWillCreateBonus",
            responseFileName: "obtain_bonus_achievability_true"
        )

        // when
        let result = coinsService.obtainBonusAchievability(promoId: promoId).expect(in: self)

        // then
        switch result {
        case let .success(isBonusAchievable):
            XCTAssertTrue(isBonusAchievable)
        default:
            XCTFail("Wrong obtain achievability result \(String(describing: result))")
        }
    }

    func test_shouldReceiveBonusAchievabilityFalse() {
        // given
        let promoId = CoinPromoType.firstLogin.identifier(isProduction: true)
        stub(
            requestPartName: "resolveWillCreateBonus",
            responseFileName: "obtain_bonus_achievability_false"
        )

        // when
        let result = coinsService.obtainBonusAchievability(promoId: promoId).expect(in: self)

        // then
        switch result {
        case let .success(isBonusAchievable):
            XCTAssertFalse(isBonusAchievable)
        default:
            XCTFail("Wrong obtain achievability result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let promoId = CoinPromoType.firstLogin.identifier(isProduction: true)
        stubError(requestPartName: "resolveWillCreateBonus", code: 500)

        // when
        let result = coinsService.obtainBonusAchievability(promoId: promoId).expect(in: self)

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
}
