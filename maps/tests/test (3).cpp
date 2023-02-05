#include "../lib/parsing_helpers.h"
#include "../lib/feedback_issue.h"

#include <library/cpp/testing/common/env.h>
#include <maps/libs/log8/include/log8.h>
#include <yandex/maps/wiki/social/feedback/attribute_names.h>
#include <yandex/maps/wiki/social/feedback/description_keys.h>

#include <boost/test/unit_test.hpp>

#include <sstream>
#include <fstream>

namespace maps::wiki::social::feedback {

namespace {

const double EPS = 1.e-7;

struct SetLogLevelFixture
{
    SetLogLevelFixture() {
        maps::log8::setLevel(maps::log8::Level::DEBUG);
    }
};

struct TestFixture : public SetLogLevelFixture
{
};

std::string getText(const std::string &id)
{
    std::ifstream t(SRC_("data/" + id));
    std::stringstream buffer;
    buffer << t.rdbuf();
    return buffer.str();
}

} // anonymous namespace

BOOST_FIXTURE_TEST_CASE(type_detection, TestFixture)
{
    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Неправильное название", {}),
        Type::Address);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Неправильна назва", {}),
        Type::Address);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Wrong place name", {}),
        Type::Address);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Я знаю кращий маршрут", {}),
        Type::PedestrianRoute);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Неверное положение метки", {}),
        Type::Building);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Я знаю маршрут лучше", {}),
        Type::PedestrianRoute);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Маршрут построен неправильно", {}),
        Type::PedestrianRoute);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions("Нет на карте", {"raw_descr"}),
        Type::Other);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions(
            "Нужно добавить объект на карту",
            {"На карте отсутствует шлагбаум"}
        ),
        Type::Barrier);

    BOOST_CHECK_EQUAL(
        typeFromSummaryAndRawDescriptions(
            "Object missing from map",
            {"На карте отсутствует подъезд номер 352", "other_line"}
        ),
        Type::Entrance);

    BOOST_CHECK_THROW(
        typeFromSummaryAndRawDescriptions("", {}),
        IssueParseError);

    BOOST_CHECK_THROW(
        typeFromSummaryAndRawDescriptions("Неправильное названиеe", {}),
        IssueParseError);

    BOOST_CHECK_THROW(
        typeFromSummaryAndRawDescriptions("Abra-kadabra", {}),
        IssueParseError);
}

BOOST_FIXTURE_TEST_CASE(fb_descr, TestFixture)
{
    {
        auto descr = fbDescrFromRawDescrs({"line1", "line2"});
        BOOST_REQUIRE(descr.isNonTranslatable());
        BOOST_REQUIRE_EQUAL(descr.asNonTranslatable(), "line1\nline2");
    }
    {
        auto descr = fbDescrFromRawDescrs(
            {"line1", "На карте отсутствует шлагбаум"});

        BOOST_REQUIRE(descr.isTranslatable());

        auto descrI18n = descr.asTranslatable();
        const auto& i18nKey = descrI18n.i18nKey();
        const auto& i18nParams = descrI18n.i18nParams();

        BOOST_REQUIRE_EQUAL(i18nKey, tanker::fb_desc::ABSENT_BARRIER_KEY);
        BOOST_REQUIRE(i18nParams.empty());
    }
    {
        auto descr = fbDescrFromRawDescrs(
            {"line1", "На карте отсутствует подъезд номер 352"});

        BOOST_REQUIRE(descr.isTranslatable());

        auto descrI18n = descr.asTranslatable();
        const auto& i18nKey = descrI18n.i18nKey();
        const auto& i18nParams = descrI18n.i18nParams();

        BOOST_REQUIRE_EQUAL(i18nKey, tanker::fb_desc::ABSENT_ENTRANCE_KEY);

        BOOST_REQUIRE(i18nParams.count(tanker::ENTRANCE_NAME));
        BOOST_REQUIRE(i18nParams.at(tanker::ENTRANCE_NAME).isNonTranslatable());
        BOOST_REQUIRE_EQUAL(
            i18nParams.at(tanker::ENTRANCE_NAME).asNonTranslatable(),
            "352"
        );
    }
}

