import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuFavoriteSaleList
class FavoritesTests: BaseTest {
    let profileData: Data = {
        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "112231"
            profile.user.profile.autoru.about = ""
            return profile
        }()
        return try! userProfile.jsonUTF8Data()
    }()

    var didLoad: Bool = false
    var currentFavoritesStub: String?
    var currentSearchesStub: String?

    lazy var mainSteps = MainSteps(context: self)

    // MARK: -

    override func setUp() {
        super.setUp()
        app.launchArguments.append("--recreateDB")
        setupServer()
    }

    // MARK: - Setup

    func setupServer() {

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            let stub = self.currentFavoritesStub ?? "favs_offers_with_updates"
            return Response.okResponse(fileName: stub)
        }

        server.addHandler("GET /user/favorites/all/subscriptions") { (_, _) -> Response? in
            let stub = self.currentSearchesStub ?? "favs_no_new_offers"
            return Response.okResponse(fileName: stub)
        }

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.responseWithStatus(body: self.profileData, userAuthorized: true)
        }

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

    func openFavorites() -> FavoritesSteps {
        return mainSteps
            .openFavoritesTab()
            .waitForLoading()
    }
}
