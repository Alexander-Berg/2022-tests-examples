import UIUtils
import XCTest

// MARK: - ReferralButton

final class ReferralButton: PageObject {

    var title: String {
        element.label
    }

    func tapReferral() -> ReferralPromocodePage {
        element.tap()
        return ReferralPromocodePage.current
    }

    func tapPartner() -> PartnerProgramPopupPage {
        element.tap()
        return PartnerProgramPopupPage.current
    }

}
