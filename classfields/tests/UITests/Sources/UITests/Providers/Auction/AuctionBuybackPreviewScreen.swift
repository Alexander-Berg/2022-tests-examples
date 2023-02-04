import XCTest

final class AuctionBuybackPreviewScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case nextButton = "next_button"
        case closeButton = "Закрыть"
    }

    static var rootElementName = "Экран превью отправленной заявки на выкуп"
    static var rootElementID = "auction_buyback_preview"
}
