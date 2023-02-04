import XCTest
import Snapshots

final class DealerVINSearchScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var collectionView = findAll(.collectionView).firstMatch

    lazy var searchBar = find(by: "Поиск по VIN").firstMatch
    lazy var cancelButton = find(by: "Отменить").firstMatch

    lazy var resetQueryButton = find(by: "Очистить запрос").firstMatch
    lazy var notFoundPlaceholder = find(by: "Ничего не найдено").firstMatch
}
