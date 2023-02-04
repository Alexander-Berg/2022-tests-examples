#include <maps/infra/yacare/include/log.h>
#include <maps/infra/yacare/include/response.h>
#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace yacare::tests {

using namespace testing;

struct TextCollector final : public maps::log8::Backend {
    void put(const maps::log8::Message& message) override {
        texts.emplace_back(message.text());
    }

    std::vector<std::string> texts;
};

Y_UNIT_TEST_SUITE(test_logger) {

Y_UNIT_TEST(test_builtin_logger_custom_error) {
    const auto textCollector = std::make_shared<TextCollector>();
    maps::log8::setBackend(textCollector);

    try {
        throw maps::RuntimeError();
    } catch (const maps::RuntimeError&) {
        Request request;
        Response response;
        response.setStatus(418);
        LogEntry logEntry {
            .request = &request,
            .response = &response,
            .duration = std::chrono::milliseconds(123),
            .exception = std::current_exception()
        };
        builtinLogger(logEntry);
    }
    ASSERT_EQ(textCollector->texts.size(), 1u);
    EXPECT_THAT(textCollector->texts.at(0), HasSubstr("HTTP 418"));
}

} //Y_UNIT_TEST_SUITE

} //namespace yacare::tests
