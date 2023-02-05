
import YandexMobileAds_PrivateForTests

class TestsVMAPAdBreakFactory {
    private init() {}

    static func makeAdBreak(with breakTypes: [String] = [], breakID: String? = nil) -> YMAVMAPAdBreak {
        let adSource = YMAVMAPAdSource(adTagURI: YMAVMAPAdTagURI(templateType: nil, uri: ""),
                                       allowMultipleAds: nil,
                                       id: nil,
                                       followRedirects: nil)

        return YMAVMAPAdBreak(adSource: adSource,
                              adBreakParameters: YMAVMAPAdBreakParameters(),
                              breakID: breakID,
                              breakTypes: breakTypes,
                              repeatAfter: nil,
                              timeOffset: YMAVMAPTimeOffset(rawValue: ""),
                              extensions: [],
                              trackingEvents: [:])
    }
}
