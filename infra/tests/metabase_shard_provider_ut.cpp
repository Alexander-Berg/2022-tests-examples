#include <library/cpp/testing/unittest/registar.h>

#include <infra/yasm/stockpile_client/metabase_shard_provider.h>
#include <infra/yasm/stockpile_client/state_ut.h>

using namespace NHistDb::NStockpile;


Y_UNIT_TEST_SUITE(TStockpileMetabaseAsyncShardProviderTest) {
    Y_UNIT_TEST(GetShardKeysAndCheck) {
        TLog log;
        TGrpcSettings::Init(NTest::SOLOMON_TESTS_CLIENT_ID, log);
        auto settings = NTest::CreateTestSettings();
        TTopologyObserver topologyObserver;
        TClusterProvider clusterProvider(EStockpileDatabase::Metabase, settings.ClusterInfo, log);
        TStockpileMetabaseAsyncShardProvider metabaseShardProvider(clusterProvider, topologyObserver, settings, log);

        UNIT_ASSERT_STRINGS_EQUAL(TGrpcSettings::Get().GetSolomonClientId(), NTest::SOLOMON_TESTS_CLIENT_ID);
        UNIT_ASSERT(clusterProvider.TryToUpdate());
        metabaseShardProvider.TryToUpdateShards();

        TVector<TAtomicSharedPtr<TMetabaseShard>> shards = metabaseShardProvider.GetShards();
        TVector<TMetabaseShardKey> shardKeys = metabaseShardProvider.GetShardKeys(true);
        THashSet<TString> itypes = metabaseShardProvider.GetItypes();

        UNIT_ASSERT_EQUAL(shardKeys.size(), shards.size());

        TVector<TMetabaseShardKey> shardKeysFromShards;
        shardKeysFromShards.reserve(shards.size());
        for (const auto& shard : shards) {
            shardKeysFromShards.push_back(shard->GetShardKey());
        }
        UNIT_ASSERT_EQUAL(shardKeys, shardKeysFromShards);

        THashSet<TString> projectsFromItypes;
        THashSet<TString> projectsFromShards;

        bool hasFoundTestShard = false;
        for (const auto& shardKey : shardKeys) {
            projectsFromShards.insert(shardKey.GetProjectId());
            UNIT_ASSERT_STRINGS_EQUAL(shardKey.GetService(), STOCKPILE_YASM_SERVICE);
            UNIT_ASSERT(shardKey.GetProjectId().StartsWith(STOCKPILE_YASM_PROJECTS_PREFIX));
            if (shardKey.GetProjectId().equal("yasm_yasm_unittest")) {
                hasFoundTestShard = true;
            }
        }
        for (const auto& itype : itypes) {
            projectsFromItypes.insert(TStringBuilder() << STOCKPILE_YASM_PROJECTS_PREFIX << itype);
        }

        UNIT_ASSERT_EQUAL(projectsFromItypes, projectsFromShards);
        UNIT_ASSERT(hasFoundTestShard);

        TMetabaseShardKey testShardKey("yasm_yasm_unittest", "host_0", "yasm");
        TMetabaseShardState testShard = metabaseShardProvider.ResolveOneShard(testShardKey);

        UNIT_ASSERT_STRINGS_EQUAL(testShard.Shard->GetShardKey().GetProjectId(), "yasm_yasm_unittest");
        UNIT_ASSERT_STRINGS_EQUAL(testShard.Shard->GetShardKey().GetCluster(), "host_0");
        UNIT_ASSERT_STRINGS_EQUAL(testShard.Shard->GetShardKey().GetService(), "yasm");

        TMaybe<TMetabaseShardState> testShardFound = metabaseShardProvider.FindOneShard(testShardKey);
        UNIT_ASSERT(testShardFound.Defined());

        TMetabaseShardKey badShardKey("not-exists", "not-exists", "not-exists");
        TMaybe<TMetabaseShardState> notExistingShardFound = metabaseShardProvider.FindOneShard(badShardKey);
        UNIT_ASSERT(!notExistingShardFound.Defined());
    }
}
