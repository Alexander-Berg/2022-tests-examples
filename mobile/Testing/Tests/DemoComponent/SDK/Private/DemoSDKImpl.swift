//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

final class DemoSDKImpl: DemoSDK {
    private let logger: YxLogger
    private let dispatcher: SkDispatcher

    init(logger: YxLogger, dispatcher: SkDispatcher) {
        self.logger = logger
        self.dispatcher = dispatcher
    }

    func makeRepositorySession(repo: RepositoryDescription) -> RepositorySession {
        RepositorySessionImpl(
            operationsFactory: self,
            description: repo
        )
    }

    private func makeOperationConfiguration() -> SkOperationConfiguration {
        SkOperationConfiguration(
            logger: logger,
            dispatcher: dispatcher,
            writeOperationsLog: true
        )
    }
}

extension DemoSDKImpl: RepositorySessionOperationFactory {
    func makeFetchRepositoryInfoOperation(repo: RepositoryDescription) -> FetchRepositoryInfoOperation {
        FetchRepositoryInfoOperation(repo: repo, configuration: makeOperationConfiguration())
    }

    func makeResultBlockOperation<Success>(
        name: String,
        opcode: OpCode,
        block: @escaping () -> Result<Success, Error>
    ) -> SkResultBlockOperation<Success, Error> {
        SkResultBlockOperation(
            name: name,
            opcode: opcode,
            configuration: makeOperationConfiguration(),
            block: block
        )
    }


}
