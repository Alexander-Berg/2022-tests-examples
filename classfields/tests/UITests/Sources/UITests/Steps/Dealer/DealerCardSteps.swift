//
//  DealerCardSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 16.07.2020.
//

import XCTest
import Snapshots

class DealerCardSteps: BaseSteps {
    func onScreen() -> DealerCardScreen {
        return self.baseScreen.on(screen: DealerCardScreen.self)
    }

    @discardableResult
    func descriptionTitleExists() -> Self {
        Step("Проверяем, что доступен элемент с текстом 'Предложения дилера'") {
            onScreen().descriptionTitle.shouldExist()
        }
        return self
    }

    @discardableResult
    func navBarTitleVisible() -> Self {
        XCTAssert(onScreen().titleView.isFullyVisible() && onScreen().titleView.isHittable)
        return self
    }

    @discardableResult
    func navBarCallButtonVisible() -> Self {
        XCTAssert(onScreen().callNavBarView.isFullyVisible() && onScreen().callNavBarView.isHittable)
        return self
    }

    @discardableResult
    func filtersButtonVisible() -> Self {
        XCTAssert(onScreen().filtersButton.isFullyVisible() && onScreen().filtersButton.isHittable)
        return self
    }

    func swipeUp() -> Self {
        onScreen().swipe(.up)
        return self
    }

    func swipeDown() -> Self {
        onScreen().scrollableElement.swipeDown()
        return self
    }

    func tapMap() -> Self {
        onScreen().map.tap()
        return self
    }

    func tapCallButton() -> Self {
        onScreen().callButton.tap()
        return self
    }

    @discardableResult
    func saveNavBarButtonHasUnsavedState() -> Self {
        XCTAssertEqual(onScreen().dealerSaveNavBarView.value as? String, "Сохранить")
        return self
    }

    @discardableResult
    func saveNavBarButtonHasSavingState() -> Self {
        XCTAssertEqual(onScreen().dealerSaveNavBarView.value as? String, "Сохранение")
        return self
    }

    @discardableResult
    func saveNavBarButtonHasSavedState() -> Self {
        XCTAssertEqual(onScreen().dealerSaveNavBarView.value as? String, "Удалить")
        return self
    }

    @discardableResult
    func saveNavBarButtonHasRemovingState() -> Self {
        XCTAssertEqual(onScreen().dealerSaveNavBarView.value as? String, "Удаление")
        return self
    }

    func tapSaveNavBarButton() -> Self {
        onScreen().dealerSaveNavBarView.tap()
        return self
    }

    func tapOnBackButton() {
        Step("Тапаем кнопку возврата в навбаре") {
            onScreen().backButton.tap()
        }
    }

    func alertExist() {
        onScreen().find(by: "Закрыть").firstMatch.shouldExist(timeout: 10)
    }

    func tapCallBackButton() -> Self {
        onScreen().callBackButton.tap()
        return self
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 32, left: 0, bottom: 32, right: 0))
            return self
        }
    }

    @discardableResult
    func checkDealerName(_ title: String) -> Self {
        step("Проверяем, название дилера '\(title)' на экране") {
            onScreen().find(by: title).firstMatch.shouldExist()
        }
    }
}

extension DealerCardSteps: UIElementProvider {
    enum Element: String {
        case moreButton = "more_button"
        case complainButton = "Пожаловаться на объявление"
        case saledButton = "Продано"
        case address = "address"
    }
}
