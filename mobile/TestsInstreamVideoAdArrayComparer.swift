
import Foundation
@testable import YandexMobileAdsInstream

final class TestsVideoAdArrayComparer {
    static func isInnerObjectsEqualByLink(_ lhs: [VideoAd]?, _ rhs: [VideoAd]?) -> Bool {
        guard let lhs = lhs, let rhs = rhs else { return false }
        return zip(lhs, rhs).filter { lhs, rhs in
            lhs !== rhs
        }.isEmpty
    }

    static func isEqual(_ lhs: [VideoAd]?, _ rhs: [VideoAd]?) -> Bool {
        let firstVideoAdsImpl = lhs?.compactMap { $0 as? InstreamVideoAdCreative }
        let secondVideoAdsImpl = rhs?.compactMap { $0 as? InstreamVideoAdCreative }
        return firstVideoAdsImpl == secondVideoAdsImpl
    }
}
