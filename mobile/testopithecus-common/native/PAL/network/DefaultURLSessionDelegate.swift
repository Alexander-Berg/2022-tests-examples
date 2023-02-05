//
//  File.swift
//  testopithecus
//
//  Created by Andrey Azarov on 5/15/20.
//

import Foundation
class DefaultURLSessionDelegate: NSObject, URLSessionDelegate {
  public func urlSession(_: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
    #if DEBUG
      return completionHandler(.useCredential, challenge.protectionSpace.serverTrust.map(URLCredential.init(trust:)))
    #else
      return completionHandler(.performDefaultHandling, nil)
    #endif
  }
}
