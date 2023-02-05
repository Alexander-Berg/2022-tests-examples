import Foundation

protocol TestDataProvider {
    func getTestConfigs() throws -> [FlexEngineTestConfig]
    func getData(for testConfig: FlexEngineTestConfig) throws -> Data
}
