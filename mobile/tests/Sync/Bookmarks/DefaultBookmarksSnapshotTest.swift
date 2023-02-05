//
//  DefaultBookmarksSnapshotTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest


class DefaultBookmarksSnapshotTest: XCTestCase {

    var database: BookmarksDatabase!
    
    override func setUp() {
        let databaseAdapter = DatabaseAdapterTestImpl(id: "bookmarks")
        database = DefaultBookmarksDatabase(database: Database(adapter: databaseAdapter))
    }
    
    func testThatBeginAndEndEditingChangesEditingField() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(!snapshot.editing)
            snapshot.beginEditing()
            XCTAssert(snapshot.editing)
            snapshot.endEditing()
            XCTAssert(!snapshot.editing)
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatSnapshotAlwaysContainsFavouriteFolder() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot,
                let resolved = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId),
                let favourites = snapshot.folder(id: BookmarkFolder.favoriteId) else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.folders.count == 1)
            XCTAssert(snapshot.resolvedFolders().count == 1)
            XCTAssert(favourites.isFavorite)
            XCTAssert(resolved.isFavorite)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAppendFolderMethodAppendsNewFolder() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
        
            snapshot.beginEditing()
            let new1 = snapshot.appendFolder(title: "title", tags: nil)
            let new2 = snapshot.appendFolder(title: "title", tags: nil)
            let new3 = snapshot.appendFolder(title: "other_title", tags: nil)
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.folders.count == 4)
            XCTAssert(!new1.isFavorite)
            XCTAssert(!new2.isFavorite)
            XCTAssert(!new3.isFavorite)
            XCTAssert(new1.id != new2.id)
            XCTAssert(new2.id != new3.id)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateFolderWithTitleMethodChangesTitleOnly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            var folder = snapshot.appendFolder(title: "title", tags: ["1"])
            folder = snapshot.updateFolder(id: folder.id, newTitle: "title2")!
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
            XCTAssert(folder.title == "title2")
            XCTAssert(folder.tags! == ["1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateFolderWithTagsMethodChangesTagsOnly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            var folder = snapshot.appendFolder(title: "title", tags: ["1"])
            folder = snapshot.updateFolder(id: folder.id, newTags: ["2", "3"])!
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
            XCTAssert(folder.tags! == ["2", "3"])
            XCTAssert(folder.title == "title")
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatDeleteFolderMethodDeletesFolder() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let folder1 = snapshot.appendFolder(title: "title1", tags: nil)
            let folder2 = snapshot.appendFolder(title: "title2", tags: nil)
            snapshot.deleteFolder(id: folder1.id)
            snapshot.deleteFolder(id: folder2.id)
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.folders.count == 1)
            XCTAssert(snapshot.folder(id: folder1.id) == nil)
            XCTAssert(snapshot.folder(id: folder2.id) == nil)
            XCTAssert(snapshot.folder(id: BookmarkFolder.favoriteId) != nil)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatMoveFolderWithIdMethodMovesFolderCorrectly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let folder1 = snapshot.appendFolder(title: "title1", tags: nil)
            let folder2 = snapshot.appendFolder(title: "title2", tags: nil)
            let folder3 = snapshot.appendFolder(title: "title3", tags: nil)
            snapshot.moveFolder(id: folder1.id, toIndex: 2)
            snapshot.moveFolder(id: folder3.id, toIndex: 1)
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.folders.count == 4)
            XCTAssert(snapshot.folders[1].id == folder3.id)
            XCTAssert(snapshot.folders[2].id == folder2.id)
            XCTAssert(snapshot.folders[3].id == folder1.id)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatMoveFolderFromIndexMethodMovesFolderCorrectly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let folder1 = snapshot.appendFolder(title: "title1", tags: nil)
            let folder2 = snapshot.appendFolder(title: "title2", tags: nil)
            let folder3 = snapshot.appendFolder(title: "title3", tags: nil)
            snapshot.moveFolder(fromIndex: 1, toIndex: 3)
            snapshot.moveFolder(fromIndex: 2, toIndex: 1)
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.folders.count == 4)
            XCTAssert(snapshot.folders[1].id == folder3.id)
            XCTAssert(snapshot.folders[2].id == folder2.id)
            XCTAssert(snapshot.folders[3].id == folder1.id)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatInsertBookmarkMethodInsetsBookmarkAtTheBeginning() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)
            let bookmark2 = snapshot.insertBookmark(title: "title2", uri: "uri2", description: "description2",
                tags: ["912", "91"], toFolderWithId: BookmarkFolder.favoriteId)
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(ids.count == 2)
            XCTAssert(ids[0] == bookmark2?.id)
            XCTAssert(ids[1] == bookmark1?.id)
            XCTAssert(bookmarks.count == 2)
            XCTAssert(bookmarks[0].id == bookmark2?.id)
            XCTAssert(bookmarks[0].title == "title2")
            XCTAssert(bookmarks[0].uri == "uri2")
            XCTAssert(bookmarks[0].description == "description2")
            XCTAssert(bookmarks[0].tags! == ["912", "91"])
            XCTAssert(bookmarks[1].id == bookmark1?.id)
            XCTAssert(bookmarks[1].title == "title1")
            XCTAssert(bookmarks[1].uri == "uri1")
            XCTAssert(bookmarks[1].description == "description1")
            XCTAssert(bookmarks[1].tags! == ["12", "1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateBookmarkWithTitleMethodUpdatesBookmarkTitleOnly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)
            snapshot.updateBookmark(id: bookmark1!.id, newTitle: "title2")
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(bookmarks.count == 1)
            XCTAssert(bookmarks[0].id == bookmark1?.id)
            XCTAssert(bookmarks[0].title == "title2")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description1")
            XCTAssert(bookmarks[0].tags! == ["12", "1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateBookmarkWithDescriptionMethodUpdatesBookmarkDescriptionOnly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)
            snapshot.updateBookmark(id: bookmark1!.id, newDescription: "description2")
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(bookmarks.count == 1)
            XCTAssert(bookmarks[0].id == bookmark1?.id)
            XCTAssert(bookmarks[0].title == "title1")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description2")
            XCTAssert(bookmarks[0].tags! == ["12", "1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateBookmarkWithTagsMethodUpdatesBookmarkTagsOnly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)
            snapshot.updateBookmark(id: bookmark1!.id, newTags: ["89"])
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(bookmarks.count == 1)
            XCTAssert(bookmarks[0].id == bookmark1?.id)
            XCTAssert(bookmarks[0].title == "title1")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description1")
            XCTAssert(bookmarks[0].tags! == ["89"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatDeleteBookmarkMethodChangesResolvedBookmarksAndBookmarkIds() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)
            let bookmark2 = snapshot.insertBookmark(title: "title2", uri: "uri2", description: "description2",
                tags: ["912", "91"], toFolderWithId: BookmarkFolder.favoriteId)
            snapshot.deleteBookmark(id: bookmark1!.id, folderId: BookmarkFolder.favoriteId)
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(ids.count == 1)
            XCTAssert(ids[0] == bookmark2?.id)
            XCTAssert(bookmarks.count == 1)
            XCTAssert(bookmarks[0].id == bookmark2?.id)
            XCTAssert(bookmarks[0].title == "title2")
            XCTAssert(bookmarks[0].uri == "uri2")
            XCTAssert(bookmarks[0].description == "description2")
            XCTAssert(bookmarks[0].tags! == ["912", "91"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatDeleteBookmarkMethodChangesNothingIfFolderDoesntContainBookmark() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)!
            snapshot.deleteBookmark(id: bookmark1.id + "ii", folderId: BookmarkFolder.favoriteId)
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(ids.count == 1)
            XCTAssert(ids[0] == bookmark1.id)
            XCTAssert(bookmarks.count == 1)
            XCTAssert(bookmarks[0].id == bookmark1.id)
            XCTAssert(bookmarks[0].title == "title1")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description1")
            XCTAssert(bookmarks[0].tags! == ["12", "1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatMoveBookmarkWithIdMethodMovesBookmarkCorrectly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)!
            let bookmark2 = snapshot.insertBookmark(title: "title2", uri: "uri2", description: "description2",
                tags: ["912", "91"], toFolderWithId: BookmarkFolder.favoriteId)!
            
            snapshot.moveBookmark(id: bookmark2.id, toIndex: 1, folderId: BookmarkFolder.favoriteId)
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(ids.count == 2)
            XCTAssert(ids[1] == bookmark2.id)
            XCTAssert(ids[0] == bookmark1.id)
            XCTAssert(bookmarks.count == 2)
            XCTAssert(bookmarks[1].id == bookmark2.id)
            XCTAssert(bookmarks[1].title == "title2")
            XCTAssert(bookmarks[1].uri == "uri2")
            XCTAssert(bookmarks[1].description == "description2")
            XCTAssert(bookmarks[1].tags! == ["912", "91"])
            XCTAssert(bookmarks[0].id == bookmark1.id)
            XCTAssert(bookmarks[0].title == "title1")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description1")
            XCTAssert(bookmarks[0].tags! == ["12", "1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatMoveBookmarkFromIndexMethodMovesBookmarkCorrectly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)!
            let bookmark2 = snapshot.insertBookmark(title: "title2", uri: "uri2", description: "description2",
                tags: ["912", "91"], toFolderWithId: BookmarkFolder.favoriteId)!
            
            snapshot.moveBookmark(fromIndex: 1, toIndex: 0, folderId: BookmarkFolder.favoriteId)
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(ids.count == 2)
            XCTAssert(ids[1] == bookmark2.id)
            XCTAssert(ids[0] == bookmark1.id)
            XCTAssert(bookmarks.count == 2)
            XCTAssert(bookmarks[1].id == bookmark2.id)
            XCTAssert(bookmarks[1].title == "title2")
            XCTAssert(bookmarks[1].uri == "uri2")
            XCTAssert(bookmarks[1].description == "description2")
            XCTAssert(bookmarks[1].tags! == ["912", "91"])
            XCTAssert(bookmarks[0].id == bookmark1.id)
            XCTAssert(bookmarks[0].title == "title1")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description1")
            XCTAssert(bookmarks[0].tags! == ["12", "1"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatMoveBookmarkWithIdFromFolderMethodMovesBookmarkCorrectly() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)!
            let bookmark2 = snapshot.insertBookmark(title: "title2", uri: "uri2", description: "description2",
                tags: ["912", "91"], toFolderWithId: BookmarkFolder.favoriteId)!
            let folder = snapshot.appendFolder(title: "folder", tags: nil)
            snapshot.moveBookmark(id: bookmark2.id, fromFolderWithId: BookmarkFolder.favoriteId, toFolderWithId: folder.id)
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds,
                let folderBookmarks = snapshot.resolvedFolder(id: folder.id)?.bookmarks,
                let folderIds = snapshot.folder(id: folder.id)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            XCTAssert(ids.count == 1)
            XCTAssert(ids[0] == bookmark1.id)
            XCTAssert(folderIds.count == 1)
            XCTAssert(folderIds[0] == bookmark2.id)
            XCTAssert(bookmarks.count == 1)
            XCTAssert(bookmarks[0].id == bookmark1.id)
            XCTAssert(bookmarks[0].title == "title1")
            XCTAssert(bookmarks[0].uri == "uri1")
            XCTAssert(bookmarks[0].description == "description1")
            XCTAssert(bookmarks[0].tags! == ["12", "1"])
            XCTAssert(folderBookmarks.count == 1)
            XCTAssert(folderBookmarks[0].id == bookmark2.id)
            XCTAssert(folderBookmarks[0].title == "title2")
            XCTAssert(folderBookmarks[0].uri == "uri2")
            XCTAssert(folderBookmarks[0].description == "description2")
            XCTAssert(folderBookmarks[0].tags! == ["912", "91"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatMoveBookmarkWithIdFromFolderMethodChangesNothingIfBookmarkOrFolderDonesntExist() {
        let exp = expectation(description: "")
        
        database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            let bookmark1 = snapshot.insertBookmark(title: "title1", uri: "uri1", description: "description1",
                tags: ["12", "1"], toFolderWithId: BookmarkFolder.favoriteId)!
            let bookmark2 = snapshot.insertBookmark(title: "title2", uri: "uri2", description: "description2",
                tags: ["912", "91"], toFolderWithId: BookmarkFolder.favoriteId)!
            let folder = snapshot.appendFolder(title: "folder", tags: nil)
            
            snapshot.moveBookmark(id: bookmark2.id + "ii", fromFolderWithId: BookmarkFolder.favoriteId, toFolderWithId: folder.id)
            snapshot.moveBookmark(id: bookmark2.id, fromFolderWithId: folder.id, toFolderWithId: BookmarkFolder.favoriteId)
            snapshot.moveBookmark(id: bookmark2.id, fromFolderWithId: BookmarkFolder.favoriteId, toFolderWithId: folder.id + "ii")
            snapshot.moveBookmark(id: bookmark2.id, fromFolderWithId: BookmarkFolder.favoriteId, toFolderWithId: BookmarkFolder.favoriteId)
            
            snapshot.endEditing()
            
            guard let bookmarks = snapshot.resolvedFolder(id: BookmarkFolder.favoriteId)?.bookmarks,
                let ids = snapshot.folder(id: BookmarkFolder.favoriteId)?.bookmarkIds,
                let folderBookmarks = snapshot.resolvedFolder(id: folder.id)?.bookmarks,
                let folderIds = snapshot.folder(id: folder.id)?.bookmarkIds else
            {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.valid)
            
            XCTAssert(folderIds.count == 0)
            XCTAssert(folderBookmarks.count == 0)
            
            XCTAssert(ids.count == 2)
            XCTAssert(ids[0] == bookmark2.id)
            XCTAssert(ids[1] == bookmark1.id)
            XCTAssert(bookmarks.count == 2)
            XCTAssert(bookmarks[1].id == bookmark1.id)
            XCTAssert(bookmarks[1].title == "title1")
            XCTAssert(bookmarks[1].uri == "uri1")
            XCTAssert(bookmarks[1].description == "description1")
            XCTAssert(bookmarks[1].tags! == ["12", "1"])
            XCTAssert(bookmarks[0].id == bookmark2.id)
            XCTAssert(bookmarks[0].title == "title2")
            XCTAssert(bookmarks[0].uri == "uri2")
            XCTAssert(bookmarks[0].description == "description2")
            XCTAssert(bookmarks[0].tags! == ["912", "91"])
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }

}
