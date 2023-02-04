//
//  ChatSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 05.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.ChatAccessibilityIdentifiers
import YRETestsUtils

final class ChatSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана Чат") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isUnauthorizedViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие вьюшки незарегистрированного пользователя") { _ -> Void in
            self.unauthorizedView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isErrorViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие вьюшки ошибки") { _ -> Void in
            self.errorView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isEmptyViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие заглушки пустого чата") { _ -> Void in
            self.emptyView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func compareEmptyViewWithSnapshot() -> Self {
        XCTContext.runActivity(named: "Сравниваем заглушку чата с имеющимся снэпшотом") { _ -> Void in
            self.emptyView
                .yreEnsureExistsWithTimeout()
                .yreWaitAndCompareScreenshot(identifier: "techSupportChatTests.emptyView")
        }
        return self
    }

    @discardableResult
    func isContentViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие чата") { _ -> Void in
            self.contentView
                .yreEnsureExistsWithTimeout()
                // Chat content view always exists. Make sure that it's really visible
                .yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isLoginButtonHittable() -> Self {
        XCTContext.runActivity(named: "Проверяем нажимаемость кнопки Войти") { _ -> Void in
            self.loginButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isSendButtonEnabled() -> Self {
        XCTContext.runActivity(named: "Проверяем нажимаемость кнопки Отправить") { _ -> Void in
            self.sendButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
        }
        return self
    }

    @discardableResult
    func isSendButtonDisabled() -> Self {
        XCTContext.runActivity(named: "Проверяем ненажимаемость кнопки Отправить") { _ -> Void in
            self.sendButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureNotEnabled()
        }
        return self
    }

    @discardableResult
    func isBlockedInputViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что чат заблокирован") { _ -> Void in
            self.blockedInputView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    // MARK: - Actions

    @discardableResult
    func tapRetryButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку Повторить") { _ -> Void in
            self.retryButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func scrollChatToTop() -> Self {
        XCTContext.runActivity(named: "Скроллим чат к началу") { _ -> Void in
            XCUIApplication().yreTapStatusBar()
        }
        return self
    }

    @discardableResult
    func typeText(_ text: String) -> Self {
        XCTContext.runActivity(named: "Вводим текст \(text)") { _ -> Void in
            self.inputTextView
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapSendButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку отправить") { _ -> Void in
            self.sendButton
                .yreEnsureExistsWithTimeout()
                // @l-saveliy: Not hittable by some reason
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func rateFirstTechSupportPoll(rate: Int) -> Self {
        XCTContext.runActivity(named: "Ставим рейтинг \(rate) в голосовалке") { _ -> Void in
            let poll = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.techSupportPoll,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()

            ElementsProvider
                .obtainButton(identifier: Identifiers.Message.techSupportPoll + ".\(rate)", in: poll)
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func selectFirstTechSupportPreset() -> Self {
        XCTContext.runActivity(named: "Выбираем первый пресет") { _ -> Void in
            let presets = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.techSupportPreset,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()

            presets
                .descendants(matching: .button)
                .element(boundBy: 0)
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func selectFirstButton() -> Self {
        XCTContext.runActivity(named: "Выбираем первый пресет") { _ -> Void in
            let buttons = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.buttons,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()

            buttons
                .descendants(matching: .button)
                .element(boundBy: 0)
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapBackButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку назад") { _ -> Void in
            ElementsProvider
                .obtainBackButton()
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOfferPanelView() -> Self {
        XCTContext.runActivity(named: "Тапаем по панели оффера") { _ -> Void in
            self.offerPanelView
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapChatActionsButton() -> Self {
        XCTContext.runActivity(named: "Тапаем по кнопке действий с чатом") { _ -> Void in
            self.chatActionsButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapUnblockChatButton() -> Self {
        XCTContext.runActivity(named: "Тапаем по кнопке \"Разлокировать чать\"") { _ -> Void in
            self.unblockChatButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOpenTechSupportButton() -> Self {
        XCTContext.runActivity(named: "Тапаем по кнопке \"Написать в ТП\"") { _ -> Void in
            self.openTechSupportButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapCallOfferButton() -> Self {
        XCTContext.runActivity(named: "Тапаем по кнопке \"Позвонить\"") { _ -> Void in
            self.callOfferButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOpenOtherSitesButton() -> Self {
        XCTContext.runActivity(named: "Тапаем по кнопке \"Посмотреть другие ЖК\"") { _ -> Void in
            self.openOtherSitesButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapFirstMessageLink() -> Self {
        guard let linkElement = XCTContext.runActivity(
            named: "Ищем ссылку в сообщении",
            block: { _ in
                ElementsProvider
                    .obtainElement(identifier: Identifiers.Message.string, in: self.contentView)
                    .links
                    .allElementsBoundByIndex
                    .last
            }
        )
        else {
            XCTFail("В сообщении нет ссылки")
            return self
        }

        XCTContext.runActivity(named: "Тапаем по ссылке (\(linkElement.label))") { _ -> Void in
            linkElement
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    // MARK: - Snapshots

    @discardableResult
    func compareFirstStringMessageWithSnapshot(snapshotID: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем первое текстовое сообщение со снапшотом") { _ -> Void in
            let screenshot = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.string,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()
                .yreWaitAndScreenshot()

            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotID)
        }
        return self
    }

    @discardableResult
    func compareFirstPhotoMessageWithSnapshot(snapshotID: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем первое фото сообщение со снапшотом") { _ -> Void in
            let screenshot = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.photo,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()
                .yreWaitAndScreenshot()

            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotID)
        }
        return self
    }

    @discardableResult
    func compareFirstTechSupportPresetsWithSnapshot(snapshotID: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем пресеты фидбека со снапшотом") { _ -> Void in
            let screenshot = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.techSupportPreset,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()
                .yreWaitAndScreenshot()

            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotID)
        }
        return self
    }

    @discardableResult
    func compareFirstButtonsMessageWithSnapshot(snapshotID: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем сообщение с кнопками со снапшотом") { _ -> Void in
            let screenshot = ElementsProvider.obtainElement(
                identifier: Identifiers.Message.buttons,
                in: self.contentView
            )
                .yreEnsureExistsWithTimeout()
                .yreWaitAndScreenshot()

            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotID)
        }
        return self
    }

    // MARK: Private

    typealias Identifiers = ChatAccessibilityIdentifiers

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.screenView)
    private lazy var unauthorizedView = ElementsProvider.obtainElement(
        identifier: Identifiers.unauthorizedView,
        in: self.screenView
    )
    private lazy var errorView = ElementsProvider.obtainElement(
        identifier: Identifiers.errorView,
        in: self.screenView
    )
    private lazy var emptyView = ElementsProvider.obtainElement(
        identifier: Identifiers.emptyView,
        in: self.screenView
    )
    private lazy var loginButton = ElementsProvider.obtainElement(
        identifier: Identifiers.loginButton,
        in: self.unauthorizedView
    )
    private lazy var retryButton = ElementsProvider.obtainElement(
        identifier: Identifiers.retryButton,
        in: self.errorView
    )
    private lazy var contentView = ElementsProvider.obtainElement(
        identifier: Identifiers.contentView,
        in: self.screenView
    )
    private lazy var inputTextView = ElementsProvider.obtainElement(
        identifier: Identifiers.inputTextView,
        in: self.screenView
    )
    private lazy var sendButton = ElementsProvider.obtainElement(
        identifier: Identifiers.sendButton,
        in: self.screenView
    )
    private lazy var blockedInputView = ElementsProvider.obtainElement(
        identifier: Identifiers.blockedInputView,
        in: self.screenView
    )
    private lazy var unblockChatButton = ElementsProvider.obtainElement(
        identifier: Identifiers.unblockChatButton,
        in: self.screenView
    )
    private lazy var openTechSupportButton = ElementsProvider.obtainElement(
        identifier: Identifiers.openTechSupportButton,
        in: self.screenView
    )
    private lazy var callOfferButton = ElementsProvider.obtainElement(
        identifier: Identifiers.callOfferButton,
        in: self.screenView
    )
    private lazy var openOtherSitesButton = ElementsProvider.obtainElement(
        identifier: Identifiers.openOtherSitesButton,
        in: self.screenView
    )
    private lazy var offerPanelView = ElementsProvider.obtainElement(
        identifier: Identifiers.offerPanelView,
        in: self.screenView
    )
    private lazy var chatActionsButton = ElementsProvider.obtainButton(identifier: Identifiers.chatActionsButton)
}
