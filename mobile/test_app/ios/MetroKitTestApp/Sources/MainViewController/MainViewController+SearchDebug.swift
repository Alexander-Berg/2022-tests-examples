//
//  MainViewController+SearchDebug.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 23/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension MainViewController {

    func checkSearch(for infoService: YMLSchemeInfoService, query: String) {
        let session = infoService.makeSearchSession(with: YMLLanguage(value: YXPlatformCurrentState.currentLanguage()))
        
        let results = session.searchResults(withQuery: query)
        
        print("\nSearch results for query ('\(query)'):")
        if results.count == 0 {
            print("No results :(")
        } else {
            results.forEach {
                print($0.customDescription)
            }
        }
    }

}
