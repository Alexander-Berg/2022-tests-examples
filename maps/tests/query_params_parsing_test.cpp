#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace yacare::tests {

using maps::http::URL;
using maps::http::GET;
using maps::http::MockRequest;
using namespace testing;

/**
 * WARN: all handlers being requested are described in
 *       yacare/testapp/lib
 */
Y_UNIT_TEST_SUITE(test_query_params_parsing) {

Y_UNIT_TEST(test_scale) {
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=2.5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "2.5");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "1");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?noscale=test"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "1");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=1"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "1");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=4"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "4");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=abc"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("scale"));
        EXPECT_THAT(response.body, HasSubstr("malformed"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale="));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("scale"));
        EXPECT_THAT(response.body, HasSubstr("malformed"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=2z"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("scale"));
        EXPECT_THAT(response.body, HasSubstr("malformed"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=0.5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("scale"));
        EXPECT_THAT(response.body, HasSubstr("1 and 4"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/scale?scale=5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("scale"));
        EXPECT_THAT(response.body, HasSubstr("1 and 4"));
    }
}


Y_UNIT_TEST(test_optional_query_param) {
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/int"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "std::nullopt");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/int?int=31337"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "int=31337");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/int?int=not_an_int"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/int?int=31337&int=31338"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        //only first value with the same key will be parsed
        EXPECT_EQ(response.body, "int=31337");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/bool?bool=true"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "bool=true");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/bool?bool=1"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "bool=true");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/optional/bool?bool=truelala"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "bool=false");
    }
}


Y_UNIT_TEST(test_tile) {
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=0&y=1&z=2"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "(0, 1, 2)");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=17&y=42&z=0"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "(0, 0, 0)");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=-1&y=32&z=5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "(31, 0, 5)");
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("x parameter"));
        EXPECT_THAT(response.body, HasSubstr("missing"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("y parameter"));
        EXPECT_THAT(response.body, HasSubstr("missing"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5&y=5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("z parameter"));
        EXPECT_THAT(response.body, HasSubstr("missing"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5&y=5&z=1.5"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("z parameter"));
        EXPECT_THAT(response.body, HasSubstr("malformed"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5&y=5&z=abc"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("z parameter"));
        EXPECT_THAT(response.body, HasSubstr("malformed"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5&y=5&z=1z"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("z parameter"));
        EXPECT_THAT(response.body, HasSubstr("malformed"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5&y=5&z=42"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("zoom should be"));
        EXPECT_THAT(response.body, HasSubstr("0 and 30"));
    }
    {
        MockRequest mockRequest(GET, URL("http://localhost/testparse/tile?x=5&y=5&z=-1"));
        auto response = performTestRequest(mockRequest);
        EXPECT_EQ(response.status, 400);
        EXPECT_THAT(response.body, HasSubstr("zoom should be"));
        EXPECT_THAT(response.body, HasSubstr("0 and 30"));
    }
}

} //Y_UNIT_TEST_SUITE

} //namespace yacare::tests
