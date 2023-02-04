import AppServer
import XCTest

private enum IndexOrError {
    case index(Int)
    case error(String)
}

extension AppApi {

    func shouldEventsBeReported(
        _ events: [AnalyticsEvent],
        strictEquality: Bool = true,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard !events.isEmpty else { return }

        var reportedEvents = newAnalyticsEventsBlocking()

        var errors: [String] = []

        for event in events {
            let result = checkEvent(event, reportedEvents: reportedEvents)

            switch result {
            case let .index(index):
                let reportedEvent = reportedEvents.remove(at: index)
                if strictEquality, reportedEvent != event {
                    errors.append(
                        "Событие '\(event.name)' залоггировано, но в тесте указаны не все свойства."
                        + "Залоггированные свойства: \(reportedEvent.properties)"
                    )
                }

            case let .error(error):
                errors.append(error)
            }
        }

        if !errors.isEmpty {
            XCTFail(
                errors.joined(separator: "\n"),
                file: file,
                line: line
            )

            return
        }

        // second pass: check that events do not have duplicates

        var duplicatedEvents: [(AnalyticsEvent, AnalyticsEvent)] = []

        for event in events {
            let result = checkEvent(event, reportedEvents: reportedEvents)

            switch result {
            case let .index(index):
                duplicatedEvents.append((event, reportedEvents[index]))

            case .error:
                continue
            }
        }

        if !duplicatedEvents.isEmpty {
            let duplicatedEventsString = duplicatedEvents
                .map { "проверяемое (\($0)), фактическое (\($1))" }
                .joined(separator: "; ")

            XCTFail("Обнаружено дублирование событий: \(duplicatedEventsString)")
        }
    }

    private func checkEvent(_ event: AnalyticsEvent, reportedEvents: [AnalyticsEvent]) -> IndexOrError {
        if let index = reportedEvents.lastIndex(of: event) {
            return .index(index)
        }

        guard let lastEventIndexWithSameName = reportedEvents.lastIndex(where: { $0.name == event.name }) else {
            return .error("Нет события '\(event.name)'")
        }

        let lastEventWithSameName = reportedEvents[lastEventIndexWithSameName]

        let diff = findDifference(expected: event, actual: lastEventWithSameName)

        // don't return error if reported event has extra properties
        if diff.missingProperties.isEmpty, diff.differentProperties.isEmpty {
            return .index(lastEventIndexWithSameName)
        }

        var errors: [String] = []

        if !diff.missingProperties.isEmpty {
            errors.append("отсутствующие свойства (\(diff.missingProperties.map { "'\($0)'" }.joined(separator: ", ")))")
        }

        if !diff.differentProperties.isEmpty {
            let sortedProperties = diff.differentProperties.sorted(by: { $0.key < $1.key })
            let joined = sortedProperties.map { "'\($0.key)': \($0.value)" }.joined(separator: ", ")
            errors.append("отличающиеся свойства (\(joined))")
        }

        let errorText = "У события '\(event.name)' есть "
            + errors.joined(separator: " и ")
            + ". Полученное событие: \(lastEventWithSameName)"

        return .error(errorText)
    }

    private func findDifference(expected: AnalyticsEvent, actual: AnalyticsEvent) -> EventPropertyDiff {
        var diff = EventPropertyDiff(missingProperties: [], differentProperties: [:])

        for (key, value) in expected.properties {
            checkPropertyDifference(
                expected: value,
                actual: actual.properties[key],
                path: key,
                diff: &diff
            )
        }

        return diff
    }

    private func checkPropertyDifference(
        expected: EventProperty,
        actual: EventProperty?,
        path: String,
        diff: inout EventPropertyDiff
    ) {
        guard let actual = actual else {
            diff.missingProperties.append(path)
            return
        }

        if actual == expected {
            return
        }

        if case let .dictionary(expected) = expected, case let .dictionary(actual) = actual {
            for (key, value) in expected {
                checkPropertyDifference(
                    expected: value,
                    actual: actual[key],
                    path: "\(path) –> \(key)",
                    diff: &diff
                )
            }
        } else {
            diff.differentProperties[path] = "\(actual) != \(expected)"
        }
    }
}

private struct EventPropertyDiff {
    var missingProperties: [String]
    var differentProperties: [String: String]
}
