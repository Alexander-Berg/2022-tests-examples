import MarketModels
import MarketProtocols
import PromiseKit

final class OutletsStorageStub: OutletsStorage {
    func obtainOutlets(withId outletsIds: [Int]) -> Guarantee<[YBMDeliveryOutlet]> {
        .value([])
    }

    func save(outlets: [YBMDeliveryOutlet]) {}

    func obtainOutlets(withId outletsIds: [Int]) -> Guarantee<[MinifiedOutlet]> {
        .value([])
    }

    func save(outlets: [MinifiedOutlet]) {}
}
