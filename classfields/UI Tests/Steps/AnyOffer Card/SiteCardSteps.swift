//
//  SiteCardSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 18.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

// swiftlint:disable:next type_body_length
final class SiteCardSteps: AnyOfferCardSteps {
    enum CellToCompareWithSnapshot {
        case noDeveloper
        case photos
        case callback
    }

    enum CardInfoCell {
        case ceilingHeight
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экран с карточкой ЖК") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func isLoadingIndicatorHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем что индикация загрузки экрана скрыта") { _ -> Void in
            self.loadingCell.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSubmitApplicationAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие формы с заявкой в квартирографии") { _ -> Void in
            self.submitApplicationView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSubmitApplicationUnavailable() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие формы с заявку в квартирографии") { _ -> Void in
            self.submitApplicationView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNoDevelopersCellAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки с ошибкой в квартирографии") { _ -> Void in
            self.noDevelopersCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSubfilterCellAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки с фильтрами") { _ -> Void in
            self.subfiltersCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isResetFiltersCellAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки для сброса фильтров") { _ -> Void in
            self.resetFiltersCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSubmitApplicationButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Оставить заявку\"") { _ -> Void in
            self.submitApplicationButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapOnSubmitApplicationButton() -> CallApplicationSteps {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Оставить заявку"#) { _ in
            self.submitApplicationButton
                .yreTap()

            return .init()
        }
    }

