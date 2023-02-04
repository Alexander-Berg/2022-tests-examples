//
//  OfferCardSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import class YREAccessibilityIdentifiers.CardGalleryAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.OfferSnippetCellAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.OfferCardAccessibilityIdentifiers
import enum YREAccessibilityIdentifiers.OfferListAccessibilityIdentifiers

final class OfferCardSteps: AnyOfferCardSteps {
    enum Block {
        case gallery
    }

    func gallery() -> CardGallerySteps {
        return CardGallerySteps(element: self.galleryView)
    }

    @discardableResult
    func isOfferCardPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экран с карточкой оффера'") { _ -> Void in
            self.offerCardScreen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isLoadingIndicatorHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем, что индикация загрузки экрана скрыта") { _ -> Void in
            self.loadingIndicator.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isUserNoteNotVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что заметка скрыта") { _ -> Void in
            userNote.yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func isUserNotePresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что заметка отображается") { _ -> Void in
            userNote.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isYandexRentPromoBannerNotVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо Я.Аренды скрыто") { _ -> Void in
            self.yandexRent.yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func tapOnUserNote() -> Self {
        userNote
            .yreEnsureExistsWithTimeout()
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnAdditionalActions() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку дополнительных действий") { _ -> Void in
            self.moreNavBarButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapBackButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Назад' на карточке оффера") { _ -> Void in
            ElementsProvider.obtainBackButton()
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSiteLink() -> SiteCardSteps {
        XCTContext.runActivity(named: "Нажимаем ссылку на ЖК") { _ -> Void in
            self.siteLink.yreTap()
        }
        return SiteCardSteps()
    }

    @discardableResult
    func tapYandexRentOffersButton() -> SiteCardSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Смотреть предложения' на баннере Я.Аренды") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: Identifiers.YandexRentPromoBanner.button)
            button
                .yreEnsureExists()
                .tap()
        }
        return SiteCardSteps()
    }

    @discardableResult
    func scrollToYandexRentPromoBanner() -> Self {
        XCTContext.runActivity(named: "Скроллим к промо Я.Аренды") { _ -> Void in
            self.yandexRent.yreEnsureExistsWithTimeout()
            self.scroll(to: self.yandexRent)
        }
        return self
    }

    @discardableResult
    func scrollToSiteLink() -> Self {
        XCTContext.runActivity(named: "Скроллим к ссылке на ЖК") { _ -> Void in
            self.siteLink.yreEnsureExistsWithTimeout()
            self.scroll(to: self.siteLink)
        }
        return self
    }

    @discardableResult
    func scrollToUserNote() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку 'Заметка'") { _ -> Void in
            self.userNote.yreEnsureExistsWithTimeout()
            self.scroll(to: self.userNote)
        }
        return self
    }

    @discardableResult
    func scrollToCallbackNode() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку 'Обратный звонок'") { _ -> Void in
            self.callbackNode.yreEnsureExistsWithTimeout()
            self.scroll(to: self.callbackNode)
        }
        return self
    }

    @discardableResult
    func scrollToSimilarOffers() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку ' Похожие объявления'") { _ -> Void in
            self.similarOfferList
                .yreEnsureExistsWithTimeout()
            self.scroll(to: self.similarOfferList)
        }
        return self
    }

    @discardableResult
    func scrollToDocuments() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку 'Образцы документов'") { _ -> Void in
            self.documentsView.yreEnsureExistsWithTimeout()
            self.scroll(to: self.documentsView)
        }
        return self
    }

    @discardableResult
    func scrollToAuthorBlock() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку автора") { _ -> Void in
            self.offerCardAuthorView.yreEnsureExistsWithTimeout()
            self.scroll(to: self.offerCardAuthorView)
        }
        return self
    }

    func similarOffer(withIndex index: Int) -> OfferSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> OfferSnippetSteps in
            self.similarOfferList
                .yreEnsureExistsWithTimeout()

            let snippetListNode = ElementsProvider.obtainElement(
                identifier: Identifiers.similarOffersListNode,
                in: self.similarOfferList
            )
            snippetListNode
                .yreEnsureExistsWithTimeout()

            let cell = ElementsProvider.obtainElement(
                identifier: OfferSnippetCellAccessibilityIdentifiers.view,
                in: snippetListNode
            )
            return OfferSnippetSteps(element: cell)
        }
    }

    @discardableResult
    func isSimilarOffersListEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список похожих офферов пуст") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: OfferSnippetCellAccessibilityIdentifiers.view, in: self.similarOfferList)
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSimilarOffersListNonEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список похожих офферов пуст") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: OfferSnippetCellAccessibilityIdentifiers.view, in: self.similarOfferList)
                .yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func tapOnCallbackNodePrivacyLink() -> Self {
        let linkText = "персональных данных"
        XCTContext.runActivity(named: "Тапаем по ссылке '\(linkText)'") { _ -> Void in
            self.callbackNode.links[linkText].tap()
        }
        return self
    }

    @discardableResult
    func tapOnDocumentsButton() -> Self {
        XCTContext.runActivity(named: "Тапаем по кнопке 'Посмотреть документы'") { _ -> Void in
            ElementsProvider.obtainButton(
                identifier: Identifiers.Documents.button,
                in: self.documentsView
            ).yreEnsureExists().yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnAuthorBlockAction() -> OfferListSteps {
        XCTContext.runActivity(named: "Тапаем по кнопке 'Смотреть предложения' на блоке автора") { _ -> Void in
            ElementsProvider.obtainElement(identifier: Identifiers.Author.action, in: self.offerCardAuthorView)
                .yreEnsureExists()
                .yreTap()
        }
        return OfferListSteps(screenID: OfferListAccessibilityIdentifiers.view)
    }

    @discardableResult
    func compareSnapshot(identifier: String, block: Block) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом блока \"\(block.title)\" на карточке оффера") { _ -> Void in
            // TODO @pavelcrane: scroll to the given block right here (?)

            let element = self.element(byBlock: block)
            let edgesToIgnore = self.edgesToIgnore(byBlock: block)

            element
                .yreEnsureExistsWithTimeout()

            // Some blocks may be in a loading state for a few seconds
            if let delay = self.delay(byBlock: block) {
                sleep(UInt32(delay))
            }

            let screenshot = element
                .yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot,
                                         identifier: identifier,
                                         ignoreEdges: edgesToIgnore)
        }
        return self
    }

    // MARK: Private

    private typealias Identifiers = OfferCardAccessibilityIdentifiers

    private lazy var offerCardScreen: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var offerCardScrollView = ElementsProvider.obtainElement(
        identifier: Identifiers.scrollView,
        in: self.offerCardScreen
    )
    private lazy var offerCardAuthorView = ElementsProvider.obtainElement(
        identifier: Identifiers.Author.view,
        in: self.offerCardScrollView
    )
    private lazy var freeExcerptView = ElementsProvider.obtainElement(
        identifier: Identifiers.freeExcerptView,
        in: self.offerCardScrollView
    )
    private lazy var galleryView = ElementsProvider.obtainElement(
        identifier: CardGalleryAccessibilityIdentifiers.view,
        in: self.offerCardScreen
    )
    private lazy var siteLink = ElementsProvider.obtainElement(
        identifier: Identifiers.categoryTitle,
        in: self.offerCardScrollView
    )
    private lazy var yandexRent = ElementsProvider.obtainElement(
        identifier: Identifiers.YandexRentPromoBanner.view,
        in: self.offerCardScrollView
    )
    private lazy var callbackNode = ElementsProvider.obtainElement(
        identifier: Identifiers.callbackNode,
        in: self.offerCardScrollView
    )
    private lazy var buttonsBlock = ElementsProvider.obtainElement(
        identifier: OfferCardAccessibilityIdentifiers.buttonsBlock,
        in: self.offerCardScrollView
    )
    private lazy var similarOfferList = ElementsProvider.obtainElement(
        identifier: Identifiers.similarOffers,
        in: self.offerCardScrollView
    )
    private lazy var documentsView = ElementsProvider.obtainElement(
        identifier: Identifiers.Documents.view,
        in: self.offerCardScrollView
    )
    private lazy var moreNavBarButton = ElementsProvider.obtainElement(identifier: Identifiers.moreNavigationBarButton)
    private lazy var userNote = ElementsProvider.obtainElement(identifier: Identifiers.userNote)
    private lazy var loadingIndicator = ElementsProvider.obtainElement(identifier: Identifiers.activityIndicator)

    private func scroll(to element: XCUIElement) {
        let submitButtonFrame = self.callButton.frame
        let normalizedOffsetY = self.galleryView.frame.height / self.offerCardScrollView.frame.height + 0.05
        self.offerCardScrollView
            .scroll(
                to: element,
                // We have a Navigation panel at the top and a Call button at the bottom
                adjustInteractionFrame: { interactionFrame in
                    var frame = interactionFrame.yreSubtract(ElementsProvider.obtainNavigationBar().frame, from: .minYEdge)
                    if element.frame.maxY < self.buttonsBlock.frame.maxY {
                        frame = frame.yreSubtract(submitButtonFrame, from: .maxYEdge)
                    }
                    return frame
                },
                // Swipe slowly to not move the element too high
                velocity: 0.3,
                // With a slower speed we need more swipes
                swipeLimits: 10,
                // Use this `dy` value to start swipe gesture below the Gallery block
                normalizedOffset: .init(dx: 0.01, dy: normalizedOffsetY)
            )
    }
}

extension OfferCardSteps.Block {
    fileprivate var title: String {
        switch self {
            case .gallery:
                return "Галерея"
        }
    }
}

extension OfferCardSteps {
    private func element(byBlock block: Block) -> XCUIElement {
        switch block {
            case .gallery:
                return self.galleryView
        }
    }

    private func edgesToIgnore(byBlock block: Block) -> UIEdgeInsets {
        switch block {
            case .gallery:
                let ignoredEdges = XCUIApplication().yre_ignoredEdges()
                return .init(top: ignoredEdges.top, left: 0, bottom: 0, right: 0)
        }
    }

    private func delay(byBlock block: Block) -> TimeInterval? {
        switch block {
            case .gallery:
                return 3
        }
    }
}
