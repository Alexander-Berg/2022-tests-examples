import XCTest
import Snapshots

final class CarfaxStandaloneCardScreen: BaseScreen, Scrollable, NavigationControllerContent {
    enum HeaderButton: String {
        case expand = "backend_layout_cell_previewContent"
        case collapse = "Свернуть" // use js id

        case pts = "header_pts"
        case recalls = "header_recalls"
        case vehiclePhotos = "header_vehicle_photos"
        case equipment = "header_equipment"
        case historyOffers = "history_offers"
        case ptsOwners = "header_pts_owners"
        case history = "header_history"
        case fines = "header_fines"
        case tax = "header_tax"
        case characteristics = "Характеристики"
    }

    enum PromoBlockButton: String {
        case recalls = "Проверка отзывных кампаний"
        case equipment = "Опции по VIN"
        case reviews = "Отзывы и рейтинг"
        case vehiclePhotos = "Фотографии автолюбителей"
    }

    lazy var reportContainer: XCUIElement = find(by: "car_report").firstMatch
    lazy var reportHeader: XCUIElement = find(by: "backend_layout_cell_full_title_cell").firstMatch
    lazy var activityHUD: XCUIElement = find(by: "ActivityHUD").firstMatch
    lazy var scrollableElement: XCUIElement = app.collectionViews.firstMatch

    lazy var headerBlockStart: XCUIElement = find(by: "backend_layout_cell_2").firstMatch
    lazy var headerBlockExpandedContent: XCUIElement = find(by: "backend_layout_cell_previewContent").firstMatch
    lazy var headerBlockEnd: XCUIElement = find(by: "backend_layout_cell_3").firstMatch

    lazy var recallsPreHeader: XCUIElement = find(by: "backend_layout_cell_recalls").firstMatch
    lazy var recallsHeaderLabel: XCUIElement = find(by: "Проверка отзывных кампаний").firstMatch

    lazy var vehiclePhotosPreHeader: XCUIElement = find(by: "backend_layout_cell_vehicle_photos").firstMatch

    lazy var brandCertificationPreHeader: XCUIElement = find(by: "backend_layout_cell_brand_certification").firstMatch

    lazy var recallsFooterLabel: XCUIElement = {
        let text = "Иногда производители отзывают свои автомобили из-за некачественных деталей или сборки. Замена всегда происходит бесплатно и независимо от гарантии. В Росстандарт собрана полная база всех отзывных кампаний с 2014 года."

        return app.staticTexts.element(
            matching: NSPredicate(format: "label == %@", text)
        ).firstMatch
    }()

    lazy var reportPollLoaderHeader: XCUIElement = find(by: "report_poll_loader_header").firstMatch
    lazy var reportPollLoaderFooter: XCUIElement = find(by: "report_poll_loader_footer").firstMatch

    lazy var offersHistoryGallery: XCUIElement = find(by: "offers_history_gallery").firstMatch
    lazy var offersHistoryCurrentOfferBadge: XCUIElement = find(by: "offers_history_current_offer_badge").firstMatch

    lazy var buyCarReportButton = find(by: "report_purchase_button").firstMatch
    lazy var notPaidFinesLabel = find(by: "numNotPaidFinesLabel").firstMatch

    lazy var creditBanner = find(by: "creditBanner").firstMatch

    func recall(like name: String) -> XCUIElement {
        return app.staticTexts.containingText(name).firstMatch
    }

    func expandableComment(at index: Int) -> XCUIElement {
        return find(by: "backend_layout_cell_offer_history_description_expand_\(index)").firstMatch
    }

    func expandedComment(at index: Int) -> XCUIElement {
        return find(by: "backend_layout_cell_offer_history_description_expand_\(index)").firstMatch
    }
}
