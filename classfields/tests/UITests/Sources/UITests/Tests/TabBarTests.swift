import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

final class TabbarTests: BaseTest {
    private let profileData: Data = {
        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "112231"
            profile.user.profile.autoru.about = ""
            return profile
        }()
        return try! userProfile.jsonUTF8Data()
    }()

    private lazy var mainSteps = MainSteps(context: self)

    // MARK: -

    override var appSettings: [String: Any] {
        var updated = super.appSettings
        updated["currentHosts"] = [
            "PHP": "https://api.test.avto.ru/rest/",
            "NodeJS": "https://api2.test.avto.ru/1.2/",
            "PublicAPI": "http://127.0.0.1:\(self.port)/"
        ]
        return updated
    }

    override func setUp() {
        super.setUp()
    }

    // MARK: - Tests

    func test_favBadgeKeptOnSelection() {
        setupServer(.forceLoggedIn)
        launch()
        let mainScreen = mainSteps.onMainScreen()

        Step("Проверяем присутствие бейджа на табе избранного") {
            let element = mainScreen.tabBarItem(kind: .favorites)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "test_favBadgeKeptOnSelection_not-sel"
            )
        }

        Step("При выборе таба бейдж должен остаться") {
            openFavorites()
            let element = mainScreen.tabBarItem(kind: .favorites)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "test_favBadgeKeptOnSelection_sel"
            )
        }
    }

    func test_messagesBadgeHiddenOnSelection() {
        setupServer(.forceLoggedIn)
        launch()
        let mainScreen = mainSteps.onMainScreen()

        Step("Должен быть бейдж на табе чатов") {
            let element = mainScreen.tabBarItem(kind: .messages)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "test_messagesBadgeHiddenOnSelection_not-sel"
            )
        }

        Step("При выборе таба чатов, бейджа быть не должно") {
            mainSteps.openChats()
            let element = mainScreen.tabBarItem(kind: .messages)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "test_messagesBadgeHiddenOnSelection_sel"
            )
        }
    }

    func test_addOfferButton_notLogined() {
        setupServer(.forceLoggedOut)
        launch()
        let mainScreen = mainSteps.onMainScreen()
        mainSteps.wait(for: 2)

        Step("Кнопка лк должна быть зеленой") {
            let element = mainScreen.tabBarItem(kind: .offers)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "tabBarAddOfferButton_attention"
            )
        }

        Step("При выборе таба, кнопка становится красной") {
            mainSteps
                .openTab(.offers).as(OffersSteps.self)
                .closeLoginScreenIfNeeded()
            let element = mainScreen.tabBarItem(kind: .offers)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "tabBarAddOfferButton_selected_attention"
            )
        }
    }

    func test_addOfferButton_logined_noUserOffers() {
        setupServer(.forceLoggedIn)
        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            let fileURL = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json", subdirectory: nil)!
            let data = try! Data(contentsOf: fileURL, options: .uncached)
            var resp = try! Auto_Api_OfferListingResponse(jsonUTF8Data: data)
            resp.offers = []
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data(), userAuthorized: true)
        }
        launch()
        let mainScreen = mainSteps.onMainScreen()
        mainSteps.wait(for: 2)

        Step("Кнопка лк должна быть зеленой") {
            let element = mainScreen.tabBarItem(kind: .offers)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "tabBarAddOfferButton_attention"
            )
        }
    }

    func test_addOfferButton_logined_hasUserOffers() {
        setupServer(.forceLoggedIn, addUserOffers: true)
        launch()
        let mainScreen = mainSteps.onMainScreen()

        Step("Кнопка лк должна быть без выделения") {
            let element = mainScreen.tabBarItem(kind: .offers)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "tabBarAddOfferButton_normal"
            )
        }

        Step("При выборе таба, кнопка становится красной") {
            mainSteps.openTab(.offers)
            let element = mainScreen.tabBarItem(kind: .offers)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "tabBarAddOfferButton_selected"
            )
        }
    }

    // MARK: - Setup

    private func setupServer(_ mode: StubServer.LoginMode, addUserOffers: Bool = false) {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            let stub = "favs_offers_with_updates"
            return Response.okResponse(fileName: stub)
        }

        server.addHandler("GET /user/favorites/all/subscriptions") { (_, _) -> Response? in
            Response.okResponse(fileName: "favs_no_new_offers")
        }

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.responseWithStatus(body: self.profileData, userAuthorized: true)
        }

        server.addHandler("GET /chat/room *") { (_, _) -> Response? in
            Response.okResponse(fileName: "chat_rooms", userAuthorized: true)
        }

        server.addHandler("GET /chat/message *") { (_, _) -> Response? in
            Response.okResponse(fileName: "chat_room", userAuthorized: true)
        }

        if addUserOffers {
            server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "user_offers_all_ok", userAuthorized: true)
            }
        }

        server.forceLoginMode = mode
        mocker.startMock()
    }

    // MARK: - Private

    @discardableResult
    private func openFavorites() -> FavoritesSteps {
        return mainSteps
            .openFavoritesTab()
            .waitForLoading()
    }
}
