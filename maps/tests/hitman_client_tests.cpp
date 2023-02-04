#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <maps/libs/common/include/exception.h>

#include <maps/libs/http/include/test_utils.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/json/include/builder.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/hitman_client/include/client.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline::hitman {

namespace tests {

Y_UNIT_TEST_SUITE(hitman_client_tests)
{

Y_UNIT_TEST(set_client_options)
{
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    const std::string& schema = "http://";
    const std::chrono::milliseconds timeout(10);
    const size_t maxRequestAttempts = 15;
    const std::chrono::milliseconds retryInitialTimeout(30);
    const double retryTimeoutBackoff = 2.;

    HitmanClient client(host, authToken);
    client.setSchema(schema)
          .setTimeout(timeout)
          .setMaxRequestAttempts(maxRequestAttempts)
          .setRetryInitialTimeout(retryInitialTimeout)
          .setRetryTimeoutBackoff(retryTimeoutBackoff);

    EXPECT_EQ(schema, client.schema());
    EXPECT_EQ(timeout, client.timeout());
    EXPECT_EQ(maxRequestAttempts, client.maxRequestAttempts());
    EXPECT_EQ(retryInitialTimeout, client.retryInitialTimeout());
    EXPECT_EQ(retryTimeoutBackoff, client.retryTimeoutBackoff());
}

Y_UNIT_TEST(run_process_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const std::string process1Code = "process1";
    const std::string process2Code = "process2";

    auto process1MockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/start/" + process1Code,
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                json::Value properties
                    = json::Value::fromString(request.body)["properties"];
                b["id"] = std::to_string(std::stoull(properties["id"].as<std::string>()) + 1);
            };
            return http::MockResponse(builder.str());
        }
    );

    auto process2MockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/start/" + process2Code,
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                json::Value properties
                    = json::Value::fromString(request.body)["properties"];
                b["id"] = std::to_string(std::stoull(properties["id"].as<std::string>()) + 2);
            };
            return http::MockResponse(builder.str());
        }
    );

    const std::string requester = "user";
    const std::string id = "1";

    HitmanJobId job1Id = client.runProcess(process1Code, requester, {{"id", id}});
    EXPECT_EQ(job1Id, std::stoull(id) + 1);

    HitmanJobId job2Id = client.runProcess(process2Code, requester, {{"id", id}});
    EXPECT_EQ(job2Id, std::stoull(id) + 2);
}

Y_UNIT_TEST(fail_run_process_with_empty_requester_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const std::string processCode = "process";

    auto processMockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/start/" + processCode,
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                json::Value jsonBody = json::Value::fromString(request.body);
                if (jsonBody["requester"].as<std::string>().empty()) {
                    b["success"] = false;
                    b["code"] = "COMMON_ERROR_CODE";
                    b["msg"] = "Illegal requester login: []";
                } else {
                    b["id"] = "1";
                }
            };
            return http::MockResponse(builder.str());
        }
    );

    EXPECT_THROW(client.runProcess(processCode, "", {}), maps::Exception);
}

Y_UNIT_TEST(server_response_code_500_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const std::string processCode = "process";

    auto processMockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/start/" + processCode,
        [&](const http::MockRequest&) {
            return http::MockResponse::withStatus(501);
        }
    );

    EXPECT_THROW(client.runProcess(processCode, "", {}), ServerError);
}

Y_UNIT_TEST(server_response_code_404_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const std::string processCode = "process";

    auto processMockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/start/" + processCode,
        [&](const http::MockRequest&) {
            return http::MockResponse::withStatus(404);
        }
    );

    EXPECT_THROW(client.runProcess(processCode, "", {}), ClientError);
}

Y_UNIT_TEST(get_job_status_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const HitmanJobId newJobId = 1;
    const HitmanJobId successJobId = 2;
    const HitmanJobId unknownJobId = 3;

    auto newJobMockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/" + std::to_string(newJobId),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["status"] = "NEW";
            };
            return http::MockResponse(builder.str());
        }
    );

    auto successJobMockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/" + std::to_string(successJobId),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["status"] = "SUCCEEDED";
            };
            return http::MockResponse(builder.str());
        }
    );

    auto unknownJobMockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/execution/" + std::to_string(unknownJobId),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["success"] = false;
                b["code"] = "DOES_NOT_EXIST";
            };
            return http::MockResponse(builder.str());
        }
    );

    EXPECT_EQ(client.getJobStatus(newJobId), HitmanJobStatus::NEW);
    EXPECT_EQ(client.getJobStatus(successJobId), HitmanJobStatus::SUCCEEDED);
    EXPECT_THROW(client.getJobStatus(unknownJobId), maps::Exception);
}

