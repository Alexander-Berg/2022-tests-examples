//
//  BookmarksViewModel.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 29/02/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class BookmarksViewModel: DatabaseViewModel {
    
    private let database: BookmarksDatabase
    private var snapshot: BookmarksSnapshot? = nil
    private var items: [BookmarkFolder] = []
    
    init(database: BookmarksDatabase) {
        self.database = database
    }
    
    // MARK: DatabaseViewModel
    
    var onDidUpdate: (() -> Void)? = nil
    var numberOfItems: Int { return items.count }
    var headerTitle: String { return "Bookmark folders (\(numberOfItems))" }
    
    func update() {
        database.sync { [weak self] error in
            guard let slf = self else { return }
            
            if let error = error {
                print(error.localizedDescription)
                slf.snapshot = nil
                slf.updateWithCurrentSnapshot()
            }
            
            slf.database.openSnapshot { snapshot, error in
                if let error = error {
                    print(error.localizedDescription)
                }
                slf.snapshot = snapshot
                slf.updateWithCurrentSnapshot()
            }
        }
    }
    
    func cellInfo(at index: Int) -> SyncTableViewCell.Info {
        let item = items[index]
        var str = "\(item.id) {"
        str += "\n    title: \(item.title)"
        str += "\n    bookmarkIds: \(item.bookmarkIds.map { $0.head(4) })"
       
        if let tags = item.tags {
            str += "\n    tags: \(tags)"
        }
        
        str += "\n}"
        
        return SyncTableViewCell.Info(title: str)
    }
    
    // MARK: Private
    
    private func updateWithCurrentSnapshot() {
        items = snapshot?.folders ?? []
        onDidUpdate?()
    }
    
}


extension String {
    
    fileprivate func head(_ n: Int) -> String {
        let m = min(n, characters.count - 1)
        let end = characters.index(startIndex, offsetBy: m)
        return self[(startIndex ..< end)]
    }
    
}
