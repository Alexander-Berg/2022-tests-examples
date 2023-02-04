#include <library/cpp/xml/document/xml-document.h>
#include <maps/analyzer/libs/calendar/include/fb/builder.h>
#include <maps/analyzer/libs/calendar/tools/builder/lib/include/tools.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/generic/string.h>
#include <util/stream/file.h>

#include <string>
#include <sstream>
#include <fstream>

const std::string TEST_DATA_ROOT = "maps/analyzer/libs/calendar/tests/data/";
const std::string VERSION = "test";

namespace calendar = maps::analyzer::calendar;

Y_UNIT_TEST_SUITE(CalendarTest) {
    Y_UNIT_TEST(BuildFlatbuffer) {
        calendar::flat_buffers::CalendarStorageBuilder builder {
            VERSION,
            2
        };

        for (const auto& file: {
            BinaryPath(TEST_DATA_ROOT + "region149.xml"),
            BinaryPath(TEST_DATA_ROOT + "region225.xml")
        }) {
            const NXml::TDocument xml{TString{file}, NXml::TDocument::File};
            calendar::tools::processXml(xml, &builder);
        }

        const auto fbPath = BinaryPath(TEST_DATA_ROOT + "calendar.fb");
        TFileInput inputEtalonData(fbPath);

        const TString etalonData = inputEtalonData.ReadAll();

        std::stringstream ss;
        builder.flushToStream(ss);

        const TString resultData{ss.str()};

        // we don't really want to output
        // TString contents here if they differ,
        // so we use EXPECT_TRUE, not EXPECT_EQ
        EXPECT_TRUE(resultData == etalonData);
    }
}
