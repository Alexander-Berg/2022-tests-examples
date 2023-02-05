//
//  EnotPresenterTests.swift
//  BackupTests
//
//  Created by Timur Turaev on 10.08.2021.
//

import XCTest
import Utils
import TestUtils
import NetworkLayer
@testable import Backup

internal final class EnotPresenterTests: XCTestCase {
    func testStartAndRerender() {
        func check(isOn: Bool) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformeer()
            let router = MockRouter()
            let presenter = EnotPresenter(networkPerformer: networkPerformer,
                                          router: router,
                                          isOn: isOn)
            presenter.render = render
            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.isSwitchOn, isOn)
            XCTAssertTrue(render.props?.isSwitchEnabled == true)
            XCTAssertEqual(render.props?.isButtonHidden, !isOn)

            render.props = nil
            presenter.rerender()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.isSwitchOn, isOn)
            XCTAssertTrue(render.props?.isSwitchEnabled == true)
            XCTAssertEqual(render.props?.isButtonHidden, !isOn)
        }
        check(isOn: true)
        check(isOn: false)
    }

    func testSwithTurnOn() {
        func check(with result: Result<YOIDType>) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformeer()
            let router = MockRouter()
            let delegate = Delegate()
            let presenter = EnotPresenter(networkPerformer: networkPerformer,
                                          router: router,
                                          isOn: false)
            presenter.delegate = delegate
            presenter.render = render
            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertTrue(render.props?.isSwitchOn == false)
            XCTAssertTrue(render.props?.isSwitchEnabled == true)
            XCTAssertTrue(render.props?.isButtonHidden == true)

            render.props?.onSwitchChanged(true)
            XCTAssertNotNil(render.props)
            XCTAssertTrue(render.props?.isSwitchOn == true, "switch should change state")
            XCTAssertTrue(render.props?.isSwitchEnabled == false, "should not be enabled until request is complete")
            XCTAssertTrue(render.props?.isButtonHidden == true, "should be hidden until request is in progress or swith is off")

            XCTAssertNotNil(networkPerformer.completion)
            networkPerformer.finish(with: result)

            result.onValue { _ in
                XCTAssertNotNil(render.props)
                XCTAssertEqual(delegate.fid, 1)
                XCTAssertTrue(render.props?.isSwitchOn == true, "switch should stay on new state")
                XCTAssertTrue(render.props?.isSwitchEnabled == true)
                XCTAssertTrue(render.props?.isButtonHidden == false, "button should be visible, if enot is on")
            }

            result.onError { error in
                XCTAssertTrue(router.error == (error.yo_isNetworkError ? .network : .enotOn))
                XCTAssertNotNil(render.props)
                XCTAssertTrue(render.props?.isSwitchOn == false, "switch should return to old state")
                XCTAssertTrue(render.props?.isSwitchEnabled == true)
                XCTAssertTrue(render.props?.isButtonHidden == true)
                XCTAssertNil(delegate.fid)
            }
        }
        check(with: .failure(TestError.networkError))
        check(with: .failure(TestError.someError))
        check(with: .success(1))
    }

    func testSwithTurnOff() {
        func check(with result: Bool) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformeer()
            let router = MockRouter()
            let presenter = EnotPresenter(networkPerformer: networkPerformer,
                                          router: router,
                                          isOn: true)
            presenter.render = render
            presenter.start()
            XCTAssertNotNil(render.props)
            XCTAssertTrue(render.props?.isSwitchOn == true)
            XCTAssertTrue(render.props?.isSwitchEnabled == true)
            XCTAssertTrue(render.props?.isButtonHidden == false)

            render.props?.onSwitchChanged(false)
            XCTAssertNotNil(render.props)
            XCTAssertTrue(render.props?.isSwitchOn == true, "switch should not change state")
            XCTAssertTrue(render.props?.isSwitchEnabled == true, "should be enabled until request is complete")
            XCTAssertTrue(render.props?.isButtonHidden == false, "should not be hidden until request is in progress or swith is off")

            XCTAssertNotNil(router.onDelete, "should open delete enot screen")
            router.onDelete?(result)

            XCTAssertNotNil(render.props)
            if result {
                XCTAssertTrue(render.props?.isSwitchOn == false, "switch should stay on new state")
                XCTAssertTrue(render.props?.isSwitchEnabled == true)
                XCTAssertTrue(render.props?.isButtonHidden == true, "button should be visible, if enot is on")
            } else {
                XCTAssertTrue(render.props?.isSwitchOn == true, "switch should return to old state")
                XCTAssertTrue(render.props?.isSwitchEnabled == true)
                XCTAssertTrue(render.props?.isButtonHidden == false)
            }
        }
        check(with: true)
        check(with: false)
    }

    func testOpenEnotMails() {
        let render = MockRender()
        let networkPerformer = MockNetworkPerformeer()
        let router = MockRouter()
        let presenter = EnotPresenter(networkPerformer: networkPerformer,
                                      router: router,
                                      isOn: true)
        presenter.render = render
        presenter.start()
        XCTAssertNotNil(render.props)
        XCTAssertTrue(render.props?.isSwitchOn == true)
        XCTAssertTrue(render.props?.isSwitchEnabled == true)
        XCTAssertTrue(render.props?.isButtonHidden == false)

        render.props?.onButtonTap()
        XCTAssertTrue(router.isOpenEnotMails)
    }

    func testDealocating() {
        func check(isOn: Bool) {
            let render = MockRender()
            let networkPerformer = MockNetworkPerformeer()
            let router = MockRouter()
            var presenter: EnotPresenter? = EnotPresenter(networkPerformer: networkPerformer,
                                                          router: router,
                                                          isOn: isOn)
            presenter?.render = render
            presenter?.start()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.isSwitchOn, isOn)
            XCTAssertTrue(render.props?.isSwitchEnabled == true)
            XCTAssertEqual(render.props?.isButtonHidden, !isOn)

            render.props = nil
            presenter?.rerender()
            XCTAssertNotNil(render.props)
            XCTAssertEqual(render.props?.isSwitchOn, isOn)
            XCTAssertTrue(render.props?.isSwitchEnabled == true)
            XCTAssertEqual(render.props?.isButtonHidden, !isOn)

            weak var weakPresenter = presenter
            XCTAssertNotNil(weakPresenter)
            presenter = nil
            XCTAssertNil(weakPresenter)
        }
        check(isOn: true)
        check(isOn: false)
    }
}

private extension EnotPresenterTests {
    final class MockRender: EnotRendering {
        var props: EnotProps?
        func render(props: EnotProps) {
            self.props = props
        }
    }

    final class MockRouter: EnotRouting {
        var onDelete: CommandWith<Bool>?
        var isOpenEnotMails = false
        var error: Router.Error?

        func openDeleteEnot(onDelete: CommandWith<Bool>) {
            self.onDelete = onDelete
        }

        func openEnotMails() {
            self.isOpenEnotMails = true
        }

        func showError(_ error: Router.Error) {
            self.error = error
        }
    }

    final class MockNetworkPerformeer: EnotNetworkPerformer {
        var completion: ((Result<YOIDType>) -> Void)?

        func finish(with result: Result<YOIDType>) {
            self.completion?(result)
        }

        func createHiddenTrash(_ completion: @escaping (Result<YOIDType>) -> Void) {
            self.completion = completion
        }
    }

    final class Delegate: EnotPresenterDelegate {
        var fid: YOIDType?
        func enotPresenter(_ enotPresenter: EnotPresenter, didCreateHiddenTrashWith fid: YOIDType) {
            self.fid = fid
        }
    }
}
