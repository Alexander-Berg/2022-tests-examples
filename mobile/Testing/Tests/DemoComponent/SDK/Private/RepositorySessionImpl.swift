//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

protocol RepositorySessionOperationFactory: AnyObject {
    func makeFetchRepositoryInfoOperation(repo: RepositoryDescription) -> FetchRepositoryInfoOperation
    func makeResultBlockOperation<Success>(
        name: String,
        opcode: OpCode,
        block: @escaping () -> Result<Success, Error>
    ) -> SkResultBlockOperation<Success,Error>

}

enum RepositorySessionError: Error {
    case fail
}

final class RepositorySessionImpl: RepositorySession {


    let description: RepositoryDescription

    private unowned let operationsFactory: RepositorySessionOperationFactory

    private let _info = SkMutableValue<Repository?>(nil)

    var info: SkObservableValue<Repository?> {
        _info.observable
    }

    init(
        operationsFactory: RepositorySessionOperationFactory,
        description: RepositoryDescription
    ) {
        self.operationsFactory = operationsFactory
        self.description = description
    }

    func acquireInfo() -> SkResultOperation<Repository, Error> {
        let op = operationsFactory.makeFetchRepositoryInfoOperation(repo: description)
        let adapter = operationsFactory.makeResultBlockOperation(name: "1", opcode: .main) { [weak self, op] () -> Result<Repository,Error> in
            guard let self = self else {
                return .failure(RepositorySessionError.fail)
            }

            switch op.result {
            case let .success(repository):
                self._info.value = repository
                return .success(repository)

            case let .failure(error):
                return .failure(error)

            case .none:
                return .failure(RepositorySessionError.fail)
            }
        }
        adapter.addDependency(op)
        op.dispatch()
        return adapter
    }
}
