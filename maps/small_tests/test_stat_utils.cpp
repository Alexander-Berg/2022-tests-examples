#include <yandex/maps/wiki/common/stat_utils.h>

#include <maps/libs/http/include/test_utils.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(stat_utils_tests)
{
    const auto STAT_UPLOAD_API_URL = "https://stat.example/api/upload";

    Y_UNIT_TEST(should_get_upload_api_url)
    {
        const auto xml = R"(<?xml version="1.0" encoding="utf-8"?>
            <config>
                <common>
                    <stat>
                        <api-url>
                            <upload>https://stat.example/api/upload</upload>
                        </api-url>
                    </stat>
                </common>
            </config>
        )";
        const common::ExtendedXmlDoc config(xml, common::ExtendedXmlDoc::SourceType::XmlString);

        UNIT_ASSERT_EQUAL(getStatUploadApiUrl(config), STAT_UPLOAD_API_URL);
    }
}

} // namespace maps::wiki::common::tests
