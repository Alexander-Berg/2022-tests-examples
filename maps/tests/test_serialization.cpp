#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/store_internal/tests/helpers.h>

namespace maps::automotive::store_internal {

TEST(serialization, parsePrintFirmware)
{
    auto fwBody = TString(readTestData("tests/firmware.prototxt"));
    Firmware firmware;
    parseFromString(fwBody, firmware);
    EXPECT_EQ(fwBody, printToString(firmware));
}

TEST(serialization, parsePrintFirmwareRollout)
{
    auto roBody = TString(readTestData("tests/firmware_rollout.prototxt"));
    FirmwareRollout rollout;
    parseFromString(roBody, rollout);
    EXPECT_EQ(roBody, printToString(rollout));
}

}
