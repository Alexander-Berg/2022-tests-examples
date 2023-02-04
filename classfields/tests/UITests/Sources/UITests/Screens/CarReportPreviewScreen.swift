import XCTest
import Snapshots

final class CarReportPreviewScreen: BaseScreen, Scrollable, NavigationControllerContent {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var firstContentCell = find(by: "backend_layout_cell").firstMatch
    lazy var promoBlock = find(by: "backend_layout_cell_promo_block").firstMatch
    lazy var refreshingControl = find(by: "spinner_view").firstMatch

    lazy var buyPackageButton = find(by: "purchaseReportsPackageButtonId").firstMatch
    lazy var buyButton = find(by: "report_purchase_button").firstMatch

    func cell(index: Int) -> XCUIElement {
        return find(by: "backend_layout_cell_\(index)").firstMatch
    }
}
