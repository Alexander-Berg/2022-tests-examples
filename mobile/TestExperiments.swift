//
// Created by Timur Turaev on 2019-05-21.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
@testable import YandexMobileMail

internal final class TestExperimentDataResolver: ExperimentDataResolving {
    private let allExperimentTypes: [Experiment.Type]

    init(allExperimentTypes: [Experiment.Type]) {
        self.allExperimentTypes = allExperimentTypes
    }

    public func experimentDataForName(_ name: String) -> ExperimentData.Type? {
        return self.allExperimentTypes.dictionariseWithItemProducer { ($0.name, $0.dataType) }[name]
    }
}

internal final class TestExperimentWithoutData: BaseExperiment<EmptyExperimentData, ExperimentContext> {
    public override static var name: String {
        return "testExperimentWithoutData"
    }
}

internal final class TestExperimentWithoutData1: BaseExperiment<EmptyExperimentData, ExperimentContext> {
    public override static var name: String {
        return "testExperimentWithoutData1"
    }
}

internal final class TestExperimentWithoutData2: BaseExperiment<EmptyExperimentData, ExperimentContext> {
    public override static var name: String {
        return "testExperimentWithoutData2"
    }
}

internal final class TestExperimentWithoutData3: BaseExperiment<EmptyExperimentData, ExperimentContext> {
    public override static var name: String {
        return "testExperimentWithoutData3"
    }
}

internal final class TestExperimentWithData: BaseExperiment<TestExperimentData, ExperimentContext> {
    public override static var name: String {
        return "testExperimentWithData"
    }
}

internal struct TestExperimentData: ExperimentData, Equatable {
    private enum CodingKeys: String, CodingKey {
        case parameter = "uaz_experiment_parameter"
    }

    public let parameter: Int
}
