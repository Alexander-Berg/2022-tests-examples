#include <maps/infra/quotateka/libs/abc/abc.h>
#include <maps/infra/quotateka/libs/abc/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::abc::tests {

Y_UNIT_TEST_SUITE(abc)
{

Y_UNIT_TEST(members)
{
    AbcFixture fixture;

    uint64_t userA = 153, userB = 154;
    fixture.addMember("xxx-service", 1, userA, ADMINS_ROLE_SCOPE);
    fixture.addMember("xxx-service", 1, userB, DEVELOPERS_ROLE_SCOPE);
    fixture.addMember("yyy-service", 2, userA, MANAGERS_ROLE_SCOPE);

    // No filters query returns everybody
    EXPECT_TRUE(abc::Members("ticket").exists());

    EXPECT_TRUE(abc::Members("ticket").serviceSlug("xxx-service").exists());
    EXPECT_FALSE(abc::Members("ticket").serviceSlug("some-other-abc-service").exists());

    // userA cases
    EXPECT_TRUE(abc::Members("ticket")
        .serviceSlug("xxx-service").personUid(userA).exists());
    EXPECT_TRUE(abc::Members("ticket")
        .serviceSlug("xxx-service").personUid(userA).roleScope(ADMINS_ROLE_SCOPE).exists());
    EXPECT_TRUE(abc::Members("ticket")
        .serviceSlug("xxx-service").personUid(userA)
        .roleScopes({ADMINS_ROLE_SCOPE, DEVELOPERS_ROLE_SCOPE}).exists());
    EXPECT_FALSE(abc::Members("ticket")
        .serviceSlug("xxx-service").personUid(userA).roleScope(DEVELOPERS_ROLE_SCOPE).exists());

    // userB cases
    EXPECT_FALSE(abc::Members("ticket")
        .serviceSlug("yyy-service").personUid(userB).exists());
    EXPECT_FALSE(abc::Members("ticket")
        .serviceSlug("yyy-service").personUid(userB)
        .roleScopes({ADMINS_ROLE_SCOPE, DEVELOPERS_ROLE_SCOPE, MANAGERS_ROLE_SCOPE}).exists());
    EXPECT_FALSE(abc::Members("ticket")
        .serviceSlug("xxx-service").personUid(userB)
        .roleScopes({ADMINS_ROLE_SCOPE}).exists());
    EXPECT_FALSE(abc::Members("ticket")
        .serviceSlug("xxxx-service").personUid(userB)
        .roleScopes({DEVELOPERS_ROLE_SCOPE}).exists());

    // Non member user
    EXPECT_FALSE(abc::Members("ticket")
        .serviceSlug("yyy-service").personUid(1111111).exists());

} // Y_UNIT_TEST

Y_UNIT_TEST(services)
{
    AbcFixture fixture;
    fixture.addService("maps-core-abrvalg", 1);

    // existing service by slug
    EXPECT_EQ(
        *abc::Service("ticket").slug("maps-core-abrvalg").lookup(),
        (abc::ServiceInfo{ .id = 1, .slug = "maps-core-abrvalg"}));

    // existing service by id
    EXPECT_EQ(
        *abc::Service("ticket").id(1).lookup(),
        (abc::ServiceInfo{ .id = 1, .slug = "maps-core-abrvalg"}));

    // nonexisting service
    EXPECT_FALSE(abc::Service("ticket").slug("abrvalg").lookup());
}

Y_UNIT_TEST(full_bypass_mode)
{
    AbcFixture fixture;

    abc::Settings::global().enableFullBypass();

    // All Service and Members checks will succeed
    EXPECT_EQ(
        *abc::Service("ticket").slug("some-imaginary-abc").lookup(),
        (abc::ServiceInfo{
            .id = std::hash<std::string>()("some-imaginary-abc"),
            .slug = "some-imaginary-abc"})
    );
    EXPECT_TRUE(
        abc::Members("ticket").serviceSlug("whatever")
            .personUid(153).exists()
    );
}

}  // Y_UNIT_TEST_SUITE(abc)

} // namespace maps::abc::tests

