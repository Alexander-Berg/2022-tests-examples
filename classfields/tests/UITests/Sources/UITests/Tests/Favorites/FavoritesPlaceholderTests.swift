import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuFavoriteSaleList
final class FavoritesPlaceholderTests: FavoritesTests {
    let errorResponse: Data = {
        let errorResponse: Auto_Api_ErrorResponse = {
            var resp = Auto_Api_ErrorResponse()
            resp.error = .unknownError
            resp.status = .error
            resp.detailedError = "[e936141df45ee8539dd562c2f22dce65] Failed request passport/get_user"
            return resp
        }()
        return try! errorResponse.jsonUTF8Data()
    }()

    // MARK: - Tests

    func test_hasPlaceholderForAuthorizedUser() {
        currentFavoritesStub = "favs_no_offers"

        launch()
        openFavorites()
            .checkHasPlaceholder(isAuthorized: true)
    }

    func test_hasPlaceholderForUnauthorizedUser() {
        currentFavoritesStub = "favs_no_offers"
        server.forceLoginMode = .preservingResponseState

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.responseWithStatus(
                body: self.errorResponse,
                protoName: "auto.api.ErrorResponse",
                userAuthorized: false,
                status: "HTTP/1.1 500 Internal Server Error"
            )
        }

        launch()
        openFavorites()
            .checkHasPlaceholder(isAuthorized: false)
    }
}
