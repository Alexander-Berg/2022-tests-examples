import XCTest

final class MerchantPopupPage: PageObject, CollectionViewPage {

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    typealias AccessibilityIdentifierProvider = MerchantPopupCollectionViewCellsAccessibility

    func title(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: MerchantPopupAccessibility.title, index: index).textViews.firstMatch
    }

    func supplierSubtitle(at index: Int) -> XCUIElement {
        cellUniqueElement(withIdentifier: MerchantPopupAccessibility.supplierSubtitle, index: index).textViews
            .firstMatch
    }

    func fullName(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.fullName, index: index)
        return InfoItem(element: el)
    }

    func inn(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.inn, index: index)
        return InfoItem(element: el)
    }

    func ogrn(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.ogrn, index: index)
        return InfoItem(element: el)
    }

    func licenseNumber(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.licenseNumber, index: index)
        return InfoItem(element: el)
    }

    func actualAddress(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.actualAddress, index: index)
        return InfoItem(element: el)
    }

    func juridicalAddress(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.juridicalAddress, index: index)
            .firstMatch
        return InfoItem(element: el)
    }

    func schedule(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.schedule, index: index)
        return InfoItem(element: el)
    }

    func support(at index: Int = 0) -> InfoItem {
        let el = cellUniqueElement(withIdentifier: MerchantPopupAccessibility.support, index: index)
        return InfoItem(element: el)
    }

    func alcoWarning(at index: Int = 0) -> XCUIElement {
        cellUniqueElement(withIdentifier: MerchantPopupAccessibility.alcoWarning, index: index)
            .textViews.firstMatch
    }

    final class InfoItem: PageObject {
        var header: XCUIElement {
            element.staticTexts.matching(identifier: MerchantPopupAccessibility.infoHeader).firstMatch
        }

        var caption: XCUIElement {
            element.staticTexts.matching(identifier: MerchantPopupAccessibility.infoCaption).firstMatch
        }
    }
}
