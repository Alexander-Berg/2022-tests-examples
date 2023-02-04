#pragma once
#include "mini_cluster_searchmap.h"
#include <saas/rtyserver_test/testerlib/system_signals.h>
#include <saas/rtyserver_test/common/hostname.h>
#include <saas/library/daemon_base/daemon/daemon.h>
#include <saas/library/daemon_base/actions_engine/controller_client.h>
#include <saas/library/daemon_base/controller_actions/shutdown.h>
#include <saas/library/daemon_base/controller_actions/get_status.h>
#include <saas/util/system/environment.h>
#include <library/cpp/yconf/patcher/unstrict_config.h>
#include <library/cpp/json/json_reader.h>
#include <util/system/shellcommand.h>
#include <util/thread/factory.h>
#include <util/system/file.h>
#include <util/system/hostname.h>
#include <util/system/env.h>

namespace NMiniCluster {
    struct TConfig {
        struct TNodeConfig {
            TNodeConfig(TConfig& owner, const NJson::TJsonValue& json)
                : Owner(owner)
                , Product(json["product"].GetString())
                , Name(json["name"].GetString())
                , MainConfigPath(json["config"].GetString())
                , Patch(json["patch"])
                , Vars(json["vars"])
                , Port(json.Has("port") ? json["port"].GetUIntegerSafe() : 0)
                , RunBeforeTest((json.Has("run_before_test") && json["run_before_test"].IsBoolean()) ? json["run_before_test"].GetBoolean() : true)
                , SkipStopping(json.Has("skip_stopping") && json["skip_stopping"].GetBoolean())
                , External(json.Has("external") && json["external"].IsBoolean() && json["external"].GetBoolean())
                , Controlling(json.Has("controlling") ? json["controlling"].GetBooleanRobust() : true)
                , RegisterController(json.Has("register_controller") ? json["register_controller"].GetBooleanRobust() : false)
            {
            }

            TNodeConfig(TConfig& owner)
                : Owner(owner)
                , Port(0)
                , RunBeforeTest(true)
                , SkipStopping(false)
                , External(false)
            {}

            inline TString GetConfigPath() const {
                return Owner.RootPath + Name + "/configs/" + MainConfigPath;
            }

            TConfig& Owner;
            TString Product;
            TString Name;
            TString MainConfigPath;
            NJson::TJsonValue Patch;
            NJson::TJsonValue Vars;
            ui16 Port;
            bool RunBeforeTest;
            bool SkipStopping;
            bool External;
            bool Controlling;
            bool RegisterController = false;
        };
        typedef TVector<TNodeConfig> TNodesConfig;
        void Init(const TString& filename);
        void ValidateSearchmap();
        void ProcessVars(const TFsPath& configPath);
        TString RootPath;
        TString BinPath;
        TNodesConfig Nodes;
        TClusterSearchmap Searchmap;
        typedef THashMap<TString, TString> TKeyValues;
        TKeyValues PreprocessorVariables;
        TKeyValues PreprocessorPatches;
    };

    struct TExecutePolicy {
        enum TOnErrorPolicy { IGNORE_ERROR, STOP_ON_ERROR, RETRY_ON_ERROR };
        TExecutePolicy(const TString& node, TOnErrorPolicy onError, ui32 retryCount = 100)
            : OnErrorPolicy(onError)
            , Node(node)
            , RetryCount(retryCount)
        {}

        TOnErrorPolicy OnErrorPolicy;
        TString Node;
        TSet<TString> ExcludedNodes;
        ui32 RetryCount;
    };

    class TSlotManager {
        Y_DECLARE_SINGLETON_FRIEND()
        TSlotManager();
        ui16 GetEmptyPortSetImpl(ui16 count);
        ui16 FreePort;
        TMutex Mutex;

    public:
        static ui16 GetEmptyPortSet(ui16 count = 4);
    };

