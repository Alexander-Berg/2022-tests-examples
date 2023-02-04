//
//  VillagesServiceMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 09.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREFiltersModel
import YREModel
import YREModelObjc
import YREServiceLayer
import YREServiceLayerBase
import YREServiceInterfaces

final class VillagesServiceMock: VillagesServiceProtocol {
    struct GoodResponse {
        let village: Village?
        let villageOffers: VillageOffers?
    }
    typealias Response = Result<GoodResponse, Error>

    let response: Response

    convenience init() {
        self.init(response: .success(.init(village: nil, villageOffers: Self.mockVillageOffers)))
    }

    init(response: Response) {
        self.response = response
    }

    func getVillageCard(
        identifier: String,
        completion: @escaping (YREDataLayerTaskProtocol, Village?, Bool) -> Void
    ) -> YREDataLayerTaskProtocol {
        let task = YREDataLayerTask()

        DispatchQueue.main.async {
            switch self.response {
                case .success(let data):
                    task.markCompleted(withSuccess: true, error: nil)
                    completion(task, data.village, false)

                case .failure(let error):
                    task.markCompleted(withSuccess: false, error: error)
                    completion(task, nil, false)
            }
        }
        return task
    }

    func getVillageOffers(
        identifier: String,
        parameters: VillageOffersRequestParameters,
        page: UInt,
        pageSize: UInt,
        completion: @escaping (YREDataLayerTaskProtocol?, VillageOffers?) -> Void
    ) -> YREDataLayerTaskProtocol {
        let task = YREDataLayerTask()

        DispatchQueue.main.async {
            switch self.response {
                case .success(let data):
                    task.markCompleted(withSuccess: true, error: nil)
                    completion(task, data.villageOffers)

                case .failure(let error):
                    task.markCompleted(withSuccess: false, error: error)
                    completion(task, nil)
            }
        }
        return task
    }

    func villageOfferSnippetListDataSource(
        villageID: String,
        parameters: VillageOffersRequestParameters,
        searchResultsReporter: SearchResultsReporterProtocol
    ) -> ListDataSourceProtocol {
        ListDataSourceMock()
    }

    private static let mockVillageOffers: VillageOffers = .init(
        items: [],
        slicing: .init(totalItemsCount: 0, page: .init(number: 0, size: 0)),
        logQueryText: "",
        logQueryId: ""
    )
}
