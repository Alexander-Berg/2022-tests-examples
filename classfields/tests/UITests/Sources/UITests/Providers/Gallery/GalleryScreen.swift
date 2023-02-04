import XCTest
import Snapshots

final class GalleryScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "gallery"
    static let rootElementName = "Полноэкранная галерея"

    enum Element {
        case callButton
        case chatButton
        case closeButton
        case pageIndex
        case favoriteButton(isFavorite: Bool)
        case reportButton
        case bestPriceSlide
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .closeButton: return "galleryOverlayCloseButton"
        case .callButton: return "call_button"
        case .chatButton: return "chat_button"
        case .pageIndex: return "page_index_counter"
        case .favoriteButton(let isFavorite):
            return "gallery_favorite_icon_" + (isFavorite ? "selected" : "not_selected")
        case .reportButton: return "report_button"
        case .bestPriceSlide: return "NewCarRequestPreview"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .closeButton: return "Кнопка закрытия"
        case .callButton: return "Кнопка Позвонить"
        case .chatButton: return "Кнопка Написать в чат"
        case .pageIndex: return "Счётчик фото"
        case .favoriteButton(let isFavorite):
            return "Кнопка избранного в статусе \(isFavorite ? "`добавлено`" : "`не добавлено`")"
        case .reportButton: return "Кнопка отчёта"
        case .bestPriceSlide: return "Слайд с получением лучшей цены"
        }
    }

    func swipeToPreviousPhoto() -> Self {
        Step("Скроллим к предыдущему фото") {
            app
                .coordinate(withNormalizedOffset: .init(dx: 0.1, dy: 0.5))
                .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: .init(dx: 0.9, dy: 0.5)))
        }
        return self
    }

    func swipeToNextPhoto() -> Self {
        Step("Скроллим к следующему фото") {
            app
                .coordinate(withNormalizedOffset: .init(dx: 0.9, dy: 0.5))
                .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: .init(dx: 0.1, dy: 0.5)))
        }
        return self
    }
}
