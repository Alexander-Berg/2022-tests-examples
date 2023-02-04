import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuFavoriteSaleList
final class FavoritesMotivationTests: FavoritesTests {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    func test_hasLastDayCallsCount() {
        currentFavoritesStub = "favs_offers_with_updates"

        let offerID = "1098669850-51fb1239"
        server.addHandler("GET /offer/CARS/\(offerID)/calls-stats *") { (request, _) -> Response? in
            var response = Auto_Api_CallsStatsResponse()
            response.count = 7
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }

        launch()
        openFavorites()
            .wait(for: 2)
            .checkHasOfferCallCounter()

        validateSnapshots(suiteName: suiteName, accessibilityId: "offer.24HoursCallsCount", snapshotId: #function)
    }
}
