import XCTest

final class SingleActionContainerWidgetPage: PageObject {

    var container: ContainerPage {
        let elem = cellUniqueElement(withIdentifier: SingleActionContainerAccessibility.container)
        return ContainerPage(element: elem)
    }

}

// MARK: - CollectionViewPage

extension SingleActionContainerWidgetPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = SingleActionContainerCellsAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }

}

// MARK: - Nested Types

extension SingleActionContainerWidgetPage {

    final class ContainerPage: PageObject, CollectionViewPage {

        typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility

        var collectionView: XCUIElement { element.collectionViews.firstMatch }

        typealias SoftUpdateWidget = SingleActionWidgetPage<HoveringSnippetPage>
        typealias AdvertisingCampaignWidget = SingleActionWidgetPage<HoveringSnippetPage>
        typealias ReferralProgramWidget = SingleActionWidgetPage<HoveringSnippetPage>
        typealias PlusInfoWidget = SingleActionWidgetPage<HoveringSnippetPage>
        typealias GrowingCashbackWidget = SingleActionWidgetPage<HoveringSnippetPage>

        var softUpdateWidget: SoftUpdateWidget {
            let elem = cellUniqueElement(withIdentifier: SoftUpdateWidgetAccessibility.root)
            return SoftUpdateWidget(element: elem)
        }

        func orderSnippet(after elements: [String] = []) -> HoveringSnippetPage {
            let elem = cellUniqueElement(withIdentifier: OrdersWidgetAccessibility.root, after: elements)
            return HoveringSnippetPage(element: elem)
        }

        var advertisingCampaignWidget: AdvertisingCampaignWidget {
            let elem = cellUniqueElement(withIdentifier: AdvertisingCampaignWidgetAccessibility.root)
            return AdvertisingCampaignWidget(element: elem)
        }

        var referralProgramWidget: ReferralProgramWidget {
            let elem = cellUniqueElement(withIdentifier: ReferralProgramWidgetAccessibility.root)
            return ReferralProgramWidget(element: elem)
        }

        var plusInfoWidget: PlusInfoWidget {
            let elem = cellUniqueElement(withIdentifier: PlusInfoWidgetAccessibility.root)
            return PlusInfoWidget(element: elem)
        }

        var growingCashbackWidget: GrowingCashbackWidget {
            let elem = cellUniqueElement(withIdentifier: GrowingCashbackWidgetAccessibility.root)
            return GrowingCashbackWidget(element: elem)
        }
    }

}
