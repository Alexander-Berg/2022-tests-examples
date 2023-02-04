import XCTest
import Snapshots

final class SaleListSortingModalSteps<SourceSteps>: ModalSteps<SourceSteps, SaleListSortingModalScreen> where SourceSteps: BaseSteps {

    @discardableResult
    func tapByProvenOwner() -> SourceSteps {
        self.onModalScreen().byProvenOwner.tap()
        return self.as(SourceSteps.self)
    }
}
