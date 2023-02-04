import Foundation
import Teamcity
@testable import Mediation

final class MockTeamcityClient: TeamcityClient {
    var buildsTriggered = 0
    var finishedBuilds = Set<TeamcityBuildID>()

    private var timer: Timer?

    func triggerBuild(
        configID: BuildConfigurationID,
        branch: String,
        parameters: [BuildParameter : BuildParameter.Value],
        comment: String?,
        completion: TriggerCompletion?
    ) {
        let build = Build(
            id: TeamcityBuildID.mock(),
            number: TeamcityBuildNumber.mock(),
            state: .queued,
            buildTypeId: "testsRunPool"
        )

        buildsTriggered += 1

        completion?(.success(build))
    }

    func observeBuild(id: Int, interval: TimeInterval, stopToken: @escaping StopToken) {
        func tick() {
            let build = Build(
                id: id,
                number: TeamcityBuildNumber.mock(),
                state: finishedBuilds.contains(id) ? .finished : .running,
                buildTypeId: "testsRunPool"
            )

            if !stopToken(.success(build)) {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: tick)
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: tick)
    }
}
