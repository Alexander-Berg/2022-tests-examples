//
//  PresetBubbleViewTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 23.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
import ChattoAdditions
@testable import YREChatModule

final class PresetBubbleViewTests: XCTestCase {
    func disabled_testLayout() {
        let view = PresetBubbleView()

        // let presets: [PresetViewModel] = [
        //     .init(title: "Долго"),
        //     .init(title: "Не помогли"),
        //     .init(title: "Непонятно"),
        // ]

        // TODO: @annmaslennikova
        // 1. Create stub PresetMessageModel using `presets` above
        // 2. Create ViewModel using PresetMessageViewModelBuilder
        // 3. Setup view

        self.assertSnapshot(view)
    }

    private let builder = PresetMessageViewModelBuilder()
    private let cellStyle = PresetMessageCollectionViewCellStyle()

    private func setupView(
        _ view: PresetBubbleView,
        with viewModel: PresetMessageViewModelProtocol,
        cellStyle: PresetMessageCollectionViewCellStyle
    ) {
        view.presetMessageStyle = cellStyle
        view.presetMessageViewModel = viewModel

        let size = view.sizeThatFits(UIScreen.main.bounds.size)
        view.frame = CGRect(origin: .zero, size: size)
    }
}
