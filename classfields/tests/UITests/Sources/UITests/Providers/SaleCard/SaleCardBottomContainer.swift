import XCTest

final class SaleCardBottomContainer: BaseSteps, UIElementProvider {
    enum Element: String {
        case callButton = "phoneButton"
        case chatButton = "chatButton"
        case chatHelpButton = "chatHelpButton"
    }
}
