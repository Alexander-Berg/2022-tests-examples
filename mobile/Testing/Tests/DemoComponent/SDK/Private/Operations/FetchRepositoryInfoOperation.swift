//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

enum FetchRepositoryInfoOperationErrors: Error {
    case failed
}

final class FetchRepositoryInfoOperation: SkResultOperation<Repository, Error> {
    private let repo: RepositoryDescription

    init(repo: RepositoryDescription, configuration: SkOperationConfiguration) {
        self.repo = repo
        super.init(configuration: configuration)
    }

    override func main() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 10.0) { [weak self] in
            guard let self = self, !self.isFinished, !self.isCancelled else {
                return
            }

            self.finish(with: .success(makeDummyRepository()))
        }
    }
}


private func makeDummyRepository() -> Repository {
    Repository(
        id: 1,
        name: "dummy",
        fullName: "example/dummy",
        isPrivate: false,
        url: URL(string: "http://github.com/example/dummy")!, // yes i known it is bad
        description: "Just a simple dummy repository"
    )
}
