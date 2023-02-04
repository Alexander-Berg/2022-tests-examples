import OHHTTPStubs
import XCTest
@testable import BeruServices

class ObtainCoinsTests: CoinsServiceTest {

    func test_shouldThrowUnauthorizedError_whenUserIsNotAuthenticated() throws {
        // given
        isAuthenticated = false
        var thrownError: Error?

        // when
        let result = coinsService.obtainCoins(for: []).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            thrownError = error
        }

        XCTAssertEqual(thrownError as? ServiceError, .unauthorized())
    }

    func test_shouldObtainResponse_whenSentValidRequest() throws {
        // given
        let expectedFeedID = 475_690
        let expectedOfferID = "200344841.10125"
        let expectedItemsCount = 1
        let expectedBundleID = UUID().uuidString

        let cartItemState = makeCartItemState(
            with: expectedFeedID,
            offerID: expectedOfferID,
            itemsCount: expectedItemsCount,
            bundleId: expectedBundleID
        )

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]
            let items = params?.first?["items"] as? [[AnyHashable: Any]]
            let item = items?.first

            let feedID = item?["feedId"] as? Int
            let offerID = item?["feedOfferId"] as? String
            let count = item?["count"] as? Int
            let bundleId = item?["bundleId"] as? String

            return feedID == expectedFeedID &&
                offerID == expectedOfferID &&
                count == expectedItemsCount &&
                bundleId == expectedBundleID
        }

        stub(
            requestPartName: "resolveBonusesForCart",
            responseFileName: "resolveBonusesForCart",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveBonusesForCart"]) &&
                verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = coinsService.obtainCoins(for: [cartItemState]).expect(in: self)

        // then
        let response = try result.get()
        XCTAssertEqual(response.applicableCoins.count, 1)
        XCTAssertEqual(response.disabledCoins.count, 2)
    }

    func test_shouldThrowInvalidResponseClassError_whenRecievedInvalidData() throws {
        // given

        stub(requestPartName: "resolveBonusesForCart", responseFileName: "resolveBonusesForCart_invalid")

        var thrownError: Error?

        // when
        let result = coinsService.obtainCoins(for: []).expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            thrownError = error
        }

        XCTAssertEqual(thrownError as? ServiceError, .invalidResponseClass())
    }

    // MARK: - Test data generation

    private func makeCartItemState(
        with feedID: Int,
        offerID: String,
        itemsCount: Int,
        bundleId: String? = nil
    ) -> YMTCartItem {
        let offerFeed = OfferFeed(id: feedID, offerId: offerID)

        let offer = YMTOffer()
        offer.setValue(offerFeed, forKeyPath: "feed")

        let cartItem = YMTCartItem()
        cartItem.updateCount(NSNumber(value: itemsCount))
        cartItem.updateOffer(offer)
        cartItem.setValue(bundleId, forKey: "bundleId")

        return cartItem
    }
}
