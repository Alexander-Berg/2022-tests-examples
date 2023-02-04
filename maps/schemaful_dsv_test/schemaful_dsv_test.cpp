#define BOOST_TEST_MAIN

#include <maps/libs/yt/common/include/schemaful_dsv.h>

#include <boost/test/unit_test.hpp>

#include <sstream>


using namespace maps::yt::format::schemaful_dsv;


BOOST_AUTO_TEST_CASE(record)
{
    Record empty;
    BOOST_CHECK(empty.empty());

    auto record = Record::fromList(1, 1.23456789, "a");
    BOOST_CHECK(!record.empty());
    BOOST_CHECK(record.size() == 3);
    BOOST_CHECK(record.hasColumn(0));
    BOOST_CHECK(!record.hasColumn(99));
    BOOST_CHECK_EQUAL(record.get<int>(0), 1);
    BOOST_CHECK_EQUAL(record.get<double>(1), 1.23456789);
    BOOST_CHECK_EQUAL(record.get<std::string>(2), "a");
    BOOST_CHECK_THROW(record.get(99), maps::Exception);
}


BOOST_AUTO_TEST_CASE(reader)
{
    std::istringstream iss("1\ta\\\\ta\n2\tb\\nb\n3\tc\\\\c");
    Reader reader(iss);

    BOOST_CHECK_EQUAL(reader.peek().get(0), "1");
    BOOST_CHECK_EQUAL(reader.peek().get(1), "a\\ta");

    BOOST_REQUIRE(!reader.read().empty());
    BOOST_CHECK_EQUAL(reader.peek().get(0), "2");
    BOOST_CHECK_EQUAL(reader.peek().get(1), "b\nb");

    BOOST_REQUIRE(!reader.read().empty());
    BOOST_CHECK_EQUAL(reader.peek().get(0), "3");
    BOOST_CHECK_EQUAL(reader.peek().get(1), "c\\c");

    BOOST_REQUIRE(reader.read().empty());
    BOOST_REQUIRE(reader.peek().empty());
}


BOOST_AUTO_TEST_CASE(writer)
{
    std::ostringstream oss;
    Writer writer(oss);
    writer.write(1, "a\ta");
    writer.write(2, "b\nb");
    writer.write(3, "c\\c");
    BOOST_CHECK_EQUAL(oss.str(), "1\ta\\ta\n2\tb\\nb\n3\tc\\\\c\n");
}


BOOST_AUTO_TEST_CASE(record_iterator)
{
    std::istringstream iss("a\nb");
    Reader reader(iss);
    auto itRecord = reader.recordIterator();

    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get(0), "a");

    ++itRecord;
    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get(0), "b");

    ++itRecord;
    BOOST_REQUIRE(!itRecord);
}


BOOST_AUTO_TEST_CASE(key_iterator)
{
    std::istringstream iss(
        "a1\tb1\tc1\n"
        "a1\tb1\tc2\n"
        "a1\tb2\tc3\n"
        "a1\tb2\tc4\n"
    );
    Reader reader(iss);
    auto itKey = reader.keyIterator({0, 1});

    BOOST_REQUIRE(itKey);
    BOOST_CHECK_EQUAL(itKey->get(0), "a1");
    BOOST_CHECK_EQUAL(itKey->get(1), "b1");
    BOOST_CHECK_THROW(itKey->get(2), maps::Exception);

    ++itKey;
    BOOST_REQUIRE(itKey);
    BOOST_CHECK_EQUAL(itKey->get(0), "a1");
    BOOST_CHECK_EQUAL(itKey->get(1), "b2");

    ++itKey;
    BOOST_REQUIRE(!itKey);
}


BOOST_AUTO_TEST_CASE(key_record_iterator)
{
    std::istringstream iss(
        "a1\tb1\tc1\n"
        "a1\tb1\tc2\n"
        "a1\tb2\tc3\n"
    );
    Reader reader(iss);

    auto itKey = reader.keyIterator({0, 1});

    BOOST_REQUIRE(itKey);
    BOOST_CHECK_EQUAL(itKey->get(0), "a1");
    BOOST_CHECK_EQUAL(itKey->get(1), "b1");

    auto itRecord = itKey.recordIterator();
    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get(0), "a1");
    BOOST_CHECK_EQUAL(itRecord->get(1), "b1");
    BOOST_CHECK_EQUAL(itRecord->get(2), "c1");

    ++itRecord;
    BOOST_REQUIRE(itKey);
    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get(0), "a1");
    BOOST_CHECK_EQUAL(itRecord->get(1), "b1");
    BOOST_CHECK_EQUAL(itRecord->get(2), "c2");

    ++itRecord;
    BOOST_REQUIRE(itKey);
    BOOST_REQUIRE(!itRecord);

    ++itKey;
    BOOST_REQUIRE(itKey);
    BOOST_CHECK_EQUAL(itKey->get(0), "a1");
    BOOST_CHECK_EQUAL(itKey->get(1), "b2");

    itRecord = itKey.recordIterator();
    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get(0), "a1");
    BOOST_CHECK_EQUAL(itRecord->get(1), "b2");
    BOOST_CHECK_EQUAL(itRecord->get(2), "c3");

    ++itRecord;
    BOOST_REQUIRE(!itKey);
    BOOST_REQUIRE(!itRecord);
}
