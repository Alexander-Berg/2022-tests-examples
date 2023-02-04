import XCTest
import Snapshots

typealias GarageListingScreen_ = GarageListingSteps

extension GarageListingScreen_: UIRootedElementProvider {
    static var rootElementID: String = "garageCardList"
    static var rootElementName: String = "Листинг карточек гаража"

    enum Element {
        case vin
        case gov_number_input
        case addNextButton
        case addByVin
        case carSnippet(Int)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .vin:
            return "vin"
        case .gov_number_input:
            return "gov_number_input"
        case .addNextButton:
            return "garage_add_my_car_continue_button"
        case .addByVin:
            return "garage_add_by_vin_button"
        case .carSnippet(let index):
            return "garage_listing_car_snippet_\(index)"
        }
    }
}
