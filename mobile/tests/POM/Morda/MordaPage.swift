import MarketUI
import XCTest

final class MordaPage: PageObject {

    // MARK: - Properties

    /// Морда из текущего `XCUIApplication`.
    static var current: MordaPage {
        let morda = XCUIApplication().otherElements[MordaAccessibility.container]
        return MordaPage(element: morda)
    }

    var promoHubButton: PromoHubButton {
        let elem = element.buttons.matching(identifier: MordaAccessibility.promoHubButton).firstMatch
        return PromoHubButton(element: elem)
    }

    var plusButton: PlusButton {
        let elem = element.buttons.matching(identifier: MordaAccessibility.plusButton).firstMatch
        return PlusButton(element: elem)
    }

    var searchButton: SearchButtonPage {
        let el = element
            .buttons
            .matching(identifier: NavigationBarAccessibility.searchViewSearchButton)
            .firstMatch
        return SearchButtonPage(element: el)
    }

}

// MARK: - Nested Types

extension MordaPage {

    class PromoHubButton: PageObject, PromoHubEntryPoint {}
    class PlusButton: PageObject {}

}

// MARK: - Widgets

extension MordaPage {

    typealias HistoryWidget = ScrollBoxWidgetPage<HistoryWidgetCellsAccessibility, SnippetPage>
    typealias PopularProductsWidget = ScrollBoxWidgetPage<PopularProductsWidgetCellsAccessibility, SnippetPage>
    typealias PersonalDealsWidget = GridWidgetPage<PersonalDealsWidgetCellsAccessibility, SnippetPage>
    typealias DealsWidget = ListBoxWidgetPage<DealsWidgetCellsAccessibility, SnippetPage>
    typealias RecommendationsWidget = GridWidgetPage<RecommendationsWidgetCellsAccessibility, SnippetPage>
    typealias LiveStreamsWidget = ScrollBoxWidgetPage<LiveStreamWidgetCellsAccessibility, SnippetPage>
    typealias HotlinksScrollWidget = HotlinksScrollWidgetPage<HotlinkPage>

    var singleActionContainerWidget: SingleActionContainerWidgetPage {
        let elem = element.collectionViews.firstMatch
        return SingleActionContainerWidgetPage(element: elem)
    }

    var hotlinksGridWidget: HotlinksGridWidgetPage {
        let elem = element.collectionViews.firstMatch
        return HotlinksGridWidgetPage(element: elem)
    }

    var hotlinksScrollWidget: HotlinksScrollWidget {
        let elem = element.collectionViews.firstMatch
        return HotlinksScrollWidget(element: elem)
    }

    var historyWidget: HistoryWidget {
        let elem = element.collectionViews.firstMatch
        return HistoryWidget(element: elem)
    }

    var popularProductsWidget: PopularProductsWidget {
        let elem = element.collectionViews.firstMatch
        return PopularProductsWidget(element: elem)
    }

    var personalDealsWidget: PersonalDealsWidget {
        let elem = element.collectionViews.firstMatch
        return PersonalDealsWidget(element: elem)
    }

    var dealsWidget: DealsWidget {
        let elem = element.collectionViews.firstMatch
        return DealsWidget(element: elem)
    }

    var recommendationsWidget: RecommendationsWidget {
        let elem = element.collectionViews.firstMatch
        return RecommendationsWidget(element: elem)
    }

    var liveStreamsWidget: LiveStreamsWidget {
        let elem = element.collectionViews.firstMatch
        return LiveStreamsWidget(element: elem)
    }
}
