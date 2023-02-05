import MarketUITestMocks
import XCTest

class RetailTestCase: LocalMockTestCase {

    func setupToggles() {
        enable(
            toggles: FeatureNames.eatsRetailInMarket,
            FeatureNames.lavkaInMarket_v2,
            FeatureNames.searchBlenderExperiment
        )
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        var defaultState = DefaultState()
        defaultState.setExperiments(experiments: [.searchBlender, .eatsRetailInMarket])
        stateManager?.setState(newState: defaultState)
    }

    func setupRetail() {
        var retailState = RetailState()
        retailState.setStartup()
        retailState.setEatsActualizedCart(with: [.retail])
        retailState.setCreateEatsCart(with: [.retail])
        stateManager?.setState(newState: retailState)
    }

    func setupFeed() {
        var feedState = FeedState()
        feedState.setSearchStateFAPI(mapper: .init(fapiOffers: [.retail], deliveryOverride: .express))
        feedState.setSearchOrRedirectState(mapper: .init(fapiOffers: [.retail], deliveryOverride: .express))
        stateManager?.setState(newState: feedState)
    }

    func setupSku() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: .retail)
        skuState.setOffersById(mapper: .retail)
        stateManager?.setState(newState: skuState)
    }

}
