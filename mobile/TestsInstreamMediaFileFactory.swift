
@testable import YandexMobileAdsInstream

class TestsInstreamMediaFileFactory {
    static func makeInstreamMediaFile() -> InstreamMediaFile {
        let skipInfo = InstreamVideoAdSkipInfo(isSkippable: true, skipOffset: 1)
        return InstreamMediaFile(adHeight: 0, adWidth: 0, skipInfo: skipInfo, url: "")
    }
}
