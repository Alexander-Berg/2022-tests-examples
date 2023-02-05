import XCTest
@testable import XPlat

private let token = ProcessInfo.processInfo.environment["TOKEN"]
private let baseURLString = ProcessInfo.processInfo.environment["BASEURL"]

class Response {
  let people, other: YSArray<Other>

  init(people: YSArray<Other>, other: YSArray<Other>) {
    self.people = people
    self.other = other
  }
}

class Other {
  let target: Target
  let showText, searchText, displayName, email: String
  let unreadCnt: Int
  let searchParams: SearchParams
  let id: String
  let highlights: Highlights

  enum CodingKeys: String, CodingKey {
    case target
    case showText = "show_text"
    case searchText = "search_text"
    case displayName = "display_name"
    case email
    case unreadCnt = "unread_cnt"
    case searchParams = "search_params"
    case id, highlights
  }

  init(target: Target, showText: String, searchText: String, displayName: String, email: String, unreadCnt: Int, searchParams: SearchParams, id: String, highlights: Highlights) {
    self.target = target
    self.showText = showText
    self.searchText = searchText
    self.displayName = displayName
    self.email = email
    self.unreadCnt = unreadCnt
    self.searchParams = searchParams
    self.id = id
    self.highlights = highlights
  }
}

class Highlights {
  let email, displayName, showText: YSArray<String>

  enum CodingKeys: String, CodingKey {
    case email
    case displayName = "display_name"
    case showText = "show_text"
  }

  init(email: YSArray<String>, displayName: YSArray<String>, showText: YSArray<String>) {
    self.email = email
    self.displayName = displayName
    self.showText = showText
  }
}

// MARK: - SearchParams

class SearchParams {
  init() {}
}

enum Target: String {
  case contact
}

private class DefaultTokenProvider: TokenProvider {
  func obtain(_: Account) -> XPromise<Token> {
    return resolve(Token(token!))
  }
}

internal class DefaultLogger: Logger {
  static let instance = DefaultLogger()
  
  func info(_ message: String) -> Void {
    NSLog(message)
  }

  func warn(_ message: String) -> Void {
    NSLog(message)
  }

  func error(_ message: String) -> Void {
    NSLog(message)
  }
}

class SampleRequest: MailNetworkRequest {
  override func version() -> NetworkAPIVersions { return .v2 }
  override func method() -> NetworkMethod { return .get }
  override func path() -> String { return "search_contacts" }
  override func urlExtra() -> NetworkUrlExtra { return MapJSONItem() }
  override func encoding() -> RequestEncoding { return UrlRequestEncoding() }
  override func params() -> NetworkParams { return MapJSONItem().putString("client", "ios") }
  override func headersExtra() -> NetworkHeadersExtra { return NetworkHeadersExtra() }
}

class NetworkTests: XCTestCase {
  override func setUp() {
    XPromisesFramework.setup()
    guard token != nil else {
      fatalError("Please specify <TOKEN> value as environment variable 'TOKEN'")
    }
    guard baseURLString != nil else {
      fatalError("Please specify <BASE URL> value as environment variable 'BASEURL'")
    }
    Registry.registerJSONSerializer(DefaultJSONSerializer())
    let logger = DefaultLogger()
    Registry.registerNetwork(DefaultNetwork(baseURL: URL(string: baseURLString!)!, sslPinning: nil))
    Log.registerDefaultLogger(logger)
  }

  func testNetworkResponseWithTypedObject() {
    let ex = expectation(description: "Network expectation")
    Registry.getNetwork().execute(SampleRequest())
      .then { (result: JSONItem) in
        print("Result \(JSONItemGetDebugDescription(result))")
        ex.fulfill()
      }.failed { error in
        print("Error \(error)")
        ex.fulfill()
      }

    waitForExpectations(timeout: 10000)
  }

  func testNetworkResponseWithJSONItem() {
    let ex = expectation(description: "Network expectation")
    Registry.getNetwork().execute(SampleRequest())
      .then { (result: JSONItem?) in
        if let result = result, result.kind == JSONItemKind.map {
          let map = result as! MapJSONItem
          print("Result \(map.getArray("people").length)")
        }
        ex.fulfill()
      }.failed { error in
        print("Error \(error)")
        ex.fulfill()
      }

    waitForExpectations(timeout: 10000)
  }
}
