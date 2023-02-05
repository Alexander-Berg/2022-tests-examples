#include "../utils.h"

#include <yandex/maps/wiki/configs/editor/config_holder.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps {
namespace wiki {
namespace configs {
namespace editor {
namespace tests {

Y_UNIT_TEST_SUITE(test_configs_editor)
{

Y_UNIT_TEST(test_loading_from_path)
{
    UNIT_ASSERT_NO_EXCEPTION(
        ConfigHolder(ArcadiaSourceRoot() + "/maps/wikimap/mapspro/cfg/editor/editor.xml"));
}

Y_UNIT_TEST(test_loading_from_resource)
{
    UNIT_ASSERT_NO_EXCEPTION(ConfigHolder());
}

Y_UNIT_TEST(test_punctuation_chars)
{
    std::string testString;
    testString = "  '',,AA,,  BB";
    deduplicatePunctuation(testString);
    UNIT_ASSERT_STRINGS_EQUAL(testString, " ',AA, BB");
}

Y_UNIT_TEST(test_trimUTF8BOM)
{
    std::string testStringNoBOM = "\xEF\xBB\xBFtest";
    std::string testBOMWithString = "test\xEF\xBB\xBF";
    std::string testStringWithBOM = "\xEF\xBB\xBFtest";
    std::string testStringWithBOMBOMBOM = "\xEF\xBB\xBF\xEF\xBB\xBF\xEF\xBB\xBFtest";
    UNIT_ASSERT_VALUES_EQUAL(testBOMWithString.length(), 7);
    UNIT_ASSERT_VALUES_EQUAL(testStringWithBOM.length(), 7);
    UNIT_ASSERT_VALUES_EQUAL(testStringWithBOMBOMBOM.length(), 13);
    UNIT_ASSERT_STRINGS_EQUAL("test", trimLeadingUTF8BOM(testStringWithBOM));
    UNIT_ASSERT_STRINGS_EQUAL("test", trimLeadingUTF8BOM(testStringWithBOMBOMBOM));
    UNIT_ASSERT_STRINGS_EQUAL("test", trimLeadingUTF8BOM(testStringNoBOM));
    UNIT_ASSERT_STRINGS_EQUAL(testBOMWithString, trimLeadingUTF8BOM(testBOMWithString));
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace editor
} // namespace configs
} // namespace wiki
} // namespace maps