    template <class C, class S>
    struct TNode {
        typedef C TController;
        typedef S TServer;
        struct ICreationCallBack {
            virtual ~ICreationCallBack() {}
            virtual void SetPorts(TNode<C, S>& node, ui16 firstPort) const = 0;
            virtual TString GetBinName() const = 0;
        };
        virtual ~TNode() {}
        TNode(const TConfig::TNodeConfig& config, size_t index, ICreationCallBack* callback, ui16 dmPort, const TString& dmUriPrefix);
        virtual void Run() = 0;
        virtual void Wait() = 0;
        virtual bool GetProcessId(TProcessId& /*pid*/){ return false; }

        const TConfig::TNodeConfig& Config;
        const TDaemonConfig* GetDaemonConfig() const {
            return ConstructParams->Daemon.Get();
        }
        THolder<NDaemonController::TControllerAgent> Agent;
        void AddPatch(const TString& key, const TString& value) {
            PreprocessorPatches[key] = value;
        }
        THolder<TServerConfigConstructorParams> ConstructParams;
        ui16 GetPort() const {
            return Port;
        }
    protected:
        TConfig::TKeyValues PreprocessorVariables;
        TConfig::TKeyValues PreprocessorPatches;
        ui16 Port;
    private:
        TDaemonConfigPatcher Preprocessor;
    };

    template <class C, class S>
    struct TInternalNode : public TNode<C, S> {
        TInternalNode(const TConfig::TNodeConfig& config, size_t index, typename TNode<C, S>::ICreationCallBack* callback, ui16 dmPort, const TString& dmUriPrefix)
            : TNode<C, S>(config, index, callback, dmPort, dmUriPrefix)
            , Controller(new typename TNode<C, S>::TController(*TNode<C, S>::ConstructParams, ServerDescription))
        {}

        virtual void Run() {
            Controller->Run();
        }

        virtual void Wait() {
            DEBUG_LOG << "Wait stop " << TNode<C, S>::Config.Name << "..." << Endl;
            Controller->WaitStopped();
            DEBUG_LOG << "Wait stop " << TNode<C, S>::Config.Name << "...OK" << Endl;
        }

    private:
        TServerDescriptor<typename TNode<C, S>::TServer> ServerDescription;
        THolder<typename TNode<C, S>::TController> Controller;
    };

    template <class C, class S>
    struct TExternalNode : public TNode<C, S>, public IThreadFactory::IThreadAble {
        TExternalNode(const TConfig::TNodeConfig& config, size_t index, typename TNode<C, S>::ICreationCallBack* callback, ui16 dmPort, const TString& dmUriPrefix)
            : TNode<C, S>(config, index, callback, dmPort, dmUriPrefix)
            , Terminated(false)
            , BinPath(config.Owner.BinPath + callback->GetBinName())
        {
            if (!TFsPath(BinPath).Exists())
                ythrow yexception() << "does not exists binary: " << BinPath;
            CreateCommand();
        }

        void CreateCommand() {
            TShellCommandOptions options;
            options.SetClearSignalMask(true);
            options.SetCloseAllFdsOnExec(true);
            options.SetAsync(true);
            options.SetLatency(100);
            options.SetDetachSession(true);
            options.SetUseShell(false);
            if (GetEnv("PROFILES_PATH") != TString()) {
                NUtil::TEnvVars env = NUtil::GetEnvironmentVariables();
                for (auto& i : env)
                    options.Environment[i.first] = i.second;

                TString profPath = GetEnv("PROFILES_PATH") + "/" + TFsPath(BinPath).Basename()
                    + "/" + TFsPath(BinPath).Basename() + "-" + ToString(TNode<C, S>::GetPort()) + "/cpu.profile";
                TFsPath(profPath).MkDirs();
                options.Environment["CPUPROFILE"] = profPath;
            }
            Command.Reset(new TShellCommand(BinPath, options));
            *Command << "-E" << (TFsPath(TNode<C, S>::Config.GetConfigPath()).Parent() / "environment").GetPath();
            for (TConfig::TKeyValues::const_iterator i = TNode<C, S>::PreprocessorVariables.begin();
                i != TNode<C, S>::PreprocessorVariables.end(); ++i)
                *Command << "-V" << (i->first + "=" + i->second);
            for (TConfig::TKeyValues::const_iterator i = TNode<C, S>::PreprocessorPatches.begin();
                i != TNode<C, S>::PreprocessorPatches.end(); ++i)
                *Command << "-P" << (i->first + "=" + i->second);
            *Command << TNode<C, S>::Config.GetConfigPath();
        }

