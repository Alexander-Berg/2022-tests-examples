//
//  MockDbProvider.swift
//  YREFeatures-Unit-Tests
//
//  Created by Aleksey Gotyanov on 11/6/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import YRESharedStorage
import RealmSwift

final class MockDbProvider: RealmProviderType {
    let realm: Realm

    init() {
        do {
            self.realm = try Realm(configuration: Realm.Configuration(inMemoryIdentifier: "memory"))
        }
        catch {
            fatalError("unable to create realm")
        }
    }

    func getRealm() -> Realm {
        self.realm
    }
}
