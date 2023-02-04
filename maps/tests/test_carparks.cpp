#include <yandex/maps/carparks/common2/carpark_info.h>
#include <yandex/maps/carparks/common2/carpark_info_collection.h>
#include <yandex/maps/mms/holder2.h>

#include <boost/lexical_cast.hpp>
#include <boost/test/unit_test.hpp>

#include <filesystem>
#include <fstream>

using namespace maps::carparks::common2;

BOOST_AUTO_TEST_SUITE(CarparksCommonSuite)

BOOST_AUTO_TEST_CASE(test_carpark_info)
{
    std::filesystem::create_directory("genfiles");
    CarparkInfo<mms::Standalone> standalone(
        123,
        CarparkType::Toll,
        "RU",
        "abc",
        "5 $",
        {0, 6});

    BOOST_CHECK_EQUAL(standalone.id(), 123);
    BOOST_CHECK_EQUAL(standalone.type(), CarparkType::Toll);
    BOOST_CHECK_EQUAL(standalone.isocode(), "RU");
    BOOST_CHECK_EQUAL(standalone.orgId(), "abc");
    BOOST_CHECK_EQUAL(standalone.price(), "5 $");
    auto expectedTags = std::vector<int>{0, 6};
    BOOST_CHECK_EQUAL_COLLECTIONS(
        standalone.tags().begin(), standalone.tags().end(),
        expectedTags.begin(), expectedTags.end());
    std::string path = "genfiles/test_carpark_info.mms";
    std::ofstream file(path);
    mms::write(file, standalone);
    file.close();
    mms::Holder2<CarparkInfo<mms::Mmapped>> mmapped(path);
    BOOST_CHECK_EQUAL(standalone, *mmapped);

    CarparkInfo<mms::Standalone> standalone2(standalone);

    BOOST_CHECK_EQUAL(standalone, standalone2);

    CarparkInfo<mms::Standalone> standaloneFromMmaped(*mmapped);
    BOOST_CHECK_EQUAL(standalone, standaloneFromMmaped);
}

BOOST_AUTO_TEST_CASE(test_type_to_string)
{
    BOOST_CHECK_EQUAL(
        boost::lexical_cast<std::string>(CarparkType::ParkAndRide),
        "parkandride");
    BOOST_CHECK_EQUAL(
        boost::lexical_cast<CarparkType>("toll"),
        CarparkType::Toll);
    BOOST_CHECK_EQUAL(
        toString(CarparkType::FreeBld),
        "free-bld");
}

BOOST_AUTO_TEST_CASE(test_info_collection)
{
    std::filesystem::create_directory("genfiles");
    CarparkInfoCollection<mms::Standalone> standalone;
    standalone.emplace(111, CarparkInfo<mms::Standalone>(
        111,
        CarparkType::Prohibited,
        "RU",
        "CXI",
        "8 rub",
        {0, 6}));
    standalone.emplace(222, CarparkInfo<mms::Standalone>(
        222,
        CarparkType::Free,
        "TR",
        "CCXXII",
        "7 euro",
        {1, 6}));
    BOOST_CHECK_EQUAL(2, standalone.size());

    std::string path = "genfiles/test_carpark_info_collection.mms";
    std::ofstream file(path);
    mms::write(file, standalone);
    file.close();
    CarparkInfoCollectionHolder mmapped(path);
    BOOST_CHECK_EQUAL(2, mmapped->size());
    BOOST_CHECK_EQUAL(standalone.at(111), mmapped->at(111));
    BOOST_CHECK_EQUAL(standalone.at(222), mmapped->at(222));
}

BOOST_AUTO_TEST_SUITE_END()
