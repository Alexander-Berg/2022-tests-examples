//
//  DevTestRequestAndResponse.swift
//  TestUtils
//
//  Created by Timur Turaev on 27.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import NetworkLayer

@objc(YODevTestRequest)
public final class DevTestRequest: YODataRequest {
    private let path: String

    public init(path: String) {
        self.path = path
    }

    public override func parameters() -> [AnyHashable: Any]? {
        return .empty
    }

    public override func targetPath() -> String {
        return self.path
    }

    public override func apiVersion() -> YOAPIVersion {
        return .V2
    }

    public override func shouldJsonifyBody() -> Bool {
        return true
    }

    public override func copy(with zone: NSZone? = nil) -> Any {
        let copy = DevTestRequest(path: self.path)
        self.copyProperties(to: copy)
        return copy
    }

    public override func isEqual(_ object: Any?) -> Bool {
        guard object is DevTestRequest else {
            return false
        }

        return super.isEqual(object)
    }

    public override var hash: Int {
        var hasher = Hasher()
        hasher.combine(super.hash)
        return hasher.finalize()
    }

    public override var description: String {
        var description = ""
        var superDescription = super.description
        if superDescription.last == ">" {
            superDescription.removeLast()
            description = "\(superDescription), \(description)>"
        } else {
            description = "<\(type(of: self)) : \(description)>"
        }
        return description
    }
}

@objc(YODevTestResponse)
public final class DevTestResponse: NSObject, YODataResponse, YOResponseWithResult {
    public let responseResult: Int

    public init?(data: Data) {
        self.responseResult = 42
    }
}
