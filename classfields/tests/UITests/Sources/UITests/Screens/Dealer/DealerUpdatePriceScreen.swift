import XCTest
import Snapshots

final class DealerUpdatePriceScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var title = find(by: "Изменение цены").firstMatch
    lazy var editInput = find(by: "price_input").textFields.firstMatch

    lazy var resetButton = find(by: "Сбросить").firstMatch
    lazy var doneButton = find(by: "Готово").firstMatch
}
