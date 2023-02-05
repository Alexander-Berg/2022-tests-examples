//
//  DefaultUriTests.swift
//  XMailTests
//
//  Created by Aleksandr A. Dvornikov on 2112//19.
//

import XCTest
@testable import XPlat

class DefaultUriTests: XCTestCase {
  func testShouldBuildFileUri() {
    let uri = Uris.fromFilePath("/path/to/file")
    XCTAssertEqual(uri.getAbsoluteString(), "file:///path/to/file")
    XCTAssertEqual(uri.getScheme(), "file")
    XCTAssertNil(uri.getHost())
    XCTAssertEqual(uri.getPath(), "/path/to/file")
    XCTAssertEqual(uri.getPathSegments(), ["path", "to", "file"])
    XCTAssertNil(uri.getQuery())
    XCTAssertEqual(uri.getAllQueryParameters().items, [])
    XCTAssertNil(uri.getFragment())
    XCTAssertTrue(uri.isFileUri())
  }

  func testShouldBuildWebUri() {
    let uri = Uris.fromString("https://ya.ru/path/to/resource?param=value&foo=bar#fragment")!
    XCTAssertEqual(uri.getAbsoluteString(), "https://ya.ru/path/to/resource?param=value&foo=bar#fragment")
    XCTAssertEqual(uri.getScheme(), "https")
    XCTAssertEqual(uri.getHost(), "ya.ru")
    XCTAssertEqual(uri.getPath(), "/path/to/resource")
    XCTAssertEqual(uri.getPathSegments(), ["path", "to", "resource"])
    XCTAssertEqual(uri.getQuery(), "param=value&foo=bar")
    XCTAssertEqual(uri.getAllQueryParameters().items, [UriQueryParameter("param", "value"), UriQueryParameter("foo", "bar")])
    XCTAssertEqual(uri.getFragment(), "fragment")
    XCTAssertFalse(uri.isFileUri())
  }

  func testShouldBuildFileUriFromString() {
    let uri = Uris.fromString("file:///path/to/file")!
    XCTAssertEqual(uri.getPath(), "/path/to/file")
    XCTAssertEqual(uri.getPathSegments(), ["path", "to", "file"])
    XCTAssertTrue(uri.isFileUri())
  }

  func testShouldFailToBuildWebUriIfTheValueIsInvalid() {
    let uri = Uris.fromString("invalid value")
    XCTAssertNil(uri)
  }

  func testShouldChangeUri() {
        let builder = Uris.fromString("https://ya.ru/path/to/resource?param=value&foo=bar#fragment")!.builder()
        XCTAssertEqual(builder.setScheme("http").build().getAbsoluteString(), "http://ya.ru/path/to/resource?param=value&foo=bar#fragment")
        XCTAssertEqual(builder.setHost("yandex.ru").build().getAbsoluteString(), "http://yandex.ru/path/to/resource?param=value&foo=bar#fragment")
        XCTAssertEqual(builder.setPath("path/to/new-resource").build().getAbsoluteString(), "http://yandex.ru/path/to/new-resource?param=value&foo=bar#fragment")
        XCTAssertEqual(builder.setPath("/path/to/yet-another-resource").build().getAbsoluteString(), "http://yandex.ru/path/to/yet-another-resource?param=value&foo=bar#fragment")
        XCTAssertEqual(builder.setAllQueryParameters(YSArray([UriQueryParameter("new_param", "value"), UriQueryParameter("bar", "bazz")])).build().getAbsoluteString(), "http://yandex.ru/path/to/yet-another-resource?new_param=value&bar=bazz#fragment")
        XCTAssertEqual(builder.setAllQueryParameters(YSArray([])).build().getAbsoluteString(), "http://yandex.ru/path/to/yet-another-resource#fragment")
        XCTAssertEqual(builder.setFragment("new-fragment").build().getAbsoluteString(), "http://yandex.ru/path/to/yet-another-resource#new-fragment")
        XCTAssertEqual(builder.setFragment("").build().getAbsoluteString(), "http://yandex.ru/path/to/yet-another-resource")
        XCTAssertEqual(builder.setPath("").build().getAbsoluteString(), "http://yandex.ru/")
    }

  func testUriQueryParameters() {
    var uri = Uris.fromString("https://ya.ru")!
    XCTAssertEqual(uri.getAllQueryParameters().items, [])
    XCTAssertNil(uri.getQueryParameter("foo"))
    XCTAssertEqual(uri.getQueryParameters("foo").items, [])
    XCTAssertEqual(uri.getQueryParameterNames().items, [])

    uri = uri.builder()
      .appendQueryParameter("foo", "bar")
      .appendQueryParameter("param", "val1")
      .appendQueryParameter("param", "val2")
      .build()
    XCTAssertEqual(uri.getAbsoluteString(), "https://ya.ru?foo=bar&param=val1&param=val2")
    XCTAssertEqual(uri.getAllQueryParameters().items, [UriQueryParameter("foo", "bar"), UriQueryParameter("param", "val1"), UriQueryParameter("param", "val2")])
    XCTAssertEqual(uri.getQueryParameter("foo"), "bar")
    XCTAssertEqual(uri.getQueryParameters("foo").items, ["bar"])
    XCTAssertEqual(uri.getQueryParameter("param"), "val1")
    XCTAssertEqual(uri.getQueryParameters("param").items, ["val1", "val2"])
    XCTAssertEqual(uri.getQueryParameterNames().items, ["foo", "param"])

    uri = uri.builder().clearQuery().build()
    XCTAssertEqual(uri.getAbsoluteString(), "https://ya.ru")
    XCTAssertEqual(uri.getQueryParameterNames().items, [])

    uri = uri.builder().setAllQueryParameters(YSArray([UriQueryParameter("bar", "bazz")])).build()
    XCTAssertEqual(uri.getAbsoluteString(), "https://ya.ru?bar=bazz")
    XCTAssertEqual(uri.getQueryParameterNames().items, ["bar"])

    uri = Uris.fromString("https://ya.ru?foo")!
    XCTAssertEqual(uri.getAllQueryParameters().items, [UriQueryParameter("foo", "")])
    XCTAssertEqual(uri.getQueryParameter("foo"), "")
    XCTAssertEqual(uri.getQueryParameters("foo").items, [""])
    XCTAssertEqual(uri.getQueryParameterNames().items, ["foo"])
  }

  func testShouldEncode() {
    XCTAssertEqual(percentEncode("ABC", false), "ABC")
    XCTAssertEqual(percentEncode("АБВ", false), "%D0%90%D0%91%D0%92")

    XCTAssertEqual(percentEncode("ABC+abc 123", false), "ABC%2Babc%20123")
    XCTAssertEqual(percentEncode("ABC+abc 123", true), "ABC%2Babc+123")
  }

  func testShouldDecode() {
    XCTAssertEqual(percentDecode("ABC", false), "ABC")
    XCTAssertEqual(percentDecode("%D0%90%D0%91%D0%92", false), "АБВ")

    XCTAssertEqual(percentDecode("ABC%2Babc%20123+456", false), "ABC+abc 123+456")
    XCTAssertEqual(percentDecode("ABC%2Babc%20123+456", true), "ABC+abc 123 456")
  }

}
