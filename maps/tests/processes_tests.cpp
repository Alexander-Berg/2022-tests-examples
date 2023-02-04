#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/http/include/test_utils.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/json/include/builder.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/hitman_processes/include/processes.h>

#include <chrono>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(hitman_processes_tests)
{

Y_UNIT_TEST(basic_test)
{
    hitman::HitmanClient client(hitman::SANDBOX_HITMAN_HOST, "fake-token");
    client.setTimeout(std::chrono::seconds(10))
          .setMaxRequestAttempts(6)
          .setRetryInitialTimeout(std::chrono::seconds(1))
          .setRetryTimeoutBackoff(2);

    // job 1 (NEW)
    const uint64_t jobId1 = 1;
    const uint64_t execId1 = 11;
    const std::string workflowId1 = "111";
    const std::string regionName1 = "1111";
    // job 2 (RUNNING)
    const uint64_t jobId2 = 2;
    const uint64_t execId2 = 22;
    const std::string workflowId2 = "222";
    const std::string regionName2 = "2222";
    // job 3 (SUCCEEDED)
    const uint64_t jobId3 = 3;
    const uint64_t execId3 = 33;
    const std::string workflowId3 = "333";
    const std::string regionName3 = "3333";
    // job 4 (FAILED)
    const uint64_t jobId4 = 4;
    const uint64_t execId4 = 44;
    const std::string workflowId4 = "444";
    const std::string regionName4 = "4444";

    http::MockHandle tolokersTasksMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + LOAD_TOLOKERS_TASKS_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ArrayBuilder b) {
                if (request.url.params() == "status=NEW") {
                    // job 1
                    b << [&](json::ObjectBuilder b) {
                        b["id"] = std::to_string(jobId1);
                        b["executions"] = [&](json::ArrayBuilder b) {
                            b << [&](json::ObjectBuilder b) {
                                b["id"] = std::to_string(execId1);
                                b["workflow"] = workflowId1;
                                b["status"] = toString(hitman::HitmanJobStatus::NEW);
                            };
                        };
                    };
                } else if (request.url.params() == "status=SUCCEEDED") {
                    // job 3
                    b << [&](json::ObjectBuilder b) {
                        b["id"] = std::to_string(jobId3);
                        b["executions"] = [&](json::ArrayBuilder b) {
                            b << [&](json::ObjectBuilder b) {
                                b["id"] = std::to_string(execId3);
                                b["workflow"] = workflowId3;
                                b["status"] = toString(hitman::HitmanJobStatus::SUCCEEDED);
                            };
                        };
                    };
                }
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle tolokersValidationMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + TOLOKERS_VALIDATION_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ArrayBuilder b) {
                if (request.url.params() == "status=RUNNING") {
                    // job 2
                    b << [&](json::ObjectBuilder b) {
                        b["id"] = std::to_string(jobId2);
                        b["executions"] = [&](json::ArrayBuilder b) {
                            b << [&](json::ObjectBuilder b) {
                                b["id"] = std::to_string(execId2);
                                b["workflow"] = workflowId2;
                                b["status"] = toString(hitman::HitmanJobStatus::RUNNING);
                            };
                        };
                    };
                }
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle assessorsTasksMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + LOAD_ASSESSORS_TASKS_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ArrayBuilder b) {
                if (request.url.params() == "status=SUCCEEDED") {
                    // job 3
                    b << [&](json::ObjectBuilder b) {
                        b["id"] = std::to_string(jobId3);
                        b["executions"] = [&](json::ArrayBuilder b) {
                            b << [&](json::ObjectBuilder b) {
                                b["id"] = std::to_string(execId3);
                                b["workflow"] = workflowId3;
                                b["status"] = toString(hitman::HitmanJobStatus::SUCCEEDED);
                            };
                        };
                    };
                }
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle assessorsValidationMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + ASSESSORS_VALIDATION_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ArrayBuilder b) {
                if (request.url.params() == "status=FAILED") {
                    // job 4
                    b << [&](json::ObjectBuilder b) {
                        b["id"] = std::to_string(jobId4);
                        b["executions"] = [&](json::ArrayBuilder b) {
                            b << [&](json::ObjectBuilder b) {
                                b["id"] = std::to_string(execId4);
                                b["workflow"] = workflowId4;
                                b["status"] = toString(hitman::HitmanJobStatus::FAILED);
                            };
                        };
                    };
                }
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle publicationMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + PUBLICATION_REQUEST_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest&) {
            return http::MockResponse("[]");
        }
    );

    http::MockHandle publicationRequestMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + PUBLICATION_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest&) {
            return http::MockResponse("[]");
        }
    );

    http::MockHandle issueMockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v2/process/" + CREATE_ST_ISSUE_PROCESS_CODE +"/jobs",
        [&](const http::MockRequest&) {
            return http::MockResponse("[]");
        }
    );


    http::MockHandle job1MockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v1/spec/job/" + std::to_string(jobId1),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["startProperties"] = [&](json::ObjectBuilder b) {
                    b["region"] = regionName1;
                };
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle job2MockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v1/spec/job/" + std::to_string(jobId2),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["startProperties"] = [&](json::ObjectBuilder b) {
                    b["region"] = regionName2;
                };
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle job3MockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v1/spec/job/" + std::to_string(jobId3),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["startProperties"] = [&](json::ObjectBuilder b) {
                    b["region"] = regionName3;
                };
            };
            return http::MockResponse(builder.str());
        }
    );

    http::MockHandle job4MockHandle = http::addMock(
        "https://" + hitman::SANDBOX_HITMAN_HOST + "/api/v1/spec/job/" + std::to_string(jobId4),
        [&](const http::MockRequest&) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["startProperties"] = [&](json::ObjectBuilder b) {
                    b["region"] = regionName4;
                };
            };
            return http::MockResponse(builder.str());
        }
    );


    std::set<std::string> testNames = loadRegionsInProcessing(client);

    EXPECT_EQ(std::set<std::string>({regionName1, regionName2}), testNames);
    EXPECT_TRUE(hasFailedJobs(client));
}

} // Y_UNIT_TEST_SUITE(hitman_processes_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
