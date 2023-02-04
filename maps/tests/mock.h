#include <aws/s3/S3Client.h>
#include <aws/core/auth/AWSCredentialsProvider.h>
#include <aws/s3/model/CopyObjectRequest.h>
#include <aws/s3/model/DeleteObjectRequest.h>
#include <aws/s3/model/GetObjectRequest.h>
#include <aws/s3/model/HeadObjectRequest.h>
#include <aws/s3/model/PutObjectRequest.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/s3/client.h>

namespace maps::automotive::s3 {

// Mock only methods used by wrapper
struct S3ClientMock: public Aws::S3::S3Client
{
    S3ClientMock()
        : Aws::S3::S3Client(std::make_shared<Aws::Auth::AnonymousAWSCredentialsProvider>()) 
    {}

    MOCK_METHOD(model::HeadObjectOutcome,
        HeadObject, (const model::HeadObjectRequest&), (const, override));
    MOCK_METHOD(model::CopyObjectOutcome,
        CopyObject, (const model::CopyObjectRequest&), (const, override));
    MOCK_METHOD(model::PutObjectOutcome,
        PutObject, (const model::PutObjectRequest&), (const, override));
    MOCK_METHOD(model::DeleteObjectOutcome,
        DeleteObject, (const model::DeleteObjectRequest&), (const, override));
    MOCK_METHOD(model::GetObjectOutcome,
        GetObject, (const model::GetObjectRequest&), (const, override));
};

}
