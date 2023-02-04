import Foundation
import CoreGraphics

final class StoryScreen: BaseSteps, UIRootedElementProvider {
    enum Element { }

    @discardableResult
    func tapToNextPage() -> Self {
        step("Тапаем справа для перехода на следующую страницу") {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.75, dy: 0.5)).tap()
        }
    }

    static let rootElementID = "NativeStoryViewController"
    static let rootElementName = "Экран истории"
}
