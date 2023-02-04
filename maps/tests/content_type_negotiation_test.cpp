#include <maps/infra/yacare/include/content_type/negotiation.h>
#include <maps/infra/yacare/include/content_type/base.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace ::testing;

namespace yacare::tests {

Y_UNIT_TEST_SUITE(accept_negotiation) {
Y_UNIT_TEST(negotiate_response)
{
    std::string accept{"application/json, text/x-protobuf;q=0.7"};
    yacare::Response response;
    using namespace content_type;
    {
        TypedResponse<application::Json> polyResponse{response};
        ASSERT_NO_THROW(negotiate(accept, polyResponse));
        EXPECT_EQ(polyResponse.contentType(), application::Json::VALUE);
    }
    {
        TypedResponse<text::Protobuf> polyResponse{response};
        ASSERT_NO_THROW(negotiate(accept, polyResponse));
        EXPECT_EQ(polyResponse.contentType(), text::Protobuf::VALUE);
    }
    {
        TypedResponse<application::Protobuf> polyResponse{response};
        EXPECT_THROW(
            negotiate(accept, polyResponse), yacare::errors::NotAcceptable);
    }
    {
        TypedResponse<application::Protobuf, text::Protobuf> polyResponse{
            response};
        ASSERT_NO_THROW(negotiate(accept, polyResponse));
        EXPECT_EQ(polyResponse.contentType(), text::Protobuf::VALUE);
    }
}

Y_UNIT_TEST(negotiate_accept_any)
{
    yacare::Response response;
    using namespace content_type;
    TypedResponse<application::Json, text::Protobuf> polyResponse{
        response};

    ASSERT_NO_THROW(negotiate("text/*", polyResponse));
    EXPECT_EQ(polyResponse.contentType(), text::Protobuf::VALUE);
    EXPECT_NO_THROW(negotiate("*/*", polyResponse));
    EXPECT_EQ(polyResponse.contentType(), application::Json::VALUE);
}
} // Y_UNIT_TEST_SUITE(accept_negotiation)
} // namespace yacare::tests
