//
//  RecentSearchesItemCellTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 3/19/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YRELegacyFiltersCore
import YREModel
import YREDesignKit

final class RecentSearchesItemCellTests: XCTestCase {
    func test1ShortParameter() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: ["Короткий текст"]
        )
        let view = self.view(with: viewModel)
        XCTAssertFalse(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func test1VeryLongParameter() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж, машину, дом, велосипед",
            parameters: ["Очень длинный текст, который не помещается в 2 строки"]
        )
        let view = self.view(with: viewModel)
        XCTAssertFalse(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func test1LongParameter() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: ["Очень длинный текст, который еле помещается"]
        )
        let view = self.view(with: viewModel)
        XCTAssertFalse(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func test2VeryLongParameters() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: ["Очень длинный текст, который не помещается в 2 строки", "Ещё какой-то параметр"]
        )
        let view = self.view(with: viewModel)
        XCTAssertTrue(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func testManyParametersAnd9Remainings() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: [
                "один",
                "два",
                "три",
                "четыре",
                "пять",
                "шесть",
                "сетте",
                "восемь",
                "девять",
                "десять",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
            ]
        )
        let view = self.view(with: viewModel)
        XCTAssertTrue(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func testManyParametersAnd10Remainings() {
        // There is enough space for the ellipsis "+ ещё 9", but not enough for the ellipsis "+ ещё 10".
        // So another parameter was hidden and we get the ellipsis "+ ещё 11".
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: [
                "один",
                "два",
                "три",
                "четыре",
                "пять",
                "шесть",
                "сетте",
                "восемь",
                "девять",
                "десять",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
            ]
        )
        let view = self.view(with: viewModel)
        XCTAssertTrue(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func test1ParameterWithVeryLongWord() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: ["одиндватричетырепятьзачемтывсёэточитаешь"]
        )
        let view = self.view(with: viewModel)
        XCTAssertFalse(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    func testEmptyParameter() {
        let search = YREMutableSearch()
        let viewModel = RecentSearchViewModel(
            search: search,
            title: "Купить гараж",
            parameters: ["Короткий текст", "ещё текст", ""]
        )
        let view = self.view(with: viewModel)
        XCTAssertFalse(view.cell.hasTruncatedParameters)
        self.assertSnapshot(view)
    }

    private class CellContainer: UIView {
        let cell: RecentSearchesItemCell

        init(cell: RecentSearchesItemCell) {
            self.cell = cell
            super.init(frame: .zero)
            self.addSubview(cell)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
    }

    private func view(with viewModel: RecentSearchViewModel) -> CellContainer {
        // Test cases are created for a specific cell size. If you change this size, then correct the text in the tests.
        let cell = RecentSearchesItemCell(frame: CGRect(origin: .zero, size: .init(width: 228, height: 92)))
        cell.configure(with: viewModel)
        let backgroundView = CellContainer(cell: cell)
        backgroundView.backgroundColor = ColorScheme.Background.primary
        cell.frame.origin = .init(x: 10, y: 10)
        backgroundView.frame.size = .init(width: 228 + 20, height: 92 + 20)
        return backgroundView
    }
}
