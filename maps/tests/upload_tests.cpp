#include <maps/wikimap/mapspro/libs/stat_client/include/upload.h>

#include <maps/libs/http/include/urlencode.h>
#include <maps/libs/stringutils/include/split.h>

#include <maps/libs/http/include/test_utils.h>
#include <library/cpp/testing/unittest/registar.h>

#include <array>

namespace maps::wiki::stat_client::tests {

Y_UNIT_TEST_SUITE(upload_tests)
{
    const auto STAT_UPLOAD_API_URL = "https://stat.example/api/upload";
    const auto EMPTY_NAME = "";
    const auto EMPTY_BODY = "";

    auto
    paramsToMap(const std::string& urlEncodedParams)
    {
        const auto paramValueVec = stringutils::split(urlEncodedParams, '&');

        std::unordered_map<std::string, std::string> paramToValue;
        for (const auto& paramValue: paramValueVec) {
            const auto splitParamValue = stringutils::split(paramValue, '=');
            UNIT_ASSERT_EQUAL(splitParamValue.size(), 2);
            UNIT_ASSERT(!paramToValue.count(splitParamValue[0]));
            paramToValue[splitParamValue[0]] = http::urlDecode(splitParamValue[1]);
        }
        return paramToValue;
    }

    Y_UNIT_TEST(should_send_with_auth_header)
    {
        auto mock = maps::http::addMock(
            STAT_UPLOAD_API_URL,
            [](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(request.headers.at("Authorization"), "OAuth StatToken");
                return maps::http::MockResponse();
            }
        );

        Uploader(STAT_UPLOAD_API_URL).upload(EMPTY_NAME, EMPTY_BODY, Scale::Daily);
    }

    Y_UNIT_TEST(should_send_name)
    {
        auto mock = maps::http::addMock(
            STAT_UPLOAD_API_URL,
            [](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(paramsToMap(request.body).at("name"), "Report/Name");
                return maps::http::MockResponse();
            }
        );

        Uploader(STAT_UPLOAD_API_URL).upload("Report/Name", EMPTY_BODY, Scale::Daily);
    }

    Y_UNIT_TEST(should_send_scale)
    {
        size_t invocationCounter = 0;
        const std::array expectedScales{"d", "w", "m", "q", "y", "h", "i", "s"};

        auto mock = maps::http::addMock(
            STAT_UPLOAD_API_URL,
            [&](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(
                    paramsToMap(request.body).at("scale"),
                    expectedScales[invocationCounter++]
                );
                return maps::http::MockResponse();
            }
        );

        for (const auto scale: enum_io::enumerateValues<Scale>()) {
            Uploader(STAT_UPLOAD_API_URL).upload(EMPTY_NAME, EMPTY_BODY, scale);
        }
    }

    Y_UNIT_TEST(should_send_append_mode)
    {
        size_t invocationCounter = 0;
        const std::array expectedAppendMode{"0", "1", "0"};

        auto mock = maps::http::addMock(
            STAT_UPLOAD_API_URL,
            [&](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(
                    paramsToMap(request.body).at("_append_mode"),
                    expectedAppendMode[invocationCounter++]
                );
                return maps::http::MockResponse();
            }
        );

        Uploader(STAT_UPLOAD_API_URL).upload(EMPTY_NAME, EMPTY_BODY, Scale::Daily);
        Uploader(STAT_UPLOAD_API_URL).mode(Mode::Append).upload(EMPTY_NAME, EMPTY_BODY, Scale::Daily);
        Uploader(STAT_UPLOAD_API_URL).mode(Mode::Truncate).upload(EMPTY_NAME, EMPTY_BODY, Scale::Daily);
    }

    Y_UNIT_TEST(should_send_data)
    {
        const auto BODY = "Report\nBody\n";

        auto mock = maps::http::addMock(
            STAT_UPLOAD_API_URL,
            [&](const maps::http::MockRequest& request) {
                UNIT_ASSERT_EQUAL(paramsToMap(request.body).at("data"), BODY);
                return maps::http::MockResponse();
            }
        );

        Uploader(STAT_UPLOAD_API_URL).upload(EMPTY_NAME, BODY, Scale::Daily);
    }

    Y_UNIT_TEST(should_throw_on_send_failure)
    {
        auto mock = maps::http::addMock(
            STAT_UPLOAD_API_URL,
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse::withStatus(500);
            }
        );

        UNIT_ASSERT_EXCEPTION(
            Uploader(STAT_UPLOAD_API_URL).upload(EMPTY_NAME, EMPTY_BODY, Scale::Daily),
            ReportUploadError
        );
    }
}

} // namespace maps::wiki::stat_client::tests
