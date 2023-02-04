//
//  OffersSteps.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/19/19.
//

import XCTest
import Snapshots

class OfferEditSteps: BaseSteps {
    func onOfferEditScreen() -> OfferEditScreen {
        return baseScreen.on(screen: OfferEditScreen.self)
    }

    @discardableResult
    func scrollToFormElement(_ element: OfferEditScreen.FormElement, swipeDirection: XCUIElement.SwipeDirection = .down) -> Self {
        onOfferEditScreen().scrollTo(element: onOfferEditScreen().cellFor(element), maxSwipes: 10, swipeDirection: swipeDirection)
        return self
    }

    @discardableResult
    func tapActivate() -> Self {
        let button = onOfferEditScreen().find(by: "activateButton").firstMatch
        button.tap()
        return self
    }

    @discardableResult
    func scrollAndTapNoActivation() -> Self {
        step("Скролл и тап по кнопке `Не публиковать сразу`") {
            self.onOfferEditScreen().scrollTo(element: self.onOfferEditScreen().noActivationButton)
            self.onOfferEditScreen().noActivationButton.tap()
        }
    }

    func tapPanorama() -> PanoramaSteps {
        let button = onOfferEditScreen().find(by: "panoramaButton").firstMatch
        button.tap()
        return PanoramaSteps(context: context)
    }

    func checkFormElementSnapshot(_ element: OfferEditScreen.FormElement, identifier: String) -> Self {
        let element = self.onOfferEditScreen().cellFor(element)
        Snapshot.compareWithSnapshot(image: element.waitAndScreenshot().image, identifier: identifier)
        return self
    }

    func checkFormElements(from: OfferEditScreen.FormElement, to: OfferEditScreen.FormElement, identifier: String) -> Self {
        scrollToFormElement(from, swipeDirection: .up)
        let fromElement = self.onOfferEditScreen().cellFor(from)
        let toElement = self.onOfferEditScreen().cellFor(to)
        let image = Snapshot.screenshotCollectionView(fromCell: fromElement, toCell: toElement, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 16, right: 0), timeout: 5)
        Snapshot.compareWithSnapshot(image: image, identifier: identifier, perPixelTolerance: 0.01, overallTolerance: 0.01, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 16, right: 16))
        return self
    }

    @discardableResult
    func shouldSeeSection(title: String) -> Self {
        step("Проверяем, что видна секция '\(title)'") {
            self.onOfferEditScreen().findStaticText(by: title).shouldBeVisible()
        }
    }
}
