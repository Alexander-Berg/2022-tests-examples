#include <maps/wikimap/mapspro/libs/rubrics/include/mapping_utils.h>

#include <boost/test/unit_test.hpp>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/env.h>

using namespace maps::wiki;
using namespace maps::wiki::rubrics;

BOOST_AUTO_TEST_CASE(mapping_utils_read_write_export_mapping)
{
    auto current = loadExportPoiMapping(NResource::Find(RUBRICS_MAPPING_CONFIG_RESOURCE_ID));
    BOOST_CHECK_NO_THROW(writeExportMapping(current, "rubrics_mapping_out.xml"));
    auto reRead = readExportPoiMapping("rubrics_mapping_out.xml");
    BOOST_CHECK(current == reRead);
}

/*
// Temporary solution
// To use these tests to generate new merged mapping file
// put backa's rubrics2.xml in to tests directory
// then copy resulting tests/rubrics_mapping_merged_out.xml
// to misc/rubrics_mapping.xml

BOOST_AUTO_TEST_CASE(mapping_utils_read_write_backa_mapping)
{
    auto current = readBackaMapping("tests/rubrics2.xml");
    BOOST_CHECK_NO_THROW(writeExportMapping(current, "tests/rubrics_mapping_new_out.xml"));
    auto reRead = readExportPoiMapping("tests/rubrics_mapping_new_out.xml");
    BOOST_CHECK(current == reRead);
}

BOOST_AUTO_TEST_CASE(mapping_utils_merge_mapping)
{
    auto currentExport = loadExportPoiMapping(NResource::Find(rubrics::RUBRICS_MAPPING_CONFIG_RESOURCE_ID));
    auto currentBacka = readBackaMapping("tests/rubrics2.xml");
    for (const auto& rec : currentExport) {
        const auto& ftTypeId = rec.first;
        const auto& mapping = rec.second;
        currentBacka[ftTypeId].insert(mapping.begin(), mapping.end());
    }
    BOOST_CHECK_NO_THROW(writeExportMapping(currentBacka, "tests/rubrics_mapping_merged_out.xml"));
}
*/
