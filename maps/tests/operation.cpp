#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/roquefort/lib/operation.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/common/include/exception.h>

#include <list>

using namespace maps::roquefort;

namespace {

template<bool value>
class OperationDummy: public Operation {
public:
    bool matches(const Fields& /*line*/) const override { return value; }
};

const std::string PING_LINE =
    std::string("tskv\ttskv_format=access-log-maps\ttimestamp=2018-03-19T20:01:34\ttimezone=+0300\t") +
    "status=500\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\t" +
    "user_agent=-\tvhost=ecstatic.maps.yandex.ru\tip=::1\tx_forwarded_for=-\tx_real_ip=-\t" +
    "request_time=0.001\tupstream_response_time=0.001\tupstream_cache_status=-\t" +
    "upstream_status=500\tscheme=http bytes_sent=275\targs=-\tssl_session_id=-\t" +
    "ssl_protocol=-\tssl_cipher=-\tssl_handshake_time=-";

const std::string STAT_LINE =
    std::string("tskv\ttskv_format=access-log-maps\ttimestamp=2018-03-19T20:02:16\ttimezone=+0300\t") +
    "status=500\tprotocol=HTTP/1.0\tmethod=GET\trequest=/stat\treferer=-\tcookies=-\t" +
    "user_agent=-\tvhost=ecstatic.maps.yandex.ru\tip=::1\tx_forwarded_for=-\tx_real_ip=-\t" +
    "request_time=0.001\tupstream_response_time=0.001\tupstream_cache_status=-\t" +
    "upstream_status=500\tscheme=http\tbytes_sent=275\targs=-\tssl_session_id=-\t" +
    "ssl_protocol=-\tssl_cipher=-\tssl_handshake_time=-";

} // namespace

