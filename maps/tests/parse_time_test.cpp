#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/parse_time/include/parse_time.h>

#include <sstream>


namespace mp = maps::analyzer::parse_time;
namespace me = maps::enum_io;

using mp::formats::ISO;
using mp::formats::SQL;
using mp::formats::ALZ;


std::optional<std::string> parse(const std::string& s, const std::string& fmt) {
    auto tm = mp::parseTime(s, fmt);
    if (!tm.has_value()) {
        return {};
    }
    return {mp::formatTime(tm.value(), SQL)};
}

std::optional<std::string> parse(const std::string& s) {
    auto tm = mp::parseTime(s);
    if (!tm.has_value()) {
        return {};
    }
    return {mp::formatTime(tm.value(), SQL)};
}

std::optional<std::string> parseNode(const NYT::TNode& node, const mp::TimeType& type) {
    auto tm = mp::parseTime(node, type);
    if (!tm.has_value()) {
        return {};
    }
    return {mp::formatTime(tm.value(), SQL)};
}

std::optional<std::string> interval(const std::string& s) {
    auto dur = mp::parseInterval(s);
    if (!dur.has_value()) {
        return {};
    }
    return mp::formatInterval(dur.value());
}

std::optional<std::pair<std::string, std::string>> parsePartial(const std::string& s, const std::string& fmt) {
    std::istringstream istr(s);
    auto tm = mp::parseTime(istr, fmt);
    if (!tm.has_value()) {
        return {};
    }
    std::string rest;
    std::getline(istr, rest);
    return {{mp::formatTime(tm.value(), SQL), std::move(rest)}};
}

std::optional<std::string> some(std::string s) {
    return {std::move(s)};
}

std::optional<std::pair<std::string, std::string>> some(std::string s, std::string r) {
    return {{std::move(s), std::move(r)}};
}


TEST(ParseTimeTests, SimpleParse) {
    EXPECT_EQ(parse("2021-01-01 12:30:11Z", SQL), some("2021-01-01 12:30:11Z"));
    EXPECT_EQ(parse("2021-01-01T12:30:11Z", ISO), some("2021-01-01 12:30:11Z"));
    EXPECT_EQ(parse("20210101T123011Z", ALZ), some("2021-01-01 12:30:11Z"));
}

TEST(ParseTimeTests, ParseFractional) {
    EXPECT_EQ(parse("2021-01-01 12:30:11.012345", SQL), some("2021-01-01 12:30:11.012345Z"));
    EXPECT_EQ(parse("2021-01-01 12:30:11.012345Z", SQL), some("2021-01-01 12:30:11.012345Z"));
    EXPECT_EQ(parse("2021-01-01 12:30:11.012345+03", SQL), some("2021-01-01 09:30:11.012345Z"));
    EXPECT_EQ(parse("2021-01-01 12:30:11.012345-03", SQL), some("2021-01-01 15:30:11.012345Z"));
}

TEST(ParseTimeTests, ParseOffset) {
    EXPECT_EQ(parse("2021-01-01 00:30:11+01", SQL), some("2020-12-31 23:30:11Z"));
    EXPECT_EQ(parse("2021-01-01T00:30:11+01", ISO), some("2020-12-31 23:30:11Z"));
    EXPECT_EQ(parse("20210101T003011+01", ALZ), some("2020-12-31 23:30:11Z"));
}

TEST(ParseTimeTests, ParsePart) {
    EXPECT_EQ(parse("2021-01-01 00:00"), some("2021-01-01 00:00:00Z"));
    EXPECT_EQ(parse("2021-01-01 00"), some("2021-01-01 00:00:00Z"));
    EXPECT_EQ(parse("2021-01-01"), some("2021-01-01 00:00:00Z"));
    EXPECT_EQ(parse("2021-01"), some("2021-01-01 00:00:00Z"));
    EXPECT_EQ(parse("2021"), some("2021-01-01 00:00:00Z"));
}

TEST(ParseTimeTests, ParseFail) {
    EXPECT_FALSE(parse("2021-01-01 00:30.123456", "%Y-%m-%d %H:%M").has_value());
    EXPECT_FALSE(parse("2021-01-01 00:10:20Zabckasdf").has_value());
    EXPECT_FALSE(parse("2021-01broken").has_value());
    EXPECT_FALSE(parse("2021-01-32").has_value());
}

TEST(ParseTimeTests, ParseAll) {
    EXPECT_EQ(parse("2021-01-01 00:30:11+01"), some("2020-12-31 23:30:11Z"));
    EXPECT_EQ(parse("2021-01-01T00:30:11+01"), some("2020-12-31 23:30:11Z"));
    EXPECT_EQ(parse("20210101T003011+01"), some("2020-12-31 23:30:11Z"));
}

TEST(ParseTimeTests, ParsePartial) {
    EXPECT_EQ(
        parsePartial("2021-01-01 00:30.123456", "%Y-%m-%d %H:%M"),
        some("2021-01-01 00:30:00Z", ".123456")
    );
    EXPECT_EQ(
        parsePartial("2021-01-01 00:30.123456", "%Y-%m-%d %H"),
        some("2021-01-01 00:00:00Z", ":30.123456")
    );
    EXPECT_EQ(
        parsePartial("2021-01-01 00:30:00.123456 and the rest of", "%Y-%m-%d %H:%M:%S"),
        some("2021-01-01 00:30:00.123456Z", " and the rest of")
    );
}

