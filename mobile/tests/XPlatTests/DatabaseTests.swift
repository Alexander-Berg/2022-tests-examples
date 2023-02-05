//
//  DatabaseTests.swift
//  xmail
//
//  Created by Dmitry Zakharov on 2019-05-28.
//
//

import Foundation
import XCTest
@testable import XPlat

class TestIDSupport: IDSupport {
  var idColumnType: DBEntityFieldType {
    .integer
  }

  @discardableResult
  func fromCursor(_ cursor: CursorValueExtractor, _ index: ColumnIndex) -> ID! {
    return cursor.isNull(index) ? nil : cursor.getInt64(index)
  }

  @discardableResult
  func toDBValue(_ id: ID) -> String {
    return idToString(id)!
  }
}

class DatabaseTests: XCTestCase {
  override func setUp() {
    XPromisesFramework.setup()
  }

  override func tearDown() {}

  func testDatabaseCreationWithSchema() {
    Scheme.setIDSupport(TestIDSupport())

    let database = DefaultStorageInitializer().open(":memory:", XmailStorageScheme())
    XCTAssertFalse(database.isError())
    let storage = database.getValue()
    Registry.registerStorage(storage)

    let created = expectation(description: "Expect all statements are executed")
    let fetched = expectation(description: "Expect fetch query are executed")

    Registry.getStorage().runQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%';", YSArray()).then {
      CursorMappers.singleInt32Column($0)
    }.then {
      res in res.length > 0 ? res[0] : nil
    }.then { count -> Void in
      created.fulfill()
      XCTAssertEqual(count, Scheme.allEntities().length)

      storage.prepareStatement("INSERT INTO thread_scn (tid, scn) VALUES (?, ?);").flatThen { statement -> XPromise<Void> in
        let statements = YSArray(statement.execute([1, 100]), statement.execute([2, 200]))
        return all(statements).flatThen { _ in
          storage.runQuery("SELECT scn FROM thread_scn WHERE tid = ?", YSArray<Any?>(2)).then { cursor -> YSArray<Int32> in
            CursorMappers.singleInt32Column(cursor)
          }
        }
        .then { scns in
          XCTAssertEqual(scns.length, 1)
          XCTAssertEqual(scns[0], 200)
          return getVoid()
        }
        .catch({ _ in
          XCTFail()
          return getVoid()
        })
        .finally {
          statement.close()
          fetched.fulfill()
        }
      }
    }.failed { _ in
      XCTFail()
      created.fulfill()
    }

    waitForExpectations(timeout: 1000)
  }
}
