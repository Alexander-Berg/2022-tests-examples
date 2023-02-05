//
//  PresenterTest.swift
//  CaptchaTests
//
//  Created by Aleksey Makhutin on 01.03.2021.
//

import Foundation
import XCTest
import TestUtils
import Utils
@testable import Captcha

internal final class PresenterTest: XCTestCase {
    private lazy var image: UIImage? = {
        let url = Bundle(for: Self.self).url(forResource: "tempFile", withExtension: "jpg")!
        let image = try? UIImage(data: Data(contentsOf: url))
        return image
    }()

    func testStart() {
        func start(withError: Bool) {
            let dataSource = MockDataSource()
            let logger = TestLogger()
            let router = MockRouter()
            let render = Render()

            dataSource.result = withError ? .failure(TestError.testError) : .success(self.image!)

            let presenter = Presenter(dataSource: dataSource,
                                      configuration: MockConfiguration(),
                                      logger: logger,
                                      router: router,
                                      render: render)

            dataSource.willLoad = {
                XCTAssertEqual(render.props?.state, .loading)
            }

            presenter.start()

            XCTAssertFalse(dataSource.new!)

            if withError {
                XCTAssertEqual(render.props?.state, .error)
            } else {
                guard case let .normal(props) = render.props?.state else {
                    XCTFail("Should render props with viewprops")
                    return
                }

                XCTAssertEqual(props.image, self.image)
            }
        }
        start(withError: true)
        start(withError: false)
    }

    func testUpdateCaptcha() {
        func start(withError: Bool) {
            let dataSource = MockDataSource()
            let logger = TestLogger()
            let router = MockRouter()
            let render = Render()

            dataSource.result = .success(self.image!)

            let presenter = Presenter(dataSource: dataSource,
                                      configuration: MockConfiguration(),
                                      logger: logger,
                                      router: router,
                                      render: render)

            dataSource.willLoad = {
                XCTAssertEqual(render.props?.state, .loading)
            }

            presenter.start()

            XCTAssertFalse(dataSource.new!)

            guard case let .normal(props) = render.props?.state else {
                XCTFail("Should render props with viewprops")
                return
            }

            XCTAssertEqual(props.image, self.image)

            dataSource.new = nil
            let newImage = self.image?.withHorizontallyFlippedOrientation()
            XCTAssertNotEqual(newImage, self.image)

            dataSource.result = withError ? .failure(TestError.testError) : .success(newImage!)

            let expectation = self.expectation(description: "wait for throttle")
            render.didRender = {
                guard render.props?.state != .loading else { return }

                expectation.fulfill()
            }

            props.didTapGenerate()

            waitForExpectations(timeout: 1, handler: nil)

            XCTAssertTrue(dataSource.new!)

            if withError {
                XCTAssertEqual(render.props?.state, .error)
            } else {
                guard case let .normal(props) = render.props?.state else {
                    XCTFail("Should render props with viewprops")
                    return
                }

                XCTAssertEqual(props.image, newImage)
            }
        }
        start(withError: true)
        start(withError: false)
    }

    func testTapHyperlink() {
        let dataSource = MockDataSource()
        let logger = TestLogger()
        let router = MockRouter()
        let render = Render()

        dataSource.result = .success(self.image!)

        let presenter = Presenter(dataSource: dataSource,
                                  configuration: MockConfiguration(),
                                  logger: logger,
                                  router: router,
                                  render: render)

        dataSource.willLoad = {
            XCTAssertEqual(render.props?.state, .loading)
        }

        presenter.start()

        XCTAssertFalse(dataSource.new!)

        guard case let .normal(props) = render.props?.state else {
            XCTFail("Should render props with viewprops")
            return
        }

        XCTAssertEqual(props.image, self.image)
        XCTAssertNil(router.url)

        props.didTapHyperlink()

        XCTAssertNotNil(router.url)
    }

    func testDidTapSend() {
        let dataSource = MockDataSource()
        let logger = TestLogger()
        let router = MockRouter()
        let render = Render()
        let delegate = Delegate()

        dataSource.result = .success(self.image!)

        let presenter = Presenter(dataSource: dataSource,
                                  configuration: MockConfiguration(),
                                  logger: logger,
                                  router: router,
                                  render: render)
        presenter.delegate = delegate

        dataSource.willLoad = {
            XCTAssertEqual(render.props?.state, .loading)
        }

        presenter.start()

        XCTAssertFalse(dataSource.new!)

        guard case let .normal(props) = render.props?.state else {
            XCTFail("Should render props with viewprops")
            return
        }

        XCTAssertEqual(props.image, self.image)
        XCTAssertNil(dataSource.entry)
        XCTAssertFalse(delegate.shouldSendCaptcha)

        let entry = "Test entry"
        props.didTapSend(entry)

        XCTAssertEqual(dataSource.entry, entry)
        XCTAssertTrue(delegate.shouldSendCaptcha)
    }
}

extension PresenterTest {
    enum TestError: Swift.Error {
        case testError
    }

    private final class MockDataSource: DataSource {
        var result: Result<UIImage>?
        var entry: String?
        var new: Bool?

        var willLoad: (() -> Void)?

        func load(new: Bool, completion: @escaping (Result<UIImage>) -> Void) {
            self.willLoad?()
            self.new = new
            self.result.map { completion($0) }
        }

        func update(entry: String) {
            self.entry = entry
        }
    }

    private final class Render: PropsRendering {
        var props: Props?
        var didRender: (() -> Void)?

        func render(props: Props) {
            self.props = props
            self.didRender?()
        }
    }

    private final class MockRouter: Routing {
        var url: URL?

        func openURL(url: URL) {
            self.url = url
        }
    }

    private final class Delegate: PresenterDelegate {
        var shouldSendCaptcha = false

        func presenterShouldSendCaptcha(_ presenter: Presenter) {
            self.shouldSendCaptcha = true
        }
    }

    private final class MockConfiguration: CaptchaConfiguration {
        var language: LanguageKind {
            return .en
        }
    }
}

extension Props.State: Equatable {
    public static func == (lhs: Props.State, rhs: Props.State) -> Bool {
        switch (lhs, rhs) {
        case (.normal(let lhsProps), .normal(let rhsProps)):
            return lhsProps == rhsProps
        case (.error, .error):
            return true
        case (.loading, .loading):
            return true
        default:
            return false
        }
    }
}

extension Props.ViewProps: Equatable {
    public static func == (lhs: Props.ViewProps, rhs: Props.ViewProps) -> Bool {
        return lhs.image == rhs.image
    }
}
