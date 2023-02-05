//
//  RequestEncodingTests.swift
//  XMailTests
//
//  Created by Aleksandr A. Dvornikov on 126//19.
//

import XCTest
@testable import XPlat

class RequestEncodingTests: XCTestCase {
  let sampleUrl = URL(string: "https://mail.yandex.ru/path?parameter=value")!
  let sampleParams = MapJSONItem()
    .putString("foo", "bar")
    .putString("фуу", "бар")
    .putString("foo?=&", "bar?=&")
    .putBoolean("boolean", true)
    .putNull("nullable")

  func buildSampleRequest(_ method: String) -> URLRequest {
    var request = URLRequest(url: sampleUrl)
    request.httpMethod = method
    return request
  }

  func testShouldUrlEncodeRequests() {
    let request = buildSampleRequest("GET")

    let result = encodeRequest(request, with: UrlRequestEncoding(), params: sampleParams)!

    XCTAssertNil(result.httpBody)
    XCTAssertNil(result.value(forHTTPHeaderField: "Content-Type"))
    XCTAssertEqual(result.url!.absoluteString, "https://mail.yandex.ru/path?parameter=value&boolean=yes&foo=bar&foo?%3D%26=bar?%3D%26&nullable=null&%D1%84%D1%83%D1%83=%D0%B1%D0%B0%D1%80")
  }

  func testShouldFormEncodeRequests() {
    let request = buildSampleRequest("PUT")

    let result = encodeRequest(request, with: UrlRequestEncoding(), params: sampleParams)!

    XCTAssertEqual(String(decoding: result.httpBody!, as: UTF8.self), "boolean=yes&foo=bar&foo?%3D%26=bar?%3D%26&nullable=null&%D1%84%D1%83%D1%83=%D0%B1%D0%B0%D1%80")
    XCTAssertEqual(result.value(forHTTPHeaderField: "Content-Type"), "application/x-www-form-urlencoded")
    XCTAssertEqual(result.url!.absoluteString, "https://mail.yandex.ru/path?parameter=value")
  }

  func testShouldJsonEncodeRequests() {
    let request = buildSampleRequest("GET")

    let result = encodeRequest(request, with: JsonRequestEncoding(), params: sampleParams)!

    let jsonObject = try! JSONSerialization.jsonObject(with: result.httpBody!) as! [String: Any]
    XCTAssertTrue(NSDictionary(dictionary: jsonObject).isEqual(to: sampleParams.toAny()! as! [AnyHashable: Any]))
    XCTAssertEqual(result.value(forHTTPHeaderField: "Content-Type"), "application/json")
    XCTAssertEqual(result.url!.absoluteString, "https://mail.yandex.ru/path?parameter=value")
  }
}
