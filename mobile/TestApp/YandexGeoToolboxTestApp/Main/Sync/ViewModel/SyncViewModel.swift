//
//  SyncViewModel.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 28/01/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import YandexRuntime
import YandexDataSync

protocol SyncViewModel: class {
    var onDidUpdate: (() -> Void)? { get set }
    var account: YRTAccount? { get set }
    var numberOfSections: Int { get }
    
    func update()
    func numberOfItems(section: Int) -> Int
    func headerTitle(section: Int) -> String
    func cellInfo(indexPath: IndexPath) -> SyncTableViewCell.Info
}


class DefaultSyncViewModel: NSObject, SyncViewModel {
    
    private let databaseManager: YDSDatabaseManager
    private let kvStorage: KeyValueStorage
    private var databaseModels: [DatabaseViewModel] = []
    
    init(databaseManager: YDSDatabaseManager, kvStorage: KeyValueStorage, account: YRTAccount? = nil) {
        self.databaseManager = databaseManager
        self.kvStorage = kvStorage
        self.account = account
        super.init()
    }
    
    // MARK: SyncViewModel
    
    var onDidUpdate: (() -> Void)? = nil
   
    var account: YRTAccount? {
        didSet {
            if (oldValue === account) {
                return
            }
            update()
        }
    }
    
    var numberOfSections: Int { return databaseModels.count }
    
    func update() {
        let searchHistoryDatabaseAdapter = YDSDatabaseAdapter(id: DatabaseID.SearchHistory.rawValue,
            databaseProvider: databaseManager, keyValueStorage: kvStorage)
        searchHistoryDatabaseAdapter.account = account
        let searchHistoryDatabase = SearchHistoryDatabase(database: Database(adapter: searchHistoryDatabaseAdapter))
        let searchHistoryModel = SearchHistoryViewModel(database: searchHistoryDatabase)
        searchHistoryModel.onDidUpdate = { [weak self] in
            self?.onDidUpdate?()
        }
        
        let routingHistoryDatabaseAdapter = YDSDatabaseAdapter(id: DatabaseID.RoutingHistory.rawValue,
            databaseProvider: databaseManager, keyValueStorage: kvStorage)
        routingHistoryDatabaseAdapter.account = account
        let routingHistoryDatabase = RoutingHistoryDatabase(database: Database(adapter: routingHistoryDatabaseAdapter))
        let routingHistoryModel = RoutingHistoryViewModel(database: routingHistoryDatabase)
        routingHistoryModel.onDidUpdate = { [weak self] in
            self?.onDidUpdate?()
        }
        
        let importantPlacesDatabaseAdapter = YDSDatabaseAdapter(id: DatabaseID.HomeWork.rawValue,
            databaseProvider: databaseManager, keyValueStorage: kvStorage)
        importantPlacesDatabaseAdapter.account = account
        let importantPlacesDatabase = DefaultImportantPlacesDatabase(database: Database(adapter: importantPlacesDatabaseAdapter))
        let importantPlacesModel = ImportantPlacesViewModel(database: importantPlacesDatabase)
        importantPlacesModel.onDidUpdate = { [weak self] in
            self?.onDidUpdate?()
        }
        
        let bookmarksDatabaseAdapter = YDSDatabaseAdapter(id: DatabaseID.Bookmarks.rawValue,
            databaseProvider: databaseManager, keyValueStorage: kvStorage)
        bookmarksDatabaseAdapter.account = account
        let bookmarksDatabase = DefaultBookmarksDatabase(database: Database(adapter: bookmarksDatabaseAdapter))
        let bookmarksModel = BookmarksViewModel(database: bookmarksDatabase)
        bookmarksModel.onDidUpdate = { [weak self] in
            self?.onDidUpdate?()
        }
        
        databaseModels = [searchHistoryModel, routingHistoryModel, importantPlacesModel, bookmarksModel]
        
        databaseModels.forEach { $0.update() }
    }
    
    func numberOfItems(section: Int) -> Int {
        return min(databaseModels[section].numberOfItems, DefaultSyncViewModel.MAX_NUMBER_OF_ITEMS)
    }
    
    func headerTitle(section: Int) -> String {
        return databaseModels[section].headerTitle
    }
    
    func cellInfo(indexPath: IndexPath) -> SyncTableViewCell.Info {
        return databaseModels[indexPath.section].cellInfo(at: indexPath.row)
    }
    
    // MARK: Private static
    
    private static let MAX_NUMBER_OF_ITEMS = 5
    
    private enum DatabaseID: String {
        case Bookmarks = ".ext.maps_common@ymapsbookmarks1"
        case HomeWork = ".ext.profile@addresses"
        case RoutingHistory = ".ext.maps_common@ymapspointshistory1"
        case SearchHistory = ".ext.maps_common@ymapssearchhistory1"
        
        static let ALL: [DatabaseID] = [.Bookmarks, .HomeWork, .RoutingHistory, .SearchHistory]

        var collectionID: CollectionID {
            switch self {
            case .Bookmarks:
                return .Bookmarks
            case .HomeWork:
                return .HomeWork
            case .RoutingHistory:
                return .RoutingHistory
            case .SearchHistory:
                return .SearchHistory
            }
        }
    }
    
    private enum CollectionID: String {
        case Bookmarks = "bookmarks"
        case HomeWork = "common_addresses"
        case RoutingHistory = "pointshistory"
        case SearchHistory = "search_history"
    }
    
}
