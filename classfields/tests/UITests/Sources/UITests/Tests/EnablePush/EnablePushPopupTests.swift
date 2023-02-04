//
//  EnablePushPopupTests.swift
//  UITests
//
//  Created by Alexander Malnev on 9/15/20.
//

import XCTest
import Snapshots

/// @depends_on AutoRuViews
final class EnablePushPopupTests: EnablePushTests {
    override func setupServer(_ server: StubServer) {
        super.setupServer(server)

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (request, _) -> Response? in
            return Response.okResponse(fileName: "enable_push_search_cars")
        }
    }

    private func validateEnableNotificationsPopup() {
        let titleText = app.staticTexts["Включите push-уведомления"].firstMatch

        titleText.shouldExist()
        app.otherElements["enable_notifications"].shouldExist()

        app.buttons["dismiss_modal_button"].tap()

        titleText.shouldNotExist()
    }

    func test_savingSearchShowsPopup() {
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .addToFavorites()
            .confirmAddToFavorites()

        validateEnableNotificationsPopup()
    }

    func test_savingOfferShowsPopup() {
        mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToOffer(with: "1")
            .likeCarOffer(withId: "1")

        validateEnableNotificationsPopup()
    }
}
