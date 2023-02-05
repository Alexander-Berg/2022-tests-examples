
import Foundation
@testable import YandexMobileAds_Private

final class TestErrorFactory {
    private init() {}

    static func makeVideoAdError() -> YMAVideoAdError {
        InstreamVideoAdError()
    }
}

private class InstreamVideoAdError: NSObject, YMAVideoAdError {
    let reason: YMAVideoAdErrorReason
    let underlyingError: Error

    override init() {
        reason = .unknown
        underlyingError = ErrorSpy()
        super.init()
    }
}
