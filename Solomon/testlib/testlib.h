#pragma once

#include <library/cpp/testing/gtest/gtest.h>

#include <solomon/libs/cpp/kv/kv_client.h>

#include <util/generic/scope.h>

#include <memory>

namespace NSolomon::NKv {

/**
 * Decorator that counts number of requests and can imitate failures.
 */
class TTestRpc: public NKikimr::IKikimrRpc {
public:
    explicit TTestRpc(std::shared_ptr<NKikimr::IKikimrRpc> realRpc) noexcept
        : RealRpc_(std::move(realRpc))
    {
    }

public:
    /**
     * Get number of requests made via this transport so far.
     */
    size_t NumRequests() const;

    /**
     * Imitate network failure. Next `numSuccessfulRequests` will succeed, then next `numFailedRequests`
     * will result in an error.
     */
    void SetHostDown(size_t numSuccessfulRequests = 0, size_t numFailedRequests = Max<size_t>());

    /**
     * Stop imitating network failure.
     */
    void SetHostUp();

    /**
     * Reset number of requests and network failure imitation.
     */
    void Reset();

public:
    NKikimr::TKikimrAsyncResponse SchemeDescribe(const NKikimrClient::TSchemeDescribe& req) override;

    NKikimr::TKikimrAsyncResponse SchemeOperation(const NKikimrClient::TSchemeOperation& req) override;

    NKikimr::TKikimrAsyncResponse SchemeOperationStatus(const NKikimrClient::TSchemeOperationStatus& req) override;

    NKikimr::TKikimrAsyncResponse HiveCreateTablet(const NKikimrClient::THiveCreateTablet& req) override;

    NKikimr::TKikimrAsyncResponse TabletStateRequest(const NKikimrClient::TTabletStateRequest& req) override;

    NKikimr::TKikimrAsyncResponse LocalEnumerateTablets(const NKikimrClient::TLocalEnumerateTablets& req) override;

    NKikimr::TKikimrAsyncResponse KeyValue(const NKikimrClient::TKeyValueRequest& req) override;

    void Stop(bool wait) override;

private:
    NKikimr::TKikimrAsyncResponse MakeFailedFuture() const;
    bool CountRequest();

private:
    std::shared_ptr<NKikimr::IKikimrRpc> RealRpc_;

    size_t NumRequests_{0};
    size_t NumRequestsToSucceed_{0};
    size_t NumRequestsToFail_{0};
    TSpinLock Lock_;
};

/**
 * A fixture that discovers local YDB installation and creates a client for said installation.
 *
 * To start local YDB in your test, add the following lines to your `ya.make`:
 *
 * ```ya.make
 * INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/ydb_recipe/recipe_stable.inc)
 * ```
 *
 * It also may be a good idea to set test size to `MEDIUM`:
 *
 * ```ya.make
 * SIZE(MEDIUM)
 * ```
 *
 * Now, when you launch your tests, ya make will start a local YDB for you.
 *
 * To get access, use `TTestFixture` with your test suite as follows:
 *
 * ```
 * TEST_F(NSolomon::NKv::TTestFixture, TestName) {
 *     auto [transport, client] = CreateKvClient();
 *     // ...
 * }
 * ```
 */
class TTestFixture: public testing::Test {
public:
    void SetUp() override;

public:
    /**
     * Create a new test transport connected to the local YDB installation.
     */
    std::shared_ptr<TTestRpc> CreateRpc();

    /**
     * Create a new test transport and a new KV client using said transport.
     */
    std::pair<std::shared_ptr<TTestRpc>, TKikimrKvClient> CreateKvClient();

public:
    TString Endpoint;
    TString Database;
};

/**
 * A fixture that creates KV tablets before each test, and deletes them after.
 *
 * @tparam NumTablets   number of tablets to create.
 */
template <size_t NumTablets = 1>
class TTestFixtureTablet: public TTestFixture {
public:
    /**
     * Test transport and KV client that are used to interact with YDB.
     */
    std::shared_ptr<TTestRpc> Rpc;
    std::unique_ptr<TKikimrKvClient> Client;

    /**
     * KV client that's never disabled. This client does not use `Rpc` so it can be used to reach YDB even if
     * `Rpc` is down.
     */
    std::shared_ptr<TTestRpc> StableRpc;
    std::unique_ptr<TKikimrKvClient> StableClient;

    /**
     * Path to the solomon volume that contains a tablet.
     */
    TString SolomonVolumePath;

    /**
     * Id of the first created tablet.
     */
    ui64 TabletId = 0;

    /**
     * List of all tablet ids.
     */
    TVector<ui64> TabletIds;

public:
    void SetUp() override {
        TTestFixture::SetUp();

        Rpc = CreateRpc();
        Client = std::make_unique<TKikimrKvClient>(Rpc.get());

        StableRpc = CreateRpc();
        StableClient = std::make_unique<TKikimrKvClient>(StableRpc.get());

        auto name = ::testing::UnitTest::GetInstance()->current_test_info()->name();
        SolomonVolumePath = "/" + Database + "/kv_" + name;

        auto create = StableClient->CreateSolomonVolume(SolomonVolumePath, NumTablets)
            .ExtractValueSync();
        Y_ENSURE(create.Success(), "cannot create solomon volume: " << create.Error().Message());

        auto tabletIds = StableClient->ResolveTablets(SolomonVolumePath)
            .ExtractValueSync();
        Y_ENSURE(tabletIds.Success(), "cannot resolve tablet ids: " << tabletIds.Error().Message());

        TabletIds = tabletIds.Extract();
        TabletId = NumTablets > 0 ? TabletIds.at(0) : 0;
    }

protected:
    void TearDown() override {
        StableClient->DropSolomonVolume(SolomonVolumePath).GetValueSync();
        TTestFixture::TearDown();
    }
};

} // namespace NSolomon::NKv
