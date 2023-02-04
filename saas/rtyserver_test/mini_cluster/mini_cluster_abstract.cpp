#include "mini_cluster_abstract.h"
#include <saas/rtyserver/common/port.h>
#include <library/cpp/http/io/stream.h>
#include <library/cpp/json/json_reader.h>
#include <util/system/env.h>
#include <util/stream/file.h>
#include <util/system/fs.h>
#include <util/system/hostname.h>
#include <util/system/shellcommand.h>

namespace NMiniCluster {
    void TConfig::Init(const TString& filename) {
        if (!filename)
            return;
        VERIFY_WITH_LOG(TFsPath(filename).Exists(), "file does not exists %s",
            (filename + (TFsPath(filename).IsAbsolute() ? "" : " cwd:" + NFs::CurrentWorkingDirectory())).data());
        TUnbufferedFileInput fi(filename);
        NJson::TJsonValue json;
        VERIFY_WITH_LOG(NJson::ReadJsonTree(&fi, true, &json), "errors in json %s", filename.data());
        TSet<TString> usedProducts;
        for (NJson::TJsonValue::TArray::const_iterator i = json.GetArray().begin(); i != json.GetArray().end(); ++i) {
            if ((*i)["product"].GetString() == "searchmap"){
                Searchmap.Init(*i);
                continue;
            }
            Nodes.push_back(TNodeConfig(*this, *i));
            if (!Nodes.back().External && Nodes.back().Product != "external-script")
                Nodes.back().External = !usedProducts.insert(Nodes.back().Product).second;
            if (i->Has("copies") && (*i)["copies"].GetInteger() > 1){
                TString baseNodeName = Nodes.back().Name;
                Nodes.back().Name = baseNodeName + "0";
                for (int j = 1; j < (*i)["copies"].GetInteger(); ++j){
                    Nodes.push_back(TNodeConfig(*this, *i));
                    Nodes.back().Name = baseNodeName + ToString(j);
                    Nodes.back().External = (Nodes.back().Product != "external-script");
                }
            }
        }
        ValidateSearchmap();
        ProcessVars(filename);
    }

    void TConfig::ValidateSearchmap(){
        if (Searchmap.Services.size() == 0){
            NJson::TJsonValue defaultMap(NJson::JSON_ARRAY);
            Searchmap.InitShort(defaultMap);
        }

        TSet<TString> nodeNames, nodeSmap, serviceNames;
        for (TConfig::TNodesConfig::const_iterator i = Nodes.begin(); i != Nodes.end(); ++i){
            if (i->Product == "rtyserver" || i->Product == "emulator")
                nodeNames.insert(i->Name);
        }
        if (Searchmap.Services.size() == 1 && Searchmap.Services[0].Replicas.size() == 0){
            for (const auto& i : nodeNames){
                NJson::TJsonValue defaultMap(NJson::JSON_ARRAY);
                defaultMap.AppendValue(i);
                Searchmap.Services[0].Replicas.push_back(TClusterReplica(defaultMap));
            }
        }

        for (TClusterServices::const_iterator i = Searchmap.Services.begin(); i != Searchmap.Services.end(); ++i){
            if (!serviceNames.insert(i->Name).second)
                ythrow yexception() << "service name not unique: " << i->Name;
            for (TClusterReplicas::const_iterator j = i->Replicas.begin(); j != i->Replicas.end(); ++j){
                for (TVector<TClusterBackend>::const_iterator k = j->Backends.begin(); k != j->Backends.end(); ++k){
                    if (!nodeNames.contains(k->Name))
                        ythrow yexception() << "unknown node in searchmap: " << k->Name;
                    else
                        nodeSmap.insert(k->Name);
                }
            }
        }
        if (nodeNames.size() != nodeSmap.size())
            ythrow yexception() << "problems with nodes count: backends in config: " << nodeNames.size() << ", in searchmap: " << nodeSmap.size();
    }

