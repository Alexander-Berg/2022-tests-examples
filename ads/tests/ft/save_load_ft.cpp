#include <ads/bsyeti/big_rt/lib/serializable_profile/mutable_row.h>

#include <ads/bsyeti/libs/yt_storage/load_profiles.h>
#include <ads/bsyeti/libs/yt_storage/save_profiles.h>
#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>

#include <ads/bsyeti/libs/yt_storage/tests/manual_test/test.h>

#include <ads/bsyeti/libs/codec_factory/factory.h>

#include <ads/bsyeti/libs/yt_mock/yt_mock_client.h>

#include <yt/yt/client/unittests/mock/transaction.h>
#include <yt/yt/core/logging/log_manager.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/yson/node/node_io.h>

#include <util/string/hex.h>

/*
    Schema:
        0 {"name": "UniqID", "type": "string", "sort_order": "ascending"},
        1 {"name": "CodecID", "type": "uint64"},
        2 {"name": "Main", "type": "string"},
        3 {"name": "MainPatch", "type": "string"},
        4 {"name": "UserItems", "type": "string"},
        5 {"name": "UserItemsPatch", "type": "string"},
        6 {"name": "Counters", "type": "string"},
        7 {"name": "CountersPatch", "type": "string"},
        8 {"name": "Applications", "type": "string"},
        9 {"name": "ApplicationsPatch", "type": "string"},
       10 {"name": "Banners", "type": "string"},
       11 {"name": "BannersPatch", "type": "string"},
       12 {"name": "Dmps", "type": "string"},
       13 {"name": "DmpsPatch", "type": "string"},
       14 {"name": "Queries", "type": "string"},
       15 {"name": "QueriesPatch", "type": "string"},
       16 {"name": "Aura", "type": "string"},
       17 {"name": "AuraPatch", "type": "string"},
       18 {"name": "DjProfiles", "type": "string"},
       19 {"name": "DjProfilesPatch", "type": "string"},

*/

const TString Profiles = "//test/Profiles";

NYT::TNode CreateSchema() {
    auto scheme = NYT::TNode::CreateList();
    auto createColumn = [] (const TString& type, const TString& name, bool key = false) {
        NYT::TNode column;
        column["Type"] = type;
        column["Name"] = name;
        if (key) {
            column["Key"] = true;
        }
        return column;
    };
    scheme
        .Add(createColumn("String", "UniqID", true))
        .Add(createColumn("Uint64", "CodecID"))
        .Add(createColumn("String", "Main"))
        .Add(createColumn("String", "MainPatch"))
        .Add(createColumn("String", "UserItems"))
        .Add(createColumn("String", "UserItemsPatch"))
        .Add(createColumn("String", "Counters"))
        .Add(createColumn("String", "CountersPatch"))
        .Add(createColumn("String", "Applications"))
        .Add(createColumn("String", "ApplicationsPatch"))
        .Add(createColumn("String", "Banners"))
        .Add(createColumn("String", "BannersPatch"))
        .Add(createColumn("String", "Dmps"))
        .Add(createColumn("String", "DmpsPatch"))
        .Add(createColumn("String", "Queries"))
        .Add(createColumn("String", "QueriesPatch"))
        .Add(createColumn("String", "Aura"))
        .Add(createColumn("String", "AuraPatch"))
        .Add(createColumn("String", "DjProfiles"))
        .Add(createColumn("String", "DjProfilesPatch"));
    return scheme;
}

class TYtClientMock: public NBSYeti::TMockupClient {
public:
    TYtClientMock(NBSYeti::TMockupClient::TMockTablePtr table) {
        Tables.emplace(Profiles, table);
    }

    NYT::TFuture<NYT::NYson::TYsonString> GetNode(
        const NYT::NYPath::TYPath& /*path*/,
        const NBSYeti::TGetNodeOptions& /*options*/) override {
        if (ReplicaID.IsEmpty()) {
            ReplicaID = NYT::TGuid::Create();
        }
        NYT::TNode node = NYT::TNode::CreateMap()(ToString(ReplicaID), NYT::TNode::CreateMap()("cluster_name", "fake")("replica_path", Profiles));
        return NYT::MakeFuture(NYT::NYson::TYsonString(NYT::NodeToYsonString(node)));
    }

