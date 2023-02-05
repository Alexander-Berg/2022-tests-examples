
import Foundation
import YandexMobileAds_PrivateForTests
import YandexMobileAds_Public

class TestsVASTCreativeFactory {
    private init() {}

    static func makeCreative(
        duration: Int = 0,
        skipOffset: YMAVASTSkipOffset? = nil,
        clickThrough: String? = nil,
        creativeExtensions: YMAVASTCreativeExtensions? = nil
    ) -> YMAVASTCreative {
        return YMAVASTCreative(
            id: nil,
            duration: duration,
            clickThrough: clickThrough,
            mediaFiles: [],
            icons: [],
            trackingEvents: [],
            skipOffset: skipOffset,
            creativeExtensions: creativeExtensions
        )
    }
}
