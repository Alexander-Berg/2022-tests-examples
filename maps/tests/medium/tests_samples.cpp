#include <maps/wikimap/mapspro/libs/assessment/include/gateway.h>

#include "helpers.h"
#include <library/cpp/testing/unittest/registar.h>

#include <array>


namespace maps::wiki::assessment::tests {

const chrono::TimePoint NOW = std::chrono::system_clock::now();


Y_UNIT_TEST_SUITE_F(samples_tests, DbFixture) {
    Y_UNIT_TEST(should_not_create_empty_sample) {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            gateway.createSample(Entity::Domain::Moderation, Qualification::Basic, "sample-name", {}, 1),
            RuntimeError,
            "Can't create an empty sample."
        );

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            gateway.createSample(Entity::Domain::Moderation, Qualification::Basic, "sample-name", {1}, 0),
            RuntimeError,
            "tasksPerUnit must be greater than zero."
        );
    }

    Y_UNIT_TEST(should_create_sample)
    {
        const std::array taskIds = {"1", "2"};
        const TId uid = 10;

        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        const std::array unitIds = {
            gateway.getOrCreateUnit({taskIds[0], Entity::Domain::Moderation}, {"resolve", uid, NOW}),
            gateway.getOrCreateUnit({taskIds[1], Entity::Domain::Moderation}, {"resolve", uid, NOW})
        };

        const auto sampleId = gateway.createSample(
            Entity::Domain::Moderation,
            Qualification::Basic,
            "sample",
            {unitIds[0], unitIds[1]},
            3
        );

        const auto rows = txn.exec(
            "SELECT unit_id, COUNT(1) as tasks_per_unit "
            "FROM assessment.sample_task "
            "WHERE sample_id = " + std::to_string(sampleId) + " "
            "GROUP BY unit_id"
        );
        UNIT_ASSERT_EQUAL(rows.size(), 2);
        UNIT_ASSERT_EQUAL(rows[0]["unit_id"].as<TId>(), unitIds[0]);
        UNIT_ASSERT_EQUAL(rows[0]["tasks_per_unit"].as<int>(), 3);
        UNIT_ASSERT_EQUAL(rows[1]["unit_id"].as<TId>(), unitIds[1]);
        UNIT_ASSERT_EQUAL(rows[1]["tasks_per_unit"].as<int>(), 3);
    }
}

} // namespace maps::wiki::assessment::tests