        virtual void Run() {
            Command->Run();
            WatchdogThread = SystemThreadFactory()->Run(this);
            for (int i = 0; i < 30; ++i) {
                NDaemonController::TStatusAction sa;
                if (TNode<C, S>::Agent->ExecuteAction(sa) && sa.IsFinished() && !sa.IsFailed())
                    return;
                Sleep(TDuration::Seconds(1));
            }
            ythrow yexception() << "start of " << TNode<C, S>::Config.Name << " too long...";
        }

        virtual void DoExecute() {
            while (!Terminated) {
                Command->Wait();
                switch (Command->GetStatus()) {
                case TShellCommand::SHELL_FINISHED:
                    return;
                case TShellCommand::SHELL_ERROR:
                    ERROR_LOG << "unexpected finish on node " << TNode<C, S>::Config.Name << ": error while execute command:" << Command->GetError() << "; output:" << Command->GetOutput() << Endl;
                    Restart();
                    break;
                case TShellCommand::SHELL_INTERNAL_ERROR:
                    ythrow yexception() << "unexpected finish on node " << TNode<C, S>::Config.Name << ": internal error: " << Command->GetInternalError();
                case TShellCommand::SHELL_RUNNING:
                    FAIL_LOG("two watchdogs?");
                default:
                    FAIL_LOG("Unexpected status");
                }
            }
        }

        virtual void Wait() {
            DEBUG_LOG << "Wait stop " << TNode<C, S>::Config.Name << "..." << Endl;
            if (!!WatchdogThread)
                WatchdogThread->Join();
            DEBUG_LOG << "Wait stop " << TNode<C, S>::Config.Name << "...OK" << Endl;
        }

        virtual bool GetProcessId(TProcessId& pid){
            pid = Command->GetPid();
            return true;
        }

        virtual ~TExternalNode() {
            Terminated = true;
            Command->Terminate();
        }

    private:
        void Restart();
        THolder<TShellCommand> Command;
        TAutoPtr<IThreadFactory::IThread> WatchdogThread;
        bool Terminated;
        TString BinPath;
    };

    template <class C, class S>
    struct TNodeSet : public TNode<C, S>::ICreationCallBack {
        typedef C TController;
        typedef S TServer;
        typedef TSimpleSharedPtr<TNode<TController, TServer> > TNodePtr;
        typedef TMap<TString, TNodePtr> TNodes;

        class INodeCallback {
        public:
            virtual ~INodeCallback() {}
            virtual bool DoOnNode(TNode<TController, TServer>& node) = 0;
        };
        bool ExecuteAtNodes(const TExecutePolicy& policy, INodeCallback& callback);
        bool ExecuteCommand(const TExecutePolicy& policy, const TString& command, TString& result, ui32 timeoutMs, const TString& postData);
        bool ExecuteAction(const TExecutePolicy& policy, NDaemonController::TAction& action);
        void Run();
        void RunServers();
        void Stop(TExecutePolicy::TOnErrorPolicy policy = TExecutePolicy::RETRY_ON_ERROR);
        bool DoWithNode(TString nodeName, TString action, NJson::TJsonValue params = NJson::TJsonValue(), NJson::TJsonValue* result = nullptr);
        TNodePtr AddNode(const TConfig::TNodeConfig& config, size_t index, ui16 dmPort, const TString& dmUriPrefix);
        const TNodes& GetNodes() const {
            return Nodes;
        }
        TSet<TString> GetNodesNames() const {
            TSet<TString> res;
            for (typename TNodes::const_iterator i = Nodes.begin(); i != Nodes.end(); ++i){
                res.insert(i->first);
            }
            return res;
        }

    private:
        bool GetNodesRange(const TString& node, typename TNodes::iterator& begin, typename TNodes::iterator& end);
        TNodes Nodes;
    };

