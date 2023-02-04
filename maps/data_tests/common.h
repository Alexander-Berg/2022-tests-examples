#pragma once

#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/include/tasks.h>

#include <map>
#include <string>
#include <vector>

namespace maps::renderer::denormalization {

enum class TestType {
    // test compares output table data with expected table data
    // (only for last task in taskNames)
    ExpectedData,
    // test checks that query will return only true boolean values
    ValidateQuery
};

struct DataTestFixture : NUnitTest::TBaseFixture {
    std::map<std::string, TaskPtr> tasksMap;
    std::map<std::string, GardenResource> outputResources;

    DataTestFixture();

    void run(
        const std::string& testName, TestType testType, const std::vector<std::string>& taskNames);
};

} // namespace maps::renderer::denormalization
