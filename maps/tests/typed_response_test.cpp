#include <maps/infra/yacare/include/content_type/negotiation.h>
#include <maps/infra/yacare/include/content_type/base.h>
#include <maps/infra/yacare/include/content_type/protobuf_adapter.h>
#include <maps/infra/yacare/include/content_type/error_support.h>
#include <maps/infra/yacare/include/typed_error.h>
#include <maps/infra/yacare/include/typed_response.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/infra/yacare/testapp/lib/testapp.pb.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/xml/include/xml.h>

#include <maps/infra/yacare/tests/gmock_json_matchers.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <contrib/restricted/boost/boost/iostreams/device/back_inserter.hpp>

struct FakeBody { };

namespace  {
const std::string JSON_STUB{"json"};
const std::string TEXT_STUB{"text"};
} // namespace

namespace text {
struct Custom : yacare::content_type::Base<Custom> {
    inline static constexpr std::string_view VALUE{"text/ascii"};
};
} // namespace text

namespace yacare::content_type {
template<>
struct Serialize<FakeBody, application::Json> {
    static void serialize(std::ostream& stream, const FakeBody&)
    {
        stream << JSON_STUB;
    }
};

template<>
struct Serialize<FakeBody, ::text::Custom> {
    static void serialize(std::ostream& stream, const FakeBody&)
    {
        stream << TEXT_STUB;
    }
};

template<>
struct Serialize<yacare::errors::Timeout, application::Json> {
    static void serialize(
        std::ostream& stream, const yacare::errors::Timeout& value)
    {
        stream << YCR_JSON(root)
        {
            root["error"] = YCR_JSON(details)
                {
                    details["status"] = value.status();
                    details["message"] = value.what();
                    details["custom"] = true;
                };
        };
    }
};
} // namespace yacare::content_type

using namespace ::testing;

namespace yacare::tests {
namespace matchers {

Matcher<Response::HeaderMap> hasContentType(Matcher<std::string> contentType)
{
    return Contains(Pair(StrCaseEq("content-type"), Contains(std::move(contentType))));
}

Matcher<maps::http::HeaderMap> hasContentTypeMock(Matcher<std::string> contentType)
{
    return Contains(Pair(StrCaseEq("content-type"), std::move(contentType)));
}
} // namespace matchers

auto postWithAccept(std::string acceptString, std::string_view exceptionMode = "no")
{
    std::ostringstream ostream;
    ostream << "http://localhost/negotiate_content?exception=" << exceptionMode;
    namespace http = maps::http;
    http::MockRequest request{http::POST, http::URL{ostream.str()}};
    request.headers["accept"] = std::move(acceptString);

    return yacare::performTestRequest(request);
}

class ScopedUserErrorHandler {
public:
    ScopedUserErrorHandler(std::string contentType)
        : contentType_{std::move(contentType)}
    {
        using namespace yacare::content_type;
        using Response = TypedResponse<application::Json, text::Xml>;
        setErrorReporter<Response>(std::ref(*this));
    }

    ~ScopedUserErrorHandler() { yacare::setDefaultErrorReporter(); }

    void operator()(
        const Request& /*unused*/,
        TypedResponse<content_type::application::Json, content_type::text::Xml>&
            response)
    {
        content_type::negotiate(contentType_, response);

        try {
            throw;
        } catch (const errors::Timeout& error) {
            response << error;
        } catch (const Error& error) {
            ERROR() << error;
            response << error;
        } catch (const std::exception& error) {
            ERROR() << error.what();
            response << (errors::InternalError{} << error.what());
        }
    }

private:
    std::string contentType_;
};

Y_UNIT_TEST_SUITE(typed_response_suite) {
Y_UNIT_TEST(serialize_as_negotiated)
{
    const std::string appJson{"application/json"};
    Response original;
    TypedResponse<content_type::application::Json, text::Custom>
        response{original};
    EXPECT_THAT(original.headers(), Not(matchers::hasContentType(appJson)));
    content_type::negotiate(appJson, response);

    FakeBody body;
    response << body;

    EXPECT_THAT(original.body(), HasSubstr(JSON_STUB));
    EXPECT_THAT(original.headers(), matchers::hasContentType(appJson));

    const std::string textAscii{"text/ascii"};
    content_type::negotiate(textAscii, response);

    response << body;
    EXPECT_THAT(original.body(), HasSubstr(TEXT_STUB));
    EXPECT_THAT(original.headers(), matchers::hasContentType(textAscii));
}

Y_UNIT_TEST(testapp_responds_protobuf)
{
    const std::string appProtobuf{"application/x-protobuf"};
    auto response{postWithAccept(appProtobuf)};

    EXPECT_EQ(response.status, 200);
    EXPECT_THAT(response.headers, matchers::hasContentTypeMock(appProtobuf));

    TestMessage message;
    message.ParseFromStringOrThrow(response.body);
    EXPECT_EQ(message.title(), "title");
    EXPECT_EQ(message.text(), "text");
}

Y_UNIT_TEST(testapp_responds_json)
{
    const std::string appJson{"application/json"};
    const auto response{postWithAccept(appJson)};

    EXPECT_EQ(response.status, 200);
    EXPECT_THAT(response.headers, matchers::hasContentTypeMock(appJson));

    auto parsed{maps::json::Value::fromString(response.body)};
    EXPECT_THAT(
        parsed,
        testing::AllOf(
            hasField<std::string>("title", "title"),
            hasField<std::string>("text", "text")));
}

Y_UNIT_TEST(testapp_explicit_exception)
{
    const std::string appJson{"application/json"};
    ScopedUserErrorHandler scoped{appJson};

    const auto response{postWithAccept(appJson, "explicit")};

    ASSERT_THAT(response.status, testing::AllOf(Ge(400), Lt(500)));
    ASSERT_THAT(response.headers, matchers::hasContentTypeMock(appJson));
    EXPECT_THAT(
        maps::json::Value::fromString(response.body),
        hasField("error", hasField<bool>("custom", true)));
}

Y_UNIT_TEST(testapp_base_exception)
{
    const std::string appProtobuf{"application/json"};
    ScopedUserErrorHandler scoped{appProtobuf};

    const auto response{postWithAccept(appProtobuf, "implicit")};

    ASSERT_THAT(response.status, testing::AllOf(Ge(400), Lt(500)));
    ASSERT_THAT(response.headers, matchers::hasContentTypeMock(appProtobuf));

    const auto& bodyJson{maps::json::Value::fromString(response.body)};
    EXPECT_THAT(
        bodyJson,
        hasField(
            "error",
            SafeMatcherCast<const maps::json::Value&>(testing::AllOf(
                hasField<int>("status", response.status),
                hasField<std::string>("message", HasSubstr("implicit"))))));
}
} // Y_UNIT_TEST_SUITE(typed_response_suite)
} // namespace yacare::tests
