#define BOOST_TEST_MAIN

#include <maps/libs/json/include/value.h>
#include <maps/libs/yt/common/include/json.h>

#include <boost/test/unit_test.hpp>

#include <map>
#include <vector>
#include <sstream>

#include <boost/tuple/tuple.hpp>
#include <boost/tuple/tuple_comparison.hpp>
#include <boost/tuple/tuple_io.hpp>


namespace json = maps::yt::format::json;

BOOST_AUTO_TEST_CASE(record)
{
    typedef boost::tuple<std::string, int, double> Tuple;

    json::Record record;
    BOOST_REQUIRE(!record);
    record.set("A", 42);
    record.set("B", "string with spaces");
    record.set("C", true);
    record.set("D", 4.2);
    auto tuple = Tuple("string", 17, 3.14);
    record.set("E", tuple);
    auto jsonValue = maps::json::Value{27};
    record.set("F", jsonValue);
    record.set("G", "32");
    BOOST_REQUIRE(record);
    BOOST_CHECK_THROW(record.get("Z"), maps::Exception);
    BOOST_CHECK_EQUAL(record.get<int>("A"), 42);
    BOOST_CHECK_CLOSE(record.get<double>("A"), 42.0, 1e-5);
    BOOST_CHECK_EQUAL(record.get("A"), "42");
    BOOST_CHECK_EQUAL(record.get("B"), "string with spaces");
    BOOST_CHECK_EQUAL(record.get<bool>("C"), true);
    BOOST_CHECK_EQUAL(record.get<double>("D"), 4.2);
    BOOST_CHECK_EQUAL(record.get<Tuple>("E"), tuple);
    BOOST_CHECK_EQUAL(record.get<int>("F"), 27);
    BOOST_CHECK_EQUAL(record.getObject("F").as<int>(), 27);
    BOOST_CHECK_NO_THROW(record.get<int>("G"));
    BOOST_CHECK_EQUAL(record.get<int>("G"), 32);
    BOOST_CHECK(record.hasColumn("A"));
    BOOST_CHECK(!record.hasColumn("nonexistent"));
}

BOOST_AUTO_TEST_CASE(record_set_erase)
{
    json::Record record;
    record.set("A", "1");
    BOOST_CHECK_EQUAL(record.get("A"), "1");
    record.set("A", "2");
    BOOST_CHECK_EQUAL(record.get("A"), "2");
    BOOST_CHECK_NO_THROW(record.erase("A"));
    BOOST_CHECK_THROW(record.erase("A"), maps::Exception);
}

BOOST_AUTO_TEST_CASE(reader)
{
    const std::string TABLE = R"({"A":1,"B":"two","C":3.4}
{"D":5,"E":6.7,"F":"eight"}
)";

    std::istringstream iss(TABLE);
    json::Reader reader(iss);

    BOOST_CHECK_EQUAL(reader.peek().get<int>("A"), 1);
    BOOST_CHECK_EQUAL(reader.peek().get("B"), "two");
    BOOST_CHECK_EQUAL(reader.peek().get<double>("C"), 3.4);
    reader.read();
    BOOST_CHECK_EQUAL(reader.peek().get<int>("D"), 5);
    BOOST_CHECK_EQUAL(reader.peek().get<double>("E"), 6.7);
    BOOST_CHECK_EQUAL(reader.peek().get("F"), "eight");
}

BOOST_AUTO_TEST_CASE(writer)
{
    const std::string& serializedRecord =
R"({"A":42,"B":"string","C":true,"D":4.2}
)";
    {
        std::ostringstream oss;
        json::Writer writer(oss);
        json::Record record;
        record.set("A", 42);
        record.set("B", "string");
        record.set("C", true);
        record.set("D", 4.2);
        writer.write(record);
        BOOST_CHECK_EQUAL(oss.str(), serializedRecord);
    }
    {
        std::ostringstream oss;
        json::Writer writer(oss);
        writer.write(
            std::make_pair("A", 42),
            std::make_pair("B", "string"),
            std::make_pair("C", true),
            std::make_pair("D", 4.2));
        BOOST_CHECK_EQUAL(oss.str(), serializedRecord);
    }
}

