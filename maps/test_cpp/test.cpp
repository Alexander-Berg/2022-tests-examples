#include <yandex/maps/tiles_locale/tiles_locale.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace maps::tiles_locale;

TEST(best_tiles_locale, tests)
{
    struct TestData
    {
        std::string input;
        std::string expected;
    };

    std::vector<TestData> tests = {
        // Tile locales
        {"ru_RU", "ru_RU"},
        {"ru_UA", "ru_UA"},
        {"uk_UA", "uk_UA"},
        {"tr_TR", "tr_TR"},
        {"en_RU", "en_RU"},
        {"en_TR", "mul-Latn_TR"},
        {"en_UA", "mul-Latn_UA"},

        // Other supported locales
        {"en_US", "mul-Latn_001"},
        {"fi_FI", "mul-Latn_UA"},
        {"en_FI", "mul-Latn_UA"},
        {"fr_FR", "mul-Latn_UA"},
        {"en_FR", "mul-Latn_UA"},
        {"en_DE", "mul-Latn_001"},
        {"de_DE", "mul-Latn_001"},
        {"be-Latn_BY", "mul-Latn_UA"},

        {"ka_GE", "mul_UA"},
        {"en_GE", "mul-Latn_UA"},
        {"ru_GE", "ru_UA"},
        {"ro_MD", "mul-Latn_UA"},
        {"en_MD", "mul-Latn_UA"},
        {"ru_MD", "ru_UA"},

        {"he_IL", "he_IL"},
        {"iw_IL", "he_IL"},
        {"en_IL", "en_IL"},

        // Unsupported locales
        {"it_IT", "mul-Latn_001"},
        {"nn-Latn-NO-hognorsk_NO_LOCAL", "mul-Latn_001"},
        {"ru_IT", "mul_001"},
        {"en-SCOTLAND_GB_GLASGOW", "mul_001"}, // should be mul-Latn_001

        {"", "mul_001"},
        {" ", "mul_001"},
    };

    for (const auto& [input, expected] : tests) {
        auto result = bestTilesLocaleStr(input);
        EXPECT_EQ(result, expected) << "Input: " << input;
    };
}
