//
//  Created by Alexey Aleshkov on 02/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import enum Swifter.HttpResponse
import protocol Swifter.HttpResponseBodyWriter
import class YRETestsUtils.ResourceProvider

final class HttpResponseProvider {
    let generator: () -> HttpResponse

    init(generator: @escaping @autoclosure () -> HttpResponse) {
        self.generator = generator
    }

    func generate() -> HttpResponse {
        let result = self.generator()
        return result
    }
}

extension HttpResponseProvider {
    static func response(_ response: HttpResponse) -> Self {
        return .init(generator: response)
    }

    static func ok(_ provider: HttpResponseBodyProvider) -> Self {
        let body = provider.generate()
        return .init(generator: .ok(body))
    }

    static func internalServerError() -> Self {
        return .init(generator: .internalServerError)
    }

    static func redirect(_ urlString: String) -> Self {
        return .init(generator: .movedTemporarily(urlString))
    }

    static func forbidden(errorJSONFileName: String) -> Self {
        let defaultResponse = HttpResponse.forbidden

        let statusCode = defaultResponse.statusCode

        let reasonPhrase = defaultResponse.reasonPhrase

        var headers = defaultResponse.headers()
        headers["Content-Type"] = "application/json"

        let contentProvider = { (writer: HttpResponseBodyWriter) -> Void in
            let data = ResourceProvider.jsonData(from: errorJSONFileName)
            try writer.write(data)
        }

        let response = HttpResponse.raw(statusCode, reasonPhrase, headers, contentProvider)

        return .init(generator: response)
    }
}
