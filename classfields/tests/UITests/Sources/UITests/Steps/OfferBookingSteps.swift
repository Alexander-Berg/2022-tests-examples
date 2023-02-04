//
//  OfferBookingSteps.swift
//  UITests
//
//  Created by Alexander Ignatyev on 12/11/20.
//

import Foundation

final class OfferBookingSteps: BaseSteps {

    func type(name: String) -> OfferBookingSteps {
        let element = onOfferBookingScreen()
            .nameTextField
            .shouldExist()

        element.tap()
        element.typeText(name)
        return self
    }

    func tapPhoneField() -> OfferBookingSteps {
        onOfferBookingScreen()
            .phoneTextField
            .shouldExist()
            .tap()
        return self
    }

    func tapAddPhoneButton() -> OfferBookingSteps {
        onOfferBookingScreen()
            .addPhoneButton
            .shouldExist()
            .tap()
        return self
    }

    func type(phone: String) -> OfferBookingSteps {
        let element = onOfferBookingScreen()
            .find(by: "custom_text_field")
            .firstMatch
            .shouldExist()

        element.tap()
        element.typeText(phone)
        return self
    }

    func type(code: String) -> OfferBookingSteps {
        let element = onOfferBookingScreen()
            .find(by: "custom_text_field")
            .firstMatch
            .shouldExist()

        element.tap()
        element.typeText(code)
        return self
    }

    func tapDoneButton() -> OfferBookingSteps {
        onOfferBookingScreen().doneButton.tap()
        return self
    }

    @discardableResult
    func tapShowFavoritesButton() -> MainSteps {
        onOfferBookingScreen().showFavoritesButton.tap()
        return self.as(MainSteps.self)
    }

    func bookButton(price: Int, enabled: Bool) -> OfferBookingSteps {
        onOfferBookingScreen()
            .bookButton
            .shouldBe(enabled: enabled)
            .buttons["Забронировать за \(price) ₽"]
            .shouldExist()
        return self
    }

    func tapBookButton() -> OfferBookingSteps {
        onOfferBookingScreen().bookButton.tap()
        return self
    }

    private func onOfferBookingScreen() -> OfferBookingScreen {
        return baseScreen.on(screen: OfferBookingScreen.self)
    }
}
