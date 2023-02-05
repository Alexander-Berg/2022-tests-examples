//
//  YDImageCacheConnectionManagerMock.swift
//  10.02.2022
//

import Foundation
@testable import YandexDisk

final class YDImageCacheConnectionManagerMock: YDImageCacheConnectionManager {
    override func download(
        _: ImageUrlProvider,
        priority _: YDNetworkPriority,
        failure _: @escaping ((NSError?) -> Void),
        success _: @escaping (Data) -> Void
    ) -> Cancellable {
        return CancellationToken.empty
    }

    static func createInstance() -> YDImageCacheConnectionManager {
        let configuration = URLSessionConfiguration.default
        let sessionManager = YDSessionManager(sessionConfiguration: configuration)!
        let storageConfig = UserStorageConfig.createDefault(urlSettings: BaseUrlSettings())
        let authSettings = AuthSettingsMock()
        return YDImageCacheConnectionManagerMock(
            sessionManager: sessionManager,
            storageConfig: storageConfig,
            authSettings: authSettings
        )
    }
}
