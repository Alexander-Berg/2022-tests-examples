#include <yandex_io/tests/testlib/test_utils.h>

#include <curl/curl.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>

Y_UNIT_TEST_SUITE(TestCurl) {
    Y_UNIT_TEST(testCurlVersion)
    {
        curl_version_info_data* versionInfo;
        versionInfo = curl_version_info(CURLVERSION_NOW);

        int versionMajor = versionInfo->version_num >> 16;
        int versionMinor = (versionInfo->version_num >> 8) & 0xFF;

        // Since version 7.20.0 we dont need to call curl_multi_socket_action many times while it returning CURLM_CALL_MULTI_PERFORM
        UNIT_ASSERT_GE(versionMajor, 7);
        if (7 == versionMajor) {
            UNIT_ASSERT_GE(versionMinor, 20);
        }

        std::cout << std::string("Curl version: ") + versionInfo->version << "\n";

        // Check we are using headers and shared library from the same version
        UNIT_ASSERT_VALUES_EQUAL(int(versionInfo->version_num), LIBCURL_VERSION_NUM);
    }

    Y_UNIT_TEST(testCurlMemoryLeak)
    {
        curl_global_init(CURL_GLOBAL_ALL);
        CURL* easy = curl_easy_init();
        curl_easy_cleanup(easy);
        curl_global_cleanup();
        UNIT_ASSERT(true);
    }
}