BOOST_FIXTURE_TEST_CASE(remove_leading_tag, TestFixture)
{
    {
        std::string str = "tat";
        removeLeadingTag(str);
        BOOST_CHECK_EQUAL(str, "tat");
    }
    {
        std::string str = "";
        removeLeadingTag(str);
        BOOST_CHECK_EQUAL(str, "");
    }
    {
        std::string str = " [tag] ";
        removeLeadingTag(str);
        BOOST_CHECK_EQUAL(str, "");
    }
    {
        std::string str = "[maps ] body";
        removeLeadingTag(str);
        BOOST_CHECK_EQUAL(str, "body");
    }
    {
        std::string str = "bo[m]dy";
        removeLeadingTag(str);
        BOOST_CHECK_EQUAL(str, "bo[m]dy");
    }
}

BOOST_FIXTURE_TEST_CASE(split_method, TestFixture)
{
    {
        auto splitted = split("a,,b", ",");
        BOOST_REQUIRE(splitted.size() == 2);
        BOOST_CHECK_EQUAL(splitted[0], "a");
        BOOST_CHECK_EQUAL(splitted[1], "b");
    }
    {
        auto splitted = split(" ,  ", ",");
        BOOST_REQUIRE(splitted.size() == 2);
        BOOST_CHECK_EQUAL(splitted[0], " ");
        BOOST_CHECK_EQUAL(splitted[1], "  ");
    }
}

BOOST_FIXTURE_TEST_CASE(as_key_value, TestFixture)
{
    {
        auto kv = asKeyValue("so:what");
        BOOST_REQUIRE(kv);
        BOOST_CHECK_EQUAL(kv->first, "so");
        BOOST_CHECK_EQUAL(kv->second, "what");
    }
    {
        auto kv = asKeyValue("so: what");
        BOOST_REQUIRE(kv);
        BOOST_CHECK_EQUAL(kv->first, "so");
        BOOST_CHECK_EQUAL(kv->second, "what");
    }
    {
        auto kv = asKeyValue("coords: 54, 43");
        BOOST_REQUIRE(kv);
        BOOST_CHECK_EQUAL(kv->first, "coords");
        BOOST_CHECK_EQUAL(kv->second, "54, 43");
    }
    {
        auto kv = asKeyValue(" this:that");
        BOOST_CHECK(!kv);
    }
    {
        auto kv = asKeyValue("https://google.com");
        BOOST_CHECK(!kv);
    }
}

BOOST_FIXTURE_TEST_CASE(extract_key_values, TestFixture)
{
    auto res = extractKeyValues({"raw descr", "k:v"});
    BOOST_REQUIRE(res.size() == 1);
    BOOST_CHECK((res.front() == KeyValue{"k","v"}));
}

BOOST_FIXTURE_TEST_CASE(extract_raw_descriptions, TestFixture)
{
    auto res = extractRawDescriptions({"raw descr", "k:v"});
    BOOST_REQUIRE(res.size() == 1);
    BOOST_CHECK((res.front(), "raw descr"));
}

BOOST_FIXTURE_TEST_CASE(extract_attrs_from_raw_descrs, TestFixture)
{
    auto attributes = extractAttrsFromRawDescriptions(
        {"line1", "На карте отсутствует подъезд номер 352"}
    );
    BOOST_REQUIRE(attributes.count(attrs::ENTRANCE_NAME));

    BOOST_REQUIRE_EQUAL(
        attributes.at(attrs::ENTRANCE_NAME), "352");
}

BOOST_FIXTURE_TEST_CASE(remove_leading_blanks, TestFixture)
{
    auto res = removeLeadingBlanks({"line1", "  line2", "   "});
    BOOST_REQUIRE(res.size() == 2);
    BOOST_CHECK((res[0], "line1"));
    BOOST_CHECK((res[0], "line2"));
}


