import XCTest
import Snapshots

final class CarfaxStandaloneScreen: BaseScreen {
    lazy var openFullReportButton: XCUIElement = find(by: "Смотреть полный отчёт").firstMatch
    lazy var searchBar = find(by: "Госномер или VIN").firstMatch
    lazy var searchButton = find(by: "Найти отчёт").firstMatch
    lazy var buySingleReportButton = find(by: "report_purchase_button").firstMatch
}