TEST(ParseTimeTests, ParseTimeFromNode) {
    EXPECT_EQ(
        parseNode(NYT::TNode("2021-01-01 12:30:11Z"), mp::TimeType::SQL),
        some("2021-01-01 12:30:11Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode("2021-01-01T12:30:11Z"), mp::TimeType::ISO),
        some("2021-01-01 12:30:11Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode("20210101T123011Z"), mp::TimeType::ALZ),
        some("2021-01-01 12:30:11Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode("20210101T123011Z"), mp::TimeType::String),
        some("2021-01-01 12:30:11Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode(ui64(1650229421)), mp::TimeType::YtDateTime),
        some("2022-04-17 21:03:41Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode(ui64(1650229421000000)), mp::TimeType::YtTimestamp),
        some("2022-04-17 21:03:41Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode(ui64(19099)), mp::TimeType::YtDate),
        some("2022-04-17 00:00:00Z")
    );
    EXPECT_EQ(
        parseNode(NYT::TNode(i64(19099)), mp::TimeType::YtDate),
        some("2022-04-17 00:00:00Z")
    );
    EXPECT_EQ(
        me::fromString<mp::TimeType>("SQL"),
        mp::TimeType::SQL
    );
    EXPECT_EQ(
        me::fromString<mp::TimeType>(("Datetime")),
        mp::TimeType::YtDateTime
    );
    EXPECT_EQ(
        mp::toString(mp::TimeType::YtDateTime),
        "Datetime"
    );
}

TEST(ParseTimeTests, ParseTimeFromNodeFail) {
    EXPECT_FALSE(parseNode(NYT::TNode("2021-01-01 00:10:20Zabckasdf"), mp::TimeType::SQL).has_value());
    EXPECT_FALSE(parseNode(NYT::TNode(ui64(123)), mp::TimeType::SQL).has_value());
    EXPECT_FALSE(parseNode(NYT::TNode(i64(-123)), mp::TimeType::YtDate).has_value());
    EXPECT_FALSE(parseNode(NYT::TNode("20210101T123011Z"), mp::TimeType::YtDate).has_value());
    EXPECT_ANY_THROW(me::fromString<mp::TimeType>("DateTime"));
}

TEST(ParseTimeTests, Format) {
    const auto tm = mp::parseTime(std::string("2021-01-01"));
    ASSERT_TRUE(tm.has_value());
    const auto d = tm.value();

    EXPECT_EQ(mp::formatTime(d, mp::formats::DATE), "2021-01-01");
    EXPECT_EQ(mp::formatTime(d, "%Y%m%d"), "20210101");
    EXPECT_EQ(mp::formatTime(d, "%Y%m%d %H"), "20210101 00");
    EXPECT_EQ(mp::formatTime(d, "%Y%m%d %H%M"), "20210101 0000Z");
    EXPECT_EQ(mp::formatTime(d, "%Y%m%d %H%M%S"), "20210101 000000Z");
}

TEST(ParseTimeTests, FormatToNode) {
    const auto tm = mp::parseTime("2021-01-01 12:30:11Z");
    ASSERT_TRUE(tm.has_value());
    const auto d = tm.value();

    EXPECT_EQ(mp::formatTime(d, mp::TimeType::DATE), NYT::TNode("2021-01-01"));
    EXPECT_EQ(mp::formatTime(d, mp::TimeType::SQL), NYT::TNode("2021-01-01 12:30:11Z"));
    EXPECT_EQ(mp::formatTime(d, mp::TimeType::YtDate), NYT::TNode(ui64(18628)));
    EXPECT_EQ(mp::formatTime(d, mp::TimeType::YtDateTime), NYT::TNode(ui64(1609504211)));
    EXPECT_EQ(mp::formatTime(d, mp::TimeType::YtTimestamp), NYT::TNode(ui64(1609504211000000)));
}

TEST(ParseIntervalTests, ParseInterval) {
    EXPECT_EQ(interval("1"), some("01"));
    EXPECT_EQ(interval("1.12345"), some("01.123450"));
    EXPECT_EQ(interval("0.12345"), some("00.123450"));
    EXPECT_EQ(interval("1:00.12345"), some("01:00.123450"));
    EXPECT_EQ(interval("1:01:00.12345"), some("01:01:00.123450"));
    EXPECT_EQ(interval("1:01:00"), some("01:01:00"));
    EXPECT_FALSE(interval("1Z").has_value());
    EXPECT_FALSE(interval("1+03").has_value());
}

TEST(ConvertTimeTests, ConvertYt) {
    EXPECT_EQ(mp::formatTime(mp::yt::fromDate(10u), "%Y-%m-%d"), "1970-01-11");
    EXPECT_EQ(mp::formatTime(mp::yt::fromDatetime(123u), "%Y-%m-%d %H:%M:%S"), "1970-01-01 00:02:03Z");
    EXPECT_EQ(mp::formatTime(mp::yt::fromTimestamp(12345), "%Y-%m-%d %H:%M:%S"), "1970-01-01 00:00:00.012345Z");
    EXPECT_EQ(mp::yt::fromInterval(1234000000u), mp::Interval{1234000000u});

    EXPECT_EQ(mp::yt::toDate(mp::yt::fromDate(12345u)), 12345u);
    EXPECT_EQ(mp::yt::toDatetime(mp::yt::fromDatetime(12345u)), 12345u);
    EXPECT_EQ(mp::yt::toTimestamp(mp::yt::fromTimestamp(12345u)), 12345u);
    EXPECT_EQ(mp::yt::toInterval(mp::Interval{1234000000u}), 1234000000u);
}
