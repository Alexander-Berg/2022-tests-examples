import Foundation
import SwiftProtobuf

extension Message {
    static func populate<T: Message>(_ mutation: (inout T) -> Void) -> T {
        var model = T()
        mutation(&model)
        return model
    }
}

extension Message {
    init(mockFile: String, subdirectory: String? = nil, setup mutation: ((inout Self) -> Void)? = nil) {
        guard let fileURL = Bundle.current.url(forResource: mockFile, withExtension: "json", subdirectory: subdirectory) else {
            fatalError("File \(mockFile) doesnt exist in \(String(describing: subdirectory)) in bundle")
        }
        let modelData = try! Data(contentsOf: fileURL, options: [.uncachedRead])

        var options = JSONDecodingOptions()
        options.ignoreUnknownFields = true

        try! self.init(jsonUTF8Data: modelData, options: options)
        mutation?(&self)
    }
}
