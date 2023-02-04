import XCTest
import Snapshots

final class DealerVASContainingScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    func expandedVASListItem(offerID: String, type: VASListItemType) -> XCUIElement {
        return self.cell(containing: "\(offerID)*_dealer_vas_\(type.rawValue)*")
    }

    func vasPurchaseElement(offerID: String, type: VASListItemType) -> XCUIElement {
        let cellQuery = self.expandedVASListItem(offerID: offerID, type: type)
        switch type {
        case .fresh, .turbo:
            return cellQuery.descendants(matching: .any).matching(identifier: "vas_button").firstMatch
        default:
            return cellQuery.descendants(matching: .switch).matching(identifier: "vas_switch").firstMatch
        }
    }

    func vasTitle(offerID: String, type: VASListItemType) -> XCUIElement {
        let cellQuery = self.expandedVASListItem(offerID: offerID, type: type)
        return cellQuery.staticTexts[type.rawValue]
    }

    private func cell(containing id: String) -> XCUIElement {
        return findAll(.collectionView).firstMatch.cells
            .containing(NSPredicate(format: "identifier LIKE %@", id))
            .element(boundBy: 0)
    }

    enum VASListItemType: String {
        case fresh = "Поднятие в поиске"
        case autoUp = "Автоподнятие"
        case turbo = "Турбо-продажа"
        case special = "Спецпредложение"
        case premium = "Премиум-объявление"

        var backendAlias: String {
            switch self {
            case .fresh:
                return "all_sale_fresh"
            case .special:
                return "all_sale_special"
            case .premium:
                return "all_sale_premium"
            case .turbo:
                return "package_turbo"
            default:
                fatalError("Not implemented now")
            }
        }

        var vasDescription: String {
            switch self {
            case .fresh:
                return "Данная услуга не просто поднимает ваше объявление в поиске, но ещё и обновляет дату публикации и количество просмотров объявления. Таким образом, она отлично работает даже в случае самых застоявшихся автомобилей."
            case .special:
                return "Ваше объявление будет отображаться в специальном блоке на страницах поисковой выдачи по марке/модели и на страницах Каталога, а также иметь приоритетную позицию в блоке «Похожие» на карточках объявлений о продаже аналогичных авто."
            case .premium:
                return "Объявление с данной услугой имеет расширенную карточку в поисковой выдаче, что значительно повышает привлекательность предложения в глазах покупателей, а также помещается в специальный блок над поисковой выдачей, что позволяет продать автомобиль в рекордно короткие сроки."
            case .turbo:
                return "Ваше предложение увидит максимум посетителей   это увеличит шансы на быструю и выгодную продажу. К объявлению будут применены услуги «Премиум» и «Спецпредложение» на 7 дней, а на 1-й, 3-й и 5-й день мы поднимем его в поиске."
            default:
                fatalError("Not implemented now")
            }
        }
    }
}
