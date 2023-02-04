import XCTest
import Snapshots

final class OfferAdvantageSteps<SourceSteps>: ModalSteps<SourceSteps, OfferAdvantageScreen> {
    @discardableResult
    func tapOnBottomButton(_ button: OfferAdvantageScreen.BottomButton) -> Self {
        step("Тапаем на кнопку снизу") {
            self.onModalScreen().bottomButton(button).tap()
        }
    }

    @discardableResult
    func checkPopupSnapshot(identifier: String) -> Self {
        step("Скриншотим и проверяем попап преимущества, скриншот '\(identifier)'") {
            validateSnapshot(of: self.onModalScreen().popup, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 34, right: 0), snapshotId: identifier)
        }
    }
}
