
import YandexMobileAds_Private
import YandexMobileAds_PrivateForTests
import YandexMobileAds_Public
@testable import YandexMobileAdsInstream

class TestsVideoAdInfoFactory {
    private init() {}

    static func makeVideoAdInfo(
        with ad: YMAVASTAd = TestsVASTAdFactory.makeAd(),
        creative: YMAVASTCreative = TestsVASTCreativeFactory.makeCreative(),
        socialAdInfo: YMASocialAdInfo? = nil,
        mediaFile: YMAVideoAdPlaybackInfo? = nil,
        adInfo: String? = nil
    ) -> YMAVideoAdInfo {
        let skipInfo = InstreamVideoAdSkipInfo(isSkippable: true, skipOffset: 0)
        let playbackInfo: YMAVideoAdPlaybackInfo = mediaFile
            ?? InstreamMediaFile(adHeight: 0, adWidth: 0, skipInfo: skipInfo, url: "")

        return YMAVideoAdInfo(ad: ad,
                              creative: creative,
                              playbackInfo: playbackInfo,
                              socialAdInfo: socialAdInfo,
                              adInfo: adInfo)
    }

    static func makeVideoAdInfoArray(
        with ad: YMAVASTAd = TestsVASTAdFactory.makeAd(),
        creative: YMAVASTCreative = TestsVASTCreativeFactory.makeCreative(),
        socialAdInfo: YMASocialAdInfo? = nil,
        adInfo: String? = nil
    ) -> [YMAVideoAdInfo] {
        let skipInfo = InstreamVideoAdSkipInfo(isSkippable: false, skipOffset: 0)
        let playbackInfo = InstreamMediaFile(adHeight: 0, adWidth: 0, skipInfo: skipInfo, url: "")
        return [
            YMAVideoAdInfo(
                ad: ad,
                creative: creative,
                playbackInfo: playbackInfo,
                socialAdInfo: socialAdInfo,
                adInfo: adInfo
            ),
            YMAVideoAdInfo(
                ad: ad,
                creative: creative,
                playbackInfo: playbackInfo,
                socialAdInfo: socialAdInfo,
                adInfo: adInfo
            ),
        ]
    }

    static func makeVideoAdArray(
        with videoAdsInfo: [YMAVideoAdInfo] = TestsVideoAdInfoFactory.makeVideoAdInfoArray()
    ) -> [VideoAd] {
        videoAdsInfo.enumerated().compactMap { index, videoAdInfo in
            guard let mediaFile = videoAdInfo.playbackInfo as? InstreamMediaFile else {
                return nil
            }

            let skipInfo = mediaFile.skipInfo.isSkippable
                ? VideoAdSkipInfo(skipOffset: mediaFile.skipInfo.skipOffset)
                : nil

            return InstreamVideoAdCreative(
                mediaFile: mediaFile,
                adPodInfo: VideoAdPodInfo(adsCount: videoAdsInfo.count, adPosition: index + 1),
                skipInfo: skipInfo,
                adInfo: videoAdInfo.adInfo
            )
        }
    }

    static func makeVideoAdCreativeArray(
        with videoAdsInfo: [YMAVideoAdInfo] = TestsVideoAdInfoFactory.makeVideoAdInfoArray()
    ) -> [InstreamVideoAdCreative] {
        makeVideoAdArray(with: videoAdsInfo).compactMap {
            $0 as? InstreamVideoAdCreative
        }
    }
}
