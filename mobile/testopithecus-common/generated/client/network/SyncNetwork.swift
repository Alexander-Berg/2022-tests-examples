// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/network/sync-network.ts >>>

import Foundation

public protocol SyncNetwork {
  @discardableResult
  func syncExecute(_ baseUrl: String, _ request: NetworkRequest, _ oauthToken: String!) -> Result<String>
  @discardableResult
  func syncExecuteWithRetries(_ retries: Int32, _ baseUrl: String, _ request: NetworkRequest, _ oauthToken: String!) -> Result<String>
}

