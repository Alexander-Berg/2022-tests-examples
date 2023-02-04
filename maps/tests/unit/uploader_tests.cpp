#include <maps/infopoint/takeout/lib/uploader.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <maps/libs/http/include/test_utils.h>

namespace takeout = maps::infopoint::takeout;

Y_UNIT_TEST_SUITE(InfopointTakeoutUploader) {

    Y_UNIT_TEST(TestNameValueFormDataSerialization) {
        takeout::Uploader uploader("https://passport.com", false);

        const char* expectedUploadBody =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"job_id\"\r\n"
            "\r\n"
            "job1\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"file\"; filename=\"data.json\"\r\n"
            "Content-Type: application/json\r\n"
            "\r\n"
            "job1_data\r\n"
            "--boundaryW6E10Y1984--\r\n";
        auto mockHandleUpload = maps::http::addMock(
            "https://passport.com/1/upload/",
            [expectedUploadBody](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.url.params(), "consumer=maps-jams-infopoints-takeout");
                UNIT_ASSERT_EQUAL(request.body, expectedUploadBody);
                return maps::http::MockResponse("{\"status\":\"ok\"}");
            });

        const char* expectedDoneBody =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"job_id\"\r\n"
            "\r\n"
            "job1\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"filename\"\r\n"
            "\r\n"
            "data.json\r\n"
            "--boundaryW6E10Y1984--\r\n";
        auto mockHandleDone = maps::http::addMock(
            "https://passport.com/1/upload/done/",
            [expectedDoneBody](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.url.params(), "consumer=maps-jams-infopoints-takeout");
                UNIT_ASSERT_EQUAL(request.body, expectedDoneBody);
                return maps::http::MockResponse("{\"status\":\"ok\"}");
            });

        auto status = uploader.upload("job1", "job1_data");
        UNIT_ASSERT_EQUAL(status, takeout::Uploader::Status::SUCCESS);
    }

    Y_UNIT_TEST(TestEmptyDataUpload) {
        takeout::Uploader uploader("https://passport.com", false);

        const char* expectedDoneBody =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"job_id\"\r\n"
            "\r\n"
            "job2\r\n"
            "--boundaryW6E10Y1984--\r\n";
        auto mockHandleDone = maps::http::addMock(
            "https://passport.com/1/upload/done/",
            [expectedDoneBody](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.url.params(), "consumer=maps-jams-infopoints-takeout");
                UNIT_ASSERT_EQUAL(request.body, expectedDoneBody);
                return maps::http::MockResponse("{\"status\":\"ok\"}");
            });

        auto status = uploader.upload("job2", "");
        UNIT_ASSERT_EQUAL(status, takeout::Uploader::Status::SUCCESS);
    }

    Y_UNIT_TEST(InvalidJobResponse) {
        takeout::Uploader uploader("https://passport.com", false);

        const char* expectedDoneBody =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"job_id\"\r\n"
            "\r\n"
            "job2\r\n"
            "--boundaryW6E10Y1984--\r\n";
        auto mockHandleDone = maps::http::addMock(
            "https://passport.com/1/upload/done/",
            [expectedDoneBody](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.url.params(), "consumer=maps-jams-infopoints-takeout");
                UNIT_ASSERT_EQUAL(request.body, expectedDoneBody);
                return maps::http::MockResponse("{\"status\":\"job_id.invalid\"}");
            });

        auto status = uploader.upload("job2", "");
        UNIT_ASSERT_EQUAL(status, takeout::Uploader::Status::INVALID_JOB);
    }

    Y_UNIT_TEST(StatusErrorResponse) {
        takeout::Uploader uploader("https://passport.com", false);

        const char* expectedDoneBody =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"job_id\"\r\n"
            "\r\n"
            "job2\r\n"
            "--boundaryW6E10Y1984--\r\n";
        auto mockHandleDone = maps::http::addMock(
            "https://passport.com/1/upload/done/",
            [expectedDoneBody](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.url.params(), "consumer=maps-jams-infopoints-takeout");
                UNIT_ASSERT_EQUAL(request.body, expectedDoneBody);
                return maps::http::MockResponse("{\"status\":\"error\"}");
            });

        auto status = uploader.upload("job2", "");
        UNIT_ASSERT_EQUAL(status, takeout::Uploader::Status::FAILURE);
    }

    Y_UNIT_TEST(StatusCodeResponse) {
        takeout::Uploader uploader("https://passport.com", false);

        const char* expectedDoneBody =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"job_id\"\r\n"
            "\r\n"
            "job2\r\n"
            "--boundaryW6E10Y1984--\r\n";
        maps::http::MockResponse response("{\"status\":\"ok\"}");
        response.status = 404;
        auto mockHandleDone = maps::http::addMock(
            "https://passport.com/1/upload/done/",
            [expectedDoneBody, &response](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.url.params(), "consumer=maps-jams-infopoints-takeout");
                UNIT_ASSERT_EQUAL(request.body, expectedDoneBody);
                return response;
            });

        auto status = uploader.upload("job2", "");
        UNIT_ASSERT_EQUAL(status, takeout::Uploader::Status::FAILURE);
    }
}

