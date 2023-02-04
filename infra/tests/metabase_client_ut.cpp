#include <library/cpp/testing/unittest/registar.h>

#include <infra/yasm/stockpile_client/metabase_client.h>
#include <infra/yasm/stockpile_client/state_ut.h>

using namespace NHistDb::NStockpile;


Y_UNIT_TEST_SUITE(TMetabaseStatusStateTest) {
    Y_UNIT_TEST(TMetabaseStatusStateHandling) {
        TLog log;
        TGrpcSettings::Init(NTest::SOLOMON_TESTS_CLIENT_ID, log);
        auto settings = NTest::CreateTestSettings();
        TClusterProvider clusterProvider(EStockpileDatabase::Metabase, settings.ClusterInfo, log);

        UNIT_ASSERT_STRINGS_EQUAL(TGrpcSettings::Get().GetSolomonClientId(), NTest::SOLOMON_TESTS_CLIENT_ID);
        UNIT_ASSERT(clusterProvider.TryToUpdate());
        auto hosts = clusterProvider.GetHosts();

        TGrpcStateHandler handler(log);

        TList<TMetabaseStatusState> states;
        for (const auto& host : clusterProvider.GetHosts()) {
            states.emplace_back(handler.GetQueue(), host, log);
        }
        for (const auto& state : states) {
            UNIT_ASSERT(!state.IsSuccess());
            UNIT_ASSERT_STRINGS_EQUAL(state.GetRequestName(), NHistDb::NStockpile::NMetrics::STOCKPILE_METABASE_GRPC_SERVER_STATUS);
            UNIT_ASSERT(FindPtr(hosts, state.GetHost().Get()));
            UNIT_ASSERT(!state.IsFinished());
            UNIT_ASSERT(!state.IsFailed());
        }
        handler.Wait();

        bool hasFoundTestShard = false;
        for (const auto& state : states) {
            UNIT_ASSERT(state.IsFinished());
            UNIT_ASSERT(state.IsSuccess() || state.IsFailed());
            if (!state.IsFailed()) {
                for (const auto& shardStatus : state.GetResult()) {
                    auto shardKey = shardStatus.Key;
                    UNIT_ASSERT_STRINGS_EQUAL(shardKey.GetService(), STOCKPILE_YASM_SERVICE);
                    UNIT_ASSERT(shardKey.GetProjectId().StartsWith(STOCKPILE_YASM_PROJECTS_PREFIX));
                    if (shardKey.GetProjectId().equal("yasm_yasm_unittest")) {
                        hasFoundTestShard = true;
                    }
                }
            }
        }
        UNIT_ASSERT(hasFoundTestShard);
    }
}
