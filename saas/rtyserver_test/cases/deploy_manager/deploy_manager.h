#pragma once

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/deploy_manager/debug/processors.h>
#include <saas/library/searchmap/slots_pool.h>
#include <saas/library/sharding/sharding.h>
#include <saas/util/logging/trace.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestDeployManager)
private:
    void Deploy(const TString& service, const TString& serviceType, const TString& forceServices = "");
    void UploadFiles(const TString& pathInResources, const TString& pathInDM);
protected:
    static const TString CTYPE;


    bool CheckSlotsByTagStructure(const TString& res, const TString& dc, const ui32 slotsCountLimit) {
        NJson::TJsonValue result;
        if (!NJson::ReadJsonFastTree(res, &result))
            ythrow yexception() << "incorrect json from get_slots: " << res;
        if (!result.IsMap() || !result.Has(dc) || !result[dc].GetMap().size())
            ythrow yexception() << "must be dc '" << dc << "' with dc-keys in result: " << res;
        NRTYCluster::TCTypeCluster cTypeCluster;
        CHECK_TEST_TRUE(cTypeCluster.Deserialize(result));
        CHECK_TEST_TRUE(cTypeCluster.GetDC(dc));
        CHECK_TEST_TRUE(cTypeCluster.GetSlots().size() > slotsCountLimit);
        return true;
    }

    bool CheckUsingSlotsStructure(const TString& res, const TString& ctype, const TString& dc, const ui32 slotsCountLimitMin, const ui32 slotsCountLimitMax, const TSet<TString>* slots = nullptr) {
        NJson::TJsonValue result;

        DEBUG_LOG << res << "/" << ctype << "/" << dc << "/" << (slots ? slots->size() : 0) << Endl;

        CHECK_TEST_TRUE(NJson::ReadJsonFastTree(res, &result));

        CHECK_TEST_TRUE(result.IsMap());
        CHECK_TEST_TRUE(result.GetMapSafe().contains(ctype));

        NJson::TJsonValue cTypeJson = result.GetMapSafe().find(ctype)->second;
        CHECK_TEST_TRUE(cTypeJson.IsMap());

        if (!slotsCountLimitMax || (slots && !slots->size())) {
            if (!cTypeJson.GetMapSafe().contains(dc))
                return true;
        }
        CHECK_TEST_TRUE(cTypeJson.GetMapSafe().contains(dc));
        NJson::TJsonValue dcJson = cTypeJson.GetMapSafe().find(dc)->second;
        CHECK_TEST_TRUE(dcJson.IsArray());

        CHECK_TEST_TRUE(dcJson.GetArraySafe().size() >= slotsCountLimitMin);
        CHECK_TEST_TRUE(dcJson.GetArraySafe().size() <= slotsCountLimitMax);

        if (slots) {
            CHECK_TEST_EQ(dcJson.GetArraySafe().size(), slots->size());
        }
        for (auto&& i : dcJson.GetArraySafe()) {
            NSearchMapParser::TSearchMapHost host;
            CHECK_TEST_TRUE(host.Deserialize(i));
            if (slots) {
                CHECK_TEST_TRUE(slots->contains(host.GetSlotName()));
            }
        }
        return true;
    }

    bool CheckCommonSlotsStructure(const TString& res, const TString& ctype, const TString& dc, const ui32 slotsCountLimitMin, const ui32 slotsCountLimitMax, const TSet<TString>* slots = nullptr) {
        NJson::TJsonValue result;
        CHECK_TEST_TRUE(NJson::ReadJsonFastTree(res, &result));
        NRTYCluster::TCluster cluster;
        DEBUG_LOG << res << "/" << ctype << "/" << dc << "/" << (slots ? slots->size() : 0) << Endl;

        CHECK_TEST_TRUE(cluster.Deserialize(result));
        CHECK_TEST_TRUE(cluster.GetCTypeCluster().contains(ctype));

        auto slotsCluster = cluster.GetSlots();

        if (!!ctype) {
            NRTYCluster::TCTypeCluster cTypeCluster = cluster.GetCTypeCluster().find(ctype)->second;
            slotsCluster = cTypeCluster.GetSlots();

            if (!!dc) {
                CHECK_TEST_TRUE(cTypeCluster.GetDC(dc));
                slotsCluster = cTypeCluster.GetDC(dc)->GetSlots();
            }
        }
        CHECK_TEST_TRUE(slotsCluster.size() >= slotsCountLimitMin);
        CHECK_TEST_TRUE(slotsCluster.size() <= slotsCountLimitMax);
        if (slots) {
            CHECK_TEST_EQ(slotsCluster.size(), slots->size());

            for (auto&& i : *slots) {
                CHECK_TEST_TRUE(slotsCluster.contains(i));
            }
        }
        return true;
    }

public:
    void CheckServiceConfig(const TString& service, const ui32 version, i32 be = -1);
    bool CheckServiceConfigSafe(const TString& service, const ui32 version, i32 be = -1);
    bool CheckServiceStatus(const TString& status, i32 be = -1);
    bool CheckBackends(const TVector<TString>& backends);
    void UploadCommon(i32 ver = -1, bool uploadEmptySearchmap = true);
    void UploadService(const TString& service, ui32 version = 0);
    void DeployBe(const TString& service, ui32 version);
    void DeployBe(const TString& service);
    void DeploySp(const TString& forceServices = "");
    void DeployIp(const TString& forceServices = "");
    void UploadServiceCommon(const TString& root, const TString& service, ui32 version);
    void DeployMetaservice(const TString& service);
    void Check(ui32 be, const TString& service);
    NSearchMapParser::TSlotsPool ConfigureCluster(ui32 replics, ui32 shardsInReplica, NSaas::ShardingType shardBy, const TString& serviceType, const TString& service);
    NSearchMapParser::TSlotsPool ConfigureCluster(ui32 replics, ui32 shardsInReplica, const NSaas::TShardsDispatcher::TContext& shardBy, const TString& serviceType, const TString& service);
    void CheckDocuments(const TVector<NRTYServer::TMessage>& messages, const TSet<std::pair<ui64, TString>>& deleted, ui32 slotsCount = 0);
    void CheckEnabled(const NSearchMapParser::TSlotsPool* disabledSlotsIndex, const NSearchMapParser::TSlotsPool* disabledSlotsSearch) const;
    void RenewActiveBackends();
    NSearchMapParser::TSearchMap GetSearchMapFromDM(const TString& service = "indexerproxy", const TString& ctype = "sbtests") const;
    NSearchMapParser::TSearchMapHost FindBackend(ui32 be, const TString& service = "tests", const TString& ctype = "sbtests") const;

    bool NeedStartBackend() override {
        return false;
    }
    void CheckConfigs(bool equal);

    bool ConfigureRuns(TTestVariants& /*variants*/, bool) override {
        return true;
    }
    bool SetAlertsUniqueNames();
    bool CheckAlertsCount(const ui16 mustBe, const TString& cgis, NJson::TJsonValue& alerts, const int jugCnt=-1);
};

class TDMRequestFailer : public TDebugProcessorDMRequestFailer {
public:
    TDMRequestFailer(const char* command);
    ~TDMRequestFailer();
};

class TDMBuildTaskFailer : public TDebugProcessorBuildTaskFailer {
public:
    TDMBuildTaskFailer(const char* taskType);
    ~TDMBuildTaskFailer();
};
