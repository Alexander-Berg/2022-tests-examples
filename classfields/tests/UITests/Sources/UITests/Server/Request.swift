import Foundation

class Request {
    let data: Data
    let comps: [String]
    let messageBody: Data?
    let headers: [String: String]
    let requestLine: String
    let uri: String
    let method: String

    init?(data: Data) {
        let dataSize = data.count

        guard dataSize > 0 else {
            return nil
        }

        var comps: [String] = []
        var messageBody: Data?

        var curComp: [UnicodeScalar] = []
        var prevChar: UnicodeScalar?

        for i in 0..<dataSize {
            let curChar = UnicodeScalar(data[i])

            if curChar == "\n" && prevChar == "\r" {
                if curComp.count > 2 {
                    comps.append(String(Character.UnicodeScalarView(curComp[0...curComp.count - 2])))
                } else {
                    let nextByte = i + 1
                    if nextByte < dataSize {
                        messageBody = data[nextByte...]
                    }
                    break
                }
                curComp = []
            } else {
                curComp.append(curChar)
            }

            if i == dataSize - 1, !curComp.isEmpty {
                comps.append(String(Character.UnicodeScalarView(curComp)))
            }

            prevChar = curChar
        }

        var headers: [String: String] = [:]
        for comp in comps.dropFirst() {
            if let separatorIndex = comp.firstIndex(of: ":") {
                var valueStartIndex = comp.index(after: separatorIndex)
                while valueStartIndex != comp.endIndex, comp[valueStartIndex] == " " {
                    valueStartIndex = comp.index(after: valueStartIndex)
                }

                if valueStartIndex != comp.endIndex {
                    let key = String(comp[comp.startIndex..<separatorIndex]).lowercased()
                    let value = String(comp[valueStartIndex...])
                    headers[key] = value
                }
            }
        }

        guard let requestLine = comps.first else {
            return nil
        }

        let requestComponents = requestLine.components(separatedBy: " ")
        guard requestComponents.count >= 2 else {
            return nil
        }
        self.uri = requestComponents[1].removingPercentEncoding ?? ""
        self.method = requestComponents[0]

        self.data = data
        self.requestLine = requestLine
        self.headers = headers
        self.comps = comps
        self.messageBody = messageBody
    }

    func messageBodyString(encoding: String.Encoding = .utf8) -> String? {
        guard let messageBody = messageBody else {
            return nil
        }

        return String(bytes: messageBody, encoding: encoding)
    }

    func valueForHeader(_ header: String) -> String? {
        return headers[header.lowercased()]
    }
}
