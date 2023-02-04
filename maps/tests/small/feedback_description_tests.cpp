#include <yandex/maps/wiki/social/feedback/description.h>
#include <yandex/maps/wiki/social/feedback/description_keys.h>
#include <yandex/maps/wiki/social/feedback/description_producers.h>
#include <yandex/maps/wiki/social/feedback/description_serialize.h>

#include <maps/libs/common/include/exception.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::social::feedback::tests {

Y_UNIT_TEST_SUITE(feedback_description) {

Y_UNIT_TEST(json_from_description_i18n)
{
    {
        // Untranslatable
        Description desc("не буду переводить");
        auto json = R"("не буду переводить")";
        UNIT_ASSERT_STRINGS_EQUAL(toJson(desc), json);
    }
    {
        // Translatable. Key with empty params
        Description desc =
            DescriptionI18n(tanker::fb_desc::ADDRESS_UNKNOWN_KEY, {});
        auto json =
            R"({)"
                R"("i18nKey":"feedback-descriptions:address-unknown",)"
                R"("i18nParams":{})"
            R"(})";
        UNIT_ASSERT_STRINGS_EQUAL(toJson(desc), json);
    }
    {
        // Translatable. Key with non-empty params
        Description desc = DescriptionI18n(
            tanker::fb_desc::ADDR_CORRECTION_KEY,
            {
                {tanker::CORRECT_STREET, Description("1")},
                {tanker::CORRECT_HOUSE, Description("2")}
            }
        );
        auto json =
            R"({)"
                R"("i18nKey":"feedback-descriptions:addr-correction",)"
                R"("i18nParams":)"
                    R"({)"
                        R"("correctHouse":"2",)"
                        R"("correctStreet":"1")"
                    R"(})"
            R"(})";

        UNIT_ASSERT_STRINGS_EQUAL(toJson(desc), json);
    }
    {
        // Translatable. Key with params, which are also keys
        Description desc = DescriptionI18n(
            tanker::fb_desc::WRONG_ROAD_DIR_KEY,
            {
                {
                    tanker::CORRECT_ROAD_DIR,
                    DescriptionI18n(
                        tanker::attr_values::RDEL_ONEWAY_FROM_KEY,
                        {}
                    )
                }
            }
        );
        auto json =
            R"({)"
                R"("i18nKey":"feedback-descriptions:wrong-road-direction",)"
                R"("i18nParams":)"
                    R"({)"
                        R"("correctRoadDirection":)"
                            R"({)"
                                R"("i18nKey":"attr-values:rd_el-oneway__f",)"
                                R"("i18nParams":{})"
                            R"(})"
                    R"(})"
            R"(})";

        UNIT_ASSERT_STRINGS_EQUAL(toJson(desc), json);
    }
}

Y_UNIT_TEST(description_i18n_from_json)
{
    {
        // Untranslatable
        auto val = json::Value::fromString(R"("непереводимое")");

        auto desc = fromJson<Description>(val);
        UNIT_ASSERT(desc.isNonTranslatable());

        const auto& descRaw = desc.asNonTranslatable();
        UNIT_ASSERT_STRINGS_EQUAL(descRaw, "непереводимое");
    }
    {
        // Translatable. Key with empty params
        auto val = json::Value::fromString(
            R"({)"
                R"("i18nKey":"feedback-descriptions:address-unknown",)"
                R"("i18nParams":{})"
            R"(})");

        auto desc = fromJson<Description>(val);
        UNIT_ASSERT(desc.isTranslatable());

        const auto& descI18n = desc.asTranslatable();
        UNIT_ASSERT_EQUAL(
            descI18n.i18nKey(), tanker::fb_desc::ADDRESS_UNKNOWN_KEY);
        UNIT_ASSERT_EQUAL(descI18n.i18nParams(), (ParamToDescription{}));
    }
    {
        // Translatable. Key with non-empty params
        auto val = json::Value::fromString(
            R"({)"
                R"("i18nKey":"feedback-descriptions:addr-correction",)"
                R"("i18nParams":)"
                    R"({)"
                        R"("correctStreet":"1",)"
                        R"("correctHouse":"2")"
                    R"(})"
            R"(})");

        auto desc = fromJson<Description>(val);
        UNIT_ASSERT(desc.isTranslatable());

        const auto& descI18n = desc.asTranslatable();
        UNIT_ASSERT_EQUAL(
            descI18n.i18nKey(), tanker::fb_desc::ADDR_CORRECTION_KEY);
        UNIT_ASSERT_EQUAL(descI18n.i18nParams(),
            (ParamToDescription{
                {tanker::CORRECT_STREET, Description("1")},
                {tanker::CORRECT_HOUSE, Description("2")}
             })
        );
    }
    {
        // Translatable. Key with params, which are also keys
        auto val = json::Value::fromString(
            R"({)"
                R"("i18nKey":"feedback-descriptions:wrong-road-direction",)"
                R"("i18nParams":)"
                    R"({)"
                        R"("correctRoadDirection":)"
                            R"({)"
                                R"("i18nKey":"attr-values:rd_el-oneway__f",)"
                                R"("i18nParams":{})"
                            R"(})"
                    R"(})"
            R"(})");

        auto desc = fromJson<Description>(val);
        UNIT_ASSERT(desc.isTranslatable());

        const auto& descI18n = desc.asTranslatable();
        UNIT_ASSERT_EQUAL(
            descI18n.i18nKey(), tanker::fb_desc::WRONG_ROAD_DIR_KEY);

        UNIT_ASSERT_EQUAL(descI18n.i18nParams(), (ParamToDescription
            {
                {
                    tanker::CORRECT_ROAD_DIR,
                    DescriptionI18n(
                        tanker::attr_values::RDEL_ONEWAY_FROM_KEY,
                        {}
                    )
                }
            }
        ));
    }
    {
        // params are missing
        auto val = json::Value::fromString(
            R"({)"
                R"("i18nKey":"feedback-descriptions:address-unknown")"
            R"(})");
        UNIT_ASSERT_EXCEPTION(fromJson<Description>(val), LogicError);
    }
    {
        // tanker key is missing
        auto val = json::Value::fromString(
            R"({)"
                R"("i18nParams":{})"
            R"(})");
        UNIT_ASSERT_EXCEPTION(fromJson<Description>(val), LogicError);
    }
    {
        // json is neither object nor string
        auto val = json::Value::fromString(
            R"(["hey", "ho"])");
        UNIT_ASSERT_EXCEPTION(fromJson<Description>(val), LogicError);
    }
}

} // Y_UNIT_TEST_SUITE(feedback_description)

} // namespace maps::wiki::social::feedback::tests
