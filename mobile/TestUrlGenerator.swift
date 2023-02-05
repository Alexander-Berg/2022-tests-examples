//
//  CommonUrlManagerTestCase.swift
//  YandexMaps
//
//  Created by Iskander Yadgarov on 09.06.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation
@testable import YandexMaps

class TestUrlGenerator {
    
    static func generateUrlSchemes(hosts: [String] = Static.possibleHosts,
                                   path: String? = nil,
                                   tokens: [String] = []) -> [URL]
    {
        return generateUrls(schemes: [Static.applicationScheme],
                            hosts: hosts,
                            path: path,
                            tokens: tokens)
    }
    
    static func generateUrls(schemes: [String] = [Static.deepLinkScheme, Static.applicationScheme],
                             hosts: [String] = Static.possibleHosts,
                             path: String? = nil,
                             tokens: [String] = []) -> [URL]
    {
        var urls: [URL] = []
        
        for scheme in schemes {
            for host in hosts {
                var urlString = scheme + "://" + host
                path.flatMap { urlString.append($0) }
                urlString.append("?")
                
                var tokensString = tokens.joined(separator: "&")
                if let encoded = tokensString.addingPercentEncoding(withAllowedCharacters: CharacterSet.urlQueryAllowed) {
                    tokensString = encoded
                }
                urlString.append(tokensString)
                
                urls.append(URL(string: urlString)!)
            }
        }
        
        return urls
    }
    
}

fileprivate extension TestUrlGenerator {
    
    struct Static {
        static let deepLinkScheme = "https"
        static let applicationScheme = "yandexmaps"
        static let possibleHosts: [String] = [
            "yandex.ru/maps",
            "yandex.com/maps",
            "maps.yandex.ru",
            "maps.yandex.com",
            "yandex.com/harita",
            "yandex.com.tr/harita"
        ]
    }
    
}