    template <class C, class S>
    bool TNodeSet<C, S>::GetNodesRange(const TString& node, typename TNodes::iterator& begin, typename TNodes::iterator& end) {
        if (node.EndsWith('*')) {
            begin = Nodes.lower_bound(node.substr(0, node.length() - 1));
            end = Nodes.lower_bound(node.substr(0, node.length() - 1).append(0xFF));
        }
        else {
            begin = Nodes.find(node);
            if (begin == Nodes.end())
                return false;
            end = begin;
            ++end;
        }
        return begin != end;
    }

    template <class C, class S>
    bool TNodeSet<C, S>::ExecuteAtNodes(const TExecutePolicy& policy, INodeCallback& callback) {
        typename TNodes::iterator begin, end;
        if (!GetNodesRange(policy.Node, begin, end))
            return false;
        bool result = true;
        for (; begin != end; ++begin) {
            if (policy.ExcludedNodes.find(begin->first) != policy.ExcludedNodes.end())
                continue;
            DEBUG_LOG << "executing at node " << begin->first << ".." << Endl;
            bool localResult = false;
            for (ui32 i = 0; i < policy.RetryCount; ++i) {
                localResult = callback.DoOnNode(*begin->second);
                if (!localResult) {
                    ERROR_LOG << "executing at node " << begin->first << "..FAIL" << Endl;
                    if (policy.OnErrorPolicy == TExecutePolicy::IGNORE_ERROR) {
                        result = false;
                        break;
                    }

                    if (policy.OnErrorPolicy == TExecutePolicy::STOP_ON_ERROR)
                        return false;
                    Sleep(TDuration::Seconds(1));
                    ERROR_LOG << "executing at node " << begin->first << "..RETRY" << Endl;
                }
                else {
                    DEBUG_LOG << "executing at node " << begin->first << "..OK" << Endl;
                    break;
                }
                if (!localResult)
                    ERROR_LOG << "executing at node " << begin->first << "..TIMEOUT" << Endl;
            }
        };
        return result;
    }

    template <class C, class S>
    bool TNodeSet<C, S>::ExecuteCommand(const TExecutePolicy& policy, const TString& command, TString& result, ui32 timeoutMs, const TString& postData) {
        class TCommandCallback : public INodeCallback {
            const TString& Command;
            TString& Result;
            ui32 TimeoutMs;
            const TString& PostData;
        public:
            TCommandCallback(const TString& command, TString& result, ui32 timeoutMs, const TString& postData)
                : Command(command)
                , Result(result)
                , TimeoutMs(timeoutMs)
                , PostData(postData)
            {}

            bool DoOnNode(TNode<C, S>& node) {
                TString result;
                bool resp = node.Agent->ExecuteCommand(Command, result, TimeoutMs, 1, PostData);
                Result += result;
                return resp;
            }

        } commandCallback(command, result, timeoutMs, postData);

        return ExecuteAtNodes(policy, commandCallback);
    }

    template <class C, class S>
    bool TNodeSet<C, S>::ExecuteAction(const TExecutePolicy& policy, NDaemonController::TAction& action) {
        class TActionCallback : public INodeCallback {
            NJson::TJsonValue Action;
        public:
            TActionCallback(NDaemonController::TAction& action)
                : Action(action.Serialize())
            {}

            bool DoOnNode(TNode<C, S>& node) {
                NDaemonController::TAction::TPtr action(NDaemonController::TAction::BuildAction(Action));
                return node.Agent->ExecuteAction(*action);
            }

        } actionCallback(action);

        return ExecuteAtNodes(policy, actionCallback);
    }

