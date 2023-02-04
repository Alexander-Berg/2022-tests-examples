#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/requests/impl/connection.h>

#include <chrono>
#include <thread>


using namespace maps::analyzer::requests;

const std::string URL = "urltoconnect.net";


TEST(ConnectionTests, HandlerFuncTest) {
    Request request{URL};
    Connection connection(std::move(request), {}, ResponseHandlers{});
    connection.complete();
}

TEST(ConnectionTests, HandlerClassTest) {
    std::size_t handleTimes = 0;
    Request request{URL};
    Connection connection(std::move(request), {}, {
        .onResponse = [&](auto&&) { ++handleTimes; }
    });
    connection.complete();

    EXPECT_EQ(handleTimes, 1u);
}

TEST(ConnectionTests, RequestTest) {
    Request post{URL};
    post.addPost("name", "value");
    Connection connection(std::move(post), {}, ResponseHandlers{});

    // TODO: check connection.handle() options :)
}

TEST(ConnectionTests, NonMultipartRequestTest) {
    Request post{URL};
    post.setBody("body");
    Connection connection(std::move(post), {}, ResponseHandlers{});

    // TODO: check connection.handle() options :)
}

TEST(ConnectionTests, MixedRequestTest) {
    Request multipart{URL};
    multipart.addPost("name", "value");
    EXPECT_THROW(multipart.setBody("body"), maps::Exception);

    Request other{URL};
    other.setBody("body");
    EXPECT_THROW(other.addPost("name", "value"), maps::Exception);
}

TEST(ConnectionTests, EasyHandleTest) {
    Request request{URL};
    Connection conn(std::move(request), {}, ResponseHandlers{});
    EXPECT_EQ(&conn, EasyHandle::connectionFromCurl(conn.easy().handle()));
}
