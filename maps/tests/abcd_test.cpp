#include <maps/infra/quotateka/libs/abcd/abcd.h>
#include <maps/infra/quotateka/libs/abcd/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::abcd::tests {

Y_UNIT_TEST_SUITE(abcd)
{

Y_UNIT_TEST(get_default_folder)
{
    AbcdFixture fixture;

    std::string folderId = "9900f06f-b1df-4213-ad95-4dd6234f17e7";
    fixture.addServiceFolder(
        12345,
        FolderInfo{.id = folderId}
    );

    EXPECT_STREQ(
        AbcdApi("ticket").defaultFolder(12345).c_str(),
        folderId.c_str());

} // Y_UNIT_TEST

} // Y_UNIT_TEST_SUITE(abcd)

} // namespace maps::abcd::tests

