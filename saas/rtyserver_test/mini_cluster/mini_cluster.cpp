#include "mini_cluster.h"
#include "mini_cluster_searchmap.h"
#include <saas/rtyserver_test/common/hostname.h>
#include <saas/library/searchmap/searchmap.h>
#include <saas/library/searchmap/parsers/json/json.h>
#include <saas/library/storage/zoo/zoo_storage.h>
#include <library/cpp/zookeeper/zookeeper.h>
#include <saas/deploy_manager/meta/cluster.h>

using namespace NMiniCluster;

namespace NRTYDeploy {
    using NSaas::TZooStorage;
}

namespace {
    TAutoPtr<TDeployManagerConfig> CreateDmConfig(TClusterDeployManagers::TNodePtr node) {
        return new TDeployManagerConfig(*node->ConstructParams);
    }
}

TMiniCluster::TMiniCluster(const TConfig& config, bool withMetaServices)
    : Config(config)
    , Running(false)
    , ControllingDeployManager(false)
{
    TRY
        TSet<ui16> usedPorts;
        ui16 dmPort = 0;
        TString dmUriPrefix;
        for (TConfig::TNodesConfig::const_iterator i = config.Nodes.begin(); i != config.Nodes.end(); ++i) {
            ui16 controllerPort = 0;
            if (i->Product == "deploy_manager") {
                TClusterDeployManagers::TNodePtr newNode = DeployManagers.AddNode(*i, i - config.Nodes.begin(), 0, "");
                if (i->Controlling) {
                    ControllingDeployManager = true;
                    auto dmConfig = CreateDmConfig(newNode);
                    controllerPort = newNode->GetDaemonConfig()->GetController().Port;
                    dmPort = dmConfig->GetHttpOptionsConfig().Port;
                    dmUriPrefix = dmConfig->GetRequiredUriPrefix();
                }
            }
            VERIFY_WITH_LOG(!controllerPort || usedPorts.insert(controllerPort).second, "%s: controller %i port already used", i->Name.data(), (ui32)controllerPort);
        }
        Searchmap = config.Searchmap;
        for (TConfig::TNodesConfig::const_iterator i = config.Nodes.begin(); i != config.Nodes.end(); ++i) {
            ui16 controllerPort = 0;
            if (i->Product == "rtyserver")
                controllerPort = Backends.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product == "indexerproxy")
                controllerPort = Indexerproxies.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product == "searchproxy")
                controllerPort = Searchproxies.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product == "intsearch")
                controllerPort = IntSearches.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product == "external-script")
                controllerPort = ExternalScripts.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product == "distributor")
                controllerPort = Distributors.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product == "emulator")
                controllerPort = Emulators.AddNode(*i, i - config.Nodes.begin(), dmPort, dmUriPrefix)->GetDaemonConfig()->GetController().Port;
            else if (i->Product != "deploy_manager")
                FAIL_LOG("unknown product %s", i->Product.data());
            VERIFY_WITH_LOG(!controllerPort || usedPorts.insert(controllerPort).second, "%s: controller %i port already used", i->Name.data(), (ui32)controllerPort);
        }
        BuildCluster(withMetaServices);
    CATCH_AND_RETHROW("while init cluster");
}

