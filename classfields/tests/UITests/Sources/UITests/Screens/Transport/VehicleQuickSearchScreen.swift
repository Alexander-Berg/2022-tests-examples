import XCTest
import Snapshots

final class VehicleQuickSearchScreen: BaseScreen, Scrollable {
    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }
}
