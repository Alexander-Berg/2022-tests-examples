#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/dao/firmware.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/matchers.h>
#include <maps/automotive/store_internal/yacare/tests/firmware.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/common/include/file_utils.h>

namespace maps::automotive::store_internal {

using namespace ::testing;

static const std::string API_PREFIX = "/store/1.x/firmware/";

using IncrementalFirmware = FirmwareFixture;

namespace {

void checkMetadata(
    const Firmware::Metadata& expected,
    const Firmware::Id& fw1,
    const Firmware::Id& fw2)
{
    Firmware actual;
    {
        auto rsp = mockGet(API_PREFIX + firmwareId(fw1));
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, actual);
        PROTO_EQ(expected, actual.metadata());
    }
    {
        auto rsp = mockGet(API_PREFIX + firmwareId(fw2));
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, actual);
        PROTO_EQ(expected, actual.metadata());
    }
}

} // namespace

TEST_F(IncrementalFirmware, CreatePutDelete)
{
    Firmware fw;
    buildFirmware(fw, "oem-ford-f150-caska-imx6", "1600200200", "1600200100");
    fw.mutable_metadata()->clear_from_min_version();
    ASSERT_TRUE(fw.id().has_from_version());
    fw.set_url("bucket/and/path");

    ASSERT_TRUE(fw.metadata().provides().empty());
    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));
    fw.set_url(firmwareUrl(fw.id()));
    auto apiUrl = API_PREFIX + firmwareId(fw.id());

    Firmware actual;
    {
        auto rsp = mockGet(apiUrl);
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, actual);
        PROTO_EQ(fw, actual);
    }

    ASSERT_EQ(204, mockPut(apiUrl, printToString(fw.metadata())).status);

    // cannot set from_min_version for incremental firmware
    fw.mutable_metadata()->set_from_min_version("1600100100");
    ASSERT_EQ(400, mockPut(apiUrl, printToString(fw.metadata())).status);

    ASSERT_EQ(204, mockDelete(apiUrl).status);
    ASSERT_EQ(404, mockGet(apiUrl).status);

    // from_version must be < id.version
    fw = actual;
    fw.set_url("bucket/and/path");
    fw.mutable_id()->set_from_version(fw.id().version());
    ASSERT_EQ(Dao::CreationStatus::Error, createFw(fw, FirmwareFixture::ValidationFailed));

    // cannot create with both from_version and from_min_version set
    *fw.mutable_id() = actual.id();
    fw.mutable_metadata()->set_from_min_version("1600100100");
    ASSERT_EQ(Dao::CreationStatus::Error, createFw(fw, FirmwareFixture::ValidationFailed));
}

TEST_F(IncrementalFirmware, EditFeatures)
{
    // create two firmwares without features
    Firmware fw;
    buildFirmware(fw, "personal-new-new-caska", "1600600600");
    fw.set_url("bucket/and/path");
    fw.mutable_metadata()->clear_from_min_version();
    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));
    auto fullId = fw.id();

    fw.mutable_id()->set_from_version("1600600100");
    fw.set_url("bucket/and/path-incremental");
    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));

    // add feature to incremental firmware
    *fw.mutable_metadata()->add_provides() = theFeature().id();
    ASSERT_EQ(
        204,
        mockPut(API_PREFIX + firmwareId(fw.id()), printToString(fw.metadata())).status);
    checkMetadata(fw.metadata(), fw.id(), fullId);

    // add feature to full firmware
    Feature::Id feature;
    feature.set_name("voice_assistant");
    feature.set_version_major(1);
    feature.set_version_minor(2);
    *fw.mutable_metadata()->add_provides() = feature;
    ASSERT_EQ(2, fw.metadata().provides_size());
    ASSERT_EQ(
        204,
        mockPut(API_PREFIX + firmwareId(fullId), printToString(fw.metadata())).status);
    checkMetadata(fw.metadata(), fw.id(), fullId);

    // delete features
    fw.mutable_metadata()->clear_provides();
    ASSERT_EQ(
        204,
        mockPut(API_PREFIX + firmwareId(fw.id()), printToString(fw.metadata())).status);
    checkMetadata(fw.metadata(), fw.id(), fullId);
}

TEST_F(IncrementalFirmware, CreateWithFeaturesFull)
{
    Firmware fw;
    buildFirmware(fw, "taxi-packard-coupe-caska", "1600500500");
    ASSERT_FALSE(fw.id().has_from_version());
    fw.set_url("bucket/and/path");
    fw.mutable_metadata()->clear_from_min_version();
    *fw.mutable_metadata()->add_provides() = theFeature().id();
    auto expectedMetadata = fw.metadata();
    auto fullId = fw.id();

    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));

    // cannot create with features
    fw.set_url("bucket/and/path-incremental");
    fw.mutable_id()->set_from_version("1600500400");
    ASSERT_EQ(Dao::CreationStatus::Error, createFw(fw));

    // can create without features
    fw.mutable_metadata()->clear_provides();
    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));

    checkMetadata(expectedMetadata, fw.id(), fullId);

    // delete full
    ASSERT_EQ(204, mockDelete(API_PREFIX + firmwareId(fullId)).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + firmwareId(fullId)).status);

    // features remain
    {
        Firmware actual;
        auto rsp = mockGet(API_PREFIX + firmwareId(fw.id()));
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, actual);
        PROTO_EQ(expectedMetadata, actual.metadata());
    }
}

TEST_F(IncrementalFirmware, CreateWithFeaturesIncremental)
{
    Firmware fw;
    buildFirmware(fw, "taxi-packard-coupe-astar", "1600600600", "1600600500");
    ASSERT_TRUE(fw.id().has_from_version());
    fw.set_url("bucket/and/path");
    fw.mutable_metadata()->clear_from_min_version();
    *fw.mutable_metadata()->add_provides() = theFeature().id();
    auto expectedMetadata = fw.metadata();
    auto incrementalId = fw.id();

    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));

    // cannot create with features
    fw.set_url("bucket/and/path-full");
    fw.mutable_id()->clear_from_version();
    ASSERT_EQ(Dao::CreationStatus::Error, createFw(fw));

    // can create without features
    fw.mutable_metadata()->clear_provides();
    ASSERT_EQ(Dao::CreationStatus::Created, createFw(fw));

    checkMetadata(expectedMetadata, fw.id(), incrementalId);

    // delete incremental
    ASSERT_EQ(204, mockDelete(API_PREFIX + firmwareId(incrementalId)).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + firmwareId(incrementalId)).status);

    // features remain
    {
        Firmware actual;
        auto rsp = mockGet(API_PREFIX + firmwareId(fw.id()));
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, actual);
        PROTO_EQ(expectedMetadata, actual.metadata());
    }
}

}
