import XCTest

final class AuctionWaitManagerCallScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case closeButton = "close_button"
        case howToPrepareLink = "show_preparation_about"
    }

    static var rootElementName = "Экран размещения успешной заявки на осмотр"
    static var rootElementID = "wait_manager_call"
}
