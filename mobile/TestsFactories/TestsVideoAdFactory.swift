
import Foundation
@testable import YandexMobileAdsInstream

class TestsVideoAdFactory {
    static func makeVideoAd() -> InstreamVideoAdCreative {
        return InstreamVideoAdCreative(
            mediaFile: TestsInstreamMediaFileFactory.makeInstreamMediaFile(),
            adPodInfo: VideoAdPodInfo(adsCount: 1, adPosition: 1),
            skipInfo: VideoAdSkipInfo(skipOffset: 0),
            adInfo: nil
        )
    }
}
