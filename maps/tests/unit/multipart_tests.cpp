#include <maps/infopoint/takeout/lib/multipart.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <sstream>

namespace takeout = maps::infopoint::takeout;

Y_UNIT_TEST_SUITE(InfopointTakeoutMultipart) {

    Y_UNIT_TEST(TestNameValueFormDataSerialization) {
        const char* expected =
            "Content-Disposition: form-data; name=\"first\"\r\n"
            "\r\n"
            "second\r\n";
        std::ostringstream sstream;
        takeout::NameValueFormData("first", "second").encode(sstream);
        UNIT_ASSERT_EQUAL(sstream.str(), expected);
    }

    Y_UNIT_TEST(TestJsonAttachementFormDataSerialization) {
        const char* expected =
            "Content-Disposition: form-data; name=\"file\"; filename=\"file1\"\r\n"
            "Content-Type: application/json\r\n"
            "\r\n"
            "content\r\n";
        std::ostringstream sstream;
        takeout::JsonAttachmentFormData("file1", "content").encode(sstream);
        UNIT_ASSERT_EQUAL(sstream.str(), expected);
    }

    Y_UNIT_TEST(TestComplexSerialization) {
        const char* expected =
            "\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"ac\"\r\n"
            "\r\n"
            "dc\r\n"
            "--boundaryW6E10Y1984\r\n"
            "Content-Disposition: form-data; name=\"file\"; filename=\"data\"\r\n"
            "Content-Type: application/json\r\n"
            "\r\n"
            "{}\r\n"
            "--boundaryW6E10Y1984--\r\n";
        std::ostringstream sstream;
        takeout::serializeMultipart(
            sstream,
            takeout::NameValueFormData("ac", "dc"),
            takeout::JsonAttachmentFormData("data", "{}"));
        UNIT_ASSERT_EQUAL(sstream.str(), expected);
    }

}
