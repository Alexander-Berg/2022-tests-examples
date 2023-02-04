import XCTest
import Snapshots

final class DealerCabinetSteps: BaseSteps {
    func onDealerCabinetScreen() -> DealerCabinetScreen {
        return baseScreen.on(screen: DealerCabinetScreen.self)
    }

    func onDealerCabinetVASConfirmationScreen() -> DealerCabinetVASConfirmationScreen {
        return baseScreen.on(screen: DealerCabinetVASConfirmationScreen.self)
    }

    @discardableResult
    func shouldSeeVINSearchBar() -> Self {
        Step("Проверяем, что есть search bar для VIN-поиска") {
            self.onDealerCabinetScreen().vinSearchBar.shouldExist()
        }

        return self
    }

    @discardableResult
    func tapOnVINSearchBar() -> DealerVINSearchSteps {
        Step("Тапаем на search bar VIN-поиска") {
            self.onDealerCabinetScreen().vinSearchBar.tap()
        }

        return DealerVINSearchSteps(context: context)
    }

    @discardableResult
    func tapOnOfferSnippet(offerID: String) -> DealerSaleCardSteps {
        Step("Тапаем по сниппету '\(offerID)'") {
            self.onDealerCabinetScreen().snippetHeader(offerID: offerID).tap()
        }

        return DealerSaleCardSteps(context: context)
    }

    @discardableResult
    func checkSnippetNotExists(offerID: String) -> DealerSaleCardSteps {
        Step("Проверяем, что есть сниппет для оффера '\(offerID)'") {
            self.onDealerCabinetScreen().snippetHeader(offerID: offerID).shouldExist()
        }

        return DealerSaleCardSteps(context: context)
    }

    @discardableResult
    func checkEmptyPlaceholderNoOffersWithoutFilters() -> Self {
        Step("Проверяем плейсхолдер, когда нет примененных фильтров") {
            let screen = self.onDealerCabinetScreen()
            let placeholder = screen.emptyPlaceholderView.waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: placeholder.image, identifier: "dealer_empty_placeholder_no_filters")
        }

