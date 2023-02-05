import OHHTTPStubs

func verifyJsonBody(_ verifyBlock: @escaping ([AnyHashable: Any]) -> Bool) -> OHHTTPStubsTestBlock {
    { req in
        guard let jsonBody = try? JSONSerialization.jsonObject(
            with: req.httpBody ?? Data(),
            options: .mutableContainers
        ) as? [AnyHashable: Any]
        else {
            return false
        }
        return verifyBlock(jsonBody)
    }
}

func checkDictionary(
    source: [AnyHashable: NSObject],
    has: [AnyHashable: NSObject],
    path: [AnyHashable] = []
) -> [[AnyHashable]] {
    var result: [[AnyHashable]] = []
    for container in has {
        if
            let a = source[container.key] as? [AnyHashable: NSObject],
            let b = has[container.key] as? [AnyHashable: NSObject] {
            let errorPath = checkDictionary(source: a, has: b, path: path + [container.key])
            result += errorPath
        }
        if let a = source[container.key], let b = has[container.key], a.isEqual(b) {
            continue
        }
        result.append(path + [container.key])
    }
    return result
}

func verifyFAPIParameters(
    _ parameter: [AnyHashable: Any?],
    errorHandler: ((_ errorPaths: [[AnyHashable]]) -> Void)? = nil
) -> OHHTTPStubsTestBlock {
    let checkBodyBlock = { (body: [AnyHashable: Any]) -> Bool in
        guard
            let data = try? JSONSerialization.data(withJSONObject: parameter, options: []),
            let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [AnyHashable: NSObject],
            let source = (body["params" as AnyHashable] as? [Any])?.first as? [AnyHashable: NSObject]
        else {
            return false
        }
        let errorPath = checkDictionary(source: source, has: json)
        if errorPath.isEmpty == false {
            errorHandler?(errorPath)
            return false
        }
        return true
    }
    return verifyJsonBody(checkBodyBlock)
}

// swiftlint:enable opening_brace

func dummyTestBlock(_ block: @escaping () -> Void) -> OHHTTPStubsTestBlock {
    { _ in
        block()
        return true
    }
}

// swiftlint:enable opening_brace