Y_UNIT_TEST(get_running_jobs_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const std::string process1Code = "process1";
    const std::string process2Code = "process2";

    const HitmanJobId job1Id = 1;
    const uint64_t exec1_1Id = 1111;
    const std::string workflow1_1Id = "11";
    const std::string instance1_1Id = "111";
    const uint64_t exec1_2Id = 1112;
    const std::string workflow1_2Id = "12";
    const std::string instance1_2Id = "112";
    const HitmanJobId job2Id = 2;
    const uint64_t exec2Id = 2222;
    const std::string workflow2Id = "222";

    auto process1MockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v2/process/" + process1Code +"/jobs",
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ArrayBuilder b) {
                b << [&](json::ObjectBuilder b) {
                    b["id"] = std::to_string(job1Id);
                    b["executions"] = [&](json::ArrayBuilder b) {
                        b << [&](json::ObjectBuilder b) {
                            b["id"] = std::to_string(exec1_1Id);
                            b["workflow"] = workflow1_1Id + "/" + instance1_1Id;
                            b["status"] = toString(HitmanJobStatus::FAILED);
                        };
                        b << [&](json::ObjectBuilder b) {
                            b["id"] = std::to_string(exec1_2Id);
                            b["workflow"] = workflow1_2Id + "/" + instance1_2Id;
                            b["status"] = toString(HitmanJobStatus::RUNNING);
                        };
                    };
                };
                b << [&](json::ObjectBuilder b) {
                    b["id"] = std::to_string(job2Id);
                    b["executions"] = [&](json::ArrayBuilder b) {
                        b << [&](json::ObjectBuilder b) {
                            b["id"] = std::to_string(exec2Id);
                            b["workflow"] = workflow2Id;
                            b["status"] = toString(HitmanJobStatus::RUNNING);
                        };
                    };
                };
            };
            return http::MockResponse(builder.str());
        }
    );

    auto process2MockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v2/process/" + process2Code + "/jobs",
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["success"] = false;
                b["code"] = "DOES_NOT_EXIST";
            };
            return http::MockResponse(builder.str());
        }
    );

    std::vector<HitmanJob> runningJobs
        = client.getJobsByStatus(process1Code, HitmanJobStatus::RUNNING);
    EXPECT_EQ(runningJobs.size(), 2u);
    EXPECT_EQ(runningJobs[0].id, job1Id);
    EXPECT_EQ(runningJobs[0].workflowId, workflow1_2Id);
    EXPECT_EQ(runningJobs[0].instanceId, instance1_2Id);
    EXPECT_EQ(runningJobs[1].id, job2Id);
    EXPECT_EQ(runningJobs[1].workflowId, workflow2Id);
    EXPECT_TRUE(runningJobs[1].instanceId.empty());

    EXPECT_THROW(client.getJobsByStatus(process2Code, HitmanJobStatus::SUCCEEDED), maps::Exception);
}

Y_UNIT_TEST(get_process_properties_test)
{
    const std::string schema = "https://";
    const std::string host = SANDBOX_HITMAN_HOST;
    const std::string authToken = "fake-token";

    HitmanClient client(host, authToken);

    client.setSchema(schema)
          .setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(5)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2.);

    const HitmanJobId jobId = 1;
    const HitmanProcessProperties properties{
        {"property1", "value1"},
        {"property2", "value2"}
    };

    auto process1MockHandle = http::addMock(
        schema + SANDBOX_HITMAN_HOST + "/api/v1/spec/job/" + std::to_string(jobId),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["startProperties"] = [&](json::ObjectBuilder b) {
                    for (const auto& [name, value] : properties) {
                        b[name] = value;
                    }
                };
            };
            return http::MockResponse(builder.str());
        }
    );

    HitmanProcessProperties testProperties = client.getProcessProperties(jobId);

    EXPECT_EQ(properties, testProperties);
}

} // Y_UNIT_TEST_SUITE(hitman_client_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline::hitman
