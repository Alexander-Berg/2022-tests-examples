//
//  YaRentContractFAQSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentContractAccessibilityIdentifiers

final class YaRentContractFAQSteps {
    @discardableResult
    func ensureFAQPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие списка вопросов на экране") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureQuestionViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие вопросов на экране") { _ -> Void in
            self.questionView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureAnswerViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ответа на экране") { _ -> Void in
            self.answerView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureAnswerViewNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие ответа на экране") { _ -> Void in
            self.answerView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnExpandButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку раскрытия/скрытия ответа") { _ -> Void in
            self.expandButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnCommentButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку задания вопроса") { _ -> Void in
            self.view.scrollToElement(element: self.commentButton, direction: .up)
            self.commentButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func ensureCommentButtonNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутсвие кнопки задания вопроса") { _ -> Void in
            self.commentButton.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnBackButton() -> Self {
        XCTContext.runActivity(named: "Закрываем экран FAQ") { _ -> Void in
            let backButton = ElementsProvider.obtainBackButton()
            backButton.yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentContractAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.FAQ.view)
    private lazy var questionView = ElementsProvider.obtainElement(identifier: Identifiers.FAQ.questionView(with: "5"))
    private lazy var answerView = ElementsProvider.obtainElement(identifier: Identifiers.FAQ.answerView(with: "5"))
    private lazy var expandButton = ElementsProvider.obtainElement(identifier: Identifiers.FAQ.expandButton(with: "5"))
    private lazy var commentButton = ElementsProvider.obtainElement(identifier: Identifiers.FAQ.commentButton)
}
