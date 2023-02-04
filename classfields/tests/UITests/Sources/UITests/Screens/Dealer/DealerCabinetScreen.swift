import XCTest
import Snapshots

final class DealerCabinetScreen: BaseScreen, Scrollable {
    static let insetsWithoutFilterAndTabBar = UIEdgeInsets(top: 0, left: 0, bottom: 150, right: 0)

    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var vinSearchBar = find(by: "Поиск по VIN").firstMatch
    lazy var sortButton = find(by: "sort_button").firstMatch

    lazy var addButton = find(by: "dealer.lk.btn.add").firstMatch
    lazy var filtersButton = find(by: "dealer.lk.btn.filters").firstMatch

    lazy var refreshingControl = self.scrollableElement.activityIndicators.firstMatch
    lazy var collectionView = findAll(.collectionView).firstMatch

    lazy var emptyPlaceholderAddButton = find(by: "empty_placeholder_add").firstMatch
    lazy var emptyPlaceholderParamsButton = find(by: "empty_placeholder_params").firstMatch
    lazy var emptyPlaceholderView = find(by: "empty_placeholder_view").firstMatch

    lazy var noAccessPlaceholderView = find(by: "forbidden_placeholder_view").firstMatch
    lazy var balanceItem = find(by: "balance_item").firstMatch
    lazy var paginationRetryCell = find(by: "load_more_error").firstMatch
    lazy var panoramaPhotographNavBarButton = find(by: "dealerCabinetPanoramaNavBarButton").firstMatch
    lazy var panoramaPhotographButton = find(by: "Панорамы автомобилей").firstMatch

    func snippetHeader(offerID: String) -> XCUIElement {
        return self.cell(containing: "user_offer_header_\(offerID)*")
    }

    func snippetDescription(offerID: String) -> XCUIElement {
        return self.cell(containing: "user_offer_description_\(offerID)*")
    }

    func snippetButton(offerID: String) -> XCUIElement {
        return self.cell(containing: "user_offer_action_button_\(offerID)*")
    }

    func snippetBanReason(offerID: String) -> XCUIElement {
        return self.cell(containing: "user_offer_ban_reason_\(offerID)*")
    }

    func snippetCollapsedVASList(offerID: String) -> XCUIElement {
        return self.cell(containing: "user_offer_vas_collapsed_\(offerID)*")
    }

    func snippetCharts(offerID: String) -> XCUIElement {
        return self.cell(containing: "user_offer_stats_\(offerID)*")
    }

    func actionButton(offerID: String, type: ActionButtonType? = nil) -> XCUIElement {
        let wrapper = find(by: "action_button_wrapper_\(offerID)")

        if let type = type {
            return wrapper.buttons[type.rawValue].firstMatch
        }
        return wrapper.firstMatch
    }

    func vinResolution(offerID: String) -> XCUIElement {
        return find(by: "vin_resolution_\(offerID)").firstMatch
    }

    func moreButton(offerID: String) -> XCUIElement {
        return find(by: "more_button_\(offerID)").firstMatch
    }

    private func cell(containing id: String) -> XCUIElement {
        return self.collectionView.cell(containing: id)
    }

    enum ActionButtonType: String {
        case expand = "Продать быстрее"
        case collapse = "Свернуть"
        case activate = "Активировать"
        case edit = "Редактировать"
        case delete = "Удалить"
    }
}

final class DealerCabinetVINResolutionScreen: BaseScreen {
    lazy var title = find(by: "Отчёт по VIN").firstMatch
    lazy var dismissButton = find(by: "dismiss_modal_button").firstMatch
}

final class DealerCabinetSortingScreen: BaseScreen {
    func optionButton(_ option: SortingType) -> XCUIElement {
        return find(by: option.rawValue).firstMatch
    }

    enum SortingType: String, CaseIterable {
        case creationNewer = "Дате размещения: новее"
        case creationOlder = "Дате размещения: старше"
        case name = "Марка, модель"
        case priceLow = "Возрастанию цены"
        case priceHigh = "Убыванию цены"
        case dateLow = "Году: старше"
        case dateHigh = "Году: новее"

        var queryParam: String {
            switch self {
            case .creationNewer:
                return "cr_date-desc"
            case .creationOlder:
                return "cr_date-asc"
            case .name:
                return "alphabet-asc"
            case .priceLow:
                return "price-asc"
            case .priceHigh:
                return "price-desc"
            case .dateLow:
                return "year-asc"
            case .dateHigh:
                return "year-desc"
            }
        }
    }
}

final class DealerCabinetVASConfirmationScreen: BaseScreen {
    lazy var cancelButton = find(by: "Отмена").firstMatch
    lazy var confirmActivateButton = find(by: "Подключить").firstMatch
    lazy var confirmDeactivateButton = find(by: "Отключить").firstMatch
}

final class DealerCabinetPanoramaListScreen: BaseScreen {
    lazy var addPanoramaButton = find(by: "addPanoramaBottomButton").firstMatch
    lazy var addPanoramaCell = find(by: "addPanoramaView").firstMatch
    lazy var addPanoramaVinField = find(by: "Введите номер").firstMatch
    lazy var addPanoramaVinButton = find(by: "Начать съёмку").firstMatch
    lazy var panoramaListOptionsButton = find(by: "panoramaListOptionsButton").firstMatch
}
