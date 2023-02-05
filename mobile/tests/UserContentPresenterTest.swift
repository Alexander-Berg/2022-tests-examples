//
//  Created by Timur Turaev on 19.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import Tabbar

internal final class UserContentPresenterTest: XCTestCase {
    func testUserContentPresenterOpeningService() {
        let render = UserContentRender()
        let viewController = UIViewController()

        var presenter: UserContentPresenter?
        presenter = UserContentPresenter(service: .mail, contentViewController: viewController, render: render)
        presenter?.start()
        XCTAssertEqual(render.props, UserContentProps(viewController: viewController, mode: .master(hideNavigationBar: false)))

        presenter = UserContentPresenter(service: .calendar, contentViewController: viewController, render: render)
        presenter?.start()
        XCTAssertEqual(render.props, UserContentProps(viewController: viewController, mode: .master(hideNavigationBar: true)))

        presenter = UserContentPresenter(service: .telemost(serviceLocation: .tabbar), contentViewController: viewController, render: render)
        presenter?.start()
        XCTAssertEqual(render.props, UserContentProps(viewController: viewController, mode: .master(hideNavigationBar: true)))

        presenter = UserContentPresenter(service: .mail360(forceBanner: nil), contentViewController: viewController, render: render)
        presenter?.start()
        XCTAssertEqual(render.props, UserContentProps(viewController: viewController, mode: .modal))

        weak var weakPresenter = presenter
        XCTAssertNotNil(weakPresenter)
        presenter = nil
        XCTAssertNil(weakPresenter)
    }
}

private final class UserContentRender: UserContentRendering {
    var props: UserContentProps?
    func render(_ props: UserContentProps) {
        self.props = props
    }
}
