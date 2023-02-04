//
//  UserChatRoomCellTests.swift
//  Unit Tests
//
//  Created by Ella Meltcina on 03.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
import ChattoAdditions
@testable import YREChatRoomsModule

final class UserChatRoomCellTests: XCTestCase {
    func testMuteAndUnreadLayout() {
        let cell = UserChatRoomCell()
        let viewModel = self.makeViewModel(
            title: "Ляля",
            lastMessage: "1",
            options: "2 комн., 62,2 м², 3 600 000 ₽",
            isUnread: true,
            isMuted: true,
            isSold: false,
            hasSeparator: true
        )
        self.setup(cell: cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }
    
    func testLongOptionsAndSoldLayout() {
        let cell = UserChatRoomCell()
        let viewModel = self.makeViewModel(
            title: "test2 sabok",
            lastMessage: "Изображение",
            options: "Машиноместо, 123 м², 12 332 132 000 ₽",
            isUnread: false,
            isMuted: false,
            isSold: true,
            hasSeparator: true
        )
        self.setup(cell: cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }
    
    func testLongMessageLayout() {
        let cell = UserChatRoomCell()
        let viewModel = self.makeViewModel(
            title: "test2 sabok",
            lastMessage: "А чего так дорого? Сделай скидку! Быстро, быстро, быстро!",
            options: "Студия, 125 м², 999 000 000 ₽",
            isUnread: false,
            isMuted: false,
            isSold: false,
            hasSeparator: false
        )
        self.setup(cell: cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }
    
    func testLongMessageAndOptionsLayout() {
        let cell = UserChatRoomCell()
        let viewModel = self.makeViewModel(
            title: "test2 sabok",
            lastMessage: "Длиннный текст на пару строк или сколько там влезет. Вот столько вот влезет!",
            options: "1 комн., 123 000 000 м², 4 556 789 120 000 000 ₽",
            isUnread: false,
            isMuted: false,
            isSold: true,
            hasSeparator: true
        )
        self.setup(cell: cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }
    
    func testDevChatLayout() {
        let cell = UserChatRoomCell()
        let viewModel = self.makeViewModel(
            title: "Capital Group",
            lastMessage: "Спасибо, мы скоро вам ответим.",
            options: "МФК «Capital Towers»",
            isUnread: false,
            isMuted: false,
            isSold: false,
            hasSeparator: true
        )
        self.setup(cell: cell, viewModel: viewModel)
        self.assertSnapshot(cell)
    }
    
    private func makeViewModel(
        title: String,
        lastMessage: String,
        options: String,
        isUnread: Bool,
        isMuted: Bool,
        isSold: Bool,
        hasSeparator: Bool
        ) -> UserChatRoomCell.ViewModel {
        UserChatRoomCell.ViewModel(
             avatarURL: nil,
             avatarEmptyStateAsset: Asset.Images.Offer.NoPhotos.offerRoom,
             title: title,
             lastMessage: lastMessage,
             options: options,
             time: "15.03.96",
             isUnread: isUnread,
             isMuted: isMuted,
             isSold: isSold,
             hasSeparator: hasSeparator
        )
    }
    
    private func setup( cell: UserChatRoomCell, viewModel: UserChatRoomCell.ViewModel) {
        cell.configure(with: viewModel)
        let size = CGSize(width: UIScreen.main.bounds.width, height: 114)
        cell.frame = .init(origin: .zero, size: size)
    }
}
