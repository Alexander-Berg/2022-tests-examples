import XCTest
import Snapshots

final class OfferAdvantageScreen: ModalScreen {
    lazy var popup = find(by: "ModalViewControllerHost").firstMatch

    enum BottomButton: String {
        case score = "Узнать оценку автомобиля"
    }

    func bottomButton(_ button: BottomButton) -> XCUIElement {
        self.find(by: button.rawValue).firstMatch
    }
}
