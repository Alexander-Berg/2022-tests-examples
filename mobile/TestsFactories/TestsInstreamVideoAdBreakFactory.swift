
@testable import YandexMobileAds_Private
@testable import YandexMobileAdsInstream

class TestsInstreamVideoAdBreakFactory {
    private init() {}

    static func makeInstreamAdBreak(
        with type: String = "",
        positionType: String = "",
        positionValue: Double = 0,
        creative: YMAVASTCreative = TestsVASTCreativeFactory.makeCreative(),
        socialAdInfo: YMASocialAdInfo? = nil
    ) -> InstreamVideoAdBreak {
        let adInfo: YMAVideoAdInfo =
            TestsVideoAdInfoFactory.makeVideoAdInfo(creative: creative, socialAdInfo: socialAdInfo)
        let adBreak = TestsVMAPAdBreakFactory.makeAdBreak(with: [kYMABreakTypeLinear])
        let position = InstreamAdBreakPosition(type: positionType, value: Int(positionValue))
        let videoAds = TestsVideoAdInfoFactory.makeVideoAdArray(with: [adInfo])
        return InstreamVideoAdBreak(
            adBreak: adBreak,
            position: position,
            type: type,
            videoAds: videoAds,
            videoAdInfoArray: [adInfo]
        )
    }

    static func makeInstreamAdBreak(
        adInfoArray: [YMAVideoAdInfo],
        videoAds: [VideoAd]
    ) -> InstreamVideoAdBreak {
        let adBreak = TestsVMAPAdBreakFactory.makeAdBreak(with: [kYMABreakTypeLinear])
        let position = InstreamAdBreakPositionMock()
        return InstreamVideoAdBreak(
            adBreak: adBreak,
            position: position,
            type: "",
            videoAds: videoAds,
            videoAdInfoArray: adInfoArray
        )
    }
}