    template <class C, class S>
    void TNodeSet<C, S>::Stop(TExecutePolicy::TOnErrorPolicy policy) {
        NDaemonController::TShutdownAction sa;
        TExecutePolicy  execPolicy("*", policy);
        for (typename TNodes::iterator i = Nodes.begin(); i != Nodes.end(); ++i) {
            if (i->second->Config.SkipStopping)
                execPolicy.ExcludedNodes.insert(i->first);
        }
        ExecuteAction(execPolicy, sa);
        DEBUG_LOG << "Wait stop nodes.." << Endl;
        for (typename TNodes::iterator i = Nodes.begin(); i != Nodes.end(); ++i) {
            if (i->second->Config.RunBeforeTest && !i->second->Config.SkipStopping)
                i->second->Wait();
            else
                DEBUG_LOG << "Skip waiting node: " << i->first << Endl;
        }
        DEBUG_LOG << "Wait stop nodes..OK" << Endl;
    }

    template <class C, class S>
    void TNodeSet<C, S>::Run() {
        for (typename TNodes::iterator i = Nodes.begin(); i != Nodes.end(); ++i){
            if (i->second->Config.RunBeforeTest)
                i->second->Run();
            else
                DEBUG_LOG << "Skip starting node: " << i->first << Endl;
        }
    }

    template <class C, class S>
    void TNodeSet<C, S>::RunServers() {
        TString result;
        DEBUG_LOG << "Run server..." << Endl;
        ExecuteCommand(TExecutePolicy("*", TExecutePolicy::RETRY_ON_ERROR, 3),
                       "?command=restart&reread_config=da",
                       result, 300000, "");
        DEBUG_LOG << "Run server... Ok " << result << Endl;
    }

    template <class C, class S>
    bool TNodeSet<C, S>::DoWithNode(TString nodeName, TString action, NJson::TJsonValue params, NJson::TJsonValue* /*result*/) {
        typename TNodes::iterator beg, en;
        bool nodesExist = GetNodesRange(nodeName, beg, en);
        if (!nodesExist){
            ERROR_LOG << "node '" << nodeName << "' not found" << Endl;
            return false;
        }
        for (typename TNodes::iterator i = beg; i != en; ++i){
            if (action == "run")
                i->second->Run();
            else if (action == "wait")
                i->second->Wait();
            else if (action == "stop"){
                TString result;
                ExecuteCommand(TExecutePolicy(nodeName, TExecutePolicy::RETRY_ON_ERROR, 2), "?command=stop", result, 300000, "");
            }
            else if (action == "restart") {
                TString result;
                ExecuteCommand(TExecutePolicy(nodeName, TExecutePolicy::RETRY_ON_ERROR, 2), "?command=restart", result, 300000, "");
            }
            else if (action == "signal"){
                size_t signum = params["signal"].GetUInteger();
                if (signum == 0)
                    ythrow yexception() << "signal in parameters must present, non-zero" << Endl;
                TProcessId pid;
                bool hasOwnId = i->second->GetProcessId(pid);
                if (!hasOwnId)
                    ythrow yexception() << "cannot get pid for such nodes" << Endl;
                SendSignal(pid, signum);
            }
            else
                ythrow yexception() << "unknown action: " << action << Endl;
            }
        return nodesExist;
    }

    template <class C, class S>
    typename TNodeSet<C, S>::TNodePtr TNodeSet<C, S>::AddNode(const TConfig::TNodeConfig& config, size_t index, ui16 dmPort, const TString& dmUriPrefix) {
        TNodePtr newNodePtr;
        if (config.External)
            newNodePtr.Reset(new TExternalNode<C, S>(config, index, this, dmPort, dmUriPrefix));
        else
            newNodePtr.Reset(new TInternalNode<C, S>(config, index, this, dmPort, dmUriPrefix));
        std::pair<typename TNodes::iterator, bool> newNode = Nodes.insert(std::make_pair(config.Name, newNodePtr));
        VERIFY_WITH_LOG(newNode.second, "%s: name already used", config.Name.data());
        return newNode.first->second;
    }

