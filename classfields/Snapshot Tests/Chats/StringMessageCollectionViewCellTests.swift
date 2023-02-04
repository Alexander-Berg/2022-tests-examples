//
//  StringMessageCollectionViewCellTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 17.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
import ChattoAdditions
@testable import YREChatModule

final class StringMessageCollectionViewCellTests: XCTestCase {
    func testShortIncomingUnreadMessage() {
        let cell = StringMessageCollectionViewCell()
        let text = "Test"
        let viewModel = self.builder.createViewModel(
            .init(
                messageModel: .makeStub(isIncoming: true),
                content: .plain(text),
                isRead: false
            )
        )
        self.setupCell(cell, with: viewModel)
        self.assertSnapshot(cell)
    }

    func testLongIncomingMessage() {
        let cell = StringMessageCollectionViewCell()
        let text = "Мой дядя самых честных правил, Когда не в шутку занемог, Он уважать себя заставил И лучше выдумать.."
        let viewModel = self.builder.createViewModel(
            .init(
                messageModel: .makeStub(isIncoming: true),
                content: .plain(text),
                isRead: false
            )
        )
        
        self.setupCell(cell, with: viewModel)
        self.assertSnapshot(cell)
    }

    func testHTMLIncomingMessage() {
        let cell = StringMessageCollectionViewCell()
        let text = "<b>bold</b> <br/> <i>italic</i> <a href=link>link</a>"
        let viewModel = self.builder.createViewModel(
            .init(
                messageModel: .makeStub(isIncoming: true),
                content: .html(text),
                isRead: false
            )
        )

        self.setupCell(cell, with: viewModel)
        self.assertSnapshot(cell)
    }

    func testShortOutgoingUnreadMessage() {
        let cell = StringMessageCollectionViewCell()
        let text = "Тест"
        let viewModel = self.builder.createViewModel(
            .init(
                messageModel: .makeStub(isIncoming: false),
                content: .plain(text),
                isRead: false
            )
        )

        self.setupCell(cell, with: viewModel)
        self.assertSnapshot(cell)
    }

    func testLongOutgoingReadMessage() {
        let cell = StringMessageCollectionViewCell()
        let text = "Мой дядя самых честных правил, Когда не в шутку занемог, Он уважать себя заставил И лучше выдумать не мог."
        let viewModel = self.builder.createViewModel(
            .init(
                messageModel: .makeStub(isIncoming: false),
                content: .plain(text),
                isRead: true
            )
        )

        self.setupCell(cell, with: viewModel)
        self.assertSnapshot(cell)
    }

    private let builder = StringMessageViewModelBuilder()

    private func setupCell(
        _ cell: StringMessageCollectionViewCell,
        with viewModel: StringMessageViewModel
    ) {
        cell.style = StringMessageCollectionViewCellStyle()
        cell.messageViewModel = viewModel

        let size = cell.sizeThatFits(UIScreen.main.bounds.size)
        cell.frame = .init(origin: .zero, size: size)
    }
}
