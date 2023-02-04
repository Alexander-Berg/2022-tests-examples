#include <maps/automotive/store_internal/lib/dao/dao.h>
#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/store_internal/tests/postgres.h>
#include <maps/infra/yacare/include/test_utils.h>

namespace maps::automotive::store_internal {

class FirmwareFixture: public AppContextPostgresFixture
{
public:
    const std::string API_PREFIX = "/store/1.x/firmware/";

    enum FailedStep {
        NoFailure,
        OriginalFileMissing,
        ValidationFailed,
        CopyFailed,
        HeadEmpty,
        HeadFailed,
    };

    FirmwareFixture();

    Dao::CreationStatus createFw(
        const Firmware& fw, FailedStep step = NoFailure);
    void setupMocks(const Firmware& fw, FailedStep step);

    yacare::tests::UserInfoFixture managerProd;
};

inline FirmwareToAdd makeFwToAdd(const Firmware& fw)
{
    FirmwareToAdd fwToAdd;
    fwToAdd.set_url(fw.url());
    *fwToAdd.mutable_metadata() = fw.metadata();
    *fwToAdd.mutable_id() = fw.id();
    return fwToAdd;
}

MATCHER_P(TargetBucketPath, fw, "")
{
    return g_ctx->s3config().buckets().external() == arg.GetBucket()
        && firmwarePath(fw.id()) == arg.GetKey();
}

MATCHER_P(FirmwareFromTo, fw, "")
{
    return fw.url() == arg.GetCopySource()
        && g_ctx->s3config().buckets().external() == arg.GetBucket()
        && firmwarePath(fw.id()) == arg.GetKey();
}

MATCHER_P2(FirmwareMetadata, bucket, key, "")
{
    return key == arg.GetKey() && bucket == arg.GetBucket();
}

} // namespace maps::automotive::store_internal
