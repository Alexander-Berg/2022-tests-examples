//
//  SaleCardReportPreviewTests.swift
//  AutoRu
//
//  Created by Sergey An. Sergeev on 26.05.2020.
//

import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuUserSaleCard AutoRuStandaloneCarHistory
class SaleCardReportPreviewTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    override var launchEnvironment: [String: String] {
        var value = super.launchEnvironment
        let userDefaults: [String: Any] = [
            "commentInfoCellPermanentlyRemoved": false
        ]
        let userDefaultsJsonData = try! JSONSerialization.data(withJSONObject: userDefaults, options: [])
        value["STANDARD_USER_DEFAULTS"] = userDefaultsJsonData.base64EncodedString()
        return value
    }

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok")
        }

        server.addHandler("GET /chat/room *") { (_, _) -> Response? in
            Response.okResponse(fileName: "chat_rooms", userAuthorized: true)
        }

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=false") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        try! server.start()
    }

    func openUserSaleCard() -> SaleCardSteps {
        let offerId = "1098105416-543819ea"

        server.addHandler("GET /user/offers/CARS/\(offerId) *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reports_test_user_offers_CARS_1098105416-543819ea_ok", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reports_test_user_offers_all_ok", userAuthorized: true)
        }

        mocker.mock_reportLayoutForOffer(bought: false, userCard: true)

        _ = mainSteps
            .openOffersTab()
            .openOffer(offerId: offerId)

        return SaleCardSteps(context: self)
    }

    func test_userOfferReportPreviewAddComment() {
        let offerId = "1098105416-543819ea"

        let reportPreviewSteps = openUserSaleCard().scrollToReportPreview()

        server.addHandler("PUT /carfax/user/comment *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reports_test_comment_sent_ok", userAuthorized: true)
        }

        server.addHandler("GET /carfax/offer/cars/\(offerId)/raw?version=2") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reports_test_report_raw_ok", userAuthorized: true)
        }

        reportPreviewSteps
            .addCommentButtonTap()
            .writeComment()
            .save()

        XCTAssertTrue(app.otherElements["ActivityHUD"].exists)
    }

    func test_userOfferReportPreviewRemoveInfoAboutComments() {
        openUserSaleCard()
            .scrollToReportPreview()
            .closeCommentInfoCell()
    }

    func test_userOfferReportPreviewOpenOfferEdit() {
        let reportPreviewSteps = openUserSaleCard()
                .scrollToReportPreview()

        reportPreviewSteps
            .addPhotoButtonTap()
            .app.otherElements["NavBarView"]
            .staticTexts["Объявление"]
            .shouldExist()
    }

    func test_userOfferReportPreviewOpenSupportChat() {
        let reportPreviewSteps = openUserSaleCard()
            .scrollToReportPreview()

        reportPreviewSteps
            .openSupportChatButtonTap()
            .should(provider: .chatScreen, .exist)
    }
}