BOOST_FIXTURE_TEST_CASE(test_issue_parser, TestFixture)
{
    {
        FeedbackIssue fbIssue(
                "[maps] Неправильное название",
                getText("descr_valid00.txt"));

        const auto& descr = fbIssue.description();
        BOOST_CHECK(descr.isNonTranslatable());
        BOOST_CHECK_EQUAL(descr.asNonTranslatable(), "Дубравная улица, 8");

        BOOST_CHECK_EQUAL(fbIssue.type(), Type::Address);

        BOOST_CHECK_CLOSE(fbIssue.coord().x(), 1912461.3934225566, EPS);
        BOOST_CHECK_CLOSE(fbIssue.coord().y(), 2966353.7585294121, EPS);

        const auto& attrs = fbIssue.attrs();
        BOOST_REQUIRE_EQUAL(attrs.size(), 3);

        BOOST_REQUIRE(attrs.count("permalink"));
        BOOST_CHECK_EQUAL(attrs.at("permalink"), "https://yandex.ru/maps/?clid=2186617&win=266&ll=36.179968%2C56.886547&z=19&l=sat%2Cskl%2Cstv%2Csta&ncrnd=1676&mode=whatshere&whatshere%5Bpoint%5D=17.179933%2C25.886279&whatshere%5Bzoom%5D=19");

        BOOST_REQUIRE(attrs.count("link"));
        BOOST_CHECK_EQUAL(attrs.at("link"), "https://yandex.ru/maps/?ll=37.17993313128281%2C55.8862786709722&z=19&pt=37.17993313128281%2C55.8862786709722%2Cpmrd");

        BOOST_REQUIRE(attrs.count("nmaps_link"));
        BOOST_CHECK_EQUAL(attrs.at("nmaps_link"), "https://n.maps.yandex.ru/#!/objects/1826423411");

        BOOST_REQUIRE(fbIssue.objectId());
        BOOST_CHECK_EQUAL(*fbIssue.objectId(), 1826423411);
    }
    {
        FeedbackIssue fbIssue(
                "[map s] Remove object from map",
                getText("descr_valid01.txt"));
        BOOST_CHECK_EQUAL(fbIssue.type(), Type::Other);
        BOOST_CHECK_CLOSE(fbIssue.coord().x(), 3359614.7883494487, EPS);
        BOOST_CHECK_CLOSE(fbIssue.coord().y(), 6568028.797495245, EPS);
    }
}

BOOST_FIXTURE_TEST_CASE(entrance_issue_parser, TestFixture)
{
    FeedbackIssue fbIssue(
        "[maps] Нет на карте",
        getText("descr_entrance.txt"));

    BOOST_CHECK_EQUAL(fbIssue.type(), Type::Entrance);

    const auto& attrs = fbIssue.attrs();
    BOOST_REQUIRE_EQUAL(attrs.size(), 1);

    BOOST_REQUIRE(attrs.count("entranceName"));
    BOOST_CHECK_EQUAL(attrs.at("entranceName"), "35");
}

BOOST_FIXTURE_TEST_CASE(barrier_issue_parser, TestFixture)
{
    FeedbackIssue fbIssue(
        "[maps] Нет объекта на карте",
        getText("descr_barrier.txt"));

    BOOST_CHECK_EQUAL(fbIssue.type(), Type::Barrier);

    BOOST_CHECK(fbIssue.attrs().empty());
}

BOOST_FIXTURE_TEST_CASE(other_issue_parser, TestFixture)
{
    FeedbackIssue fbIssue(
        "[maps] Нет объекта на карте",
        getText("descr_other.txt"));

    BOOST_CHECK_EQUAL(fbIssue.type(), Type::Other);
    BOOST_CHECK(fbIssue.attrs().empty());
}

BOOST_FIXTURE_TEST_CASE(test_issue_parser_errors, TestFixture)
{
    BOOST_CHECK_THROW(FeedbackIssue fbIssue(
            "[maps] Issue: Неправильное название",
            getText("descr_valid00.txt")), IssueParseError);

    BOOST_CHECK_THROW(FeedbackIssue fbIssue(
            "[maps] Неправильное название",
            getText("descr_error00.txt")), IssueParseError);

    BOOST_CHECK_THROW(FeedbackIssue fbIssue(
            "[maps] Неправильное название",
            getText("descr_error01.txt")), IssueParseError);

    BOOST_CHECK_THROW(FeedbackIssue fbIssue(
            "[maps] Неправильное название",
            getText("descr_error02.txt")), IssueParseError);
}

BOOST_FIXTURE_TEST_CASE(test_issue_parser_clean_email, TestFixture)
{
    FeedbackIssue fbIssue(
            "[maps] Неправильное название",
            getText("descr_valid02.txt"));

    const auto& descr = fbIssue.description();
    BOOST_CHECK(descr.isNonTranslatable());
    BOOST_CHECK_EQUAL(descr.asNonTranslatable(), "Я, тут живу. в 54 доме.");
}

BOOST_FIXTURE_TEST_CASE(test_issue_parser_url_in_desc, TestFixture)
{
    FeedbackIssue fbIssue(
            "[maps] Неправильное название",
            getText("descr_valid03.txt"));

    const auto& descr = fbIssue.description();
    BOOST_CHECK(descr.isNonTranslatable());
    BOOST_CHECK_EQUAL(descr.asNonTranslatable(), "http://yandex.ru");
}

} // namespace maps::wiki::social::feedback