BOOST_AUTO_TEST_CASE(record_iterator)
{
    const std::string TABLE = R"({"A":1,"B":"two","C":3.4}
{"D":5,"E":6.7,"F":"eight"}
)";

    std::istringstream iss(TABLE);
    json::Reader reader(iss);
    auto itRecord = reader.recordIterator();
    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get<int>("A"), 1);
    BOOST_REQUIRE(++itRecord);
    BOOST_CHECK_EQUAL(itRecord->get("F"), "eight");
    BOOST_REQUIRE(!++itRecord);
}

BOOST_AUTO_TEST_CASE(key_iterator)
{
    const std::string TABLE =
R"({"A": 1, "B": "one", "C": 1.1}
{"A": 1, "B": "one", "C": 2.2}
{"A": 2, "B": "two", "C": 3.3}
)";

    std::istringstream iss(TABLE);
    json::Reader reader(iss);
    auto itKey = reader.keyIterator({"A", "B"});
    BOOST_REQUIRE(itKey);
    BOOST_CHECK_EQUAL(itKey->get<int>("A"), 1);
    BOOST_CHECK_EQUAL(itKey->get("B"), "one");
    auto itRecord = itKey.recordIterator();
    BOOST_REQUIRE(itRecord);
    BOOST_CHECK_EQUAL(itRecord->get<double>("C"), 1.1);
    BOOST_REQUIRE(++itRecord);
    BOOST_CHECK_EQUAL(itRecord->get<double>("C"), 2.2);
    BOOST_REQUIRE(!++itRecord);
    ++itKey;
    BOOST_CHECK_EQUAL(itKey->get<int>("A"), 2);
    BOOST_CHECK_EQUAL(itKey->get("B"), "two");
    BOOST_REQUIRE(itRecord = itKey.recordIterator());
    BOOST_CHECK_EQUAL(itRecord->get<double>("C"), 3.3);
    BOOST_REQUIRE(!++itRecord);
    BOOST_REQUIRE(!++itKey);
}


BOOST_AUTO_TEST_CASE(subkey_iterator)
{
    const std::string TABLE =
            R"({"A": 1, "B": "one", "C": 1}
               {"A": 1, "B": "one", "C": 2}
               {"A": 1, "B": "two", "C": 3}
               {"A": 2, "B": "two", "C": 4})";

    std::istringstream iss(TABLE);
    json::Reader reader(iss);
    auto itKey = reader.keyIterator({"A"});
    BOOST_REQUIRE(itKey);
    BOOST_CHECK_EQUAL(itKey->get<int>("A"), 1);

    auto itSubkey = itKey.keyIterator({"B"});

    BOOST_REQUIRE(itSubkey);
    BOOST_CHECK_EQUAL(itSubkey->get("B"), "one");

    {
        auto itRecord = itSubkey.recordIterator();
        BOOST_REQUIRE(itRecord);
        BOOST_CHECK_EQUAL(itRecord->get<int>("C"), 1);
        BOOST_REQUIRE(++itRecord);
        BOOST_CHECK_EQUAL(itRecord->get<int>("C"), 2);
        BOOST_REQUIRE(!++itRecord);
    }

    BOOST_REQUIRE(++itSubkey);
    BOOST_CHECK_EQUAL(itSubkey->get("B"), "two");

    {
        auto itRecord = itSubkey.recordIterator();
        BOOST_REQUIRE(itRecord);
        BOOST_CHECK_EQUAL(itRecord->get<int>("C"), 3);
        BOOST_REQUIRE(!++itRecord);
    }

    BOOST_REQUIRE(!++itSubkey);

    BOOST_REQUIRE(++itKey);
    BOOST_CHECK_EQUAL(itKey->get<int>("A"), 2);

    itSubkey = itKey.keyIterator({"B"});

    BOOST_CHECK_EQUAL(itSubkey->get("B"), "two");
    BOOST_REQUIRE(!++itSubkey);
    BOOST_REQUIRE(!++itKey);
}


