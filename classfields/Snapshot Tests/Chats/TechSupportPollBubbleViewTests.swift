//
//  TechSupportPollBubbleViewTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 22.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
import ChattoAdditions
@testable import YREChatModule

final class TechSupportPollBubbleViewTests: XCTestCase {
    func testTechSupportPollUnvotedLayout() {
        let view = TechSupportPollBubbleView()

        let messageModel = TechSupportPollMessageModel.makeStub(pollState: .initial)
        let viewModel = self.builder.createViewModel(messageModel)
        self.setupView(view, with: viewModel, cellStyle: self.cellStyle)

        self.assertSnapshot(view)
    }

    func testTechSupportPollVotedBadLayout() {
        let view = TechSupportPollBubbleView()

        let messageModel = TechSupportPollMessageModel.makeStub(pollState: .selected(.bad))
        let viewModel = self.builder.createViewModel(messageModel)
        self.setupView(view, with: viewModel, cellStyle: self.cellStyle)

        self.assertSnapshot(view)
    }

    func testTechSupportPollVotedNormalLayout() {
        let view = TechSupportPollBubbleView()

        let messageModel = TechSupportPollMessageModel.makeStub(pollState: .selected(.normal))
        let viewModel = self.builder.createViewModel(messageModel)
        self.setupView(view, with: viewModel, cellStyle: self.cellStyle)

        self.assertSnapshot(view)
    }

    func testTechSupportPollVotedExcellentLayout() {
        let view = TechSupportPollBubbleView()

        let messageModel = TechSupportPollMessageModel.makeStub(pollState: .selected(.excellent))
        let viewModel = self.builder.createViewModel(messageModel)
        self.setupView(view, with: viewModel, cellStyle: self.cellStyle)

        self.assertSnapshot(view)
    }

    private let builder = TechSupportPollMessageViewModelBuilder()
    private let cellStyle = TechSupportPollMessageCollectionViewCellStyle()

    private func setupView(
        _ view: TechSupportPollBubbleView,
        with viewModel: TechSupportPollMessageViewModelProtocol,
        cellStyle: TechSupportPollMessageCollectionViewCellStyle
    ) {
        view.techSupportPollMessageStyle = cellStyle
        view.techSupportPollMessageViewModel = viewModel
        view.updateStyle(with: cellStyle.techSupportPollBubbleViewStyle(viewModel: viewModel))

        let size = view.sizeThatFits(UIScreen.main.bounds.size)
        view.frame = .init(origin: .zero, size: size)
    }
}
