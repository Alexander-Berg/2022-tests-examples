//
//  EnablePushBlockTests.swift
//  UITests
//
//  Created by Alexander Malnev on 9/15/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuViews
final class EnablePushBlockTests: EnablePushTests {
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    override func setupServer(_ server: StubServer) {
        super.setupServer(server)

        server.addHandler("GET /user?with_auth_types=true") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "enable_push_fav_offers")
        }

        server.addHandler("GET /user/favorites/all/subscriptions") { (_, _) -> Response? in
            Response.okResponse(fileName: "enable_push_saved_searches")
        }

        server.addHandler("GET /chat/room *") { (_, _) -> Response? in
            Response.okResponse(fileName: "enable_push_chat_rooms", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "enable_push_user_sales", userAuthorized: true)
        }
    }

    private func validateEnableNotificationsBlock() {
        let titleText = app.staticTexts["Включите push-уведомления"].firstMatch

        titleText.shouldExist()
        app.otherElements["enable_notifications"].shouldExist()

        app.otherElements["close_enable_notifications"].tap()

        titleText.shouldNotExist()
    }

    func test_savedSearchesShowsBlock() {
        mainSteps
            .wait(for: 1)
            .openFavoritesTab()
            .tapSegment(at: .searches)

        validateEnableNotificationsBlock()
    }

    func test_savedOfferShowsBlock() {
        mainSteps
            .wait(for: 1)
            .openFavoritesTab()

        validateEnableNotificationsBlock()
    }

    func test_openingChatsShowsPopup() {
        mainSteps
            .wait(for: 1)
            .openChats()

        validateEnableNotificationsBlock()
    }

    func test_officeShowsPopup() {
        mainSteps
            .wait(for: 1)
            .openOffersTab()
            .wait(for: 1)

        validateEnableNotificationsBlock()
    }
}
