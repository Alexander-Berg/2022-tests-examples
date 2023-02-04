//
//  AnyOffersListSearchServiceMocks.swift
//  YREServiceLayer
//
//  Created by Pavel Zhuravlev on 07.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREModel
import YREModelObjc
import YREServiceLayer
import YREServiceLayerBase
import YREServiceInterfaces
import class YRECoreUtils.YREError

final class AnyOffersListSearchServiceMock: AnyOffersListSearchService {
    struct GoodResponse {
        let items: [YREAbstractOffer]
        let promotedOffersCount: Int
        let pageSize: Int
    }
    typealias Response = Result<GoodResponse, Error>

    let response: Response

    convenience init<T: IteratorProtocol>(
        itemsGenerator: T,
        pageSize: Int,
        promotedOffersCount: Int
    ) where T.Element: YREAbstractOffer {
        var items: [YREAbstractOffer] = []
        var generator = itemsGenerator

        while let item = generator.next() {
            items.append(item)
        }
        self.init(items: items, pageSize: pageSize, promotedOffersCount: promotedOffersCount)
    }

    // `promotedOffersCount` - count of additional (promoted) items on the first page
    convenience init(
        items: [YREAbstractOffer],
        pageSize: Int,
        promotedOffersCount: Int
    ) {
        self.init(
            response: .success(
                GoodResponse(
                    items: items,
                    promotedOffersCount: promotedOffersCount,
                    pageSize: pageSize
                )
            )
        )
    }

    convenience init(
        error: Error?
    ) {
        self.init(response: .failure(error ?? YREError.lazyAss(errorDescription: "Any error")))
    }

    init(response: Response) {
        self.response = response
    }

    func getOfferSnippets(
        parameters: [String: AnyObject]?,
        from: Date?,
        to: Date?,
        page: Int,
        pageSize: Int,
        completionBlock: YREGetOfferSnippetsCompletionBlock?
    ) -> YREDataLayerTaskProtocol {
        let task = YREDataLayerTask()

        DispatchQueue.main.async {
            switch self.response {
                case .success(let data):
                    task.markCompleted(withSuccess: true, error: nil)

                    // Negative pageSize may be provided - usually that means we expect a real page size from the backend
                    let realPageSize = pageSize > 0 ? pageSize : data.pageSize

                    Self.handleOfferSnippetsRequest(
                        page: page,
                        pageSize: realPageSize,
                        completion: completionBlock,
                        withGoodResponse: data,
                        task: task
                    )

                case .failure(let error):
                    task.markCompleted(withSuccess: false, error: error)
                    completionBlock?(task, nil, nil, nil, nil, nil, nil)
            }
        }
        return task
    }

    @discardableResult
    func getOfferSnippetsCount(
        parameters: [String: AnyObject]?,
        from: Date?,
        to: Date?,
        isSearchExtendingEnabled: Bool,
        completionBlock: YREGetOfferSnippetsCountCompletionBlock?
    ) -> YREDataLayerTaskProtocol {
        let task = YREDataLayerTask()

        DispatchQueue.main.async {
            switch self.response {
                case .success(let data):
                    task.markCompleted(withSuccess: true, error: nil)
                    completionBlock?(task, UInt(data.items.count), nil, nil)

                case .failure(let error):
                    task.markCompleted(withSuccess: false, error: error)
                    completionBlock?(task, 0, nil, nil)
            }
        }
        return task
    }

    // MARK: Private

    private static func handleOfferSnippetsRequest(
        page: Int,
        pageSize: Int,
        completion: YREGetOfferSnippetsCompletionBlock?,
        withGoodResponse response: GoodResponse,
        task: YREDataLayerTaskProtocol
    ) {
        let isFirstPage = (page == 0)
        let currentPageSize = pageSize + (isFirstPage ? response.promotedOffersCount : 0)

        let firstIndex = page * pageSize + (isFirstPage ? 0 : response.promotedOffersCount)
        let lastIndex = min(response.items.count, firstIndex + currentPageSize)

        let items: [YREAbstractOffer] = Array(response.items[firstIndex ..< lastIndex])

        let pager = YREPager(
            page: UInt(page),
            pageSize: UInt(currentPageSize),
            totalItems: UInt(response.items.count),
            totalPages: UInt(response.items.count / pageSize) + 1
        )

        if let offers = items as? [YREOfferSnippet] {
            completion?(task, offers, nil, nil, pager, nil, nil)
        }
        else if let sites = items as? [YRESiteSnippet] {
            completion?(task, nil, sites, nil, pager, nil, nil)
        }
        else if let villages = items as? [VillageSnippet] {
            completion?(task, nil, nil, villages, pager, nil, nil)
        }
        else {
            if let item = items.first {
                assertionFailure("Unknown type of items in a list: \(item)")
            }
            completion?(task, nil, nil, nil, pager, nil, nil)
        }
    }
}
