import App
import Vapor

LoggingSystem.bootstrap { _ in
    VertisDeployLogHandler()
}

var env = try Environment.detect()
let app = Application(env)
defer { app.shutdown() }

let apiPort = Environment.get("API_PORT").flatMap({ Int($0) }) ?? 80

guard let teamcityHost = Environment.get("TEAMCITY_HOST") else {
    app.logger.critical("Unable to get env parameter TEAMCITY_HOST")
    exit(EXIT_FAILURE)
}

guard let teamcityPort = Environment.get("TEAMCITY_PORT").flatMap({ Int($0) }) else {
    app.logger.critical("Unable to get env parameter TEAMCITY_PORT")
    exit(EXIT_FAILURE)
}

guard let teamcityToken = Environment.get("TEAMCITY_TOKEN") else {
    app.logger.critical("Unable to get env parameter TEAMCITY_TOKEN")
    exit(EXIT_FAILURE)
}

let useTeamcityStub = Environment.get("TEAMCITY_STUB") == "1"

let config = Config(
    teamcityHost: teamcityHost,
    teamcityPort: teamcityPort,
    teamcityToken: teamcityToken,
    useTeamcityStub: useTeamcityStub,
    apiPort: apiPort
)

// Main server

try configure(app, config: config)

// Metrics server for vertis deploy

let metricsPort = Environment.get("_DEPLOY_METRICS_PORT").flatMap({ Int($0) }) ?? 81

let metricsApp = Application(env)
metricsApp.logger.info("Start metrics server at port \(metricsPort)")

defer { metricsApp.shutdown() }

try configureForMetrics(metricsApp, metricsPort: metricsPort)

// Run
try metricsApp.start()

try app.run()
