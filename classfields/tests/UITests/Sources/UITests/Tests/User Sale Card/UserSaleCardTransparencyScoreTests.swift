import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuUserSaleCard AutoRuOfferAdvantage
final class UserSaleCardTransparencyScoreTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        self.setupServer()
        self.launch()
    }

    // MARK: - Transparency

    func test_openForm_longDescription() {
        Step("Проверяем переход в редактирование с подскроллом в форме с бейджа про скор") { }

        self.openUserSaleCard()
            .tapOnScoreBadge()
            .tapOnAction(.longDescription)
            .as(OfferEditSteps.self)
            .wait(for: 2)
            .shouldSeeSection(title: "Описание")
    }

    func test_openProvenOwner() {
        Step("Проверяем открытие попапа проверенного собственника с бейджа про скор") { }

        self.openUserSaleCard()
            .tapOnScoreBadge()
            .tapOnAction(.provenOwner)
            .wait(for: 2)

        Step("Проверяем, что открылся попап с проверенным собственником") {
            self.mainSteps.baseScreen.findStaticText(by: "Вы собственник?").shouldBeVisible()
        }
    }

    func test_openMosRu() {
        Step("Проверяем открытие привязки мос.ру с бейджа про скор") { }

        self.openUserSaleCard()
            .tapOnScoreBadge()
            .tapOnAction(.mosRu)
            .wait(for: 3)

        Step("Проверяем, что открылся мос.ру в вебвью") {
            self.mainSteps.baseScreen.findStaticText(by: "MOS.RU").shouldBeVisible()
        }
    }

    // MARK: - Private

    private func openUserSaleCard() -> OffersSteps {
        let expectation = self.expectationForRequest { req -> Bool in
            req.method == "GET" && req.uri == "/user/offers/CARS/1102585808-a9d8c0c7/transparency-scoring"
        }

        let steps = self.mainSteps
            .openTab(.offers)
            .as(OffersSteps.self)
            .wait(for: 1)
            .openOffer(offerId: "1102585808-a9d8c0c7")
            .as(OffersSteps.self)

        self.wait(for: [expectation], timeout: 10.0)
        return steps
    }

    private func setupServer() {

        self.advancedMockReproducer.setup(server: self.server, mockFolderName: "UserSaleCardScoreDraft")
        mocker.mock_userOfferDescriptionParseOptions(isNds: false)
        mocker.server.addHandler("GET /auth/login-social/auth-uri/MOSRU *") { (req, _) -> Response? in
            let model: Vertis_Passport_SocialProviderAuthUriResult = .with { model in
                model.uri = "http://mos.ru"
            }

            return Response.okResponse(message: model, userAuthorized: true)
        }

        mocker.startMock()
    }
}
