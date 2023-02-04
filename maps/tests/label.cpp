#include <maps/jams/renderer2/common/create_map/lib/label.h>

#include <maps/libs/jams/router/jams.pb.h>
#include <yandex/maps/jams/router/jams.h>

#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <boost/optional.hpp>

#include <limits>
#include <sstream>

using namespace maps::jams::renderer;
using namespace maps::jams;
namespace jr = maps::jams::router;

const std::vector<std::string> TYPES_WITHOUT_LABEL = {
    "OTHER", "DANGER",  "POLICE", "CHAT", "POLICE_POST",
    "SPEED_CAMERA", "LANE_CAMERA", "FEEDBACK", "ANY"
};

class Fixture : public NUnitTest::TBaseFixture {
public:

    void setNow(time_t now, size_t tzOffset = 0)
    {
        now_ = now;
        tzOffset_ = tzOffset;
    }

    jr::Event getEvent() { return jr::Event(&protoEvent_); }

    void addEvent(const std::string& type)
    {
        jr::proto::Event protoEvent;
        protoEvent.set_type(TString(type));
        protoEvent_ = std::move(protoEvent);
    }

    void addLanes(const std::vector<std::string>& lanes)
    {
        protoEvent_.mutable_lane()->Reserve(lanes.size());
        for (const auto& lane: lanes) {
            protoEvent_.mutable_lane()->Add(TString(lane));
        }
    }

    void addDuration(
        time_t startTime,
        const boost::optional<time_t>& endTime = boost::none)
    {
        protoEvent_.set_start_time(startTime);
        if (endTime) {
            protoEvent_.set_end_time(*endTime);
        }
    }

    void addRegion(size_t region)
    {
         protoEvent_.set_region(region);
    }

    void checkLaneLabelKey(
        const std::vector<std::string>& lanes, const std::string& result)
    {
        for (const auto& type : {"RECONSTRUCTION", "ACCIDENT"}) {
            addEvent(type);
            addLanes(lanes);
            UNIT_ASSERT_EQUAL(labelKey(getEvent(), now_, tzOffset_), result);
        }
    }

    void checkEventsWithoutLabels()
    {
        setNow(1564181400, 10800);
        for (const auto& type : TYPES_WITHOUT_LABEL) {
            addEvent(type);
            addLanes({"left"});
            addDuration(1564181000, 1564182400);
            UNIT_ASSERT_EQUAL(labelKey(getEvent(), now_, tzOffset_), "");
        }
    }

    void checkDurationLabelKey(
        const std::string& result,
        const std::string& type,
        time_t start,
        const boost::optional<time_t>& end = boost::none,
        size_t region = 2)
    {
        addEvent(type);
        addDuration(start, end);
        addRegion(region);
        UNIT_ASSERT_EQUAL(labelKey(getEvent(), now_, tzOffset_), result);
    }

    jr::proto::Event protoEvent_;
    time_t now_ = 0;
    size_t tzOffset_ = 0;
};

Y_UNIT_TEST_SUITE_F(LabelKeyTests, Fixture)
{
    Y_UNIT_TEST(CheckLaneLabelKeys)
    {
        checkLaneLabelKey({}, "");
        checkLaneLabelKey({"left", "right", "middle"}, "");
        checkLaneLabelKey({"bad", "bad_again"}, "");
        checkLaneLabelKey({"left", "right", "bad"}, "left_and_right_lane");
        checkLaneLabelKey({"left"}, "left_lane");
        checkLaneLabelKey({"right"}, "right_lane");
        checkLaneLabelKey({"middle"}, "middle_lane");
        checkLaneLabelKey({"right", "left"}, "left_and_right_lane");
        checkLaneLabelKey({"middle", "left"}, "left_and_middle_lane");
        checkLaneLabelKey({"middle", "right"}, "middle_and_right_lane");
    }


    Y_UNIT_TEST(CheckDurationLabels)
    {
        setNow(1564170000, 10800); // 26.07.2019 22:40 (GMT+3)
        // No end time
        checkDurationLabelKey("", "CLOSED", 1564171000);
        checkDurationLabelKey("", "DRAWBRIDGE", 1564171000);
        // From the past
        checkDurationLabelKey("", "CLOSED", 1564160000, 1564169990);
        checkDurationLabelKey("", "DRAWBRIDGE", 1564160000);
        // World event
        checkDurationLabelKey("", "CLOSED", 1564160000, 1564180000, 10000);
        checkDurationLabelKey("", "DRAWBRIDGE", 1564160000, 1564180000, 10000);
        // Too long
        checkDurationLabelKey("", "DRAWBRIDGE", 1564182000, 1564269000);

        checkDurationLabelKey("until_time", "DRAWBRIDGE", 1564160000, 1564180000);
        checkDurationLabelKey("time", "DRAWBRIDGE", 1564175000, 1564180000);

        // Current events
        // 01.05.2019 - 30.12.2019
        checkDurationLabelKey("until_date", "CLOSED", 1556739600, 1577734800);
        // 01.05.2018 - 30.12.2019
        checkDurationLabelKey("until_date", "CLOSED", 1525203600, 1577734800);
        // 01.05.2019 - 01.01.2020
        checkDurationLabelKey("until_date", "CLOSED", 1556739600, 1577907600);
        // 01.05.2019 - 01.05.2020
        checkDurationLabelKey("until_date_with_year", "CLOSED", 1556739600, 1588362000);

        // Future events
        // 27.07.2019 - 29.07.2019
        checkDurationLabelKey("date_time", "CLOSED", 1564256400, 1564429200);
        // 27.07.2019 - 30.12.2019
        checkDurationLabelKey("date", "CLOSED", 1564256400, 1577734800);
        // 27.07.2019 - 01.05.2020
        checkDurationLabelKey("date_with_year", "CLOSED", 1564256400, 1588362000);
        // 30.12.2019 - 26.07.2020
        checkDurationLabelKey("date_with_year", "CLOSED", 1577734800, 1595792400);
        // 30.12.2019 - 01.05.2020
        checkDurationLabelKey("date_with_year", "CLOSED", 1577734800, 1588362000);
        // 27.07.2019 - 01.01.2020
        checkDurationLabelKey("date", "CLOSED", 1564256400, 1577907600);
        // 01.01.2020 - 03.01.2020
        checkDurationLabelKey("date_time_with_year", "CLOSED", 1577907600, 1578080400);
        // 01.01.2020 - 26.07.2020
        checkDurationLabelKey("date_with_year", "CLOSED", 1577907600, 1595792400);

        setNow(1576006800, 10800); // 10.12.2019
        // 30.12.2019 - 01.01.2020
        checkDurationLabelKey("date_time", "CLOSED", 1577734800, 1577907600);
        // 01.01.2020 - 26.07.2020
        checkDurationLabelKey("date_with_year", "CLOSED", 1577907600, 1595792400);
        // 01.01.2020 -  03.01.2020
        checkDurationLabelKey("date_time", "CLOSED", 1577907600, 1578080400);
    }

    Y_UNIT_TEST(CheckEmptyLabels)
    {
        checkEventsWithoutLabels();
    }

}
