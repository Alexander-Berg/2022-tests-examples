#include <maps/sprav/callcenter/libs/dao/request_type_definition.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

namespace maps::sprav::callcenter::tests {

namespace {
    auto makeTx() {
        static DbFixture db;
        return db.pool().masterWriteableTransaction();
    }

    const TString TEST_ID = "test_request_type";
} // namespace

TEST(RequestTypeDefinitionTest, GetAllList) {
    auto tx = makeTx();
    tx->exec("INSERT INTO tasker.request_type (internal_name, priority) VALUES ('test_name', 0)");
    tx->exec(
        "INSERT INTO tasker.request_type"
        "  (internal_name, description, priority, is_deleted, complexity_factor, project_id)"
        "  VALUES ('test_name_2', 'some description', 100, true, 1, 101)");
    tx->commit();

    {
        auto tx = makeTx();
        auto allList = dao::RequestTypeDefinition::getAllList(*tx);

        EXPECT_THAT(allList, NGTest::EqualsProto(test_helpers::protoFromTextFormat<proto::RequestTypeDefinitionList>(R"(
            items {
                ang_groups: []
                complexity_factor: 100
                disregard_work_time: false
                internal_name: "test_name"
                is_deleted: false
                issued_tasks_threshold: 100
                priority: 0
                project_id: 212
                template_id: 0
                yang_pool_priority: 0
            }
            items {
                ang_groups: []
                complexity_factor: 1
                description: "some description"
                disregard_work_time: false
                internal_name: "test_name_2"
                is_deleted: true
                issued_tasks_threshold: 100
                priority: 100
                project_id: 101
                template_id: 0
                yang_pool_priority: 0
            }
        )")));
    }
}

} // maps::sprav::callcenter::tests
