//
//  Created by Alexey Aleshkov on 02/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import UIKit
import enum Swifter.HttpResponseBody
import YRETestsUtils

final class HttpResponseBodyProvider {
    let generator: () -> HttpResponseBody

    init(generator: @escaping @autoclosure () -> HttpResponseBody) {
        self.generator = generator
    }

    func generate() -> HttpResponseBody {
        let result = self.generator()
        return result
    }
}

extension HttpResponseBodyProvider {
    static func contentsOfJSON(_ filename: String) -> Self {
        let json = ResourceProvider.jsonObject(from: filename)
        let result = HttpResponseBody.json(json)
        return .init(generator: result)
    }

    static func contentsOfHTML(_ filename: String) -> Self {
        let data = ResourceProvider.htmlData(from: filename)
        let result = HttpResponseBody.data(data, contentType: "text/html")
        return .init(generator: result)
    }

    static func contentsOfImage(_ image: UIImage?) -> Self {
        let stubImage = image?.pngData() ?? Data()
        let result = HttpResponseBody.data(stubImage, contentType: "image/png")
        return .init(generator: result)
    }
}
