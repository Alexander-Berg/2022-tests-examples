#include <yandex/maps/wiki/social/feedback/attributes.h>
#include <yandex/maps/wiki/social/feedback/attribute_names.h>
#include <maps/libs/json/include/builder.h>

#include <maps/libs/common/include/exception.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::social::feedback::tests {

namespace {

const std::string FIRST_NAME = "first";
const std::string FIRST_VAL = "firstVal";
const std::string SECOND_NAME = "second";
const std::string SECOND_VAL = "secondVal";

} // anonymous

Y_UNIT_TEST_SUITE(feedback_attributes) {

Y_UNIT_TEST(predefined_attr)
{

    Attrs attrs;
    attrs.add(
        AttrType::SourceContext,
        json::Value::fromString("{}")
    );

    UNIT_ASSERT(attrs.exist(AttrType::SourceContext));
    UNIT_ASSERT(!attrs.exist(AttrType::ObjectDiff));
    UNIT_ASSERT(!attrs.exist(AttrType::UserData));
    UNIT_ASSERT(!attrs.exist(AttrType::UserDataPhotoUrls));

    UNIT_ASSERT_NO_EXCEPTION(attrs.get(AttrType::SourceContext));
    UNIT_ASSERT_EXCEPTION(attrs.get(AttrType::ObjectDiff), LogicError);
    UNIT_ASSERT_EXCEPTION(attrs.get(AttrType::UserData), LogicError);
    UNIT_ASSERT_EXCEPTION(attrs.get(AttrType::UserDataPhotoUrls), LogicError);

    const auto& val = attrs.get(AttrType::SourceContext);
    UNIT_ASSERT(val.isObject());
    UNIT_ASSERT(val.empty());
}

Y_UNIT_TEST(custom_attr)
{
    Attrs attrs;
    attrs.addCustom(FIRST_NAME, FIRST_VAL);

    UNIT_ASSERT(attrs.existCustom(FIRST_NAME));
    UNIT_ASSERT(!attrs.existCustom(SECOND_NAME));

    UNIT_ASSERT_NO_EXCEPTION(attrs.getCustom(FIRST_NAME));
    UNIT_ASSERT_EXCEPTION(attrs.getCustom(SECOND_NAME), LogicError);

    const auto& val = attrs.getCustom(FIRST_NAME);
    UNIT_ASSERT(val == FIRST_VAL);

    UNIT_ASSERT_EXCEPTION(attrs.addCustom(attrs::SOURCE_CONTEXT, FIRST_VAL), LogicError);
}

Y_UNIT_TEST(from_json)
{
    auto json = R"(
        {
            "custom":"attr",
            "objectDiff":
                {
                    "before":"after"
                },
            "sourceContext":
                {
                    "some":"image"
                },
            "userData":
                {
                    "k":"v"
                },
            "userDataPhotoUrls":
                [
                    "url1", "url2"
                ]
        })";

    Attrs attrs(json::Value::fromString(json));

    UNIT_ASSERT(attrs.exist(AttrType::ObjectDiff));
    UNIT_ASSERT(attrs.exist(AttrType::SourceContext));
    UNIT_ASSERT(attrs.exist(AttrType::UserData));
    UNIT_ASSERT(attrs.exist(AttrType::UserDataPhotoUrls));
    UNIT_ASSERT(attrs.existCustom("custom"));
}

Y_UNIT_TEST(to_json)
{
    Attrs attrs;
    attrs.add(
        AttrType::SourceContext,
        json::Value::fromString(R"({"some":"image"})")
    );

    attrs.add(
        AttrType::UserData,
        json::Value::fromString(R"({"k":"v"})")
    );

    attrs.add(
        AttrType::ObjectDiff,
        json::Value::fromString(R"({"before":"after"})")
    );

    attrs.add(
        AttrType::UserDataPhotoUrls,
        json::Value::fromString(R"(["http://1", "http://2"])")
    );

    attrs.addCustom("custom", "attr");

    auto result =
        R"({)"
            R"("custom":"attr",)"
            R"("objectDiff":)"
                R"({)"
                    R"("before":"after")"
                R"(},)"
            R"("sourceContext":)"
                R"({)"
                    R"("some":"image")"
                R"(},)"
            R"("userData":)"
                R"({)"
                    R"("k":"v")"
                R"(},)"
            R"("userDataPhotoUrls":)"
                R"([)"
                    R"("http://1",)"
                    R"("http://2")"
                R"(])"
        R"(})";

    json::Builder builder;
    builder << attrs.toJson();
    UNIT_ASSERT_STRINGS_EQUAL(builder.str(), result);
}

} // Y_UNIT_TEST_SUITE(feedback_attributes)

} // namespace maps::wiki::social::feedback::tests
