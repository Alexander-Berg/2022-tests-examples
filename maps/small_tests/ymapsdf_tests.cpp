#include <yandex/maps/wiki/tasks/ymapsdf.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::tasks::ymapsdf::tests {

Y_UNIT_TEST_SUITE(ymapsdf) {

Y_UNIT_TEST(test_arcadia_dir)
{
    UNIT_ASSERT_EQUAL(getArcadiaSchemasDirectory(), "maps/doc/schemas/ymapsdf/package");
}

Y_UNIT_TEST(test_script_path)
{
    const std::string file = "test.sql";
    UNIT_ASSERT_EQUAL(getScriptPath(file), getArcadiaScriptPath(file));
}

Y_UNIT_TEST(test_get_set_default)
{
    auto prev = getDefaultSchemasDirectory();
    UNIT_ASSERT_EQUAL(prev, "/usr/share/yandex/maps/ymapsdf2");

    const std::string file = "test.sql";
    UNIT_ASSERT_EQUAL(getDefaultScriptPath(file), getDefaultSchemasDirectory() + "/" + file);

    const auto newDir = "/test/ymapsdf";
    setDefaultSchemasDirectory(newDir);
    UNIT_ASSERT_EQUAL(getDefaultSchemasDirectory(), newDir);

    setDefaultSchemasDirectory(prev);
    UNIT_ASSERT_EQUAL(getDefaultSchemasDirectory(), prev);
}

Y_UNIT_TEST(test_garden_dir)
{
    auto prev = getDefaultSchemasDirectory();
    UNIT_ASSERT_EQUAL(prev, "/usr/share/yandex/maps/ymapsdf2");

    UNIT_ASSERT_EQUAL(getGardenSchemasDirectory(), "/usr/share/yandex/maps/garden/ymapsdf2");

    setDefaultSchemasDirectory("~/arcadia/" + getArcadiaSchemasDirectory());
    UNIT_ASSERT_EQUAL(getGardenSchemasDirectory(), "~/arcadia/maps/doc/schemas/ymapsdf/garden");

    setDefaultSchemasDirectory(prev);
    UNIT_ASSERT_EQUAL(getDefaultSchemasDirectory(), prev);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tasks::ymapsdf::tests
