//
//  FeedTests.swift
//  AutoRu
//
//  Created by Igor Shamrin on 29.09.2021.
//

import Foundation
import XCTest
import AutoRuProtoModels
import AutoRuUtils
import Snapshots
@testable import AutoRuFeed

final class FeedTests: BaseUnitTest {

    override func setUp() {
        super.setUp()
        setReplaceImagesWithStub("audi_snippet_stub")
    }

    func test_FeedCell() {
        let model = makeModel(withImage: true)
        let cell = FeedItemCell(model: model, feedIndex: 0, actionBlock: {})
        Snapshot.compareWithSnapshot(layout: cell, maxWidth: DeviceWidth.iPhone11)
    }

    func test_FeedCellWithoutPhoto() {
        let model = makeModel(withImage: false)
        let cell = FeedItemCell(model: model, feedIndex: 0, actionBlock: {})
        Snapshot.compareWithSnapshot(layout: cell, maxWidth: DeviceWidth.iPhone11)
    }

    func test_FeedCellLongTitle() {
        let model = makeModel(withImage: false, title: "Article test Article test Article test Article test Article test Article test")
        let cell = FeedItemCell(model: model, feedIndex: 0, actionBlock: {})
        Snapshot.compareWithSnapshot(layout: cell, maxWidth: DeviceWidth.iPhone11)
    }

    func test_Feed_canLoadMore() throws {
        let carInfo = Auto_Api_CarInfo.with { info in
            info.markInfo.name = "BMW"
            info.modelInfo.name = "5ER"
        }
        let model = FeedFactory.Model(
            feedSize: 10,
            feedSegment: .all,
            feedContent: .init(
                items: [
                    FeedModule.FeedItem(value: makeModel(withImage: true)),
                    FeedModule.FeedItem(value: makeModel(withImage: true)),
                    FeedModule.FeedItem(value: makeModel(withImage: true))
                ],
                canLoadMore: true,
                totalFeedSize: 10
            ),
            currentCarInfo: .init()
        )
        let feedFacroty = FeedFactory(carInfo: carInfo)
        let feedCells = feedFacroty.makeFeed(
            from: model,
            onDisplayPreview: { _ in },
            onTapReview: { _ in },
            onTapArticle: { _, _ in },
            loadMore: {},
            rollUp: {},
            writeReview: {},
            segmentChanged: { _ in }
        ).tableCells
        Snapshot.compareWithSnapshot(layout: try XCTUnwrap(feedCells.last?.layout), maxWidth: DeviceWidth.iPhone11)
    }

    func test_FeedAllWithoutButton() throws {
        let carInfo = Auto_Api_CarInfo.with { info in
            info.markInfo.name = "BMW"
            info.modelInfo.name = "5ER"
        }
        let model = FeedFactory.Model(
            feedSize: 10,
            feedSegment: .all,
            feedContent: .init(
                items: [
                    FeedModule.FeedItem(value: makeModel(withImage: true)),
                    FeedModule.FeedItem(value: makeModel(withImage: true))
                ],
                canLoadMore: false,
                totalFeedSize: 2
            ),
            currentCarInfo: .init()
        )
        let feedFacroty = FeedFactory(carInfo: carInfo)
        let feedCells = feedFacroty.makeFeed(
            from: model,
            onDisplayPreview: { _ in },
            onTapReview: { _ in },
            onTapArticle: { _, _ in },
            loadMore: {},
            rollUp: {},
            writeReview: {},
            segmentChanged: { _ in }
        ).tableCells
        Snapshot.compareWithSnapshot(layout: try XCTUnwrap(feedCells.last?.layout), maxWidth: DeviceWidth.iPhone11)
    }

    func test_FeedRollUp() throws {
        let carInfo = Auto_Api_CarInfo.with { info in
            info.markInfo.name = "BMW"
            info.modelInfo.name = "5ER"
        }
        let model = FeedFactory.Model(
            feedSize: 4,
            feedSegment: .all,
            feedContent: .init(
                items: [
                    FeedModule.FeedItem(value: makeModel(withImage: true)),
                    FeedModule.FeedItem(value: makeModel(withImage: true)),
                    FeedModule.FeedItem(value: makeModel(withImage: true)),
                    FeedModule.FeedItem(value: makeModel(withImage: true))
                ],
                canLoadMore: false,
                totalFeedSize: 4
            ),
            currentCarInfo: .init()
        )
        let feedFacroty = FeedFactory(carInfo: carInfo)
        let feedCells = feedFacroty.makeFeed(
            from: model,
            onDisplayPreview: { _ in },
            onTapReview: { _ in },
            onTapArticle: { _, _ in },
            loadMore: {},
            rollUp: {},
            writeReview: {},
            segmentChanged: { _ in }
        ).tableCells
        Snapshot.compareWithSnapshot(layout: try XCTUnwrap(feedCells.last?.layout), maxWidth: DeviceWidth.iPhone11)
    }

    func test_FeedEmptyReviews() throws {
        let carInfo = Auto_Api_CarInfo.with { info in
            info.markInfo.name = "BMW"
            info.modelInfo.name = "5ER"
        }

        let model = FeedFactory.Model(
            feedSize: 3,
            feedSegment: .reviews,
            feedContent: .init(
                items: [],
                canLoadMore: false,
                totalFeedSize: 0
            ),
            currentCarInfo: .init()
        )

        let feedFacroty = FeedFactory(carInfo: carInfo)
        let feedCells = feedFacroty.makeFeed(
            from: model,
            onDisplayPreview: { _ in },
            onTapReview: { _ in },
            onTapArticle: { _, _ in },
            loadMore: {},
            rollUp: {},
            writeReview: {},
            segmentChanged: { _ in }
        ).tableCells
        Snapshot.compareWithSnapshot(layout: try XCTUnwrap(feedCells[1].layout), maxWidth: DeviceWidth.iPhone11)
        Snapshot.compareWithSnapshot(layout: try XCTUnwrap(feedCells[2].layout), maxWidth: DeviceWidth.iPhone11, identifier: "test_FeedEmptyReviews_button")
    }

    private func makeModel(withImage: Bool, title: String = "Article test") -> FeedItemModel {
        let apiModel = Auto_Lenta_Payload.with { snippet in
            snippet.title = title
            snippet.created = .init(date: Date(timeIntervalSince1970: .zero))
            if withImage {
                do {
                    snippet.mainImage = try Auto_Api_Photo.init(jsonString: Self.mainPhoto)
                } catch {
                    XCTFail(error.localizedDescription)
                }
            }
        }

        return FeedItemModel(apiModel)
    }

    private static let mainPhoto = """
    {
        "sizes": {
            "wide": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide",
            "mobile": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/mobile",
            "wide@3": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide@3",
            "wide@2": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide@2",
            "4x3@3": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3@3",
            "desktop": "https://autoru-mag.s3.yandex.net/2021/05/26/08a408fa2a1744b4b11a7080082cc9e1.jpg/desktop",
            "4x3": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3",
            "4x3@1.5": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3@1.5",
            "4x3@2": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3@2",
            "wide@1.5": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide@1.5"
        }
    }
    """
}