        return self
    }

    @discardableResult
    func tapOnAddOfferButtonOnPlaceholder() -> DealerNewListingCategoryPickerSteps {
        Step("Тапаем на 'Добавить' на плейсхолдере") {
            self.onDealerCabinetScreen().emptyPlaceholderAddButton.tap()
        }

        return DealerNewListingCategoryPickerSteps(context: context)
    }

    @discardableResult
    func checkEmptyPlaceholderNoOffersWithFilters() -> Self {
        Step("Проверяем плейсхолдер, когда есть фильтры") {
            let screen = self.onDealerCabinetScreen()
            let placeholder = screen.emptyPlaceholderView.waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: placeholder.image, identifier: "dealer_empty_placeholder_with_filters")
        }

        return self
    }

    @discardableResult
    func checkElementsVisibleWhenEmptyListing() -> Self {
        Step("Проверяем отсутствие элементов на пустом листинге: VIN поиск и сортировка") {
            let screen = self.onDealerCabinetScreen()
            screen.vinSearchBar.shouldNotExist()
            screen.sortButton.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждем, пока загрузится ЛК дилера") {
            self.onDealerCabinetScreen().refreshingControl.shouldNotExist(timeout: 5.0)
        }

        return self
    }

    @discardableResult
    func scrollToOfferTitle(offerID: String) -> Self {
        Step("Скроллим к хедеру сниппета '\(offerID)'") {
            let screen = self.onDealerCabinetScreen()
            let snippetHeader = screen.snippetHeader(offerID: offerID)
            screen.scrollTo(element: snippetHeader)
        }

        return self
    }

    @discardableResult
    func scrollToBottom() -> Self {
        Step("Скроллим в самый низ") {
            let screen = self.onDealerCabinetScreen()
            let bottom = self.onDealerCabinetScreen().find(by: "bottom_space").firstMatch
            screen.scrollTo(element: bottom)
        }

        return self
    }

    @discardableResult
    func tapOnSortButton() -> DealerCabinetSortingSteps {
        Step("Тапаем на сортировку") {
            self.onDealerCabinetScreen().sortButton.tap()
        }

        return DealerCabinetSortingSteps(context: context)
    }

    @discardableResult
    func scrollToVINResolution(offerID: String) -> Self {
        Step("Скроллим до вин резолюции для оффера '\(offerID)'") {
            let screen = self.onDealerCabinetScreen()
            let vinResolution = screen.vinResolution(offerID: offerID)
            screen.scrollTo(element: vinResolution, windowInsets: DealerCabinetScreen.insetsWithoutFilterAndTabBar)
        }

        return self
    }

    @discardableResult
    func tapOnVINResolution(offerID: String) -> CarReportPreviewSteps {
        Step("Тапаем на вин резолюцию для оффера '\(offerID)'") {
            self.onDealerCabinetScreen().vinResolution(offerID: offerID).tap()
        }

        return CarReportPreviewSteps(context: context)
    }

    @discardableResult
    func scrollToActionButton(offerID: String) -> Self {
        Step("Скроллим до кнопки на сниппете для оффера '\(offerID)'") {
            let screen = self.onDealerCabinetScreen()
            let actionButton = screen.actionButton(offerID: offerID)
            screen.scrollTo(element: actionButton)
        }

        return self
    }

    @discardableResult
    func tapOnActionButton(offerID: String, type: DealerCabinetScreen.ActionButtonType) -> Self {
        Step("Тапаем на кнопку '\(type.rawValue)' на сниппете оффера '\(offerID)'") {
            self.onDealerCabinetScreen().actionButton(offerID: offerID, type: type).tap()
        }

        return self
    }

    @discardableResult
    func scrollToMoreButton(offerID: String) -> Self {
        Step("Скроллим до кнопки '...' на сниппете оффера '\(offerID)'") {
            let screen = self.onDealerCabinetScreen()
            let moreButton = screen.moreButton(offerID: offerID)
            screen.scrollTo(element: moreButton)
        }

        return self
    }

    @discardableResult
    func tapOnMoreButton(offerID: String) -> DealerOfferActionSteps {
        Step("Тапаем на кнопку '...' на сниппете оффера '\(offerID)'") {
            self.onDealerCabinetScreen().moreButton(offerID: offerID).tap()
        }

        return DealerOfferActionSteps(context: context)
    }

    @discardableResult
    func tapOnFiltersButton() -> DealerFiltersSteps {
        Step("Тапаем кнопку `Фильтры`") {
            self.onDealerCabinetScreen().filtersButton.tap()
        }

        return DealerFiltersSteps(context: context)
    }

    @discardableResult
    func tapOnAddButton() -> DealerNewListingCategoryPickerSteps {
        Step("Тапаем кнопку `Добавить` в навбаре") {
            self.onDealerCabinetScreen().addButton.tap()
        }

        return DealerNewListingCategoryPickerSteps(context: context)
    }

    @discardableResult
    func scrollToCollapsedVASList(offerID: String) -> Self {
        Step("Скроллим до списка васов в свернутом состоянии на сниппете для оффера '\(offerID)'") {
            let screen = self.onDealerCabinetScreen()
            let list = screen.snippetCollapsedVASList(offerID: offerID)
            screen.scrollTo(element: list)
        }

        return self
    }

    @discardableResult
    func shouldNotSeeCollapsedVASList(offerID: String) -> Self {
        Step("Проверяем, что нет свернутого списка васов на сниппете оффера '\(offerID)'") {
            self.onDealerCabinetScreen().snippetCollapsedVASList(offerID: offerID).shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkBalanceItem(isVisible: Bool) -> Self {
        Step("Проверяем, что виден баланс") {
            if isVisible {
                self.onDealerCabinetScreen().balanceItem.shouldBeVisible()
            } else {
                self.onDealerCabinetScreen().balanceItem.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func shouldSeeNoAccessPlaceholder() -> Self {
        Step("Проверяем плейсхолдер 'Нет доступа'") {
            self.onDealerCabinetScreen().noAccessPlaceholderView.shouldBeVisible()
        }

        return self
    }

    @discardableResult
    func shouldNotSeeAddButton() -> Self {
        Step("Проверяем, что нет кнопки 'Добавить'") {
            self.onDealerCabinetScreen().addButton.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func checkSnippetSnapshot(offerID: String, identifier: String) -> Self {
        Step("Делаем скриншот сниппета оффера '\(offerID)' и сравниваем с '\(identifier)'") {
            let screen = self.onDealerCabinetScreen()
            let header = screen.snippetHeader(offerID: offerID)
            let button = screen.snippetButton(offerID: offerID)

            let snippet = Snapshot.screenshotCollectionView(
                fromCell: header,
                toCell: button,
                windowInsets: DealerCabinetScreen.insetsWithoutFilterAndTabBar
            )
            Snapshot.compareWithSnapshot(image: snippet, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func checkSnippetChartSnapshot(offerID: String, identifier: String, scrollTo: Bool = false) -> Self {
        Step("Делаем скриншот графиков сниппета оффера '\(offerID)' и сравниваем с '\(identifier)'") {
            let screen = self.onDealerCabinetScreen()
            let cell = screen.snippetCharts(offerID: offerID)

            if scrollTo {
                screen.scrollTo(element: cell)
            }

            let charts = Snapshot.screenshotCollectionView(
                fromCell: cell,
                toCell: cell,
                windowInsets: DealerCabinetScreen.insetsWithoutFilterAndTabBar
            )
            Snapshot.compareWithSnapshot(image: charts, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func checkSnippetVINResolutionSnapshot(offerID: String, identifier: String) -> Self {
        Step("Делаем скриншот вин резолюции сниппета оффера '\(offerID)' и сравниваем с '\(identifier)'") {
            let screen = self.onDealerCabinetScreen()
            let element = screen.vinResolution(offerID: offerID)

            let screenshot = element.waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func checkCollapsedVASListSnapshot(offerID: String, identifier: String) -> Self {
        Step("Делаем скриншот свернутых васов сниппета оффера '\(offerID)' и сравниваем с '\(identifier)'") {
            let element = self.onDealerCabinetScreen().snippetCollapsedVASList(offerID: offerID)
            Snapshot.compareWithSnapshot(image: element.waitAndScreenshot().image, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func tapOnPaginationRetryButton() -> Self {
        Step("Тапаем на 'Повторить' на ошибке при пагинации") {
            self.onDealerCabinetScreen().paginationRetryCell.tap()
        }

        return self
    }

    @discardableResult
    func tapOnPanoramaPhotographButton() -> DealerCabinetPanoramaListSteps {
        Step("Нажимаем кнопку панорам внизу") {
            self.onDealerCabinetScreen().panoramaPhotographButton.tap()
        }

        return DealerCabinetPanoramaListSteps(context: self.context)
    }

    @discardableResult
    func tapOnPanoramaNavBarButton() -> DealerCabinetPanoramaListSteps {
        Step("Нажимаем кнопку панорам в NavBar") {
            self.onDealerCabinetScreen().panoramaPhotographNavBarButton.tap()
        }

        return DealerCabinetPanoramaListSteps(context: self.context)
    }
}

final class DealerCabinetPanoramaListSteps: BaseSteps {
    func onDealerCabinetPanoramaListScreen() -> DealerCabinetPanoramaListScreen {
        return baseScreen.on(screen: DealerCabinetPanoramaListScreen.self)
    }

    @discardableResult
    func addPanoramaButtonTap() -> Self {
        Step("Нажимаем кнопку добавления панорамы") {
            self.onDealerCabinetPanoramaListScreen().addPanoramaButton.tap()
        }
        return self
    }

    @discardableResult
    func addPanoramaCellTap() -> Self {
        Step("Нажимаем ячейку добавления панорамы") {
            self.onDealerCabinetPanoramaListScreen().addPanoramaCell.tap()
        }
        return self
    }

    @discardableResult
    func addPanoramaVinTap() -> Self {
        Step("Нажмём на текстовое поле VIN") {
            self.onDealerCabinetPanoramaListScreen().addPanoramaVinField.tap()
        }
        return self
    }

    @discardableResult
    func addPanoramaVinType() -> Self {
        Step("Вобьём VIN") {
            self.onDealerCabinetPanoramaListScreen().addPanoramaVinField.typeText("AAAAA")
        }
        return self
    }

    @discardableResult
    func addPanoramaVinButtonExist() -> Self {
        Step("Можно добавить панораму с VIN") {
            self.onDealerCabinetPanoramaListScreen().addPanoramaVinButton.shouldExist()
        }
        return self
    }

    @discardableResult
    func panoramaOptionsTap() -> Self {
        Step("Нажмём на опции панорамы") {
            self.onDealerCabinetPanoramaListScreen().panoramaListOptionsButton.tap()
        }
        return self
    }
}

final class DealerCabinetVINResolutionSteps: BaseSteps {
    func onDealerCabinetVINResolutionScreen() -> DealerCabinetVINResolutionScreen {
        return baseScreen.on(screen: DealerCabinetVINResolutionScreen.self)
    }

    @discardableResult
    func shouldSeePopup() -> Self {
        Step("Проверяем, что показан попап для invalid / untrusted вин-резолюций") {
            let screen = self.onDealerCabinetVINResolutionScreen()
            screen.title.shouldExist()
            screen.dismissButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func close() -> DealerCabinetSteps {
        Step("Тапаем на крестик на попапе резолюции") {
            self.onDealerCabinetVINResolutionScreen().dismissButton.tap()
        }

        return DealerCabinetSteps(context: self.context)
    }
}

final class DealerCabinetSortingSteps: BaseSteps {
    func onDealerCabinetSortingScreen() -> DealerCabinetSortingScreen {
        return baseScreen.on(screen: DealerCabinetSortingScreen.self)
    }

    @discardableResult
    func tap(on option: DealerCabinetSortingScreen.SortingType) -> DealerCabinetSteps {
        Step("Тапаем на опцию \"\(option.rawValue)\" в пикере сортировки") {
            self.onDealerCabinetSortingScreen().optionButton(option).tap()
        }

        return DealerCabinetSteps(context: context)
    }
}

final class DealerNewListingCategoryPickerSteps: BaseSteps {
    @discardableResult
    func shouldSeeCommonContent(options: [String]) -> Self {
        let stepOptionsDescription = options.joined(separator: ", ")
        Step("Проверяем, что на пикере категорий ТС есть пункты: \(stepOptionsDescription)") {
            let screen = self.onDealerNewListingCategoryPickerScreen()
            screen.hasExactlyPickerOptions(options)
        }

        return self
    }

    @discardableResult
    func tapOption(named name: String) -> Self {
        Step("Тапаем на `\(name)` в пикере категорий ТС") {
            let screen = self.onDealerNewListingCategoryPickerScreen()
            let cell = screen.scrollableElement.cells.matching(identifier: name).firstMatch

            Step("Ищем вариант `\(name)`") {
                cell.shouldExist()
            }

            cell.tap()
        }

        return self
    }

    // MARK: - Actions

    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждём окончания загрузки") {
            self.onDealerNewListingCategoryPickerScreen()
                .refreshingControl.shouldNotExist(timeout: 5.0)
        }

        return self
    }

    // MARK: - Screens

    func onDealerNewListingCategoryPickerScreen() -> DealerNewListingCategoryPickerScreen {
        return baseScreen.on(screen: DealerNewListingCategoryPickerScreen.self)
    }

    func onDealerFormScreen() -> DealerFormScreen {
        return baseScreen.on(screen: DealerFormScreen.self)
    }
}

extension DealerCabinetSteps: UIElementProvider {
    enum Element: String {
        case addPanoramaDealerBanner = "addPanoramaDealerBanner"
        case addPanoramaDealerBannerLink = "addPanoramaDealerBannerLink"
        case addPanoramaDealerBannerCloseButton = "addPanoramaDealerBannerCloseButton"
    }
}