Y_UNIT_TEST_SUITE(OperationTestSuite)
{
    Y_UNIT_TEST(OperationConstructTest)
    {
        std::string json = R"({"And":[{"StartsWith":{"request":"/ping"}},{"Equals":{"status":"500"}},{"Regex":{"vhost":"ecstatic.*"}}]})";
        auto op = Operation::createFromJson(maps::json::Value(std::stringstream(json)));

        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(op->matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!op->matches(parsed));
        }

        {
            std::string wrongStatusLine =
                std::string("tskv\ttskv_format=access-log-maps\ttimestamp=2018-03-19T20:01:34\ttimezone=+0300\t") +
                "status=400\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\t";
            auto parsed = Fields::parseTskv(wrongStatusLine);
            UNIT_ASSERT(!op->matches(parsed));
        }

        std::string unknownOp = R"({"Xor":[{"StartsWith":{"request":"/ping"}},{"Equals":{"status":"500"}}]})";
        UNIT_CHECK_GENERATED_EXCEPTION(
            Operation::createFromJson(maps::json::Value(std::stringstream(unknownOp))), maps::Exception);
    }

    Y_UNIT_TEST(OperationAndTest)
    {
        {
            std::list<std::shared_ptr<Operation>> args;
            args.push_back(std::make_shared<OperationDummy<true>>());
            args.push_back(std::make_shared<OperationDummy<false>>());
            args.push_back(std::make_shared<OperationDummy<true>>());
            UNIT_ASSERT(!OperationAnd(args).matches({}));
        }

        {
            std::list<std::shared_ptr<Operation>> args;
            args.push_back(std::make_shared<OperationDummy<true>>());
            args.push_back(std::make_shared<OperationDummy<true>>());
            UNIT_ASSERT(OperationAnd(args).matches({}));
        }

        {
            std::list<std::shared_ptr<Operation>> args;
            UNIT_ASSERT(OperationAnd(args).matches({}));
        }
    }

    Y_UNIT_TEST(OperationOrTest)
    {
        {
            std::list<std::shared_ptr<Operation>> args;
            args.push_back(std::make_shared<OperationDummy<true>>());
            args.push_back(std::make_shared<OperationDummy<false>>());
            args.push_back(std::make_shared<OperationDummy<true>>());
            UNIT_ASSERT(OperationOr(args).matches({}));
        }

        {
            std::list<std::shared_ptr<Operation>> args;
            args.push_back(std::make_shared<OperationDummy<false>>());
            args.push_back(std::make_shared<OperationDummy<false>>());
            UNIT_ASSERT(!OperationOr(args).matches({}));
        }

        {
            std::list<std::shared_ptr<Operation>> args;
            UNIT_ASSERT(!OperationOr(args).matches({}));
        }
    }

    Y_UNIT_TEST(OperationNotTest)
    {
        {
            std::list<std::shared_ptr<Operation>> args;
            args.push_back(std::make_shared<OperationDummy<true>>());
            UNIT_ASSERT(!OperationNot(args).matches({}));
        }

        {
            std::list<std::shared_ptr<Operation>> args;
            args.push_back(std::make_shared<OperationDummy<false>>());
            UNIT_ASSERT(OperationNot(args).matches({}));
        }
    }

    Y_UNIT_TEST(OperationStartsWithTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationStartsWith("request", "/ping").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationStartsWith("request", "/ping").matches(parsed));
        }

        {
            std::string line = "tskv\ttskv_format=access-log-maps\ttimestamp=2018-03-19T20:00:02\t"
                "timezone=+0300\tstatus=404\tprotocol=HTTP/1.1\tmethod=GET\treferer=-\t";
            auto parsed = Fields::parseTskv(line);
            UNIT_ASSERT(!OperationStartsWith("request", "/ping").matches(parsed));
        }
    }

    Y_UNIT_TEST(OperationEndsWithTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationEndsWith("vhost", "yandex.ru").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationEndsWith("request", "yandex.net").matches(parsed));
        }
    }

    Y_UNIT_TEST(OperationEqualsTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationEquals("method", "GET").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationEquals("method", "POST").matches(parsed));
        }
    }

    Y_UNIT_TEST(OperationContainsTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationContains("vhost", "maps").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationContains("vhost", "google").matches(parsed));
        }
    }

    Y_UNIT_TEST(OperationRegexTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationRegex("vhost", ".*maps.*").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationRegex("vhost", ".*google.*").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationRegex("vhost", "ecstatic.*").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(!OperationRegex("vhost", "ecstatic").matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(!OperationRegex("vhost", "maps.*").matches(parsed));
        }
        {
            std::vector<std::string> requests{
                "tskv\trequest=/ping", "tskv\trequest=/ping?a=b",
                "tskv\trequest=/ping/", "tskv\trequest=/ping?a=b&c=d",
                "tskv\trequest=/ping/?", "tskv\trequest=/ping/?a=b",
                "tskv\trequest=/ping?", "tskv\trequest=/ping/?a=b&c=d"
            };
            for (std::string_view request : requests) {
                UNIT_ASSERT_C(
                    OperationRegex("request", "/ping/.*").matches(Fields::parseTskv(request)),
                    "/ping/.* doesn't match " << request
                );
            }
        }
        {
            std::vector<std::string> requests{
                "tskv\trequest=/pinga", "tskv\trequest=/pinga/",
                "tskv\trequest=/aping", "tskv\trequest=/apinga/"
            };
            for (std::string_view request : requests) {
                UNIT_ASSERT_C(
                    !OperationRegex("request", "/ping/.*").matches(Fields::parseTskv(request)),
                    "/ping/.* matches " << request
                );
            }
        }
    }

    Y_UNIT_TEST(OperationLessThanTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationLessThan("request_time", 0.1).matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationLessThan("request_time", -0.1).matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(OperationLessThan("status", 500).matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationLessThan("protocol", 500).matches(parsed));
        }
    }

    Y_UNIT_TEST(OperationGreaterThanTest)
    {
        {
            auto parsed = Fields::parseTskv(PING_LINE);
            UNIT_ASSERT(OperationGreaterThan("request_time", -0.1).matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationGreaterThan("request_time", 0.1).matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(OperationGreaterThan("status", 500).matches(parsed));
        }

        {
            auto parsed = Fields::parseTskv(STAT_LINE);
            UNIT_ASSERT(!OperationGreaterThan("protocol", 500).matches(parsed));
        }
    }

    Y_UNIT_TEST(OperationMatchUrl)
    {
        {
            std::string line = "tskv\trequest=/abc/xyz?a=b&c=d";
            auto parsed = Fields::parseTskv(line);
            UNIT_ASSERT(OperationMatchUrl("request", "/abc/xyz").matches(parsed));
            UNIT_ASSERT(OperationMatchUrl("request", "/abc/xyz/").matches(parsed));
            UNIT_ASSERT(!OperationMatchUrl("request", "/abc/xyz/def").matches(parsed));
            UNIT_ASSERT(!OperationMatchUrl("request", "/abc/xy").matches(parsed));
            UNIT_ASSERT(!OperationMatchUrl("request", "/abc/").matches(parsed));
            UNIT_ASSERT(!OperationMatchUrl("request", "/abc").matches(parsed));
            UNIT_ASSERT(!OperationMatchUrl("request", "/ab").matches(parsed));
        }

        {
            std::string line = "tskv\trequest=/abc/xyz";
            auto parsed = Fields::parseTskv(line);
            UNIT_ASSERT(OperationMatchUrl("request", "/abc/xyz/").matches(parsed));
            UNIT_ASSERT(OperationMatchUrl("request", "/abc/xyz").matches(parsed));
        }
    }
}