void TMiniCluster::Run() {
    if (Running)
        return;
    CleanZooKeeper();
    GenerateTagsInfo();
    if (DeployManagers.GetNodes().size()) {
        auto cfg = CreateDmConfig(DeployManagers.GetNodes().begin()->second);
        THolder<NRTYDeploy::IVersionedStorage> storage(NRTYDeploy::IVersionedStorage::Create(cfg->GetStorageOptions()));
        TString smap;
        CHECK_WITH_LOG(storage->GetValue("/common/sbtests/cluster.meta", smap));
        CHECK_WITH_LOG(smap == " ");
    }
    DeployManagers.Run();

    if (DeployManagers.GetNodes().size()) {
        auto cfg = CreateDmConfig(DeployManagers.GetNodes().begin()->second);
        THolder<NRTYDeploy::IVersionedStorage> storage(NRTYDeploy::IVersionedStorage::Create(cfg->GetStorageOptions()));
        TString smap;
        CHECK_WITH_LOG(storage->GetValue("/common/sbtests/cluster.meta", smap));
        CHECK_WITH_LOG(smap == " ");
    }

    DEBUG_LOG << "Starting all binaries..." << Endl;
    Backends.Run();
    Emulators.Run();
    Indexerproxies.Run();
    Searchproxies.Run();
    IntSearches.Run();
    ExternalScripts.Run();
    Distributors.Run();
    DEBUG_LOG << "Starting all binaries... Ok" << Endl;
    Running = true;
}

void TMiniCluster::RunServers() {
    DEBUG_LOG << "Run all servers..." << Endl;
    Indexerproxies.RunServers();
    Searchproxies.RunServers();
    IntSearches.RunServers();
    DEBUG_LOG << "Run all servers... Ok" << Endl;
}

void TMiniCluster::Stop(bool clearStorage) {
    if (!Running)
        return;
    DEBUG_LOG << "Stopping all binaries..." << Endl;
    Distributors.Stop();
    ExternalScripts.Stop(TExecutePolicy::IGNORE_ERROR);
    DeployManagers.Stop(clearStorage);
    Searchproxies.Stop();
    IntSearches.Stop();
    Indexerproxies.Stop();
    Emulators.Stop();
    Backends.Stop();
    DEBUG_LOG << "Stopping all binaries... Ok" << Endl;
    Running = false;
}

bool TMiniCluster::DoWithNode(TString nodeName, TString action, TRtyTestNodeType product, NJson::TJsonValue params){
    switch (product){
    case TNODE_SCRIPT:
        return ExternalScripts.DoWithNode(nodeName, action, params);
    case TNODE_INDEXERPROXY:
        return Indexerproxies.DoWithNode(nodeName, action, params);
    case TNODE_SEARCHPROXY:
        return Searchproxies.DoWithNode(nodeName, action, params);
    case TNODE_RTYSERVER:
        return Backends.DoWithNode(nodeName, action, params);
    case TNODE_DISTRIBUTOR:
        return Distributors.DoWithNode(nodeName, action, params);
    default:
        ythrow yexception() << "not implemented for another products";
    }
}

TSet<TString> TMiniCluster::GetNodesNames(TRtyTestNodeType product) const {
    switch (product){
    case TNODE_SCRIPT:
        return ExternalScripts.GetNodesNames();
    case TNODE_INDEXERPROXY:
        return Indexerproxies.GetNodesNames();
    case TNODE_SEARCHPROXY:
        return Searchproxies.GetNodesNames();
    case TNODE_RTYSERVER:
        return Backends.GetNodesNames();
    case TNODE_DISTRIBUTOR:
        return Distributors.GetNodesNames();
    default:
        ythrow yexception() << "not implemented for another products";
    }
}

