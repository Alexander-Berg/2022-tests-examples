#include "gmock_json_matchers.h"

#include <maps/infra/yacare/include/log.h>
#include <maps/infra/yacare/include/logbroker/topic.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/infra/yacare/inspect.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <sstream>

namespace yacare::tests {
auto hasTag(const maps::log8::Tag& tag)
{
    return ::testing::AllOf(
        yacare::tests::hasField<std::string>("tag", tag.value()));
}

namespace {

YCR_LOG_DECLARE(DECLARED_TAG)
YCR_LOG_DECLARE(ANOTHER_DECLARED_TAG)

class BackendMock : public maps::log8::Backend {
public:
    MOCK_METHOD(void, put, (const maps::log8::Message& message));
    MOCK_METHOD(int64_t, takeOutPeakQueueSize, ());
};

} // namespace

Y_UNIT_TEST_SUITE(test_logbroker_topic) {
Y_UNIT_TEST(tag_forwarding_configuration)
{
    std::stringstream configStream;
    yacare::impl::inspect(configStream);

    maps::json::Value parsed{configStream};
    EXPECT_THAT(
        parsed["logbroker"]["topics"],
        ::testing::IsSupersetOf(
            {hasTag(DECLARED_TAG), hasTag(ANOTHER_DECLARED_TAG)}));
}

namespace detail {

class LoggingBackendPatcher {
public:
    LoggingBackendPatcher()
        : originalBackend_{maps::log8::backend()}
        , backendMock_{std::make_shared<BackendMock>()}
    {
        maps::log8::setBackend(createDetachedBackend(backendMock_));
    }

    ~LoggingBackendPatcher()
    {
        if (originalBackend_)
            maps::log8::setBackend(std::move(originalBackend_));
    }

    BackendMock& mock() { return *backendMock_; }

    const BackendMock& mock() const { return *backendMock_; }

private:
    std::shared_ptr<maps::log8::Backend> originalBackend_;
    std::shared_ptr<BackendMock> backendMock_;
};


template<typename Iter>
class Streamer {
public:
    template<typename U>
    explicit Streamer(const U& sequence, std::string_view delimiter = ", ", std::size_t limit = 10)
        : range_{begin(sequence), end(sequence)}
        , delimiter_{delimiter}
        , limit_{limit}
    {
    }

    friend std::ostream& operator<<(std::ostream& stream, const Streamer& streamer)
    {
        if (!streamer.limit_ || streamer.range_.first == streamer.range_.second)
            return stream;

        stream << *streamer.range_.first;
        for (auto [iter, itemsLeft]{std::pair{
                 std::next(streamer.range_.first), streamer.limit_ - 1}};
             iter != streamer.range_.second;
             ++iter, --itemsLeft)
        {
            stream << streamer.delimiter_;
            if (!itemsLeft) {
                return stream << "<"
                              << std::distance(iter, streamer.range_.second)
                              << " more items>";
            }

            stream << *iter;
        }

        return stream;
    }

private:
    std::pair<Iter, Iter> range_;
    std::string_view delimiter_;
    std::size_t limit_;
};

template<typename U>
Streamer(const U& sequence, std::string_view delimiter = ", ", std::size_t limit = 10)
    -> Streamer<std::decay_t<decltype(begin(sequence))>>;

auto requestLoggingInTopics(std::initializer_list<std::string_view> topics)
{
    std::ostringstream uri;
    uri << "http://localhost/logbroker_logging?topics="
        << Streamer{topics, ",", topics.size()};

    return yacare::performTestRequest(
        {maps::http::POST, maps::http::URL{uri.str()}});
}

} // namespace detail

namespace matchers {
template<typename ValueMatcher>
testing::Matcher<const maps::log8::Message&> checkTagIs(
    ValueMatcher tagMatcher)
{
    return testing::Property(
        "tag", &maps::log8::Message::tag, std::move(tagMatcher));
}

const auto TAG_IS_NDA{checkTagIs(maps::log8::Tag{"NDA"})};
} // namespace matchers

Y_UNIT_TEST(default_and_topic_write_from_handler)
{
    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(matchers::TAG_IS_NDA)).Times(1);
    EXPECT_CALL(patcher.mock(), put(testing::Not(matchers::TAG_IS_NDA)))
        .Times(testing::AtLeast(1));

    EXPECT_EQ(detail::requestLoggingInTopics({"default", "nda"}).status, 200);
}

Y_UNIT_TEST(default_and_topic_write_outside_handler)
{
    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(matchers::TAG_IS_NDA)).Times(1);
    EXPECT_CALL(patcher.mock(), put(testing::Not(matchers::TAG_IS_NDA)))
        .Times(testing::AtLeast(1));

