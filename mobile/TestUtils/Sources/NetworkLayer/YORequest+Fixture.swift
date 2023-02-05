//
//  YORequest+Fixture.swift
//  TestUtils
//
//  Created by Timur Turaev on 30.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import NetworkLayer

internal extension YORequest {
    func isEqualToJson(_ json: [AnyHashable: Any]) -> Bool {
        let parametersAreEqual = { () -> Bool in
            guard let parameters = self.parameters() else { return true }

            return (parameters as NSDictionary).isEqual(json["parameters"])
        }()

        return parametersAreEqual
            && (self.targetPath() as NSString).isEqual(json["targetPath"])
            && (NSStringFromHTTPMethod(self.httpMethodForRequest()) as NSString).isEqual(json["http_method"])
    }

    var json: [String: Any] {
        var result: [String: Any] = [
            "class": Self.description(),
            "http_method": NSStringFromHTTPMethod(self.httpMethodForRequest()),
            "targetPath": self.targetPath()
        ]
        if let parameters = self.parameters() {
            result["parameters"] = parameters as NSDictionary
        }

        if self.responds(to: NSSelectorFromString("data")),
           let data = self.value(forKey: "data") as? Data {
            result["data"] = data.base64EncodedString(options: .lineLength64Characters)
        }

        return result
    }
}
