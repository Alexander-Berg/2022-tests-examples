#include <maps/automotive/store_internal/yacare/tests/firmware.h>

#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/store_internal/lib/dao/dao.h>
#include <maps/automotive/store_internal/lib/dao/introspection.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/matchers.h>
#include <maps/automotive/store_internal/yacare/helpers.h>

namespace maps::automotive::store_internal {

using namespace ::testing;

FirmwareFixture::FirmwareFixture()
    : managerProd(makeUserInfo("manager-prod"))
{}

void FirmwareFixture::setupMocks(const Firmware& fw, FailedStep step)
{
    if (step == OriginalFileMissing) {
        EXPECT_CALL(s3Mock(), HeadObject(BucketPath(fw.url())))
            .WillOnce(Return(notFoundOutcome()));
        return;
    } else {
        EXPECT_CALL(s3Mock(), HeadObject(BucketPath(fw.url())))
            .WillOnce(Return(s3::model::HeadObjectResult()
                .WithETag("\"" + fw.md5() + "\"")));
    }

    if (step == ValidationFailed) {
        return;
    }

    if (step == CopyFailed) {
        EXPECT_CALL(s3Mock(), CopyObject(FirmwareFromTo(fw)))
            .WillRepeatedly(Return(s3Failed<s3::model::CopyObjectOutcome>()));
        return;
    } else {
        EXPECT_CALL(s3Mock(), CopyObject(FirmwareFromTo(fw)))
            .WillOnce(Return(s3::model::CopyObjectResult()));
    }

    if (step == HeadFailed) {
        EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(fw)))
            .WillRepeatedly(Return(s3Failed<s3::model::HeadObjectOutcome>()));
        return;
    } else if (step == HeadEmpty) {
        EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(fw)))
            .Times(2)
            .WillRepeatedly(Return(notFoundOutcome()));
        return;
    } else {
        EXPECT_CALL(s3Mock(), HeadObject(TargetBucketPath(fw)))
            .WillOnce(Return(notFoundOutcome()))
            .WillOnce(Return(s3::model::HeadObjectResult()
                .WithETag("\"" + fw.md5() + "\"")
                .WithContentLength(fw.image_size())));
    }
}

Dao::CreationStatus FirmwareFixture::createFw(
    const Firmware& fw, FailedStep step)
{
    setupMocks(fw, step);

    auto fwToAdd = makeFwToAdd(fw);

    auto rsp = mockPost("/store/1.x/firmware", printToString(fwToAdd));
    if (rsp.status != 202) {
        return Dao::CreationStatus::Error;
    }

    Firmware::Id fwId;
    parseFromString(rsp.body, /*out*/ fwId);
    if (fwId != fw.id()) {
        return Dao::CreationStatus::Error;
    }

    auto status = waitCreationStatus(
        API_PREFIX + "creation_status/" + firmwareId(fw.id()));
    return enum_io::fromString<Dao::CreationStatus>(status.status());
}

} // namespace maps::automotive::store_internal