    void GetResources(const TString& nodeName, const TFsPath& configPath, const TString& outFile){
        TFsPath getterPath = (configPath.GetName().StartsWith('_') ? configPath.Parent().Parent() : configPath.Parent() ) / "get_resources.py";
        if (!getterPath.Exists())
            ythrow yexception() << "file " << getterPath << " required for getting resources" << Endl;

        TShellCommand cmd(getterPath.GetPath());
        cmd << configPath.GetPath() << nodeName << outFile;
        INFO_LOG << "getting resources, node " << nodeName << Endl;
        cmd.Run();
        INFO_LOG << "get_resources result, node " << nodeName << ": " << cmd.GetOutput() << Endl;
        INFO_LOG << "err.out: " << cmd.GetError() << Endl;
        if (cmd.GetStatus() != TShellCommand::SHELL_FINISHED){
            if (cmd.GetStatus() == TShellCommand::SHELL_INTERNAL_ERROR)
                ERROR_LOG << "err_internal: " << cmd.GetInternalError() << Endl;
            ythrow yexception() << "while getting resources, status: " << (int)cmd.GetStatus() << Endl;
        }
    }

    void TConfig::ProcessVars(const TFsPath& configPath){
        for (size_t i = 0; i < Nodes.size(); ++i){
            NJson::TJsonValue& vars = Nodes[i].Vars;
            if (!vars.IsMap())
                continue;
            NJson::TJsonValue fixedVars(NJson::JSON_MAP);
            size_t resourcesCount = 0;
            for (NJson::TJsonValue::TMapType::const_iterator j = vars.GetMap().begin(); j != vars.GetMap().end(); ++j){
                if (j->second.IsString())
                    fixedVars[j->first] = j->second;
                else if (j->second.IsMap()){
                    if (j->second.Has("resource"))
                        resourcesCount++;
                }
            }
            if (resourcesCount > 0){
                TString filename = Nodes[i].Name + ".VARS";
                GetResources(Nodes[i].Name, configPath, filename);
                TUnbufferedFileInput fi(filename);
                NJson::TJsonValue json;
                VERIFY_WITH_LOG(NJson::ReadJsonTree(&fi, true, &json), "errors in json %s", filename.data());
                if (json.GetMap().size() != resourcesCount)
                    ythrow yexception() << "internal error, counts " << resourcesCount << "!=" << json.GetMap().size() << Endl;
                for (NJson::TJsonValue::TMapType::const_iterator j = json.GetMap().begin(); j != json.GetMap().end(); ++j)
                    fixedVars[j->first] = j->second;

                Nodes[i].Vars = fixedVars;
            }
        }
    }

    TSlotManager::TSlotManager()
        : FreePort(3000)
    {
        if (GetEnv("RTY_TESTS_START_PORT") != TString()){
            FreePort = FromString<ui16>(GetEnv("RTY_TESTS_START_PORT"));
        }
    }

    ui16 CheckEmpty(ui16 port, ui16 count) {
        for (ui16 i = 0; i < count; ++i)
            if (!IsPortFree(port + i)) {
                for (ui16 j = port + i + 1; j < port + 1990; ++j)
                    if (IsPortFree(j))
                        return j;
                FAIL_LOG("Incorrect port was chosen");
            }
        return 0;
    }

    ui16 TSlotManager::GetEmptyPortSetImpl(ui16 count) {
        TGuard<TMutex> g(Mutex);
        ui16 port = FreePort;
        while (port - FreePort < 1990) {
            DEBUG_LOG << "Port checking for: " << port << "/" << count << Endl;
            ui16 newPort = CheckEmpty(port, count);
            if (!newPort) {
                FreePort = port + count + 1;
                DEBUG_LOG << "Empty ports detected: " << port << "/" << count << Endl;
                return port;
            }
            port = newPort;
        }
        FAIL_LOG("Incorrect port was chosen");
        return 0;
    }

    ui16 TSlotManager::GetEmptyPortSet(ui16 count) {
        return Singleton<TSlotManager>()->GetEmptyPortSetImpl(count);
    }
}
