import XCTest

final class AuctionInspectionConfirmationScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case nextButton = "submit_button"
        case howItWorksLink = "show_buyback_about"
    }

    static var rootElementName = "Экран подтверждения заявки на выкуп"
    static var rootElementID = "buyback_inspection_confirmation"
}
