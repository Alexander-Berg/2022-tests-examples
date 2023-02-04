#pragma once

#include <library/cpp/testing/unittest/registar.h>

#include <balancer/serval/contrib/cone/cold.h>
#include <balancer/serval/contrib/cone/cone.hh>
#include <balancer/serval/core/address.h>
#include <balancer/serval/core/buffer.h>
#include <balancer/serval/core/config.h>
#include <balancer/serval/core/io.h>
#include <balancer/serval/core/http.h>

#include <util/network/socket.h>

static inline std::pair<NSv::TFile, NSv::TFile> SocketPair() {
    int fds[2];
    UNIT_ASSERT(socketpair(AF_UNIX, SOCK_STREAM, 0, fds) == 0);
    SetNonBlock(fds[0], true);
    SetNonBlock(fds[1], true);
    return {fds[0], fds[1]};
}

static inline std::pair<NSv::TAction, NSv::TAuxData> FromConfig(const YAML::Node& node) {
    NSv::TAuxData aux;
    NSv::TAction action = aux.Action(node);
    return {std::move(action), std::move(aux)};
}

namespace {
    class TStreamWithFakeSource : public NSv::TStreamProxy {
    public:
        using NSv::TStreamProxy::TStreamProxy;

        NSv::IP Peer() const noexcept override {
            auto rqh = const_cast<TStreamWithFakeSource*>(this)->Head();
            auto xff = rqh->find("x-forwarded-for");
            auto url = NSv::URL::Parse(xff != rqh->end() ? TString(xff->second) : "");
            return url->Host ? NSv::IP::Parse(url->Host, url->Port) : NSv::TStreamProxy::Peer();
        }
    };
}

template <typename F, typename G>
static inline void ServerClient(F&& server, G&& client, bool h2 = false) {
    auto chan = SocketPair();
    cone::guard s = [&, fd = std::move(chan.first)]() mutable {
        return NSv::Serve(fd, [&](NSv::IStreamPtr s) {
            NSv::IStreamPtr req = std::make_shared<TStreamWithFakeSource>(std::move(s));
            return server(req);
        }, nullptr);
    };
    auto fd = std::move(chan.second); // close before terminating the server
    client(*NSv::H2Client(fd, nullptr, {.ForceH2 = h2}));
}

static inline auto ExpectError(int num, const std::pair<NSv::TAction, NSv::TAuxData>& f) {
    return [num, &f](NSv::IStreamPtr& req) {
        UNIT_ASSERT(!f.first(req));
        if (mun_errno != num)
            mun_error_show("unexpected", nullptr);
        UNIT_ASSERT_VALUES_EQUAL(mun_errno, num);
        return false;
    };
}

static inline NSv::TFile BindBackend(ui32 port) {
    auto sk = NSv::TFile::Bind(NSv::IP::Parse("[::1]", port), 256, [](int f) {
        return setsockopt(f, SOL_SOCKET, SO_REUSEADDR, &NSv::ConstantOne, sizeof(int)) < 0;
    });
    if (!sk MUN_RETHROW)
        return {};
    return sk;
}

template <typename F>
static cone::ref RunBackend(ui32 port, F&& f) {
    auto sk = BindBackend(port);
    if (!sk MUN_RETHROW)
        return {};
    return [sk = std::move(sk), f = std::forward<F>(f)]() mutable {
        TVector<cone::guard> spawned;
        while (auto child = NSv::TFile::Accept(sk))
            spawned.emplace_back([&f, fd{std::move(child)}]() mutable {
                return NSv::Serve(fd, [&](NSv::IStreamPtr stream) { return f(stream); }, nullptr); });
        return false;
    };
}

#define UT_ACTION_IMPL(name, fname) \
    static bool fname(NSv::IStreamPtr&); \
    SV_DEFINE_ACTION(name, [](const YAML::Node&, NSv::TAuxData&) { return fname; }); \
    static bool fname(NSv::IStreamPtr& req Y_DECLARE_UNUSED)

#define UT_ACTION(name) UT_ACTION_IMPL(name, Y_GENERATE_UNIQUE_ID(TestAction))

#define Y_UNIT_TEST_TEMPLATE(N, T, ...) \
    using TBase##N = T;                                                   \
    struct TTest##N : public TCurrentTestCase, TBase##N {                 \
        TTest##N(size_t i, TBase##N p) : TBase##N(std::move(p)) {         \
            NameStr_ = TString::Join(#N "/", ToString(i));                \
            Name_ = NameStr_.c_str();                                     \
        }                                                                 \
        void Execute_(NUnitTest::TTestContext&) override;                 \
        TString NameStr_;                                                 \
    };                                                                    \
    static size_t Register##N = []() {                                    \
        size_t i = 0;                                                     \
        for (auto& p : (TBase##N[]){__VA_ARGS__})                         \
            TCurrentTest::AddTest([i = i++, p = std::move(p)]() mutable { \
                return MakeHolder<TTest##N>(i, std::move(p)); });         \
        return i;                                                         \
    }();                                                                  \
    void TTest##N::Execute_(NUnitTest::TTestContext& ut_context Y_DECLARE_UNUSED)

// These are macros to preserve line numbers.
#define EXPECT_HEADER(head, name, value) do {                   \
    UNIT_ASSERT((head).find(name) != (head).end());             \
    UNIT_ASSERT_VALUES_EQUAL((head).find(name)->second, value); \
} while (0)

#define EXPECT_NO_HEADER(head, name) do {                       \
    UNIT_ASSERT_C((head).find(name) == (head).end(), name);     \
} while (0)

#define EXPECT_HEAD(head, code, payload, ...) do {    \
    auto __h = (head);                                \
    UNIT_ASSERT(__h);                                 \
    UNIT_ASSERT_VALUES_EQUAL(__h->Code, code);        \
    NSv::THeaderVector __e{__VA_ARGS__};              \
    for (const auto& __eh : __e)                      \
        EXPECT_HEADER(*__h, __eh.first, __eh.second); \
} while (0)

#define EXPECT_RESPONSE(rsp, code, payload, ...) do {         \
    auto __v = rsp;                                           \
    UNIT_ASSERT(__v);                                         \
    EXPECT_HEAD(__v->Head(), code, payload, __VA_ARGS__);     \
    UNIT_ASSERT_VALUES_EQUAL(::NSv::ReadFrom(*__v), payload); \
} while (0)
