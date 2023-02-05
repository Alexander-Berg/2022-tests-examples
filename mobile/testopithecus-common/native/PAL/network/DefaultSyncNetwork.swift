//
//  DefaultNetworkFetch.swift
//  XMail
//
//  Created by Dmitry Zakharov.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation

public final class DefaultSyncNetwork: NSObject, SyncNetwork {
  private let logger: Logger
  private let errorThrower: ErrorThrower
  private let urlSession: URLSession

  private lazy var decoder: JSONDecoder = JSONDecoder()

  public override init() {
    logger = Log.getDefaultLogger()
    errorThrower = Registry.get().errorThrower
    urlSession = URLSession(configuration: .default, delegate: DefaultURLSessionDelegate(), delegateQueue: nil)
  }

  public func syncExecute(_ baseUrl: String, _ request: NetworkRequest, _ oauthToken: String!) -> Result<String> {
    guard let httpRequest = buildRequest(baseUrl: baseUrl, request: request, token: oauthToken) else {
      errorThrower.fail("Can't build request!")
      fatalError("Can't build request!")
    }
    logger.info("Performing: \(httpRequest.url?.absoluteString ?? "")")
    let semaphore = DispatchSemaphore(value: 0)
    var result: String?
    var error: String = ""
    var responseStatusCode: Int = 200
    let task = urlSession.dataTask(with: httpRequest) { data, response, errorData in
      if let data = data {
        result = String(data: data, encoding: .utf8)
      }
      if let response = response as? HTTPURLResponse {
        responseStatusCode = response.statusCode
      }
      if let errorData = errorData {
        error = errorData.localizedDescription
      }
      semaphore.signal()
    }
    task.resume()
    semaphore.wait()
    if result == nil {
      return resultError(YSError("Http request failure! " + error))
    }
    if request.targetPath().startsWith("v2"),
       responseStatusCode >= 400 {
      return resultError(YSError("Http response status code: \(responseStatusCode)"))
    }
    return resultValue(result!)
  }

  public func syncExecuteWithRetries(_ retries: Int32, _ baseUrl: String, _ request: NetworkRequest, _ oauthToken: String!) -> Result<String> {
    var retriesLeft: Int32 = retries
    var result: Result<String>?
    while retriesLeft >= 0 {
      result = syncExecute(baseUrl, request, oauthToken)
      if result!.isValue() {
        return result!
      }
      retriesLeft -= 1
    }
    return result!
  }

  private func buildRequest(baseUrl: String, request: NetworkRequest, token: String?) -> URLRequest? {
    guard let url = url(for: request, baseUrl: baseUrl) else {
      return nil
    }
    var httpRequest = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: TimeInterval(60))
    if let token = token {
      httpRequest.setValue("OAuth \(token)", forHTTPHeaderField: "Authorization")
    }
    request.headersExtra().asMap().items.forEach { key, value in
      if let param = stringifyQueryParam(value) {
        httpRequest.setValue(param, forHTTPHeaderField: key)
      }
    }
    httpRequest.httpMethod = request.method().toString()
    return encodeRequest(httpRequest, with: request.encoding(), params: request.params())
  }

  private func stringifyQueryParam(_ value: JSONItem) -> String? {
    switch value.kind {
    case .double:
      return doubleToString((value as! DoubleJSONItem).value)
    case .integer:
      return int64ToString((value as! IntegerJSONItem).asInt64())
    case .boolean:
      return (value as! BooleanJSONItem).value ? "yes" : "no"
    case .string:
      return (value as! StringJSONItem).value
    case .nullItem:
      return "null"
    case .map:
      fatalError("Maps are not supported as URL query request parameters: \(String(describing: value))")
    case .array:
      fatalError("Arrays are not supported as URL query request parameters: \(String(describing: value))")
    }
    return nil
  }

  private func url(for request: NetworkRequest, baseUrl: String) -> URL? {
    let url = fullPath(for: request, baseUrl: baseUrl)
    guard var urlComponents = URLComponents(url: url, resolvingAgainstBaseURL: true) else {
      NSLog("Unable to build URL for \(request.method()) request; P: \(request.targetPath())")
      return nil
    }
    let items = queryItems(fromUrlExtra: request.urlExtra())
    urlComponents.queryItems = [URLQueryItem(name: "client", value: "iphone")]
    if !items.isEmpty {
      urlComponents.queryItems?.append(contentsOf: items)
    }

    return urlComponents.url
  }

  private func fullPath(for request: NetworkRequest, baseUrl: String) -> URL {
    let baseURL = URL(string: baseUrl)!
    return baseURL.appendingPathComponent(request.targetPath())
  }
}
