#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/store_internal/tests/postgres.h>
#include <maps/infra/yacare/include/test_utils.h>

namespace maps::automotive::store_internal {

struct PackageFixture: public AppContextPostgresFixture
{
    const std::string API_PREFIX = "/store/1.x/package/";

    enum FailedStep {
        NoFailure,
        OriginalFileMissing,
        ValidationFailed,
        CopyFailed,
        HeadEmpty,
        HeadFailed,
        CopiedNoFailure,
        CopiedCopyFailed,
        CopiedHeadEmpty,
        CopiedHeadFailed,
        CopiedDeleteFailed,
    };

    PackageFixture();

    Status createPkg(const Package& pkg, FailedStep step = NoFailure);
    void setupMocks(const Package& pkg, FailedStep step);

    yacare::tests::UserInfoFixture managerProd;
};

} // namespace maps::automotive::store_internal
