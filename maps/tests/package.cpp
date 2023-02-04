#include <maps/automotive/store_internal/yacare/tests/package.h>

#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/dao/dao.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/matchers.h>
#include <maps/automotive/store_internal/yacare/helpers.h>

namespace maps::automotive::store_internal {

namespace {

bool isBadMd5(PackageFixture::FailedStep step) {
    return step == PackageFixture::CopiedNoFailure
        || step == PackageFixture::CopiedCopyFailed
        || step == PackageFixture::CopiedHeadEmpty
        || step == PackageFixture::CopiedHeadFailed
        || step == PackageFixture::CopiedDeleteFailed;
}

MATCHER_P(TargetBucketPath, pkg, "")
{
    return g_ctx->s3config().buckets().external() == arg.GetBucket()
        && packagePath(pkg) == arg.GetKey();
}

MATCHER_P(PackageFromTo, pkg, "")
{
    return pkg.url() == arg.GetCopySource()
        && g_ctx->s3config().buckets().external() == arg.GetBucket()
        && packagePath(pkg) == arg.GetKey();
}

} // namespace

using namespace ::testing;

PackageFixture::PackageFixture()
    : managerProd(makeUserInfo("manager-prod"))
{}

void PackageFixture::setupMocks(const Package& pkg, FailedStep step)
{
    auto tmpPkg = pkg;
    if (isBadMd5(step)) {
        tmpPkg.set_md5(pkg.md5() + "-bad");
    }
    if (step == OriginalFileMissing) {
        EXPECT_CALL(s3Mock(), HeadObject(BucketPath(pkg.url())))
            .WillOnce(Return(notFoundOutcome()));
        return;
    } else {
        EXPECT_CALL(s3Mock(), HeadObject(BucketPath(pkg.url())))
            .WillOnce(Return(s3::model::HeadObjectResult()
                .WithETag("\"" + tmpPkg.md5() + "\"")));
    }

    if (step == ValidationFailed) {
        return;
    }

    if (step == CopyFailed) {
        EXPECT_CALL(s3Mock(), CopyObject(PackageFromTo(tmpPkg)))
            .WillRepeatedly(Return(s3Failed<s3::model::CopyObjectOutcome>()));
        return;
    } else {
        EXPECT_CALL(s3Mock(), CopyObject(PackageFromTo(tmpPkg)))
            .WillOnce(Return(s3::model::CopyObjectResult()));
    }

    if (step == HeadFailed) {
        EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(tmpPkg)))
            .WillRepeatedly(Return(s3Failed<s3::model::HeadObjectOutcome>()));
        return;
    } else if (step == HeadEmpty) {
        EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(tmpPkg)))
            .Times(2)
            .WillRepeatedly(Return(notFoundOutcome()));
        return;
    } else {
        EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(tmpPkg)))
            .WillOnce(Return(notFoundOutcome()))
            .WillOnce(Return(s3::model::HeadObjectResult()
                .WithETag("\"" + pkg.md5() + "\"")
                .WithContentLength(pkg.size())));
    }

    if (isBadMd5(step)) {
        tmpPkg.set_url(
            g_ctx->s3config().buckets().external() + "/" + packagePath(tmpPkg));
        tmpPkg.set_md5(pkg.md5());

        if (step == CopiedCopyFailed) {
            EXPECT_CALL(s3Mock(), CopyObject(PackageFromTo(tmpPkg)))
                .WillRepeatedly(Return(s3Failed<s3::model::CopyObjectOutcome>()));
            return;
        } else {
            EXPECT_CALL(s3Mock(), CopyObject(PackageFromTo(tmpPkg)))
                .WillOnce(Return(s3::model::CopyObjectResult()));
        }

        if (step == CopiedHeadFailed) {
            EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(pkg)))
                .WillRepeatedly(Return(s3Failed<s3::model::HeadObjectOutcome>()));
            return;
        } else if (step == CopiedHeadEmpty) {
            EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(pkg)))
                .Times(2)
                .WillRepeatedly(Return(notFoundOutcome()));
            return;
        } else {
            EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(pkg)))
                .WillOnce(Return(notFoundOutcome()))
                .WillOnce(Return(s3::model::HeadObjectResult()
                    .WithETag("\"" + pkg.md5() + "\"")
                    .WithContentLength(pkg.size())));
        }

        tmpPkg.set_md5(pkg.md5() + "-bad");
        if (step == CopiedDeleteFailed) {
            EXPECT_CALL(s3Mock(), DeleteObject(TargetBucketPath(tmpPkg)))
                .WillRepeatedly(Return(s3Failed<s3::model::DeleteObjectOutcome>()));
        } else {
            EXPECT_CALL(s3Mock(), DeleteObject(TargetBucketPath(tmpPkg)))
                .WillOnce(Return(s3::model::DeleteObjectResult()));
        }
    }
}

Status PackageFixture::createPkg(const Package& pkg, FailedStep step)
{
    setupMocks(pkg, step);
    EXPECT_EQ(202, mockPost("/store/1.x/package", printToString(pkg)).status);
    return waitCreationStatus(API_PREFIX + "creation_status/" + packageId(pkg.id()));
}

} // namespace maps::automotive::store_internal
