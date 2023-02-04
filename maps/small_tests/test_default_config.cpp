#include <yandex/maps/wiki/common/default_config.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>

#include <vector>

namespace maps::wiki::common::tests {

namespace {

const std::vector<std::string> ENVIRONMENTS = {
    "unstable", "development",
    "testing",
    "load", "stress",
    "stable", "production",
};

} // namespace

Y_UNIT_TEST_SUITE(default_config) {

Y_UNIT_TEST(test_load_config_from_resource)
{
    vault_boy::MemoryContext context;
    context.add("POSTGRESQL_MAPSPRO", "testpassword_old");
    context.add("POSTGRESQL_MAPSPRO_MDB", "testpassword_new");
    context.add("POSTGRESQL_STAT", "testpassword_stat");

    for (const auto& env : ENVIRONMENTS) {
        auto docPtr = loadDefaultConfigFromResource(env, context);

        UNIT_ASSERT_STRINGS_EQUAL(docPtr->node("/config/extends").value<std::string>(), "services-base.xml");
        UNIT_ASSERT_STRINGS_EQUAL(docPtr->node("/config/common/project").value<std::string>(), "wikimaps");

        UNIT_ASSERT_STRINGS_EQUAL(getCoreDbPassword(*docPtr), "testpassword_new");
    }
}

Y_UNIT_TEST(test_load_config_from_resource_empty_environment)
{
    const vault_boy::MemoryContext context;
    UNIT_CHECK_GENERATED_EXCEPTION(loadDefaultConfigFromResource("", context), maps::Exception);
}

Y_UNIT_TEST(test_load_config_from_resource_invalid_environment)
{
    const vault_boy::MemoryContext context;
    UNIT_CHECK_GENERATED_EXCEPTION(loadDefaultConfigFromResource("invalid", context), maps::Exception);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
