import XCTest
import Snapshots

final class CarReportScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case callButton = "call_button"
        case favoriteButton = "favorites"
        case purchaseReportButton = "report_purchase_button"
    }

    static let rootElementID = "car_report"
    static let rootElementName = "Экран репорта"
}
