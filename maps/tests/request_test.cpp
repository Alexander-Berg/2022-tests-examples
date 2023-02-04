#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/requests/include/request.h>

#include <chrono>
#include <thread>


using namespace maps::analyzer::requests;

const std::string URL = "urltoconnect.net";

TEST(RequestTests, Simple) {
    Request req{URL};
}

TEST(RequestTests, Get) {
    Request get{URL};
    get.setMethod(HttpMethod::GET);
    EXPECT_THROW(get.addPost("field", "value"), maps::Exception);
    EXPECT_THROW(get.setBody("body"), maps::Exception);
}

TEST(RequestTests, Post) {
    Request post{URL};
    post.addPost("field", "value");
    EXPECT_THROW(post.setBody("body"), maps::Exception);
    EXPECT_THROW(post.setMethod(HttpMethod::GET), maps::Exception);
    post.setMethod(HttpMethod::POST);
}

TEST(RequestTests, Body) {
    Request post{URL};
    post.setBody("body");
    EXPECT_THROW(post.addPost("field", "value"), maps::Exception);
    EXPECT_THROW(post.setMethod(HttpMethod::GET), maps::Exception);
    post.setMethod(HttpMethod::POST);
}
