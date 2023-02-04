#include <yandex/maps/wiki/tasks/tasks.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps {
namespace wiki {
namespace tasks {
namespace tests {

namespace {

const std::string TASK_NAME = "task_name";
const std::string ATTR1 = "attr1";
const std::string ATTR2 = "attr2";

} // namespace

Y_UNIT_TEST_SUITE(task_attributes) {

Y_UNIT_TEST_F(test_long_tasks_attributes, unittest::ArcadiaDbFixture)
{
    auto work = pool().masterWriteableTransaction();

    auto attributes = tasks::attributesForTaskType(*work, TASK_NAME);
    UNIT_ASSERT(attributes.empty());

    tasks::concatenateAttributesForTaskType(*work, TASK_NAME, {});

    attributes = tasks::attributesForTaskType(*work, TASK_NAME);
    UNIT_ASSERT_VALUES_EQUAL(attributes.size(), 0);

    tasks::concatenateAttributesForTaskType(*work, TASK_NAME, {{ATTR1, "1"}});

    attributes = tasks::attributesForTaskType(*work, TASK_NAME);
    UNIT_ASSERT_VALUES_EQUAL(attributes.size(), 1);
    UNIT_ASSERT_STRINGS_EQUAL(attributes[ATTR1], "1");

    tasks::concatenateAttributesForTaskType(*work, TASK_NAME,
        {{ATTR1, "0"},
        {ATTR2, "a"}});

    attributes = tasks::attributesForTaskType(*work, TASK_NAME);
    UNIT_ASSERT_VALUES_EQUAL(attributes.size(), 2);
    UNIT_ASSERT_STRINGS_EQUAL(attributes[ATTR1], "0");
    UNIT_ASSERT_STRINGS_EQUAL(attributes[ATTR2], "a");
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace tasks
} // namespace wiki
} // namespace maps
