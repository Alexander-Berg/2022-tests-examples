import XCTest
import Snapshots

final class DealerSaleCardScreen: BaseScreen, Scrollable, NavigationControllerContent {
    static let insetsWithoutFloatingButton = UIEdgeInsets(top: 0, left: 0, bottom: 100, right: 0)

    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var collectionView = findAll(.collectionView).firstMatch
    lazy var refreshingControl = self.scrollableElement.activityIndicators.firstMatch

    lazy var titleLabel = find(by: "title").firstMatch
    lazy var priceLabel = find(by: "price_label").firstMatch
    lazy var updatePriceButton = find(by: "update_price_button").firstMatch

    lazy var moreButton = find(by: "more_button").firstMatch
    lazy var editButton = find(by: "Редактировать").firstMatch
    lazy var activationButton = find(by: "Активировать").firstMatch
    lazy var deleteButton = find(by: "Удалить").firstMatch

    lazy var counters = find(by: "dealer_sale_card_statistics_counters").firstMatch
    lazy var viewsChart = find(by: "dealer_chart_views").firstMatch
    lazy var phoneViewsChart = find(by: "dealer_chart_phone_views").firstMatch

    lazy var banReasons = find(by: "ban_reasons").firstMatch
    lazy var header = find(by: "header").firstMatch
    lazy var gallery = find(by: "images").firstMatch
    lazy var galleryPlaceholder = find(by: "images_stub").firstMatch
    lazy var days = find(by: "days").firstMatch
    lazy var stats = find(by: "statistics").firstMatch
    lazy var characteristics = find(by: "characteristics").firstMatch
    lazy var offerDescription = find(by: "description").firstMatch

    lazy var carReportPreviewLabel = find(by: "backend_layout_cell_previewContent").firstMatch
    lazy var freeCarReport = find(by: "Смотреть бесплатный отчёт").firstMatch
    lazy var buyCarReportButton = find(by: "report_purchase_button").firstMatch

    func alertButton(text: String) -> XCUIElement {
        return self.app.alerts.firstMatch.buttons[text].firstMatch
    }
}
