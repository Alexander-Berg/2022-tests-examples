import Vapor
import Mediation

func routes(_ app: Application) throws {
    app.post("start") { req in
        startTestRun(req: req)
    }

    app.post("next", ":testRun") { req -> EventLoopFuture<Response> in
        guard let testRun = req.parameters.get("testRun") else {
            return ErrorResponse(status: .invalidParameter, message: "Invalid testrun for next batch")
                .encodeResponse(status: .badRequest, for: req)
        }

        return nextBatch(req: req, testRunID: testRun)
    }

    app.get("complete", ":testRun", "build", ":buildID") { req -> EventLoopFuture<Response> in
        guard let testRun = req.parameters.get("testRun") else {
            return ErrorResponse(status: .invalidParameter, message: "Invalid test run for complete build")
                .encodeResponse(status: .badRequest, for: req)
        }

        guard let buildID = req.parameters.get("buildID").flatMap({ TeamcityBuildID($0) }) else {
            return ErrorResponse(status: .invalidParameter, message: "Invalid test run for complete build")
                .encodeResponse(status: .badRequest, for: req)
        }

        return completeBuild(req: req, testRunID: testRun, buildID: buildID)
    }

    app.get("complete", ":testRun", "batch", ":batchID") { req -> EventLoopFuture<Response> in
        guard let testRun = req.parameters.get("testRun") else {
            return ErrorResponse(status: .invalidParameter, message: "Invalid test run for complete batch")
                .encodeResponse(status: .badRequest, for: req)
        }

        guard let batchID = req.parameters.get("batchID"), !batchID.isEmpty else {
            return ErrorResponse(status: .invalidParameter, message: "Invalid batch id for complete batch")
                .encodeResponse(status: .badRequest, for: req)
        }

        return completeBatch(req: req, testRunID: testRun, batchID: batchID)
    }

    app.post("retry", ":testRun") { req -> EventLoopFuture<Response> in
        guard let testRun = req.parameters.get("testRun") else {
            return ErrorResponse(status: .invalidParameter, message: "Invalid test run for complete build")
                .encodeResponse(status: .badRequest, for: req)
        }

        return retryTests(req: req, testRunID: testRun)
    }

    app.get("ping") { req in "" }

    app.get("debug") { req in
        return ""
    }
}
