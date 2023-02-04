//
//  TechSupportCellTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 29.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
import ChattoAdditions
@testable import YREChatRoomsModule

final class TechSupportCellTests: XCTestCase {
    func testCommonLayout() {
        let cell = TechSupportCell()
        let viewModel = TechSupportCell.ViewModel(
            lastMessage: "Очень длинный текст от ТП. Такой длинный, что хватит на три строчки, а то и больше. Добавим ещё несколько слов.",
            time: "13.05.96",
            isUnread: false,
            hasSeparator: false
        )

        self.setupCell(cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }

    func testUnreadAndHasSeparatorLayout() {
        let cell = TechSupportCell()
        let viewModel = TechSupportCell.ViewModel(
            lastMessage: "Обычный текст от ТП",
            time: "13.05.96",
            isUnread: true,
            hasSeparator: true
        )

        self.setupCell(cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }
    
    private func setupCell(_ cell: TechSupportCell, viewModel: TechSupportCell.ViewModel) {
        cell.configure(with: viewModel)
        let size = CGSize(width: UIScreen.main.bounds.width, height: 114)
        cell.frame = .init(origin: .zero, size: size)
    }
}
