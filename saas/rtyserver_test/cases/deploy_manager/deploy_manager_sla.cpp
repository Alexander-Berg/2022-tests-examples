
#include "deploy_manager.h"

#include <saas/deploy_manager/protos/sla_description.pb.h>
#include <saas/deploy_manager/scripts/process_sla_description/action/action.h>

#include <google/protobuf/text_format.h>

START_TEST_DEFINE_PARENT(TestDeployManagerStoreSLA, TestDeployManager)

bool Run() override {
    static const TString service = "tests";

    UploadCommon();
    UploadService(service);
    ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", service);
    DeployBe(service);

    const TSet<TString> ownersLogins({"o-login1", "o-login2"});
    const TSet<TString> respLogins({"r-login1", "r-login2", "r-login3"});
    const TSet<TString> ferrymans({"saas_fm_1", "saas_fm_2"});
    const TString ticket = "TICKET-123";
    const ui64 maxDocs = 100;
    const ui64 searchRPS = 1000;
    const ui64 searchRPSPlanned = 2000;
    const ui64 indexRPS = 100;
    const ui64 totalIndexSizeBytes = 1024 * 1024 * 1024;
    const ui64 searchQ99Ms = 10;
    const ui64 searchQ999Ms = 20;
    const double unanswers5MinPercWarn = 0.01;
    const double unanswers5MinPercCrit = 0.02;
    const ui64 serviceWeight = 2;
    const ui64 abcUserService = 644;
    const ui64 abcQuotaService = 644;

    NDaemonController::TProcessSLADescriptionAction action(service, CTYPE);
    action.SetOwners(ownersLogins).SetResponsibles(respLogins).SetTicket(ticket).SetMaxDocsPerService(maxDocs);

    action.SetSearchRPS(searchRPS).SetSearchRPSPlanned(searchRPSPlanned).SetIndexRPS(indexRPS);
    action.SetTotalIndexSizeBytes(totalIndexSizeBytes);
    action.SetSearchQ99Ms(searchQ99Ms).SetSearchQ999Ms(searchQ999Ms);
    action.SetUnanswers5MinPercWarn(unanswers5MinPercWarn).SetUnanswers5MinPercCrit(unanswers5MinPercCrit);
    action.SetServiceWeight(serviceWeight);
    action.SetABCUserService(abcUserService);
    action.SetABCQuotaService(abcQuotaService);
    action.SetFerrymans(ferrymans);

    Controller->ExecuteActionOnDeployManager(action);

    NDaemonController::TProcessSLADescriptionAction sameAction(service, CTYPE);
    sameAction.SetOwners(ownersLogins).SetResponsibles(respLogins).SetTicket(ticket);
    sameAction.SetSearchRPS(searchRPS).SetSearchRPSPlanned(searchRPSPlanned).SetIndexRPS(indexRPS);
    sameAction.SetTotalIndexSizeBytes(totalIndexSizeBytes);
    sameAction.SetSearchQ99Ms(searchQ99Ms).SetSearchQ999Ms(searchQ999Ms);
    sameAction.SetUnanswers5MinPercWarn(unanswers5MinPercWarn).SetUnanswers5MinPercCrit(unanswers5MinPercCrit);
    sameAction.SetABCUserService(abcUserService).SetABCQuotaService(abcQuotaService);
    sameAction.SetFerrymans(ferrymans);
    Controller->ExecuteActionOnDeployManager(sameAction); //Absent ticket means "do not touch"
    CHECK_TEST_EQ(sameAction.GetInfo(), "Unchanged");
    {
        const TString path = "/configs/" + service + "/sla_description.conf";
        const TString result = Controller->SendCommandToDeployManager("process_storage?path=" + path + "&download=yes&action=get");
        DEBUG_LOG << "Fetched file: " << result << Endl;
        google::protobuf::TextFormat::Parser parser;
        NSaasProto::TSlaDescription description;
        VERIFY_WITH_LOG(parser.ParseFromString(result, &description), "Failed to parse prototext");


        CHECK_TEST_EQ(description.OwnerLoginSize(), ownersLogins.size());
        for (ui32 i = 0; i < description.OwnerLoginSize(); ++i) {
            CHECK_TEST_TRUE(ownersLogins.contains(description.GetOwnerLogin(i)));
        }

        CHECK_TEST_EQ(description.ResponsiblesSize(), respLogins.size());
        for (ui32 i = 0; i < description.ResponsiblesSize(); ++i) {
            CHECK_TEST_TRUE(respLogins.contains(description.GetResponsibles(i)));
        }

        CHECK_TEST_EQ(description.CTypeSLASize(), 1);
        const  NSaasProto::TCtypeSLA& ctypeSLA = description.GetCTypeSLA(0);
        CHECK_TEST_EQ(ctypeSLA.GetCtype(), CTYPE);
        CHECK_TEST_EQ(ctypeSLA.GetTicket(), ticket);
        CHECK_TEST_EQ(ctypeSLA.GetSoftMaxDocsPerService(), maxDocs);

        CHECK_TEST_EQ(ctypeSLA.GetSearchRPS(), searchRPS);
        CHECK_TEST_EQ(ctypeSLA.GetSearchRPSPlanned(), searchRPSPlanned);
        CHECK_TEST_EQ(ctypeSLA.GetIndexRPS(), indexRPS);
        CHECK_TEST_EQ(ctypeSLA.GetTotalIndexSizeBytes(), totalIndexSizeBytes);
        CHECK_TEST_EQ(ctypeSLA.GetSearchQ99Ms(), searchQ99Ms);
        CHECK_TEST_EQ(ctypeSLA.GetSearchQ999Ms(), searchQ999Ms);
        CHECK_TEST_EQ(ctypeSLA.GetUnanswers5MinPercWarn(), unanswers5MinPercWarn)
        CHECK_TEST_EQ(ctypeSLA.GetUnanswers5MinPercCrit(), unanswers5MinPercCrit);
        CHECK_TEST_EQ(ctypeSLA.GetServiceWeight(), serviceWeight);
        CHECK_TEST_EQ(ctypeSLA.GetABCUserService(), abcUserService);
        CHECK_TEST_EQ(ctypeSLA.GetABCQuotaService(), abcQuotaService);
        CHECK_TEST_EQ(ctypeSLA.NannyFerrymanSize(), ferrymans.size());
        for (ui32 i = 0; i < ctypeSLA.NannyFerrymanSize(); ++i) {
            CHECK_TEST_TRUE(ferrymans.contains(ctypeSLA.GetNannyFerryman(i)));
        }
    }

    {
        const TString result = Controller->SendCommandToDeployManager("process_sla_description?service=" + service + "&ctype=" + CTYPE + "&action=get");
        DEBUG_LOG << "Fetched file: " << result << Endl;
        NJson::TJsonValue newRes;
        VERIFY_WITH_LOG(NJson::ReadJsonFastTree(result, &newRes), "Failed to parse json");
        CHECK_TEST_EQ(newRes["ticket"].GetString(), ticket);
        CHECK_TEST_EQ(newRes["maxdocs"].GetStringRobust(), ToString<ui64>(maxDocs));
        CHECK_TEST_EQ(newRes["owners"].GetArray().size(), ownersLogins.size());
        for(const auto& elem: newRes["owners"].GetArray()) {
            CHECK_TEST_TRUE(ownersLogins.contains(elem.GetString()));
        }
        CHECK_TEST_EQ(newRes["responsibles"].GetArray().size(), respLogins.size());
        for(const auto& elem: newRes["responsibles"].GetArray()) {
            CHECK_TEST_TRUE(respLogins.contains(elem.GetString()));
        }

        CHECK_TEST_EQ(newRes["search_rps"].GetUInteger(), searchRPS);
        CHECK_TEST_EQ(newRes["search_rps_planned"].GetUInteger(), searchRPSPlanned);
        CHECK_TEST_EQ(newRes["index_rps"].GetUInteger(), indexRPS);
        CHECK_TEST_EQ(newRes["total_index_size_bytes"].GetUInteger(), totalIndexSizeBytes);
        CHECK_TEST_EQ(newRes["search_q_99_ms"].GetUInteger(), searchQ99Ms);
        CHECK_TEST_EQ(newRes["search_q_999_ms"].GetUInteger(), searchQ999Ms);
        CHECK_TEST_TRUE(Abs(newRes["unanswers_5min_perc_warn"].GetDouble() - unanswers5MinPercWarn) < 1e-5);
        CHECK_TEST_TRUE(Abs(newRes["unanswers_5min_perc_crit"].GetDouble() - unanswers5MinPercCrit) < 1e-5);
        CHECK_TEST_EQ(newRes["service_weight"].GetUInteger(), serviceWeight);
        CHECK_TEST_EQ(newRes["abc_user_service"].GetUInteger(), abcUserService);
        CHECK_TEST_EQ(newRes["abc_quota_service"].GetUInteger(), abcUserService);

        CHECK_TEST_EQ(newRes["ferrymans"].GetArray().size(), ferrymans.size());
        for(const auto& elem: newRes["ferrymans"].GetArray()) {
            CHECK_TEST_TRUE(ferrymans.contains(elem.GetString()));
        }
    }
    {
        NDaemonController::TProcessSLADescriptionAction patchAction(service, CTYPE);
        patchAction.SetMaxDocsPerService(10).SetPatch();
        Controller->ExecuteActionOnDeployManager(patchAction);
        const TString result = Controller->SendCommandToDeployManager("process_sla_description?service=" + service + "&ctype=" + CTYPE + "&action=get");
        DEBUG_LOG << "Fetched file: " << result << Endl;
        NJson::TJsonValue newRes;
        VERIFY_WITH_LOG(NJson::ReadJsonFastTree(result, &newRes), "Failed to parse json");
        CHECK_TEST_EQ(newRes["ticket"].GetString(), ticket);
        CHECK_TEST_EQ(newRes["maxdocs"].GetStringRobust(), "10");
        CHECK_TEST_EQ(newRes["owners"].GetArray().size(), ownersLogins.size());
        for(const auto& elem: newRes["owners"].GetArray()) {
            CHECK_TEST_TRUE(ownersLogins.contains(elem.GetString()));
        }
        CHECK_TEST_EQ(newRes["responsibles"].GetArray().size(), respLogins.size());
        for(const auto& elem: newRes["responsibles"].GetArray()) {
            CHECK_TEST_TRUE(respLogins.contains(elem.GetString()));
        }
    }
    {
        const TString result = Controller->SendCommandToDeployManager("process_sla_description?service=absent_service&action=get&ctype=" + CTYPE);
        NJson::TJsonValue newRes;
        VERIFY_WITH_LOG(NJson::ReadJsonFastTree(result, &newRes), "Failed to parse json");
        CHECK_TEST_TRUE(newRes.IsMap());
        CHECK_TEST_EQ(newRes.GetMap().size(), 0);
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerSLADefaults, TestDeployManager)

TString GenerateDefaultSLAConfig(const ui16 serviceWieght, const double warn, const double crit) const {
    NSaasProto::TCtypeSLA proto;
    proto.SetServiceWeight(serviceWieght);
    proto.SetUnanswers5MinPercWarn(warn);
    proto.SetUnanswers5MinPercCrit(crit);

    NProtoBuf::TextFormat::Printer printer;
    printer.SetHideUnknownFields(true);

    TString res;
    VERIFY_WITH_LOG(printer.PrintToString(proto, &res),  "Failed to serialize message : %s", res.data());
    return res;
}

bool Run() override {
    static const TString service = "tests";
    const TSet<TString> ownersLogins({"o-login1", "o-login2"});
    const TSet<TString> respLogins({"r-login1", "r-login2", "r-login3"});
    const TString ticket = "TICKET-123";

    const ui16 weight = 1;
    const double warn = 0.01;
    const double crit = 0.05;

    UploadCommon();
    UploadService(service);
    Controller->UploadDataToDeployManager(GenerateDefaultSLAConfig(weight, warn, crit), "/common/" + CTYPE + "/sla_description.conf");
    ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", service);
    DeployBe(service);


    NDaemonController::TProcessSLADescriptionAction action(service, CTYPE);
    action.SetOwners(ownersLogins).SetResponsibles(respLogins).SetTicket(ticket);
    action.SetUseDefault();
    Controller->ExecuteActionOnDeployManager(action);

    {
        const TString path = "/configs/" + service + "/sla_description.conf";
        const TString result = Controller->SendCommandToDeployManager("process_storage?path=" + path + "&download=yes&action=get");
        DEBUG_LOG << "Fetched file: " << result << Endl;
        google::protobuf::TextFormat::Parser parser;
        NSaasProto::TSlaDescription description;
        VERIFY_WITH_LOG(parser.ParseFromString(result, &description), "Failed to parse prototext");


        CHECK_TEST_EQ(description.OwnerLoginSize(), ownersLogins.size());
        for (ui32 i = 0; i < description.OwnerLoginSize(); ++i) {
            CHECK_TEST_TRUE(ownersLogins.contains(description.GetOwnerLogin(i)));
        }

        CHECK_TEST_EQ(description.ResponsiblesSize(), respLogins.size());
        for (ui32 i = 0; i < description.ResponsiblesSize(); ++i) {
            CHECK_TEST_TRUE(respLogins.contains(description.GetResponsibles(i)));
        }

        CHECK_TEST_EQ(description.CTypeSLASize(), 1);
        const  NSaasProto::TCtypeSLA& ctypeSLA = description.GetCTypeSLA(0);
        CHECK_TEST_EQ(ctypeSLA.GetCtype(), CTYPE);
        CHECK_TEST_EQ(ctypeSLA.GetTicket(), ticket);

        CHECK_TEST_EQ(ctypeSLA.GetUnanswers5MinPercWarn(), warn)
        CHECK_TEST_EQ(ctypeSLA.GetUnanswers5MinPercCrit(), crit);
        CHECK_TEST_EQ(ctypeSLA.GetServiceWeight(), weight);
    }
    return true;
}
};
