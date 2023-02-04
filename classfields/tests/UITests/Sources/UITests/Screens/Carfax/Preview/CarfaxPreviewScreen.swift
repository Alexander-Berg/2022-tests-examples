import XCTest
import Snapshots

final class CarfaxPreviewScreen: BaseScreen, ScreenWithButtons, Scrollable {
    enum ContentButton: String {
        case expand = "backend_layout_cell_previewContent"
        case recalls = "Проверка отзывных кампаний"
        case equipment = "Опции по VIN"
        case reviews = "Отзывы и рейтинг"
        case vehiclePhotos = "Фотографии автолюбителей"
    }

    typealias ButtonType = ContentButton

    lazy var scrollableElement: XCUIElement = app.collectionViews.firstMatch
}
