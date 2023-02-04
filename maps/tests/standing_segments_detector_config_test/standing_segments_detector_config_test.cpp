#define BOOST_TEST_MAIN

#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/standing_segments_detector_config.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/config.h>

#include <boost/test/unit_test.hpp>

#include <iostream>

BOOST_AUTO_TEST_CASE(config_test) {
    Config commonConfig(makeUsershandlerConfig("usershandler.conf"));
    StandingSegmentsDetectorConfig config(commonConfig.standingSegmentDetectorConfig());
    BOOST_CHECK_EQUAL(config.maxCategory(), 6);
    BOOST_CHECK_CLOSE(config.mergeRadius(), 50, 1e-6);
    BOOST_CHECK_EQUAL(config.minDuration(), pt::seconds(120));
    BOOST_CHECK(config.allowedClid("auto"));
    BOOST_CHECK(config.allowedClid("ru.yandex.mobile.navigator"));
    BOOST_CHECK(config.allowedClid("ru.yandex.yandexnavi"));
    BOOST_CHECK(config.allowedClid("ru.yandex.traffic"));
    BOOST_CHECK(config.allowedClid("ru.yandex.yandexmaps"));
    BOOST_CHECK(!config.allowedClid("fake"));
}
