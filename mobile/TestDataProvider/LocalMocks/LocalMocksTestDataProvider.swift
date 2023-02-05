import Foundation

class LocalMocksTestDataProvider: TestDataProvider {

    // MARK: - Public

    func getTestConfigs() throws -> [FlexEngineTestConfig] {
        let testDataUrl = try getTestDataUrl()
        let testBundles = try FileManager.default.contentsOfDirectory(at: testDataUrl, includingPropertiesForKeys: nil)
        return testBundles
            .filter { $0.pathExtension == Constants.bundleExtension }
            .map(\.lastPathComponent)
            .compactMap { testBundle in
                let testBundleNameComponents = testBundle.components(separatedBy: ".")
                guard
                    testBundleNameComponents.count == 3,
                    let testCase = testBundleNameComponents.first
                else { return nil }
                return FlexEngineTestConfig(
                    testCase: testCase,
                    testMethod: testBundleNameComponents[1]
                )
            }
    }

    func getData(for testConfig: FlexEngineTestConfig) throws -> Data {
        let mockUrl = try getBundleUrl(for: testConfig).appendingPathComponent(Constants.mockName)
        return try Data(contentsOf: mockUrl)
    }

    func getBundleUrl(for testConfig: FlexEngineTestConfig) throws -> URL {
        try getTestDataUrl()
            .appendingPathComponent(testConfig.testCase)
            .appendingPathExtension(testConfig.testMethod)
            .appendingPathExtension(Constants.bundleExtension)
    }

    func getSourceUrl(for testConfig: FlexEngineTestConfig) -> URL {
        getSourceDataUrl()
            .appendingPathComponent(testConfig.testCase)
            .appendingPathExtension(testConfig.testMethod)
            .appendingPathExtension(Constants.bundleExtension)
    }

    private func getTestDataUrl() throws -> URL {
        guard let mainBundleResourceUrl = Bundle.main.resourceURL else {
            throw LocalMocksTestDataProviderError.mainBundleResourceIsMissing
        }
        return mainBundleResourceUrl.appendingPathComponent(Constants.resourcesDirectoryPath)
    }

    private func getSourceDataUrl() -> URL {
        var currentFileUrl = URL(fileURLWithPath: #file)
        repeat {
            currentFileUrl = currentFileUrl.deletingLastPathComponent()
        } while
            FileManager.default.subpaths(atPath: currentFileUrl.path)?
            .contains(Constants.sourceDataDirectory) == false &&
            !currentFileUrl.pathComponents.isEmpty

        return currentFileUrl.appendingPathComponent(Constants.sourceDataDirectory)
    }
}

extension LocalMocksTestDataProvider {
    enum Constants {
        static let resourcesDirectoryPath = "PlugIns/FlexSnapshotTests.xctest/"
        static let sourceDataDirectory = "__Tests__"
        static let bundleExtension = "bundle"
        static let mockName = "response.json"
    }
}

enum LocalMocksTestDataProviderError: Error {
    case mainBundleResourceIsMissing
}