    @discardableResult
    func isResaleOffersCellTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"От агенств и частных лиц\"") { _ -> Void in
            self.resaleOffersCell
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isPlanCellTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки с планировками в ЖК") { _ -> Void in
            self.planCell
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isRoomStatisticsCellTappable(title: String) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки со статистикой в ЖК (\(title))") { _ -> Void in
            self.scrollToRoomStatisticsCell(title: title)

            self.roomStatisticsCell(title: title)
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult 
    func isAnchorsCellAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки с якорями") { _ -> Void in
            self.anchorsCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult 
    func isPriceHistoryCellAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки с историей цен") { _ -> Void in
            self.priceHistoryCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isToursCellAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ячейки с 3д-турами") { _ -> Void in
            self.toursCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isConstructionStateCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Ход строительства""#) { _ -> Void in
            self.constructionStateCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isLocationCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Расположение""#) { _ -> Void in
            self.locationCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isCallbackCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Обратный звонок""#) { _ -> Void in
            self.callbackCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isDocumentsCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Документация""#) { _ -> Void in
            self.documentsCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSpecialProposalsCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Скидки и акции""#) { _ -> Void in
            self.specialProposalsCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isDeveloperCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Участники строительства""#) { _ -> Void in
            self.developerCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isDecorationCellAvailable() -> Self {
        XCTContext.runActivity(named: #"Проверяем наличие блока "Варианты отделки""#) { _ -> Void in
            self.decorationCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapSubmitOrResetButtonInSubfilterCell() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку посмотреть все квартиры (сбросить фильтры) в ЖК") { _ -> Void in
            let resetButton = ElementsProvider.obtainButton(
                identifier: AccessibilityIdentifiers.Subfilters.submitButton,
                in: self.subfiltersView
            )

            resetButton.yreTap()
        }
        return self
    }

    @discardableResult
    func tapPhotosCell() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ячейку с фотографиями о ЖК") { _ -> Void in
            self.photosCell
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnAnchor(_ name: String) -> Self {
        XCTContext.runActivity(named: "Нажимаем на якорь `\(name)`") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: name, in: self.anchorsCell)
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }
    
    @discardableResult
    func isAnchorExist(_ name: String) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие якоря `\(name)`") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: name, in: self.anchorsCell)
                .yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func isAnchorNotExist(_ name: String) -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие якоря `\(name)`") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: name, in: self.anchorsCell)
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func scrollToCardInfoCell(_ cell: CardInfoCell) -> Self {
        XCTContext.runActivity(named: "Скроллим до ячейки '\(cell.name)'") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: AccessibilityIdentifiers.CardInfo.ceilingHeight,
                in: self.screen
            )
            self.screen.scrollToElement(element: cell, direction: .up)
            self.scrollToElement(element: cell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func makeCardInfoCellScreenshot(_ cell: CardInfoCell, identifier: String) -> Self {
        XCTContext.runActivity(named: "Скриншотим ячейку '\(cell.name)'") { _ -> Void in
            ElementsProvider.obtainElement(
                identifier: AccessibilityIdentifiers.CardInfo.ceilingHeight,
                in: self.screen
            ).yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    @discardableResult
    func tapRoomStatisticsCell(title: String) -> SiteOfferListByPlanSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку со статистикой в ЖК (\(title))") { _ -> Void in            
            self.roomStatisticsCell(title: title)
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return SiteOfferListByPlanSteps()
    }

    @discardableResult
    func scrollToPlanCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к ячейке с планировками") { _ -> Void in
            self.screen.scrollToElement(element: self.planCell, direction: .up, swipeLimits: 10)
        }
        return self
    }

    @discardableResult
    func tapPlanCell() -> OfferPlanListSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку с планировками в ЖК") { _ -> Void in
            self.planCell
                .yreTap()
        }
        return OfferPlanListSteps()
    }

    @discardableResult
    func scrollToSubfilters() -> SiteSubfilterSteps {
        XCTContext.runActivity(named: "Скроллим к блоку с фильтрами") { _ -> SiteSubfilterSteps in
            self.subfiltersView.yreEnsureExistsWithTimeout()
            self.screen.scrollToElement(element: self.subfiltersView, direction: .up)
            return SiteSubfilterSteps()
        }
    }

    @discardableResult
    func scrollToResaleOffers() -> Self {
        XCTContext.runActivity(named: "Скроллим к ячейке \"От агентств и частных лиц\"") { _ -> Void in
            self.screen.scrollToElement(element: self.resaleOffersCell, direction: .up)
        }
        return self
    }

    @discardableResult
    func tapOnResaleOffers() -> OfferListSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку \"От агентств и частных лиц\"") { _ -> Void in
            self.resaleOffersCell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return OfferListSteps(screenID: ResaleOffersListAccessibilityIdentifiers.view)
    }

    @discardableResult
    func tapPriceStatisticsButton() -> SitePriceStatisticsSteps {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Показать динамику цен""#) { _ -> Void in
            self.priceHistoryButton
                .yreTap()
        }
        return SitePriceStatisticsSteps()
    }

    @discardableResult
    func taOnTourCell(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем тур с индексом \(index) ") { _ -> Void in
            let tourCell = ElementsProvider.obtainElement(
                identifier: SiteCardAccessibilityIdentifiers.Tours.tourCell(index: index),
                in: self.toursCell
            )

            tourCell
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func compareCellWithScreenshot(identifier: String, cell: CellToCompareWithSnapshot) -> Self {
        XCTContext.runActivity(named: "Сравниваем ячейку \"\(cell.title)\" с имеющимся скриншотом") { _ -> Void in
            let element = self.element(byCell: cell)
            let screenshot = element.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func scrollToNoDevelopersCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу с ошибкой в квартирографии") { _ -> Void in
            self.screen.scrollToElement(element: self.noDevelopersCell, direction: .up, swipeLimits: 2)
        }
        return self
    }

    @discardableResult
    func scrollNoDevelopersCellToCenter() -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу с ошибкой в квартирографии") { _ -> Void in
            self.scrollToElement(element: self.noDevelopersCell, velocity: 0.5)
        }
        return self
    }

    @discardableResult
    func scrollToSubmitApplicationView() -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу с обратной заявкой квартирографии") { _ -> Void in
            self.screen.scrollToElement(element: self.submitApplicationView, direction: .up, swipeLimits: 2)
        }
        return self
    }

    @discardableResult
    func scrollSubmitApplicationViewToCenter() -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу с обратной заявкой квартирографии") { _ -> Void in
            self.scrollToElement(element: self.submitApplicationView, velocity: 0.5)
        }
        return self
    }

    @discardableResult
    func scrollToResetFiltersCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу с ошибкой в квартирографии") { _ -> Void in
            self.resetFiltersCell.yreEnsureExistsWithTimeout()
            self.scrollToElement(element: self.resetFiltersCell)
        }
        return self
    }

    @discardableResult
    func scrollToRoomStatisticsCell(title: String) -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу со статистикой квартир (\(title))") { _ -> Void in
            let cell = self.roomStatisticsCell(title: title)
            self.screen.scrollToElement(element: cell, direction: .up)
            self.scrollToElement(element: cell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToCallbackCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку обратного звонка") { _ -> Void in
            self.screen.scrollToElement(element: self.callbackCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.callbackCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToConstructionStateCell() -> Self {
        XCTContext.runActivity(named: #"Скроллим к блоку "Ход строительства""#) { _ -> Void in
            self.screen.scrollToElement(element: self.constructionStateCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.constructionStateCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToLocationCell() -> Self {
        XCTContext.runActivity(named: #"Скроллим к блоку "Расположение""#) { _ -> Void in
            self.screen.scrollToElement(element: self.locationCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.locationCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func tapOnCallbackCellPrivacyLink() -> Self {
        let linkText = "персональных данных"
        XCTContext.runActivity(named: "Тапаем по ссылке '\(linkText)'") { _ -> Void in
            self.callbackCell.links[linkText].tap()
        }
        return self
    }

    @discardableResult
    func scrollToPriceHistoryCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку истории цен") { _ -> Void in
            self.screen.scrollToElement(element: self.priceHistoryCell, direction: .up, swipeLimits: 15)
        }
        return self
    }

    @discardableResult
    func scrollToSiteWrapperCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к ячейке ЖК") { _ -> Void in
            self.screen.scrollToElement(element: self.siteWrapperCell, direction: .up, swipeLimits: 30)
            self.scrollToElement(element: self.siteWrapperCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToToursCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к блоку с 3д-турами") { _ -> Void in
            self.screen.scrollToElement(element: self.toursCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.toursCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToDocumentsCell() -> Self {
        XCTContext.runActivity(named: #"Скроллим к блоку ""Документация"#) { _ -> Void in
            self.screen.scrollToElement(element: self.documentsCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.documentsCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToSpecialProposalsCell() -> Self {
        XCTContext.runActivity(named: #"Скроллим к блоку ""Скидки и акции"#) { _ -> Void in
            self.screen.scrollToElement(element: self.specialProposalsCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.specialProposalsCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToDeveloperCell() -> Self {
        XCTContext.runActivity(named: #"Скроллим к блоку ""Участники строительства"#) { _ -> Void in
            self.screen.scrollToElement(element: self.developerCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.developerCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func scrollToDecorationCell() -> Self {
        XCTContext.runActivity(named: #"Скроллим к блоку ""Варианты отделки"#) { _ -> Void in
            self.screen.scrollToElement(element: self.decorationCell, direction: .up, swipeLimits: 15)
            self.scrollToElement(element: self.decorationCell, velocity: 0.4)
        }
        return self
    }

    @discardableResult
    func siteSnippet() -> SiteSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент с ячейкой ЖК") { _ -> SiteSnippetSteps in
            let snippetView = ElementsProvider.obtainElement(
                identifier: SiteSnippetCellAccessibilityIdentifiers.viewIdentifier,
                in: self.siteWrapperCell
            )
            snippetView.yreEnsureExistsWithTimeout()

            let snippet = SiteSnippetSteps(element: snippetView)
            return snippet
        }
    }

    // MARK: Private

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)

    private var submitApplicationView: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.SubmitApplicationForm.view,
            type: .any,
            in: self.screen
        )
    }

    private var submitApplicationButton: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.SubmitApplicationForm.submitButton,
            type: .button,
            in: self.submitApplicationView
        )
    }

    private var noDevelopersCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.NoDevelopersCell.cell,
            type: .cell,
            in: self.screen
        )
    }

    private var resetFiltersCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.ResetFiltersCell.cell,
            type: .any,
            in: self.screen
        )
    }

    private var resaleOffersCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.resaleOffersCell,
            type: .any,
            in: self.screen
        )
    }

    private var planCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.FlatPlans.cell,
            type: .any,
            in: self.screen
        )
    }

    private func roomStatisticsCell(title: String) -> XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.roomStatisticsCell(title: title),
            type: .any,
            in: self.screen
        )
    }

    private var photosCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.photosCell,
            type: .any,
            in: self.screen
        )
    }

    private var anchorsCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.anchorsCell,
            type: .any,
            in: self.screen
        )
    }

    private var subfiltersCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.Subfilters.cell)
    }

    private var subfiltersView: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.Subfilters.view)
    }

    private var loadingCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: CardComponentsAccessibilityIdentifiers.loadingCell)
    }

    private var callbackCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.callbackCell)
    }

    private var priceHistoryButton: XCUIElement {
        ElementsProvider.obtainButton(
            identifier: AccessibilityIdentifiers.priceHistoryButton,
            in: priceHistoryCell
        )
    }
    
    private var priceHistoryCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.priceHistory)
    }

    private var toursCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.Tours.cell)
    }

    private var constructionStateCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.ConstructionState.cell)
    }

    private var locationCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.Location.cell)
    }

    private var documentsCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.Documents.cell)
    }

    private var specialProposalsCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.SpecialProposals.cell)
    }

    private var developerCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.Developer.cell)
    }

    private func compareWithScreenshot(element: XCUIElement, identifier: String) {
        let screenshot = element.yreWaitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
    }

    private var siteWrapperCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.siteWrapperCell)
    }

    private var decorationCell: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.SiteDecoration.cell)
    }

    private func scrollToElement(
        element: XCUIElement,
        velocity: CGFloat = 1.0,
        swipeLimits: UInt = 5
    ) {
        XCTContext.runActivity(named: "Скроллим к элементу") { _ -> Void in
            guard element.exists else { assertionFailure("Element doesn't exists"); return }

            let submitButtonFrame = self.callButton.frame
            self.screen.scroll(
                to: element,
                adjustInteractionFrame: { interactionFrame in
                    interactionFrame
                        .yreSubtract(submitButtonFrame, from: .maxYEdge)
                        .yreSubtract(ElementsProvider.obtainNavigationBar().frame, from: .minYEdge)
                },
                velocity: velocity,
                swipeLimits: swipeLimits
            )
        }
    }
    
    private typealias AccessibilityIdentifiers = SiteCardAccessibilityIdentifiers
}

extension SiteCardSteps.CellToCompareWithSnapshot {
    fileprivate var title: String {
        switch self {
            case .noDeveloper:
                return "Нет предложений от застройщика" // ?
            case .photos:
                return "Фото"
            case .callback:
                return "Обратный звонок"
        }
    }
}

extension SiteCardSteps {
    private func element(byCell cell: CellToCompareWithSnapshot) -> XCUIElement {
        switch cell {
            case .noDeveloper:
                return self.noDevelopersCell
            case .photos:
                return self.photosCell
            case .callback:
                return self.callbackCell
        }
    }
}

extension SiteCardSteps.CardInfoCell {
    fileprivate var name: String {
        switch self {
            case .ceilingHeight:
                return "Высота потолков"
        }
    }

    fileprivate var element: String {
        switch self {
            case .ceilingHeight:
                return AccessibilityIdentifiers.ceilingHeight
        }
    }

    private typealias AccessibilityIdentifiers = SiteCardAccessibilityIdentifiers.CardInfo
}
