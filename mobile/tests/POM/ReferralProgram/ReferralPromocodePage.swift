import UIUtils
import XCTest

final class ReferralPromocodePage: PageObject {

    static var current: ReferralPromocodePage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: ReferralAccessibility.root)
            .firstMatch
        return ReferralPromocodePage(element: elem)
    }

    var closeButton: XCUIElement {
        NavigationBarPage.current.element
            .buttons
            .matching(identifier: ReferralAccessibility.Button.close)
            .firstMatch
    }

    var rulesButton: XCUIElement {
        cellUniqueElement(withIdentifier: ReferralAccessibility.Button.rules)
            .buttons
            .firstMatch
    }

    var partnerLink: XCUIElement {
        cellUniqueElement(withIdentifier: ReferralAccessibility.partnerLink)
            .otherElements
            .firstMatch
    }

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: ReferralAccessibility.title)
            .textViews
            .firstMatch
    }

    var text: XCUIElement {
        cellUniqueElement(withIdentifier: ReferralAccessibility.text)
            .textViews
            .firstMatch
    }

    var button: XCUIElement {
        cellUniqueElement(withIdentifier: ReferralAccessibility.Button.primary)
            .buttons
            .firstMatch
    }

    func copyClipboard() -> DefaultToastPopupPage {
        title.tap()
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: DefaultToastPopupPage.rootIdentifier)
            .firstMatch
        return DefaultToastPopupPage(element: elem)
    }

    func openDetails() {
        rulesButton.tap()
    }

    func share() {
        button.tap()
    }

    func close() {
        closeButton.tap()
    }

    func openPartnerSite() {
        partnerLink.tap()
    }
}

extension ReferralPromocodePage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = ReferralPromoCollectionViewCellsAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
