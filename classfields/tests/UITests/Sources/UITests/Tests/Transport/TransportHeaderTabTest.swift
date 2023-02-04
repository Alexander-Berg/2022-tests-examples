import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRu AutoRuServices AutoRuVehicleTextSearch AutoRuCellHelpers AutoRuJournal
final class TransportHeaderTabTest: BaseTest {
    private let userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        profile.user.profile.autoru.clientID = "dealer_id"
        return profile
    }()

    private lazy var mainSteps = MainSteps(context: self)

    // MARK: -

    // MARK: - Tests

    func test_proAvto() {
        setupServer()
        launch()

        Step("ПроАвто") {}
        Step("Должен быть инпут поиска по VIN") {
            let screen = mainSteps.openCarReportsList()
            screen.checkHasSearchField()
        }
    }

    func test_credits() {
        setupServer()
        launch()

        Step("Кредиты") {}
        Step("Должен быть запрос на кредиты") {
            let exp = expectationForRequest(method: "GET", uri: "/shark/credit-application/active?with_offers=true&with_person_profiles=true")
            mainSteps.openCredits()
            wait(for: [exp], timeout: 5)
        }
    }

    func test_insurance() {
        setupServer()
        launch()

        Step("Страховки") {}
        Step("Должнен быть вебвью") {
            mainSteps
                .openInsurance()
                .checkHasWebView(timeout: 10)
        }
    }

    func test_reviews() {
        setupServer()
        launch()

        Step("Отзывы") {}
        Step("Должна быть кнопка `Добавить отзыв`") {
            mainSteps.openReviews()
                .onMainScreen().find(by: "Добавить отзыв")
                .firstMatch.shouldExist()
        }
    }

    func test_journal() {
        setupServer()
        Step("Журнал") {}
        launchMain()
            .step("Должен быть журнал", perform: {
                $0
                    .should(provider: .mainScreen, .exist)
                    .focus { screen in
                        screen.tap(.navBarTab(.journal))
                    }
                    .should(provider: .journalTabScreen, .exist)
            })
    }

    func test_electrocars() {
        setupServer()
        Step("Электромобили") {}
        launchMain()
            .step("Должна быть Электромобили", perform: { 
                $0
                    .should(provider: .mainScreen, .exist)
                    .focus { screen in
                        screen.tap(.navBarTab(.electrocars))
                    }
                    .should(provider: .webControllerScreen, .exist)
            })
    }

    func testHasNoDealerTabs() {
        setupServerForDealer()
        launch()

        Step("Дилер") {}
        Step("Нет кредитов и страховки") {
            mainSteps
                .checkHasNoTab(.credits)
                .openJournal()
                .checkHasNoTab(.insurance)
        }
    }

    // MARK: - Setup

    private func setupServer() {
        server.addHandler("POST /device/hello") { _, _ -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

    private func setupServerForDealer() {
        server.addHandler("POST /device/hello") { _, _ -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user?with_auth_types=true") { _, _ -> Response? in
            Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=false") { _, _ -> Response? in
            Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }
}
