import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDealerCabinet
final class DealerAccessGrantsTests: DealerBaseTest {
    enum Consts {
        static let singleOfferID = "1096920474-dae8fa6d"
    }

    // MARK: - Tests

    func test_walletNoAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_wallet_no_grants", userAuthorized: true)
        }

        launch()
        self.openCabinet().checkBalanceItem(isVisible: false)
    }

    func test_walletReadWriteAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_all_grants", userAuthorized: true)
        }

        launch()
        self.openCabinet().checkBalanceItem(isVisible: true)
    }

    func test_offersNoAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_offers_no_grants", userAuthorized: true)
        }

        launch()
        self.openCabinet()
            .shouldSeeNoAccessPlaceholder()
            .shouldNotSeeAddButton()
    }

    func test_offersReadAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_offers_read_grants", userAuthorized: true)
        }

        launch()
        let steps = self.openCabinet()
            .shouldNotSeeAddButton()

        steps.tapOnOfferSnippet(offerID: Consts.singleOfferID)
            .shouldNotSeeActionControls()
            .tapOnBackButton()

        steps.scrollToActionButton(offerID: Consts.singleOfferID)
            .tapOnActionButton(offerID: Consts.singleOfferID, type: .expand)
            .checkSnippetSnapshot(offerID: Consts.singleOfferID, identifier: "snippet_no_access_expanded")
    }

    func test_chatsReadAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_chats_read_grants", userAuthorized: true)
        }

        launch()
        let steps = self.mainSteps
            .openDealerCabinetTab()
            .as(MainSteps.self)

        Step("Проверяем присутствие бейджа на табе") {
            let element = steps.onMainScreen().tabBarItem(kind: .messages)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "dealer_read_access_chat_tabbar_item"
            )
        }

        Step("Проверяем отсутствие пресетов и поля ввода в чате") {
            steps.openChats()
                .tapOnChatRoom(index: 2)
                .should(provider: .chatScreen, .exist)
                .focus { screen in
                    screen
                        .should(.inputBar, .be(.hidden))
                        .should(.presets, .be(.hidden))
                }
        }
    }

    func test_chatsNoAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_chats_no_grants", userAuthorized: true)
        }

        launch()
        let steps = self.mainSteps
            .openDealerCabinetTab()
            .as(MainSteps.self)

        Step("Проверяем отсутствие бейджа на табе") {
            let element = steps.onMainScreen().tabBarItem(kind: .messages)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "dealer_no_access_chat_tabbar_item"
            )
        }
    }

    func test_chatsReadWriteAccess() {
        self.server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_chats_all_grants", userAuthorized: true)
        }

        launch()
        let steps = self.mainSteps
            .openDealerCabinetTab()
            .as(MainSteps.self)

        Step("Проверяем присутствие бейджа на табе") {
            let element = steps.onMainScreen().tabBarItem(kind: .messages)
            Snapshot.compareWithSnapshot(
                image: element.waitAndScreenshot().image,
                identifier: "dealer_read_access_chat_tabbar_item"
            )
        }

        Step("Проверяем наличие пресетов и поля ввода в чате") {
            steps.openChats()
                .tapOnChatRoom(index: 4)
                .should(provider: .chatScreen, .exist)
                .focus { screen in
                    screen
                        .should(.inputBar, .exist)
                        .should(.presets, .exist)
                }
        }
    }

    // MARK: - Private

    @discardableResult
    private func openCabinet() -> DealerCabinetSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
    }

    // MARK: - Setup

    override func setupServer() {
        super.setupServer()

        self.server.addHandler("GET /user/offers/CARS/\(Consts.singleOfferID)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }

        self.server.addHandler("GET /chat/room *") { (_, _) -> Response? in
            Response.okResponse(fileName: "chat_rooms", userAuthorized: true)
        }

        self.server.addHandler("GET /chat/message *") { (_, _) -> Response? in
            Response.okResponse(fileName: "chat_room", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/all/dictionaries/v1/message_presets") { (_, _) -> Response? in
            Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/all/dictionaries/v1/message_hello_presets") { (_, _) -> Response? in
            Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/all/dictionaries/v1/seller_message_presets") { (_, _) -> Response? in
            Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }

        self.server.addHandler("GET /reference/catalog/all/dictionaries/v1/seller_message_hello_presets") { (_, _) -> Response? in
            Response.okResponse(fileName: "message_presets", userAuthorized: true)
        }
    }
}
