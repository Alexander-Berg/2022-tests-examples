//
//  WebDAVFileManagerMock.swift
//  15.02.2022
//

import Foundation
@testable import YandexDisk

class WebDAVFileManagerMock: YOWebDAVFileManager {
    let testDownloadInfo = DownloadQueueData()
    private(set) var lastCancelledDownload: YOFile?
    private(set) var cacheBodyMoved = false
    private(set) var lastStartedDownload: YOFile?

    init(settings: YOSettings = YOSettings()) {
        let fileStorage = FileStorageMock()
        let storage = BaseWebDAVStorageMock(fileStorage: fileStorage)
        let authSettings = AuthSettingsMock()
        let adapter = YDReachabilityAdapter(reachability: YOReachability(authSettings: authSettings))
        let connectionManager = YDConnectionManager(authSettings: authSettings, settings: settings)

        let downloadQueueManager = YDDownloadQueueManager(
            storage: storage,
            connectionManager: connectionManager,
            reachabilityAdapter: adapter,
            fileStorage: FileStorageMock(),
            authSettings: authSettings
        )

        let storageConfig = UserStorageConfig.createDefault(urlSettings: BaseUrlSettings())

        let coordinator = UploadMetricsCoordinator(
            storage: storage,
            uploadSettings: settings,
            reachability: adapter
        )
        let uploadingQueueManager = YOUploadingQueueManager(
            storage: storage,
            authSettings: authSettings,
            storageConfig: storageConfig,
            connectionManager: connectionManager,
            settings: settings,
            userSettings: UserSettings(uid: 1),
            fileStorage: FileStorageMock(),
            uploadedStorage: YDPhotoStreamUploadedStorage(storage: storage),
            backgroundPusher: BackgroundPusherStub(),
            uploadMetricsCoordinator: coordinator,
            reachabilityAdapter: adapter
        )

        super.init(
            storage: storage,
            authSettings: authSettings,
            fileStorage: FileStorageMock(),
            uploaderManager: uploadingQueueManager,
            imageCacheStorage: YDImageCacheStorage(caches: [:], fetcherProviders: [:], errorHandler: nil),
            objc_reachabilityAdapter: adapter,
            avatarStorage: RootServicesContainer.shared.avatarStorage(),
            indexStorage: RootServicesContainer.shared.indexStorage,
            downloaderManager: downloadQueueManager,
            indexEnqueuer: YDIndexEnqueuerProtocolMock(),
            settings: settings
        )
    }

    override func cancelUserDownload(_ file: YOFile!, produceResumeData _: Bool) {
        lastCancelledDownload = file
    }

    override func moveCachedBodyOfFileItem(with _: URL!, toNewURL _: URL!) {
        cacheBodyMoved = true
    }

    override func startDownloadUserFile(_ file: YOFile!) {
        lastStartedDownload = file
    }

    override var downloadInfo: DownloadQueueInfo! {
        return testDownloadInfo
    }
}

private final class BackgroundPusherStub: BackgroundPusherProtocol {
    func start() {}
    func stop() {}
}
