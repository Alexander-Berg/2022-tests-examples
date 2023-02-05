@file:Suppress("ObjectPropertyName")

package ru.yandex.yandexmaps.tools.analyticsgenerator.swift

val `complex name without parameters` = """
extension GenaMetricsEventTracker {
    public func mapAddBookmarkSubmit() {
        eventTracker.trackEvent("map.add-bookmark.submit")
    }
}
""".trim('\n')

val `single bool parameter` = """
extension GenaMetricsEventTracker {
    public func mapAddBookmarkSubmit(with authorized: Bool?) {
        var options: [String: String] = [:]
        options["authorized"] = authorized.flatMap { "\($0)" }
        eventTracker.trackEvent("map.add-bookmark.submit", withOptions: options)
    }
}
""".trim('\n')

val `single int parameter` = """
extension GenaMetricsEventTracker {
    public func event(with parameter: Int?) {
        var options: [String: String] = [:]
        options["parameter"] = parameter.flatMap { "\($0)" }
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')

val `single double parameter` = """
extension GenaMetricsEventTracker {
    public func event(with parameter: Double?) {
        var options: [String: String] = [:]
        options["parameter"] = parameter.flatMap { "\($0)" }
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')

val `single float parameter` = """
extension GenaMetricsEventTracker {
    public func event(with parameter: Float?) {
        var options: [String: String] = [:]
        options["parameter"] = parameter.flatMap { "\($0)" }
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')

val `single string parameter` = """
extension GenaMetricsEventTracker {
    public func event(with parameter: String?) {
        var options: [String: String] = [:]
        options["parameter"] = parameter
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')

val `dictionary parameter` = """
extension GenaMetricsEventTracker {
    public func event(with dictionary: [String: String]?) {
        eventTracker.trackEvent("event", withOptions: dictionary ?? [:])
    }
}
""".trim('\n')

val `single enum parameter` = """
public enum GenaMetricsEventEventApplicationLayerType: String {
    case map = "map"
    case satellite = "satellite"
    case hybrid = "hybrid"
}

extension GenaMetricsEventTracker {
    public func event(with applicationLayerType: GenaMetricsEventEventApplicationLayerType?) {
        var options: [String: String] = [:]
        options["application_layer_type"] = applicationLayerType.flatMap { $0.rawValue }
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')

val `single enum parameter with true and false` = """
public enum GenaMetricsEventEventBooleanVariants: String {
    case `true` = "true"
    case `false` = "false"
}

extension GenaMetricsEventTracker {
    public func event(with booleanVariants: GenaMetricsEventEventBooleanVariants?) {
        var options: [String: String] = [:]
        options["boolean_variants"] = booleanVariants.flatMap { $0.rawValue }
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')

val `many parameters` = """
public enum GenaMetricsEventEventApplicationLayerType: String {
    case map = "map"
    case satellite = "satellite"
    case hybrid = "hybrid"
}

extension GenaMetricsEventTracker {
    public func event(
        with eventName: String?,
        applicationLayerType: GenaMetricsEventEventApplicationLayerType?,
        amount: Int?
    ) {
        var options: [String: String] = [:]
        options["event_name"] = eventName
        options["application_layer_type"] = applicationLayerType.flatMap { $0.rawValue }
        options["amount"] = amount.flatMap { "\($0)" }
        eventTracker.trackEvent("event", withOptions: options)
    }
}
""".trim('\n')