BOOST_AUTO_TEST_CASE(switch_table)
{
    const std::string TABLE =
R"({"A":1,"B":"first"}
{"$value":null,"$attributes":{"table_index":1}}
{"A":1,"B":"second"}
{"A":2,"B":"third"}
{"$value":null,"$attributes":{"table_index":2}}
{"A":2,"B":"fourth"}
)";
    {
        std::istringstream iss(TABLE);
        json::Reader reader(iss);
        auto itKey = reader.keyIterator({"A"});

        auto itRecord = itKey.recordIterator();
        BOOST_CHECK_EQUAL(itRecord.tableIndex(), 0);
        BOOST_CHECK_EQUAL(itRecord->get("B"), "first");
        BOOST_REQUIRE(++itRecord);
        BOOST_CHECK_EQUAL(itRecord.tableIndex(), 1);
        BOOST_CHECK_EQUAL(itRecord->get("B"), "second");
        BOOST_REQUIRE(!++itRecord);
        BOOST_REQUIRE(++itKey);
        BOOST_REQUIRE(itRecord = itKey.recordIterator());
        BOOST_CHECK_EQUAL(itRecord.tableIndex(), 1);
        BOOST_CHECK_EQUAL(itRecord->get("B"), "third");
        BOOST_REQUIRE(++itRecord);
        BOOST_CHECK_EQUAL(itRecord.tableIndex(), 2);
        BOOST_CHECK_EQUAL(itRecord->get("B"), "fourth");
    }
    {
        std::ostringstream oss;
        json::Writer writer(oss);
        writer.write(
            std::make_pair("A", 1),
            std::make_pair("B", "first"));
        writer.switchTable(1);
        writer.write(
            std::make_pair("A", 1),
            std::make_pair("B", "second"));
        writer.write(
            std::make_pair("A", 2),
            std::make_pair("B", "third"));
        writer.switchTable(2);
        writer.write(
            std::make_pair("A", 2),
            std::make_pair("B", "fourth"));
        BOOST_CHECK_EQUAL(oss.str(), TABLE);
    }
}

BOOST_AUTO_TEST_CASE(row_index)
{
    const std::string TABLE =
R"({"$value":null,"$attributes":{"table_index":1}}
{"$value":null,"$attributes":{"row_index":256}}
{"A":"one"}
{"A":"two"}
{"$value":null,"$attributes":{"table_index":2}}
{"$value":null,"$attributes":{"row_index":512}}
{"A":"three"}
{"A":"four"}
)";
    std::istringstream iss(TABLE);
    json::Reader reader(iss);
    auto itRecord = reader.recordIterator();
    BOOST_CHECK_EQUAL(itRecord.tableIndex(), 1);
    BOOST_CHECK(itRecord.hasRowIndex());
    BOOST_CHECK_EQUAL(itRecord.rowIndex(), 256);
    BOOST_CHECK_EQUAL(itRecord->get("A"), "one");
    BOOST_REQUIRE(++itRecord);
    BOOST_CHECK_EQUAL(itRecord.tableIndex(), 1);
    BOOST_CHECK(itRecord.hasRowIndex());
    BOOST_CHECK_EQUAL(itRecord.rowIndex(), 257);
    BOOST_CHECK_EQUAL(itRecord->get("A"), "two");
    BOOST_REQUIRE(++itRecord);
    BOOST_CHECK_EQUAL(itRecord.tableIndex(), 2);
    BOOST_CHECK(itRecord.hasRowIndex());
    BOOST_CHECK_EQUAL(itRecord.rowIndex(), 512);
    BOOST_CHECK_EQUAL(itRecord->get("A"), "three");
    BOOST_REQUIRE(++itRecord);
    BOOST_CHECK_EQUAL(itRecord.tableIndex(), 2);
    BOOST_CHECK(itRecord.hasRowIndex());
    BOOST_CHECK_EQUAL(itRecord.rowIndex(), 513);
    BOOST_CHECK_EQUAL(itRecord->get("A"), "four");
    BOOST_REQUIRE(!++itRecord);
}

BOOST_AUTO_TEST_CASE(parse_columns)
{
    const auto columns = json::parseColumns("first,second,third");
    BOOST_CHECK_EQUAL(columns.size(), 3);
    BOOST_CHECK_EQUAL(columns[0], "first");
    BOOST_CHECK_EQUAL(columns[1], "second");
    BOOST_CHECK_EQUAL(columns[2], "third");
}

BOOST_AUTO_TEST_CASE(big_doubles)
{
    // ensures, that serialized double will contain 'e' or '.' so that YT will parse it as double
    std::vector<double> big_doubles = {
        122333444455555666666777777788888888999999999.0,
        -7172945302416810921850568704.0
    };

    for (auto d : big_doubles) {
        json::Record rec;
        rec.set("value", d);

        std::ostringstream ostr;
        json::Writer w { ostr };

        w.write(rec);

        BOOST_CHECK(ostr.str().find_first_of("e.") != std::string::npos);
    }
}
