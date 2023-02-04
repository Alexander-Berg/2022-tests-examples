import Foundation
import SwiftProtobuf

private let decodingOptions: JSONDecodingOptions = {
    var opt = JSONDecodingOptions()
    opt.ignoreUnknownFields = true
    return opt
}()

extension Message {
    static func fromFile(path: URL) -> Self {
        return try! Self(jsonUTF8Data: Data(contentsOf: path), options: decodingOptions)
    }

    static func fromFile(named name: String) -> Self {
        let url = Bundle.resources.url(forResource: name, withExtension: "json")!
        return try! Self(jsonUTF8Data: Data(contentsOf: url), options: decodingOptions)
    }
}