void TMiniCluster::AdaptBackendProxyConfig(TBackendProxyConfig& proxyConfig) const {
    VERIFY_WITH_LOG(Searchproxies.GetNodes().size() <= 1, "more than one Searchproxies");
    VERIFY_WITH_LOG(Indexerproxies.GetNodes().size() <= 1, "more than one Indexerproxies");
    VERIFY_WITH_LOG(DeployManagers.GetNodes().size() <= 1, "more than one deploy_manager");
    VERIFY_WITH_LOG(Backends.GetNodes().size() < 2 || !Searchproxies.GetNodes().empty() && !Indexerproxies.GetNodes().empty(), "if more than 1 rtyserver must be Searchproxies and Indexerproxies");
    proxyConfig.Indexer.PackSend = true;
    proxyConfig.Indexer.Protocol = "";
    proxyConfig.Controllers.clear();
    proxyConfig.ControllingDeployManager = ControllingDeployManager;
    for (TClusterBackends::TNodes::const_iterator i = Backends.GetNodes().begin(); i != Backends.GetNodes().end(); ++i) {
        proxyConfig.Searcher.Port = i->second->GetPort();
        proxyConfig.Indexer.Port = i->second->GetPort() + 2;
        proxyConfig.Controllers.push_back(TBackendProxyConfig::TAddress());
        proxyConfig.Controllers.back().Port = i->second->GetDaemonConfig()->GetController().Port;
    }

    if (!Searchproxies.GetNodes().empty()) {
        proxyConfig.Searcher.Port = Searchproxies.GetNodes().begin()->second->GetPort();
        proxyConfig.HasSearchproxy = true;
    }

    if (!Indexerproxies.GetNodes().empty()) {
        proxyConfig.Indexer.Port = Indexerproxies.GetNodes().begin()->second->GetPort();
        proxyConfig.Indexer.PackSend = false;
        proxyConfig.Indexer.Protocol = "proto2json";
        proxyConfig.Export.Port = proxyConfig.Indexer.Port + 2;
        proxyConfig.HasIndexerproxy = true;
    }
    if (!DeployManagers.GetNodes().empty()) {
        auto dmConfig = CreateDmConfig(DeployManagers.GetNodes().begin()->second);
        proxyConfig.DeployManager.Port = dmConfig->GetHttpOptionsConfig().Port;
        if (!!dmConfig->GetRequiredUriPrefix())
            proxyConfig.DeployManager.UriPrefix = "/" + dmConfig->GetRequiredUriPrefix();
    }
    for (auto&& node : Emulators.GetNodes()) {
        TBackendProxyConfig::TAddress addr;
        addr.Port = node.second->GetPort();
        proxyConfig.Emulators.insert(std::make_pair(node.first, addr));
        if (node.second->Config.RegisterController) {
            proxyConfig.Controllers.push_back(TBackendProxyConfig::TAddress());
            proxyConfig.Controllers.back().Port = node.second->GetDaemonConfig()->GetController().Port;
        }
    }
    DEBUG_LOG<<"search port " << proxyConfig.Searcher.Port << Endl;
    DEBUG_LOG<<"index port " << proxyConfig.Indexer.Port << Endl;
    DEBUG_LOG<<"dm port " << proxyConfig.DeployManager.Port << Endl;
}

void TMiniCluster::BuildCluster(const bool withMetaServices) {
    for (TClusterServices::const_iterator i = Searchmap.Services.begin(); i != Searchmap.Services.end(); ++i) {
        TString serviceName = i->Name;
        if (withMetaServices) {
            serviceName += "-search";
            NSearchMapParser::TMetaService metaService;
            metaService.Name = i->Name;
            metaService.Components.push_back(NSearchMapParser::TMetaComponent());
            metaService.Components.back().ServiceName = serviceName;
            Cluster.AddMetaService(metaService);
        }

        NSearchMapParser::TSearchMapServiceParser spr(serviceName, i->Options, { true, NSearchMapParser::SearchMapShards, {}});
        NSearchMapParser::TSearchMapService service = spr.Get();

        NSearchMapParser::TSearchMapReplica& replica = *service.AddReplica("default");
        for (size_t j = 0; j < i->Replicas.size(); ++j) {

            int nParts = i->Replicas[j].Backends.size();
            Y_VERIFY(nParts > 0);
            int shardsPerPart = NSearchMapParser::SearchMapShards / nParts;
            for (int k = 0; k < nParts; ++k) {
                ui16 sPort = 0;
                for (TClusterBackends::TNodes::const_iterator b = Backends.GetNodes().begin(); b != Backends.GetNodes().end(); ++b) {
                    if (i->Replicas[j].Backends[k].Name == b->second->Config.Name) {
                        sPort =  b->second->GetPort();
                        break;
                    }
                }
                if (sPort == 0) {
                    for (TClusterEmulators::TNodes::const_iterator b = Emulators.GetNodes().begin(); b != Emulators.GetNodes().end(); ++b) {
                        if (i->Replicas[j].Backends[k].Name == b->second->Config.Name) {
                            sPort = b->second->GetPort();
                            break;
                        }
                    }
                }
                if (sPort == 0) {
                    ERROR_LOG << "backend '" << i->Replicas[j].Backends[k].Name << "' not found" << Endl;
                }
                NSearchMapParser::TSearchMapHost host = i->Replicas[j].Backends[k].Host;

                host.Name = TestsHostName();
                host.SearchPort = sPort;
                host.IndexerPort = sPort + 2;
                if (host.Shards.GetMin() == 0 && host.Shards.GetMax() == 0) {
                    int shardMin = k * shardsPerPart;
                    int shardMax = (nParts - 1 == k) ? NSearchMapParser::SearchMapShards : (k + 1) * shardsPerPart - 1;
                    host.Shards.SetMin(shardMin);
                    host.Shards.SetMax(shardMax);
                }
                replica.Add(host);
            }
        }
        *Cluster.AddRTYService(serviceName) = service;
        DEBUG_LOG << service.SerializeToProto().DebugString() << Endl;
    }
}

