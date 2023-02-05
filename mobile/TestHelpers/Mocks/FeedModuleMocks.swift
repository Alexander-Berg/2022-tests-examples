//
// Created by Valeriy Popov on 24/02/2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import Foundation
import UIAtoms

#if !DEV_TEST
@testable import YandexDisk
#endif

class FeedInterfaceMock: FeedInterfaceProtocol {
    var feedContentOffset: CGFloat { return 80.0 }
    var visibleFeeds: [FeedData] { return [] }

    private(set) var didCallShowTitleWithParam: String?
    func showTitle(_ title: String) {
        didCallShowTitleWithParam = title
    }

    private(set) var didCallShowFeedDataWithParam: [FeedData]?
    func showFeedData(_ feedData: [FeedData]) {
        didCallShowFeedDataWithParam = feedData
    }

    private(set) var didCallShowEmptyFeed = false
    func showEmptyFeed() {
        didCallShowEmptyFeed = true
    }

    func showAutouploadTooltip(_: AutouploadPromoModel) {}

    private(set) var didCallScrollToTop = false
    func scrollToTop() {
        didCallScrollToTop = true
    }

    func scrollToBlock(block _: FeedData, whenAppear _: Bool) {}

    private(set) var didCallStartPullToRefresh = false
    func startPullToRefresh(animated _: Bool) {
        didCallStartPullToRefresh = true
    }

    func showPullToRefresh(animated _: Bool) {}

    private(set) var didCallStopPullToRefresh = false
    func stopPullToRefresh(animated _: Bool) {
        didCallStopPullToRefresh = true
    }

    func updateNewItemButton(visible _: Bool) {}

    func showToast(type _: ToastNotificationType) {}

    func updateHiddenAd(with _: FeedData) {}

    func showBuyPromoBlock(model _: BuyPromoBlockController.Model) {}

    func hideBuyPromoBlock() {}

    func didScroll(scrollView _: UIScrollView) {}

    func didFinishScroll(scrollView _: UIScrollView) {}
}

class FeedInteractorMock: FeedInteractorInputProtocol {
    var itemsContainerInteractor: FileItemsContainerInteractorInputProtocol!
    var userUID: String = ""
    var isAuthorized: Bool = false
    var hasNextCollection: Bool = false

    private(set) var releasedImageCacheTypes = [YDImageCacheType]()
    func releaseImageCacheMemory(cacheType: YDImageCacheType) {
        releasedImageCacheTypes.append(cacheType)
    }

    private(set) var didLoadData = false
    func loadData(completion _: (() -> Void)?) {
        didLoadData = true
    }

    func refreshFeedData(completion _: (() -> Void)?) {}

    private(set) var didCallFetchFeedRevision = false
    func fetchFeedRevision() {
        didCallFetchFeedRevision = true
    }

    func clearExcessCollectionsAndLoadData() {}

    private(set) var didCallLoadMoreFeedData = false
    func loadMoreFeedData() {
        didCallLoadMoreFeedData = true
    }

    private(set) var didFetchMetaForBlocks: Bool = false
    func fetchMeta(for _: [FeedBlockProtocol], blockCompletion _: ((Error?) -> Void)?) {
        didFetchMetaForBlocks = true
    }

    private(set) var fetchContext: (id: String, offset: Int)?
    func fetchMeta(
        for _: [FeedBlockProtocol],
        openedBlockContext: (id: String, offset: Int)?,
        blockCompletion _: ((Error?) -> Void)?
    ) {
        didFetchMetaForBlocks = true
        fetchContext = openedBlockContext
    }

    private(set) var didFetchMetaForBlock: Bool = false
    func cancelFetchMeta(for _: FeedBlockProtocol) {
        didFetchMetaForBlock = true
    }

    private(set) var didCallTryLoadMoreItems = false
    func tryLoadMoreItems(forBlockData _: FeedData) {
        didCallTryLoadMoreItems = true
    }

    func getPreloadedBlockData(context _: XivaContext) -> FeedData? {
        return nil
    }

    func preloadBlockImages() {}

    func startPreload() {}

    func didScroll(scrollView _: UIScrollView) {}

    func didFinishScroll(scrollView _: UIScrollView) {}
}

class FileItemsContainerPresenterMock: FileItemsContainerPresenterProtocol {
    var feedDataSource: GenericFeedDataSource! = AnyEnrichedSource(source: FeedDataSourceNew())

    var modalBlockContextReturnValue: (id: String, offset: Int)?
    var modalBlockContext: (id: String, offset: Int)? {
        return modalBlockContextReturnValue
    }

    let deletingUrls: Set<URL>? = nil
    let hasPreviewController = false

    private(set) var didCallViewDidLoad = false
    func viewDidLoad() {
        didCallViewDidLoad = true
    }

    var isMultiselectionOn: Bool = false

    func switchMultiselectionOn() {}

    private(set) var didCallPressedShowAllFiles = false
    func switchMultiselectionOff() {
        didCallPressedShowAllFiles = true
    }

    func isItemSelected(_: YOFileItem) -> Bool {
        fatalError("isItemSelected(item:) has not been implemented")
    }

    func selectOrDeselectItem(_: YOFileItem) {}

    func didSelectAction(_: YDOperationType, moreButtons _: [OperationButtonType], source _: YDPopoverPresentationSource?) {}

    private(set) var didCallPressedShare = false
    func pressedShare(block _: FeedData, sender _: YDPopoverPresentationSource?, analyticsSource _: ShareAnalyticsSource) {
        didCallPressedShare = true
    }

    func pressedHide(block _: FeedData, sender _: YDPopoverPresentationSource?) {}

    func didSuccessfullyShareFiles() {}

    func didSelectFileItem(with _: String, context _: FeedSelectedItemContext) {}

    func dismissPreviewController(animated _: Bool) {}

    func pressedOpen(block _: FeedData) {}
}

class FeedDataSourceMock: GenericFeedDataSource {
    var lastCommand: CommandType?
    override func process(command: CommandType) {
        lastCommand = command

        if let cmd = command as? FeedDataSourceCommands.Get<FeedData>, let data = stubData {
            cmd.result = data
            return
        }

        if command is AdsEnricherCommands.Refresh {
            return
        }

        super.process(command: command)
    }

    var stubData: FeedData?
}

class FeedAdProviderMock: FeedAdProvider {
    override func prefetch(key _: String?) {}
}

class FeedRouterMock: FeedRouterInputProtocol {
    let isFeedVisible = false

    private(set) var didCallShowBlockModalVC = false
    func showBlockModalViewController(blockData _: FeedData, useDataContents _: Bool, replaceStack _: Bool, animated _: Bool) {
        didCallShowBlockModalVC = true
    }

    private(set) var didCallGoToDirectory = false
    func goToDirectory(by _: URL) {
        didCallGoToDirectory = true
    }

    private(set) var didCallGoToPhotoslice = false
    func goToPhotoslice(with _: YOFile) {
        didCallGoToPhotoslice = true
    }

    private(set) var didCallLogout = false
    func updateTokenOrLogout() {
        didCallLogout = true
    }

    private(set) var didDismissModalPreview = false
    func dismissModalPreviewController(animated _: Bool) {
        didDismissModalPreview = true
    }

    func dismissFeedBlock(animated _: Bool) {}

    private(set) var logger: FeedBlockLoggerProtocol?
}
