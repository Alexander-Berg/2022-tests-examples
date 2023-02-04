//
//  FavoritesSteps.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/18/19.
//

import Foundation
import UIKit

class FavoritesSteps: BaseSteps {
    func onFavoritesScreen() -> FavoritesScreen {
        return baseScreen.on(screen: FavoritesScreen.self)
    }

    func scrollOnce() -> Self {
        onFavoritesScreen().scrollableElement.gentleSwipe(.up)
        return self
    }

    func scrollToSpecials() -> Self {
        onFavoritesScreen().scrollTo(element: onFavoritesScreen().recommendationsCell, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 50, right: 0))
        return self
    }

    func scrollRecommendations() -> Self {
        onFavoritesScreen().recommendationsCell.swipeLeft()
        return self
    }

    func tapRecommendations() -> Self {
        let normalCoord = onFavoritesScreen().recommendationsCell.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        let coord = normalCoord.withOffset(CGVector(dx: 35, dy: 50))
        coord.tap()
        return self
    }

    func scrollDown() -> Self {
        onFavoritesScreen().collectionView.swipeUp()
        return self
    }

    func tapHideRecommendations() -> Self {
        onFavoritesScreen().find(by: "HideRecommendationsButton").firstMatch.tap()
        return self
    }

    func tapHideRecommendationsInternal() -> Self {
        onFavoritesScreen().find(by: "Скрыть рекомендации").firstMatch.tap()
        return self
    }

    func tapOnCreditPrice() -> SaleCardSteps {
        onFavoritesScreen().find(by: "carCreditPrice").firstMatch.tap()
        return SaleCardSteps(context: context)
    }

    // MARK: - Actions
    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждем, пока загрузится экран") {
            self.onFavoritesScreen().refreshingControl.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkHasUpdatesView(title: String) -> Self {
        Step("Проверяем плашку наличия обновлений с текстом `\(title)`") {
            let view = self.onFavoritesScreen().updatesView
            view.shouldExist()

            Step("Сравниваем текст") {
                view.staticTexts[title].shouldExist()
            }
        }

        return self
    }

    @discardableResult
    func checkHasTabbarUpdates() -> Self {
        Step("Проверяем бейдж обновлений в таббаре") {
            self.onFavoritesScreen()
                .tabbarBadge.shouldExist()
        }

        return self
    }

    @discardableResult
    func checkSegmentHasUpdates(at tab: FavoritesScreen.Segment) -> Self {
        Step("Проверяем бейдж обновлений на сегменте [\(tab.rawValue)]") {
            self.onFavoritesScreen()
                .segmentBadge(at: tab).shouldExist()
        }

        return self
    }

    @discardableResult
    func checkSegmentHasNoUpdates(at tab: FavoritesScreen.Segment) -> Self {
        Step("Проверяем, что нет бейджа обновлений на сегменте [\(tab.rawValue)]") {
            self.onFavoritesScreen()
                .segmentBadge(at: tab).shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkHasNoUpdatesView() -> Self {
        Step("Проверяем, что нет плашки обновлений") {
            self.onFavoritesScreen()
                .updatesView.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkHasNoTabbarUpdates() -> Self {
        Step("Проверяем, что нет бейджа обновлений в таббаре") {
            self.onFavoritesScreen()
                .tabbarBadge.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func hideUpdates() -> Self {
        Step("Закрываем плашку обновлений") {
            self.onFavoritesScreen()
                .updatesView.tap()
        }

        return self
    }

    @discardableResult
    func checkHasPlaceholder(isAuthorized: Bool) -> Self {
        Step("Ищем плейсхолдер объявлений") {
            if isAuthorized {
                let labels = self.app.staticTexts
                labels["Войти"].shouldNotExist()

                let text = "Сохраняйте понравившиеся объявления, узнавайте об изменении цен"
                let predicate = NSPredicate(format: "label LIKE %@", text)
                labels.matching(predicate).firstMatch.shouldExist()
            } else {
                let labels = self.app.staticTexts
                labels["Войти"].shouldExist()

                let text = "Войдите, чтобы сохранять понравившиеся объявления и узнавать об изменении цен на всех устройствах."
                let predicate = NSPredicate(format: "label LIKE %@", text)
                labels.matching(predicate).firstMatch.shouldExist()
            }
        }

        return self
    }

    @discardableResult
    func tapSegment(at tab: FavoritesScreen.Segment) -> Self {
        Step("Тапаем сегмент [\(tab.rawValue)]") {
            self.onFavoritesScreen()
                .segmentControl(at: tab).tap()
        }

        return self
    }

    @discardableResult
    func tapSavedSearch(id: String, index: Int) -> Self {
        Step("Тапаем сохраненный поиск [\(index)]") {
            self.onFavoritesScreen()
                .savedSearch(id: id, index: index).tap()
        }

        return self
    }

    @discardableResult
    func goBack() -> Self {
        Step("Возвращаемся назад") {
            self.onFavoritesScreen()
                .navbarView
                .buttons.firstMatch
                .tap()
        }

        return self
    }

    @discardableResult
    func checkHasOfferCallButton() -> Self {
        Step("Проверяем, что показывается кнопка звонка на оффере") {
            self.onFavoritesScreen()
                .offerCallButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func checkHasOfferCallCounter() -> Self {
        Step("Проверяем, что показывается счётчик звонков за сутки") {
            self.onFavoritesScreen()
                .offer24HoursCallsCount.shouldExist()
        }

        return self
    }

    @discardableResult
    func checkHasNotOfferCallButton() -> Self {
        Step("Проверяем, что не показывается кнопка звонка на проданном оффере") {
            self.onFavoritesScreen()
                .offerCallButton.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkVerifiedDealerSubtitle() -> Self {
        Step(#"Проверяем, что показан подзаголовок "Проверенный дилер""#) {
            self.onFavoritesScreen()
                .find(by: "Проверенный дилер").firstMatch.shouldExist()

        }

        return self
    }

    @discardableResult
    func checkHasShowReportButton() -> Self {
        Step("Проверяем, что показывается кнопка Смотреть отчет") {
            self.onFavoritesScreen()
                .offerShowReportButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func showReport() -> ReportCreditSteps {
        Step("Открываем отчет из листинга") {
            self.onFavoritesScreen().offerShowReportButton.tap()
        }

        return ReportCreditSteps(context: context)
	}

    @discardableResult
    func checkHasNotOfferRecommendations() -> Self {
        Step("Проверяем, что не показывается блок рекомендаций") {
            self.onFavoritesScreen()
                .recommendationsCell.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkTitleLabelHasText(_ text: String) -> Self {
        Step("Проверяем, что нужный заголовок объявления") {
            self.onFavoritesScreen()
                .titleNavLabel.containsText(text)
        }

        return self
    }

    func creditPromo() -> CreditPromoSteps {
        return CreditPromoSteps(context: context)
    }
}
