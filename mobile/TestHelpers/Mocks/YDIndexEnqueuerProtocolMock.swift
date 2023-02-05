//
//  YDIndexEnqueuerProtocolMock.swift
//  15.02.2022
//

@testable import YandexDisk

final class YDIndexEnqueuerProtocolMock: NSObject, YDIndexEnqueuerProtocol {
    var isUpdating: Bool = false

    private(set) var downloadStarted = false
    func startDownloadingIndex(forOfflineFolder _: YODirectory!) {
        downloadStarted = true
    }

    private(set) var downloadCancelled = false
    func cancelDownloadingIndex(forOfflineFolder _: YODirectory!) {
        downloadCancelled = true
    }

    func startSyncing(withCompletionHandler _: YDCompletionHandlerProtocol!) {}
    func cancel() {}
    func resumeDownloadingOutdatedOfflineFolders() {}
    func redownloadIndexForOfflineFolderThatContainItem(with _: URL!) {}
    func restoreBackgroundOfflineIndexTasks() {}
}
