import MarketUITestMocks
import XCTest

class ServicesTestCase: LocalMockTestCase {
    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: .withServices)
        stateManager?.setState(newState: skuState)
    }

    func setupAddToCartState() {
        var cartState = CartState()
        cartState.addItemsToCartState(with: .init(offers: [.withServices]))
        cartState.setCartStrategy(with: [.withServices])
        stateManager?.setState(newState: cartState)
    }

    func setupEmptyCartState() {
        var cartState = CartState()
        cartState.deleteItemsFromCartState()
        cartState.setCartStrategy(with: [])
        stateManager?.setState(newState: cartState)
    }

    func setupFeedState(isServiceFilterEnabled: Bool) {
        var feedState = FeedState()
        let filter = FilterToValues.withService(
            visibleSearchResultId: ResolveSearch.visibleSearchResultId,
            isEnabled: isServiceFilterEnabled
        )
        if isServiceFilterEnabled {
            feedState.setSearchStateFAPI(mapper: .init(
                fromOffers: [],
                fapiOffers: [.withServices],
                filterToValues: [filter.id: filter]
            ))
        } else {
            feedState.setSearchOrUrlTransformState(mapper: .init(
                fromOffers: [.protein],
                fapiOffers: [.withServices],
                filterToValues: [filter.id: filter]
            ))
        }
        stateManager?.setState(newState: feedState)
    }
}
