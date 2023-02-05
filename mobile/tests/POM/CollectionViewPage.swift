import UIUtils
import XCTest

// MARK: - Base

/// Протокол для наследников `PageObject`, которые являются `UICollectionView` или `UITableView`,
/// добавляющий методы для получения ячеек
/// с помощью `CollectionViewCellsAccessibiltiy`. Для использования нужно задать наследника
/// `CollectionViewCellsAccessibility` с нужным `baseIdentifier`, и им же задавать идентификаторы доступности
/// для ячеек при их отображении в коде таргета приложения.
protocol CollectionManagerPage: PageObject {
    associatedtype AccessibilityIdentifierProvider: CollectionViewCellsAccessibility

    typealias Index = Int

    /// Метод для поиска элементов-ячеек в коллекции по индексу, когда нам не нужна секция
    func cellElement(at index: Index) -> XCUIElement

    /// Метод для поиска элементов-ячеек в коллекции по индексам
    func cellElement(at indexPath: IndexPath) -> XCUIElement

    /// Метод для поиска элементов-ячеек в коллекции по уникальному идентификатору
    /// Позволяет не привязываться к положению ячейки в коллекции
    func cellUniqueElement(withIdentifier identifier: String) -> XCUIElement

    /// Метод для поиска элементов-ячеек в коллекции по уникальному идентификатору и его порядковому индексу
    /// Позволяет не привязываться к положению ячейки в коллекции
    func cellUniqueElement(withIdentifier identifier: String, index: Int) -> XCUIElement

    /// Метод для поиска элементов-ячеек в коллекции по идентификатору после переданного уникального иденификатора
    /// Позволяет не привязываться к положению ячейки в коллекции
    func cellUniqueElement(withIdentifier identifier: String, after elements: [String]) -> XCUIElement

    /// Возвращает массив элементов с переданным уникальным идентификатором
    func allCellUniqueElement(withIdentifier identifier: String) -> [XCUIElement]
}

extension CollectionManagerPage {
    func cellElement(at indexPath: IndexPath) -> XCUIElement {
        let identifier = AccessibilityIdentifierProvider.accessibilityIdentifier(forCellAt: indexPath)
        return cellUniqueElement(withIdentifier: identifier)
    }

    func cellElement(at index: Index) -> XCUIElement {
        let identifier = AccessibilityIdentifierProvider.accessibilityIdentifier(forCellWithIndexAt: index)
        return matchingStartsWithPredicate(withIdentifier: identifier).firstMatch
    }

    func cellUniqueElement(withIdentifier identifier: String) -> XCUIElement {
        matchingContainsPredicate(
            withIdentifier: identifier,
            baseIdentifier: AccessibilityIdentifierProvider.baseIdentifier
        ).firstMatch
    }

    func cellUniqueElement(withIdentifier identifier: String, index: Int) -> XCUIElement {
        let identifier = AccessibilityIdentifierProvider.uniqueAccessibilityIdentifier(identifier, uniqueIndex: index)
        return matchingContainsPredicate(
            withIdentifier: identifier,
            baseIdentifier: AccessibilityIdentifierProvider.baseIdentifier
        ).firstMatch
    }

    func cellUniqueElement(withIdentifier identifier: String, after prevIdentifiers: [String]) -> XCUIElement {
        let predicate =
            NSPredicate(format: "(identifier CONTAINS '\(identifier)') AND NOT (identifier IN '\(prevIdentifiers)')")
        return element.cells.matching(predicate).firstMatch
    }

    func allCellUniqueElement(withIdentifier identifier: String) -> [XCUIElement] {
        matchingContainsPredicate(
            withIdentifier: identifier,
            baseIdentifier: AccessibilityIdentifierProvider.baseIdentifier
        ).allElementsBoundByIndex
    }

    private func matchingContainsPredicate(
        withIdentifier identifier: String,
        baseIdentifier: String
    ) -> XCUIElementQuery {
        let format = "identifier CONTAINS '\(identifier)' AND identifier CONTAINS '\(baseIdentifier)'"
        let predicate = NSPredicate(format: format)
        return element.cells.matching(predicate)
    }

    private func matchingStartsWithPredicate(withIdentifier identifier: String) -> XCUIElementQuery {
        let format = "identifier BEGINSWITH '\(identifier)'"
        let predicate = NSPredicate(format: format)
        return element.cells.matching(predicate)
    }
}

// MARK: - Uniform

/// Уточнение `CollectionManagerPage` для коллекций, отображающих только один `PageObject`.
protocol UniformCollectionManagerPage: CollectionManagerPage {
    associatedtype CellPage: PageObject

    func cellPage(at indexPath: IndexPath) -> CellPage
    func cellPage(at index: Int) -> CellPage
}

extension UniformCollectionManagerPage {
    func cellPage(at indexPath: IndexPath) -> CellPage {
        let element = cellElement(at: indexPath)
        return CellPage(element: element)
    }

    func cellPage(at index: Int) -> CellPage {
        let element = cellElement(at: index)
        return CellPage(element: element)
    }
}

// MARK: - Collection view

protocol CollectionViewPage: CollectionManagerPage {
    var collectionView: XCUIElement { get }
}

// MARK: - Table view

protocol TableViewPage: UniformCollectionManagerPage {
    var tableView: XCUIElement { get }
}

// MARK: - Uniform collection view

/// Уточнение `CollectionViewPage` для коллекций, отображающих только один `PageObject`.
protocol UniformCollectionViewPage: UniformCollectionManagerPage, CollectionViewPage {}

extension UniformCollectionViewPage {
    /// Перечисление по ячейкам в коллекции. Будет автоматически скроллить к следующей ячейке перед вызовом блока.
    /// Подразумевает, что элемент первой ячейки существует.
    ///
    /// - Parameters:
    ///   - cellCount: Количество ячеек в коллекции
    ///   - onEachCell: Блок, вызывающиеся на каждой новой итерации.
    func enumerateCells(cellCount: Int, onEachCell: (CellPage, IndexPath) -> Void) {
        for i in 0 ..< cellCount {
            let indexPath = IndexPath(item: i, section: 0)
            let snippet = cellPage(at: indexPath)
            collectionView.ybm_swipe(toFullyReveal: snippet.element)

            onEachCell(snippet, indexPath)
        }
    }
}
