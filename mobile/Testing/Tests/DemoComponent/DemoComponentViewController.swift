//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation
import UIKit

final class DemoComponentViewController: UIViewController, DemoComponentViewDelegate {

    typealias BlockOperationFactory = (_ name: String, _ opcode: OpCode, _ block: @escaping () -> Void) -> SkBlockOperation

    private let demoSDK: DemoSDK
    private let blockOperationFactory: BlockOperationFactory
    private let disposables = SkDisposeBag()

    private var repoSession: RepositorySession?

    private var demoView: DemoComponentView {
        view as! DemoComponentView // intentionally
    }

    init(
        demoSDK: DemoSDK,
        blockOperationFactory: @escaping BlockOperationFactory
    ) {
        self.demoSDK = demoSDK
        self.blockOperationFactory = blockOperationFactory
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func loadView() {
        // super.loadView() NOT!!!!

        view = DemoComponentView()
        view.translatesAutoresizingMaskIntoConstraints = true

        demoView.delegate = self
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        let session = demoSDK.makeRepositorySession(
            repo: RepositoryDescription(
                owner: "mrdekk",
                name: "DataKernel"
            )
        )
        repoSession = session
        let token = session.info.observe { [weak self] update in
            guard let self = self else {
                return
            }
            self.demoView.modify { vm in
                .init(
                    infoText: vm.infoText,
                    observedText: "observed \(update.new)"
                )
            }
        }
        disposables.append(token)
    }

    func didTapAcquireInfo() {
        guard let session = repoSession else {
            return
        }

        let setNewInfo: (String) -> Void = { [weak self] info in
            self?.demoView.modify { vm in
                .init(
                    infoText: info,
                    observedText: vm.observedText
                )
            }
        }

        let op = session.acquireInfo()
        let uiOp = blockOperationFactory("repo update", .main) { [op] in
            guard let result = op.result else {
                setNewInfo("no result")
                return
            }

            switch result {
            case let .success(repo):
                setNewInfo("\(repo.id): \(repo.name); \(repo.description)")
            case let .failure(error):
                setNewInfo("error: \(error)")
            }
        }
        uiOp.addDependency(op)
        [op, uiOp].dispatch()
    }
}