    EXPECT_EQ(
        detail::requestLoggingInTopics({"dedicated", "nda_dedicated"}).status,
        200);
}

Y_UNIT_TEST(same_topic_mixed_reporting)
{
    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(testing::_));
    EXPECT_CALL(patcher.mock(), put(matchers::TAG_IS_NDA)).Times(2);

    EXPECT_EQ(
        detail::requestLoggingInTopics({"nda", "nda_dedicated"}).status, 200);
}

Y_UNIT_TEST(different_topics_outside_handler)
{
    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(testing::_));
    EXPECT_CALL(patcher.mock(), put(matchers::TAG_IS_NDA)).Times(1);
    EXPECT_CALL(patcher.mock(), put(matchers::checkTagIs(maps::log8::Tag{"IMPORTANT"}))).Times(1);

    EXPECT_EQ(
        detail::requestLoggingInTopics({"important_dedicated", "nda_dedicated"})
            .status,
        200);
}

Y_UNIT_TEST(same_topic_outside_handler_multiple_times)
{
    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(testing::_));
    EXPECT_CALL(patcher.mock(), put(matchers::TAG_IS_NDA)).Times(2);

    EXPECT_EQ(
        detail::requestLoggingInTopics({"nda_dedicated", "nda_dedicated"}).status,
        200);
}

Y_UNIT_TEST(same_topic_inside_handler_multiple_times)
{
    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(testing::_));
    EXPECT_CALL(patcher.mock(), put(matchers::TAG_IS_NDA)).Times(2);

    EXPECT_EQ(detail::requestLoggingInTopics({"nda", "nda"}).status, 200);
}

namespace matchers {
const auto HAS_TIME_OFFSET_PREFIX{testing::Property(
    "text",
    &maps::log8::Message::text,
    testing::ContainsRegex(R"([0-9]+ +ms:)"))};
} // namespace matchers

Y_UNIT_TEST(time_offset_prefix_omitted_for_tagged_messages)
{
    using namespace testing;

    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(_)).Times(AnyNumber());
    EXPECT_CALL(
        patcher.mock(),
        put(testing::AllOf(
            Not(matchers::TAG_IS_NDA), matchers::HAS_TIME_OFFSET_PREFIX)))
        .Times(1);
    EXPECT_CALL(
        patcher.mock(),
        put(testing::AllOf(
            matchers::TAG_IS_NDA, Not(matchers::HAS_TIME_OFFSET_PREFIX))))
        .Times(1);

    EXPECT_EQ(detail::requestLoggingInTopics({"default", "nda"}).status, 200);
}

Y_UNIT_TEST(time_offset_prefix_omitted_for_dedicated_message_without_topic)
{
    using namespace testing;

    detail::LoggingBackendPatcher patcher;
    EXPECT_CALL(patcher.mock(), put(_)).Times(AnyNumber());
    EXPECT_CALL(
        patcher.mock(),
        put((Not(testing::AnyOf(
            matchers::TAG_IS_NDA, matchers::HAS_TIME_OFFSET_PREFIX)))))
        .Times(2);

    EXPECT_EQ(
        detail::requestLoggingInTopics({"dedicated", "nda_dedicated"}).status,
        200);
}
} // Y_UNIT_TEST_SUITE(test_logbroker_topic)

} // namespace yacare::tests
