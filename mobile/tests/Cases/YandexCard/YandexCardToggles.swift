import XCTest

class YandexCardTests: LocalMockTestCase {

    func setupFlags() {
        "Настраиваем тоглы".ybm_run { _ in
            let toggleInfo: String = {
                let name = FeatureNames.yaBankPayment.lowercased()
                let toggleAdditionalInfo = [name: ["ignoreExperiment": true]]

                guard let toggleInfosData = try? JSONSerialization.data(
                    withJSONObject: toggleAdditionalInfo,
                    options: .prettyPrinted
                )
                else {
                    return ""
                }
                return String(data: toggleInfosData, encoding: .utf8) ?? ""
            }()

            enable(toggles: FeatureNames.yaCard, FeatureNames.yaBankPayment)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo
        }
    }
}
