//
//  ChatInputViewTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 06.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable line_length

import XCTest
import YREDesignKit
import ChattoAdditions
@testable import YREChatModule

final class ChatInputViewTests: XCTestCase {
    func testBlockedByMeLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .button(title: "Разблокировать", accessibilityID: "", onTap: {}),
            description: nil
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }
    
    func testBlockedByOtherUserLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .noAction,
            description: "Ой, кажется собеседник заблокровал вас, поэтому написать ему не получится. Наверное беседа не задалась".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }
    
    func testMeWasBannedLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .button(title: "Написать в поддержку", accessibilityID: "", onTap: {}),
            description: "Заметили подозрительную активность и заблокировали ваши чаты. Возможно, вы отправили слишком много  похожих сообщений. Подождите 24 часа, и доступ к чатам вернётся".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }
    
    func testOtherUserWasBannedLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .noAction,
            description: "Заметили у пользователя подозрительную активность и отключили его чаты. Через некоторое время можно будет написать снова.".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }

    func testMeChangedTypeLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .button(title: "Написать в поддержку", accessibilityID: "", onTap: {}),
            description: "Вы стали агентом и чаты с покупателями теперь неактивны. Старые добрые звонки работают как и прежде.".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }
    
    func testOtherUserChangedTypeLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .button(title: "Позвонить", accessibilityID: "", onTap: {}),
            description: "К сожалению, написать этому человеку больше нельзя — он стал агентом, а чатов у них пока нет. Но вы можете позвонить.".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }
    
    func testOtherUserDisabledChatLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .button(title: "Позвонить", accessibilityID: "", onTap: {}),
            description: "У пользователя отключены чаты и написать ему больше нельзя. Но всё ещё можно позвонить".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }
    
    func testflatCanNoLongerBeRentedLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .noAction,
            description: "К сожалению, квартира больше не сдаётся".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
    }

    func testRentCallCenterClosedChatLayout() {
        let view = ChatInputView()
        let blockedViewModel = BlockedInputView.ViewModel(
            kind: .onlyDescription,
            description: "Этот чат завершён. Спасибо за беседу 😊".yre_insertNBSPs()
        )
        let mode = ChatInputView.Mode.blocked(blockedViewModel)

        let viewController = UIViewController()
        self.setupView(view, with: mode, in: viewController)

        self.assertSnapshot(viewController.view)
      }
                                                          
    private func setupView(
        _ view: ChatInputView,
        with mode: ChatInputView.Mode,
        in viewController: UIViewController
    ) {
        viewController.view.addSubview(view)
        view.yre_edgesToSuperview(insets: .init(top: .zero, left: .zero, bottom: .nan, right: .zero))

        view.backgroundColor = ColorScheme.Background.primary
        view.mode = mode
        view.setNeedsLayout()
        view.layoutIfNeeded()

        let size = CGSize(width: UIScreen.main.bounds.width, height: view.bounds.height)
        viewController.view.frame = .init(origin: .zero, size: size)
    }
}
