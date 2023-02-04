#include <yandex/maps/wiki/common/compound_token.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(compound_token) {

Y_UNIT_TEST(test_empty)
{
    UNIT_ASSERT(CompoundToken::isValid(""));

    CompoundToken token;
    UNIT_ASSERT_STRINGS_EQUAL(token.str(), "");
}

Y_UNIT_TEST(test_garbage)
{
    UNIT_ASSERT(!CompoundToken::isValid("."));
    UNIT_ASSERT(!CompoundToken::isValid(":"));
    UNIT_ASSERT(!CompoundToken::isValid("asdasd"));
    UNIT_ASSERT(!CompoundToken::isValid("123"));
    UNIT_ASSERT(!CompoundToken::isValid("\"123:456\""));
    UNIT_ASSERT(!CompoundToken::isValid("123:456:core:123"));

    UNIT_ASSERT(!CompoundToken::isValid("x123:456:core"));
    UNIT_ASSERT(!CompoundToken::isValid("123:x456:core"));

    UNIT_ASSERT(!CompoundToken::isValid("123:456:"));
    UNIT_ASSERT(!CompoundToken::isValid("123:456:.456:123:social"));
    UNIT_ASSERT(!CompoundToken::isValid("123:456:core.456:123:xxx"));
    UNIT_ASSERT(!CompoundToken::isValid("123:456.123:456:core"));
}

Y_UNIT_TEST(test_non_core_first)
{
    UNIT_ASSERT(!CompoundToken::isValid("123:456:social"));
    UNIT_ASSERT(!CompoundToken::isValid("123:456:core.456:123:core"));
}

Y_UNIT_TEST(test_duplicates)
{
    UNIT_ASSERT(!CompoundToken::isValid("123:456:core.123:456:social.123:456:social"));
}

Y_UNIT_TEST(test_valid_token)
{
    UNIT_ASSERT(CompoundToken::isValid("123:456"));
    UNIT_ASSERT(CompoundToken::isValid("123:456:core"));

    CompoundToken token;
    token.append("core", "123:456");
    token.append("social", "456:789");
    token.append("trunk", "123:456");
    token.append("stable", "456:789");
    token.append("trunkL", "123:456");
    token.append("stableL", "456:789");

    auto tokenStr = token.str();
    UNIT_ASSERT_STRINGS_EQUAL(
        tokenStr,
        "123:456:core.456:789:social.123:456:trunk.456:789:stable.123:456:trunkL.456:789:stableL");
    UNIT_ASSERT(CompoundToken::isValid(tokenStr));

    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken(tokenStr, "core"), "123:456:core");
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken(tokenStr, "social"), "456:789:social");
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken(tokenStr, "trunk"), "123:456:trunk");
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken(tokenStr, "stable"), "456:789:stable");
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken(tokenStr, "trunkL"), "123:456:trunkL");
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken(tokenStr, "stableL"), "456:789:stableL");

    // not existed aliases
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken("", ""), "");
    UNIT_ASSERT_STRINGS_EQUAL(CompoundToken::subToken("xxx", ""), "");
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
