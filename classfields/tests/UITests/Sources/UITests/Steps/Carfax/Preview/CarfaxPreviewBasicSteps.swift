import XCTest
import Snapshots

final class CarfaxPreviewBasicSteps: BaseSteps {
    func onScreen() -> CarfaxPreviewScreen {
        return baseScreen.on(screen: CarfaxPreviewScreen.self)
    }

    @discardableResult
    func tap(button: CarfaxPreviewScreen.ContentButton) -> Self {
        let screen = onScreen()
        screen.scrollTo(element: screen.find(by: button.rawValue).firstMatch).tap()
        return self
    }
}