void TMiniCluster::GenerateSearchmap(const TString& filename) const {
    TUnbufferedFileOutput fo(filename);
    fo << Cluster.Serialize();

    TString smap_filename = (TFsPath(filename).Parent() / GetSmapFilename()).GetPath();
    NSaas::TCluster::TSearchMaps smaps;
    Cluster.BuildSearchMaps(smaps, true);
    TUnbufferedFileOutput fo_json(smap_filename);
    fo_json << smaps.IP.SerializeToJson();
}

void TMiniCluster::GenerateTagsInfo() const {
    if (DeployManagers.GetNodes().empty())
        return;
    NJson::TJsonValue json(NJson::JSON_MAP);
    NJson::TJsonValue& ip = json.InsertValue("indexerproxy", NJson::JSON_MAP).InsertValue("slots", NJson::JSON_ARRAY);
    for (TClusterIndexerproxies::TNodes::const_iterator i = Indexerproxies.GetNodes().begin(); i != Indexerproxies.GetNodes().end(); ++i)
        ip.AppendValue(TestsHostName() + ":" + ToString(i->second->GetPort()));

    NJson::TJsonValue& sp = json.InsertValue("searchproxy", NJson::JSON_MAP).InsertValue("slots", NJson::JSON_ARRAY);
    for (TClusterSearchproxies::TNodes::const_iterator i = Searchproxies.GetNodes().begin(); i != Searchproxies.GetNodes().end(); ++i)
        sp.AppendValue(TestsHostName() + ":" + ToString(i->second->GetPort()));

    NJson::TJsonValue& is = json.InsertValue("intsearch", NJson::JSON_MAP).InsertValue("slots", NJson::JSON_ARRAY);
    for (TClusterSearchproxies::TNodes::const_iterator i = IntSearches.GetNodes().begin(); i != IntSearches.GetNodes().end(); ++i)
        is.AppendValue(TestsHostName() + ":" + ToString(i->second->GetPort()));

    NJson::TJsonValue& be = json.InsertValue("rtyserver", NJson::JSON_MAP).InsertValue("slots", NJson::JSON_ARRAY);
    for (TClusterBackends::TNodes::const_iterator i = Backends.GetNodes().begin(); i != Backends.GetNodes().end(); ++i)
        be.AppendValue(TestsHostName() + ":" + ToString(i->second->GetPort()));

    NJson::TJsonValue& dm = json.InsertValue("deploy_manager", NJson::JSON_MAP).InsertValue("slots", NJson::JSON_ARRAY);
    for (TClusterDeployManagers::TNodes::const_iterator i = DeployManagers.GetNodes().begin(); i != DeployManagers.GetNodes().end(); ++i)
        dm.AppendValue(TestsHostName() + ":" + ToString(i->second->GetPort()));

    TStringStream ss;
    NJson::WriteJson(&ss, &json, true, true);
    DEBUG_LOG << "Try to send init data to storage.." << Endl;
    auto cfg = CreateDmConfig(DeployManagers.GetNodes().begin()->second);
    ui32 maxAtt = 5;
    for (ui32 i = 0; i < maxAtt; ++i) {
        {
            THolder<NRTYDeploy::IVersionedStorage> storage(NRTYDeploy::IVersionedStorage::Create(cfg->GetStorageOptions()));
            DEBUG_LOG << "Try to send tags.info to storage.." << Endl;
            CHECK_WITH_LOG(storage->SetValue("/common/sbtests/tags.info", ss.Str()));
            DEBUG_LOG << "Try to send tags.info to storage..OK" << Endl;
            DEBUG_LOG << "Try to send cluster.meta to storage.." << Endl;
            CHECK_WITH_LOG(storage->SetValue("/common/sbtests/cluster.meta", " "));
            DEBUG_LOG << "Try to send cluster.meta to storage..OK" << Endl;
            DEBUG_LOG << "Try to send unused diff to storage.." << Endl;
            CHECK_WITH_LOG(storage->SetValue("/configs/unused/rtyserver.diff-unused", "{}"));
            DEBUG_LOG << "Try to send unused diff to storage..OK" << Endl;
            DEBUG_LOG << "Try to send init data to storage..OK" << Endl;
        }
        {
            ui32 errors = 0;
            THolder<NRTYDeploy::IVersionedStorage> storage(NRTYDeploy::IVersionedStorage::Create(cfg->GetStorageOptions()));
            if (!storage->ExistsNode("/common/sbtests/tags.info")) {
                if (i == maxAtt)
                    ythrow yexception() << "Incorrect zoo processing";
                else {
                    ERROR_LOG << "Incorrect zoo processing!!!" << Endl;
                }
                ++errors;
            }
            if (!storage->ExistsNode("/common/sbtests/cluster.meta")) {
                if (i == maxAtt)
                    ythrow yexception() << "Incorrect zoo processing";
                else {
                    ERROR_LOG << "Incorrect zoo processing!!!" << Endl;
                }
                ++errors;
            }
            if (!storage->ExistsNode("/configs/unused/rtyserver.diff-unused")) {
                if (i == maxAtt)
                    ythrow yexception() << "Incorrect zoo processing";
                else {
                    ERROR_LOG << "Incorrect zoo processing!!!" << Endl;
                }
                ++errors;
            }
            if (!errors)
                break;
            Sleep(TDuration::Seconds(1));
        }
    }
}

void TMiniCluster::CleanZooKeeper() const {
    if (DeployManagers.GetNodes().empty())
        return;
    auto cfg = CreateDmConfig(DeployManagers.GetNodes().begin()->second);
    if (!cfg->GetStorageOptions().Type == NRTYDeploy::IVersionedStorage::TOptions::ZOO)
        return;
    ui64 now = Now().MilliSeconds();
    NRTYDeploy::TStorageOptions opts = cfg->GetStorageOptions();
    opts.ZooOptions.Root.clear();
    NRTYDeploy::TZooStorage storage(opts);
    TVector<TString> nodes;
    storage.GetNodes("/", nodes, true);
    for (const auto& node : nodes) {
        try {
            TVector<TString> split = SplitString(node, "_");
            if (split.size() >= 3 && split[0] == "st" &&(now - FromString<ui64>(split[2]) > 24 * 3600 * 1000)) {
                DEBUG_LOG << "Delete old node " << node << ": " << storage.RemoveNode(node, false) << Endl;
            }
        } catch (...) {
            WARNING_LOG << "Removing node " << node << " failed: " << CurrentExceptionMessage() << Endl;
        }
    }

}

TMiniCluster::~TMiniCluster() {
    Stop(false);
}