    NYT::TFuture<std::vector<NYT::NTabletClient::TTableReplicaId>> GetInSyncReplicas(
        const NYT::NYPath::TYPath& /*path*/,
        const NYT::NTableClient::TNameTablePtr& /*nameTable*/,
        const NYT::TSharedRange<NYT::NTableClient::TLegacyKey>& /*keys*/,
        const NBSYeti::TGetInSyncReplicasOptions& /*options*/) override {
        std::vector<NYT::NTabletClient::TTableReplicaId> result = {ReplicaID};
        return NYT::MakeFuture(result);
    }
};

TEST(TSaveLoadTest, UserProfile) {
    NYT::NLogging::TLogManager::Get()->ConfigureFromEnv();

    auto table = NYT::New<NBSYeti::TMockupClient::TMockTable>(CreateSchema());
    auto master = NYT::New<TYtClientMock>(table);
    auto replica = NYT::New<TYtClientMock>(table);
    auto multiClient = NYTRpc::CreateYTMasterClient(
        {{NYT::TIntrusivePtr<TYtClientMock>(replica), "fake"}},
        NYT::TIntrusivePtr<TYtClientMock>(master));

    ManualTest(
        Profiles,
        "./bsyeti-configs/counter_info.json",
        multiClient);

    NYT::Shutdown();
}

TEST(TSaveLoadTest, Codecs) {
    NYT::NLogging::TLogManager::Get()->ConfigureFromEnv();

    auto table = NYT::New<NBSYeti::TMockupClient::TMockTable>(CreateSchema());
    auto master = NYT::New<TYtClientMock>(table);
    auto replica = NYT::New<TYtClientMock>(table);
    auto multiClient = NYTRpc::CreateYTMasterClient(
        {{NYT::TIntrusivePtr<TYtClientMock>(replica), "fake"}},
        NYT::TIntrusivePtr<TYtClientMock>(master));

    ManualTestCodecs(
        Profiles,
        "./bsyeti-configs/counter_info.json",
        multiClient);

    NYT::Shutdown();
}

TEST(TSaveLoadTest, Xdelta) {
    NYT::NLogging::TLogManager::Get()->ConfigureFromEnv();

    auto table = NYT::New<NBSYeti::TMockupClient::TMockTable>(CreateSchema());
    auto master = NYT::New<TYtClientMock>(table);
    auto replica = NYT::New<TYtClientMock>(table);
    auto multiClient = NYTRpc::CreateYTMasterClient(
        {{NYT::TIntrusivePtr<TYtClientMock>(replica), "fake"}},
        NYT::TIntrusivePtr<TYtClientMock>(master));

    ManualTestXdeltaCodec(
        Profiles,
        "./bsyeti-configs/counter_info.json",
        multiClient);

    NYT::Shutdown();
}

TEST(TSaveLoadTest, LoadSaveUserProfile) {
    NYT::NLogging::TLogManager::Get()->ConfigureFromEnv();

    auto table = NYT::New<NBSYeti::TMockupClient::TMockTable>(CreateSchema());
    auto master = NYT::New<TYtClientMock>(table);
    auto replica = NYT::New<TYtClientMock>(table);
    auto multiClient = NYTRpc::CreateYTMasterClient(
        {{NYT::TIntrusivePtr<TYtClientMock>(replica), "fake"}},
        NYT::TIntrusivePtr<TYtClientMock>(master));

    ManualTestLoadSaveUserProfile(
        Profiles,
        "./bsyeti-configs/counter_info.json",
        multiClient);

    NYT::Shutdown();
}

TEST(TSaveLoadTest, LoadProfilesWithWrongCodec) {
    NYT::NLogging::TLogManager::Get()->ConfigureFromEnv();

    auto table = NYT::New<NBSYeti::TMockupClient::TMockTable>(CreateSchema());
    auto master = NYT::New<TYtClientMock>(table);
    auto replica = NYT::New<TYtClientMock>(table);

    auto row = NYT::TNode::CreateMap();
    row["UniqID"] = "duid/1009822503601904909";
    auto main = "009502cad80362";
    row["Main"] = HexDecode(main, strlen(main));
    row["CodecID"] = 42u;
    master->InsertRow(Profiles, row);
    replica->InsertRow(Profiles, row);

    auto multiClient = NYTRpc::CreateYTMasterClient(
        {{NYT::TIntrusivePtr<TYtClientMock>(replica), "fake"}},
        NYT::TIntrusivePtr<TYtClientMock>(master));

    ManualTestLoadProfilesWithWrongCodecId(
        Profiles,
        "./bsyeti-configs/counter_info.json",
        multiClient);

    NYT::Shutdown();
}
