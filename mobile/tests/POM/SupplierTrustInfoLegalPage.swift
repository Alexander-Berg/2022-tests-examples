import UIUtils
import XCTest

class SupplierTrustInfoLegalPage: PageObject {

    var legalInfoText: XCUIElement {
        element
            .textViews
            .matching(identifier: SupplierTrustInfoLegalAccessibility.legalInfoText)
            .firstMatch
    }

}
