import XCTest

final class LargeGalleryCell: BaseSteps, UIElementProvider {
    typealias Element = Void

    func swipeToNextPhoto() -> Self {
        Step("Скроллим к следующему фото") {
            rootElement
                .coordinate(withNormalizedOffset: .init(dx: 0.9, dy: 0.5))
                .press(forDuration: 0.1, thenDragTo: rootElement.coordinate(withNormalizedOffset: .init(dx: 0.1, dy: 0.5)))
        }
        return self
    }
}
