import Mediation

final class MockLogger: Logger {
    func info(_ string: String) { print(string) }

    func error(_ string: String) { print(string) }
}
