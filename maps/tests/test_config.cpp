#include <google/protobuf/text_format.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/updater/config/config.pb.h>
#include <util/datetime/base.h>
#include <util/stream/file.h>
#include <maps/libs/common/include/exception.h>

#include <string>

namespace maps::automotive::updater {

TEST(config, load)
{
    for (const std::string env: {"development", "testing", "stress", "production"}) {
        proto::Config config;
        REQUIRE(
            NProtoBuf::TextFormat::ParseFromString(
                TFileInput(ArcadiaSourceRoot()
                    + "/maps/automotive/updater/config/config."
                    + env + ".prototxt").ReadAll(),
                &config),
            "Error parsing config");
        EXPECT_NO_THROW(TDuration::Parse(config.drive().timeout()).Seconds());
        EXPECT_NO_THROW(TDuration::Parse(config.drive().service_session_duration()).Minutes());
        EXPECT_NO_THROW(TDuration::Parse(config.store().timeout()).Seconds());
        EXPECT_NO_THROW(TDuration::Parse(config.store().refresh_interval()).Seconds());
        EXPECT_NO_THROW(TDuration::Parse(config.passport().timeout()).Seconds());
        EXPECT_NO_THROW(TDuration::Parse(config.yt().timeout()).Seconds());
    }
}

}
