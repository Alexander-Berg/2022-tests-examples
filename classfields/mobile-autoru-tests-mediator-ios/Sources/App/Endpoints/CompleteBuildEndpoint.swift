import Foundation
import Mediation
import Vapor

private struct EndpointResponse: Content {
    let status: ResponseCode
}

func completeBuild(
    req: Request,
    testRunID: String,
    buildID: TeamcityBuildID
) -> EventLoopFuture<Response> {
    do {
        try req.application.mediation?.completeBuild(
            testRunID: testRunID,
            executorBuildID: buildID
        )
    } catch MediationState.Error.noActiveRunsForInitiator {
        return ErrorResponse(status: .invalidInitiator, message: "No runs for \(testRunID)")
            .encodeResponse(status: .badRequest, for: req)
    } catch MediationState.Error.uncompletedBuild {
        return ErrorResponse(status: .internalError, message: "Unable to complete build \(buildID)")
            .encodeResponse(status: .badRequest, for: req)
    } catch {
        return ErrorResponse(status: .internalError, message: "Internal error: \(error)")
            .encodeResponse(status: .internalServerError, for: req)
    }

    return EndpointResponse(status: .completeBuildOk)
        .encodeResponse(status: .ok, for: req)
}
