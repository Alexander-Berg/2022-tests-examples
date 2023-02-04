import Foundation
import Basics
import TSCBasic
import Workspace

@main
struct Searcher {
    static func main() async throws {
        try await Searcher(
            projectRoot: URL(
                fileURLWithPath: "../../../",
                relativeTo: URL(fileURLWithPath: #filePath)
            ).absoluteURL
        )
        .run()
    }

    let projectRoot: URL

    static var rootPackages: [String] {
        [
            "packages"
        ]
    }

    func run() async throws {
        var packagesToCheck = Self.rootPackages.map { projectRoot.appendingPathComponent($0, isDirectory: true).path }
        var checkedPackages = Set(packagesToCheck)

        let observability = ObservabilitySystem({ _, _ in })
        var packageTestTargets: [PackageTestTargets] = []

        while let path = packagesToCheck.popLast() {
            let packagePath = AbsolutePath(path)
            let workspace = try Workspace(forRootPackage: packagePath)

            let package = try await workspace.loadRootPackage(at: packagePath, observabilityScope: observability.topScope)

            let testTargets = package.targets.filter { $0.type == .test }.map { target in
                PackageTestTargets.TestTarget(
                    name: target.name,
                    sources: target.sources.paths.map(\.pathString)
                )
            }

            if !testTargets.isEmpty {
                packageTestTargets.append(
                    PackageTestTargets(packagePath: path, testTargets: testTargets)
                )
            }

            for dependency in package.manifest.dependencies {
                guard case let .fileSystem(fsDependency) = dependency,
                      checkedPackages.insert(fsDependency.path.pathString).inserted
                else {
                    continue
                }

                packagesToCheck.append(fsDependency.path.pathString)
            }
        }

        let json = try JSONEncoder().encode(packageTestTargets)
        print(String(data: json, encoding: .utf8) ?? "")
    }
}

private struct PackageTestTargets: Codable {
    struct TestTarget: Codable {
        var name: String
        var sources: [String]
    }

    var packagePath: String
    var testTargets: [TestTarget]
}
