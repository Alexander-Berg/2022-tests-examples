//
//  MosRuConnectionPanelTests.swift
//  Unit Tests
//
//  Created by Ella Meltcina on 17.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import UIKit
import YREDesignKit

class MosRuConnectionPanelTests: XCTestCase {
    func testMainActionButtonLayout() {
        let viewModel = MosRuConnectionPanel.ViewModel(
            text: "Проверенные собственники получают специальный знак".yre_insertNBSPs(),
            button: .init(style: .mainAction, regularPart: "Привязать mos.ru"),
            shouldDisplayHintIcon: true,
            trustText: "Никому не покажем и не передадим ваши данные".yre_insertNBSPs()
        )
        let view = MosRuConnectionPanel(viewModel: viewModel, viewStyle: .elevated)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testWithoutButtonAndGreyBackgroundLayout() {
        let viewModel = MosRuConnectionPanel.ViewModel(
            text: "Вы получили отметку «Продаёт\u{00a0}собственник»".yre_insertNBSPs(),
            shouldDisplayHintIcon: true,
            trustText: "Никому не покажем и не передадим ваши данные".yre_insertNBSPs()
        )
        let view = MosRuConnectionPanel(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testLightButtonLayout() {
        let viewModel = MosRuConnectionPanel.ViewModel(
            text: "Укажите ещё номер квартиры, чтобы получить отметку «Сдаёт\u{00a0}собственник»".yre_insertNBSPs(),
            button: .init(style: .light, regularPart: "Указать номер квартиры"),
            shouldDisplayHintIcon: true,
            trustText: "Никому не покажем и не передадим ваши данные".yre_insertNBSPs()
        )
        let view = MosRuConnectionPanel(viewModel: viewModel, viewStyle: .elevated)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testWithoutButtonAndWhiteBackgroundLayout() {
        let viewModel = MosRuConnectionPanel.ViewModel(
            text: "Идёт проверка учётной записи mos.ru".yre_insertNBSPs(),
            shouldDisplayHintIcon: true,
            trustText: "Никому не покажем и не передадим ваши данные".yre_insertNBSPs()
        )
        let view = MosRuConnectionPanel(viewModel: viewModel, viewStyle: .elevated)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }


    private func setupView(
        _ view: MosRuConnectionPanel,
        with viewModel: MosRuConnectionPanel.ViewModel
    ) {
        let width = UIScreen.main.bounds.width
        let height = MosRuConnectionPanel.height(viewModel: viewModel, width: width)

        let size = CGSize(width: width, height: height)
        view.frame = .init(origin: .zero, size: size)
    }
}
