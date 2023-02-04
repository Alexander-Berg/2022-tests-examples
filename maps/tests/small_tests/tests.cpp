#include <yandex/maps/wiki/tasks/tool_commands.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps {
namespace wiki {
namespace tasks {
namespace tests {

Y_UNIT_TEST_SUITE(other) {

Y_UNIT_TEST(test_stages_str)
{
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({}), "all");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::ViewAttrs }), "view,attrs");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::Labels }), "labels");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::Bboxes }), "bbox");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::ViewAttrs, SyncStage::Labels }), "view,attrs,labels");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::ViewAttrs, SyncStage::Bboxes }), "view,attrs,bbox");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::Labels, SyncStage::Bboxes }), "labels,bbox");
    UNIT_ASSERT_STRINGS_EQUAL(computeStagesStr({ SyncStage::ViewAttrs, SyncStage::Labels, SyncStage::Bboxes }), "view,attrs,labels,bbox");
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace tasks
} // namespace wiki
} // namespace maps
