import MarketModels
import MarketProtocols
import PromiseKit

class StubRegionService: RegionService {

    var stubbedRegion: YMTRegion?

    var autodetectCalled = false

    var currentRegion: YMTRegion? {
        stubbedRegion
    }

    var isAutodetected = false

    func getRegion(regionId: Int) -> Promise<YMTRegion> {
        Promise.value(YMTRegion.dummy())
    }

    func getAutodetectedRegion() -> Promise<YMTRegion> {
        autodetectCalled = true
        return Promise.value(YMTRegion.dummy())
    }

    func getRegionId(latitude: Double, longitude: Double) -> Promise<Int> {
        Promise.value(213)
    }

    func checkDelivery(regionId: Int) -> Promise<YBMDeliveryRegion> {
        Promise.value(YBMDeliveryRegion.dummy())
    }

    func getRegionSuggests(text: String, count: Int) -> Promise<[YMTRegion]> {
        Promise.value([])
    }

    func getClosestDeliveryRegions(regionId: Int) -> Promise<ClosestRegion> {
        let closestRegion = ClosestRegion(
            region: YMTRegion.dummy(),
            nearestRegions: []
        )
        return Promise.value(closestRegion)
    }

    func updateSelectedRegion(_ region: YMTRegion?) {}
}

private extension YBMDeliveryRegion {
    static func dummy() -> YBMDeliveryRegion {
        YBMDeliveryRegion()
    }
}

private extension YMTRegion {
    static func dummy() -> YMTRegion {
        YMTRegion(id: 213, name: "Moscow", type: .city)
    }
}
