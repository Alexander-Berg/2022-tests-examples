// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM utils/stat-network-request.ts >>>

import Foundation

open class StatNetworkRequest: BaseNetworkRequest {
  private let results: ArrayJSONItem
  public init(_ results: ArrayJSONItem) {
    self.results = results
    super.init()
  }

  @discardableResult
  open override func encoding() -> RequestEncoding {
    return JsonRequestEncoding()
  }

  @discardableResult
  open override func method() -> NetworkMethod {
    return NetworkMethod.post
  }

  @discardableResult
  open override func targetPath() -> String {
    return "_api/report/data/Mail/Others/MobAutoTestsStat"
  }

  @discardableResult
  open override func params() -> NetworkParams {
    return MapJSONItem().put("data", self.results).putString("scale", "s")
  }

}

