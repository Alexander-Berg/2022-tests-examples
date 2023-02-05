
import Foundation
import YandexMobileAds_Private
import YandexMobileAds_PrivateForTests
import YandexMobileAds_Public

class TestsVASTAdFactory {
    private init() {}

    static func makeAd(extensions: YMAVASTExtensions? = nil, sequence: Int? = nil) -> YMAVASTAd {
        return YMAVASTAd(adType: YMAVASTAdType.inLine,
                         adSystem: nil,
                         adTitle: nil,
                         adDescription: nil,
                         survey: nil,
                         creatives: [],
                         vastAdTagURI: nil,
                         rawVAST: nil,
                         extensions: extensions,
                         errors: nil,
                         impressions: nil,
                         viewableImpression: nil,
                         trackingEvents: nil,
                         wrapper: nil,
                         sequence: sequence as NSNumber?)
    }
}