    template <class C, class S>
    TNode<C, S>::TNode(const TConfig::TNodeConfig& config, size_t index, typename TNode<C, S>::ICreationCallBack* callback, ui16 dmPort, const TString& dmUriPrefix)
    : Config(config)
    , PreprocessorVariables(config.Owner.PreprocessorVariables)
    , PreprocessorPatches(config.Owner.PreprocessorPatches)
    , Port(config.Port ? config.Port : TSlotManager::GetEmptyPortSet(5))
    {
        VERIFY_WITH_LOG(TFsPath(config.GetConfigPath()).Exists(), "%s: config does not exists: %s", Config.Name.data(), Config.GetConfigPath().data());
        PreprocessorVariables["SERVER_NAME"] = config.Name;
        PreprocessorVariables["SERVER_INDEX"] = ToString(index);
        PreprocessorVariables["CONF_PATH"] = config.Owner.RootPath + config.Name +"/configs/";
        PreprocessorVariables["STORAGE_PATH"] = config.Owner.RootPath + config.Name + "/storage/";
        AddPatch("DaemonConfig.Controller.ConfigsRoot", config.Owner.RootPath + config.Name + "/configs/");
        AddPatch("DaemonConfig.Controller.StateRoot", config.Owner.RootPath + config.Name + "/state/");
        AddPatch("DaemonConfig.Controller.ReinitLogsOnRereadConfigs", "0");
        if (dmPort) {
            AddPatch("DaemonConfig.Controller.DMOptions.Host", TestsHostName());
            AddPatch("DaemonConfig.Controller.DMOptions.Port", ToString(dmPort));
            AddPatch("DaemonConfig.Controller.DMOptions.UriPrefix", dmUriPrefix);
            AddPatch("DaemonConfig.Controller.DMOptions.Slot", TestsHostName() + ":" + ToString(Port));
            AddPatch("DaemonConfig.Controller.DMOptions.Attemptions", "3");
            AddPatch("DaemonConfig.Controller.DMOptions.Enabled", "true");
        }
        callback->SetPorts(*this, Port);
        AddPatch("DaemonConfig.Controller.Port", ToString(Port + 3));
        if (config.Vars.IsMap()){
            for (NJson::TJsonValue::TMapType::const_iterator i = config.Vars.GetMap().begin(); i != config.Vars.GetMap().end(); ++i){
                DEBUG_LOG << "Set var " << i->first << " = " << i->second.GetString() << Endl;
                PreprocessorVariables[i->first] = i->second.GetString();
            }
        }
        if (config.Patch.IsMap()){
            for (NJson::TJsonValue::TMapType::const_iterator i = config.Patch.GetMap().begin(); i != config.Patch.GetMap().end(); ++i){
                PreprocessorPatches[i->first] = i->second.GetStringRobust();
                DEBUG_LOG << "Set patch " << i->first << " = " << i->second.GetStringRobust() << Endl;
            }
        }
        for (TConfig::TKeyValues::const_iterator i = PreprocessorVariables.begin(); i != PreprocessorVariables.end(); ++i)
            Preprocessor.SetVariable(i->first, i->second);
        for (TConfig::TKeyValues::const_iterator i = PreprocessorPatches.begin(); i != PreprocessorPatches.end(); ++i)
            Preprocessor.AddPatch(i->first, i->second);
        TString text = Preprocessor.ReadAndProcess(config.GetConfigPath());
        DEBUG_LOG << "Config parsed: " << text << Endl;
        ConstructParams.Reset(new TServerConfigConstructorParams(text.data(), (config.GetConfigPath()).data(), &Preprocessor));
        VERIFY_WITH_LOG(GetDaemonConfig()->GetController().Enabled, "%s: controller must be enabled", Config.Name.data());
        Agent.Reset(new NDaemonController::TControllerAgent(TestsHostName(), GetDaemonConfig()->GetController().Port));
    }

    template<class C, class S>
    void TExternalNode<C, S>::Restart() {
        if (Terminated)
            return;
        DEBUG_LOG << "Try to restart node " << TNode<C, S>::Config.Name << "...Start Command..." << Endl;
        CreateCommand();
        (*Command) << "-P" << "DaemonConfig.Controller.StartServer=1";
        Command->Run();
        DEBUG_LOG << "Try to restart node " << TNode<C, S>::Config.Name << "...Start Command...OK" << Endl;
        DEBUG_LOG << "Try to restart node " << TNode<C, S>::Config.Name << "...OK" << Endl;
    }
}
