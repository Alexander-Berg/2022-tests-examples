#include <saas/rtyserver_test/testerlib/backend.h>
#include <saas/rtyserver_test/testerlib/tester.h>
#include <saas/rtyserver_test/testerlib/config_fields.h>
#include <saas/rtyserver_test/testerlib/cluster_cb.h>
#include <saas/rtyserver_test/mini_cluster/mini_cluster.h>

#include <library/cpp/getopt/opt.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/malloc/api/malloc.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/svnversion/svnversion.h>

#include <util/random/random.h>
#include <util/system/env.h>
#include <dict/dictutil/str.h>
#include <util/folder/dirut.h>

namespace {
    void PrintVersionAndExit() {
        Cout << GetProgramSvnVersion() << Endl;
        exit(0);
    }
}

int RunRtyserverTests(int argc, const char* argv[]) {
#ifdef _win_
    SetErrorMode(SetErrorMode(SEM_FAILCRITICALERRORS | SEM_NOGPFAULTERRORBOX) | SEM_FAILCRITICALERRORS | SEM_NOGPFAULTERRORBOX);
#endif
    NMalloc::MallocInfo().SetParam("FillMemoryOnAllocation", "false");
    int result = 0;
    try {
        TString clusterConfig;
        NMiniCluster::TConfig clusterConf;
        TFsPath rootDir = TFsPath(NFs::CurrentWorkingDirectory()) / "root";
        TDaemonConfig daemonConfig(GetDaemonConfigText(rootDir.GetPath()).data(), true);
        daemonConfig.SetLoggerType("console");

        TString resourceDir;
        TString testConfigPatches;
        TString keyPreffixed;
        ui32 shardsNumber;
        TVector<TString> preprocessorVars;
        int maxDocs = -1;
        TString protocolType;

        NLastGetopt::TOpts opts;
        opts.AddHelpOption();
        opts.AddCharOption('v', "Print program version").Handler(&::PrintVersionAndExit).NoArgument();

        opts.AddCharOption('A', "Count tries to run test").DefaultValue("1");
        opts.AddCharOption('B', "Do not start tests that have tag matched this regexp");
        opts.AddCharOption('C', "Check specified logs");
        opts.AddCharOption('c', "Tests to exclude (regular expression)");
        opts.AddCharOption('D', "Use distributor").NoArgument();
        opts.AddCharOption('d', "Resources directory").StoreResult(&resourceDir).DefaultValue("");
        opts.AddCharOption('E', "Set ReArrangeOptions");
        opts.AddCharOption('e', "Tests, that must not run");
        opts.AddCharOption('F', "Path to file search cache");
        opts.AddCharOption('f', "First test, may be part of name");
        opts.AddCharOption('G', "Get and save responses").NoArgument();
        opts.AddCharOption('g', "Set cluster config path").StoreResult(&clusterConfig);
        opts.AddLongOption('h', "metaservices", "Use MetaServices instead Service in searhmap").NoArgument();
        opts.AddCharOption('I', "Set lower log level").Handler0([&daemonConfig](){daemonConfig.SetLogLevel(6); }).NoArgument();
        opts.AddCharOption('k', "Use key prefixed").StoreResult(&keyPreffixed).DefaultValue("");
        opts.AddCharOption('L', "Time to live for cache");
        opts.AddCharOption('l', "Set daemon logger type").Handler1T<TString>([&daemonConfig](const TString& arg){daemonConfig.SetLoggerType(arg);});
        opts.AddCharOption('M', "Use memory search cache").NoArgument();
        opts.AddCharOption('m', "max count of documents in index").StoreResultT<int>(&maxDocs).DefaultValue("-1");
        opts.AddCharOption('N', "Navigation source file name");
        opts.AddCharOption('n', "No reply on index messages").NoArgument();
        opts.AddCharOption('O', "Print all tests").NoArgument();
        opts.AddCharOption('o', "dict.dict path");
        opts.AddCharOption('P', "External patches (json map with parameters range)").StoreResult(&testConfigPatches).DefaultValue("");
        opts.AddCharOption('p', "protocol type (default.local, default.socket)").StoreResult(&protocolType);
        opts.AddCharOption('R', "NoSearch").NoArgument();
        opts.AddCharOption('r', "Index root directory").StoreResult(&rootDir).DefaultValue(NFs::CurrentWorkingDirectory() + GetDirectorySeparator() + "root");
        opts.AddCharOption('S', "Not uSe parSerS").NoArgument();
        opts.AddCharOption('s', "ShardsNumber").StoreResult(&shardsNumber).DefaultValue("5");
        opts.AddCharOption('T', "FactorsFileName");
        opts.AddCharOption('t', "Tests for run");
        opts.AddCharOption('u', "User relevance library");
        opts.AddCharOption('V', "Preprocessor variables (repeats)").AppendTo(&preprocessorVars);
        opts.AddCharOption('W', "DeployManager storage type (one of ZOO, RTY, LOCAL)").Handler1T<TString>([&clusterConf](const TString& arg){clusterConf.PreprocessorPatches["DeployManager.StorageType"] = arg;});
        opts.AddCharOption('w', "Run dolbilo n times (load tests only)").DefaultValue("0");
        opts.AddCharOption('X', "Start only tests that have tag matched this regexp");
        opts.AddCharOption('x', "Regexp for tests filter");
        opts.AddCharOption('Z', "Generate IndexedDocZ").NoArgument();
        opts.AddCharOption('z', "Use files for stderr/out").NoArgument();

        NLastGetopt::TOptsParseResult parseOpt(&opts, argc, argv);
        Cerr << "Argc: " << argc << Endl;
        Cerr << "Argv: ";
        for (int i = 0; i < argc; ++i) {
            Cerr << argv[i] << " ";
        }
        Cerr << Endl;
        Cerr << "Parsed free args count " << parseOpt.GetFreeArgCount() << Endl;
        for (const TString& arg : parseOpt.GetFreeArgs()) {
            Cerr << "\"" << arg << "\"" << Endl;
        }

        TConfigPatches internalPatches;
        if (!keyPreffixed) {
            PutPreffixedPatch(&internalPatches, false, shardsNumber);
            PutPreffixedPatch(&internalPatches, true, shardsNumber);
        } else if (keyPreffixed == "notset") {
            internalPatches.push_back(new TConfigFields());
        } else {
            PutPreffixedPatch(&internalPatches, IsTrue(keyPreffixed), shardsNumber);
        }

        if (!preprocessorVars.empty()) {
            for (const TString& val : preprocessorVars) {
                size_t pos = val.find('=');
                if (pos == TStringBuf::npos)
                    throw yexception() << "ERROR: invalid value for option -V " << val << ": expect key=value";
                clusterConf.PreprocessorVariables[val.substr(0, pos)] = val.substr(pos + 1);
            }
        }
        if (GetEnv("LOG_PATH") == TString()) {
            SetEnv("LOG_PATH", rootDir.GetPath());
        }
        if (parseOpt.Has('z')) {
            TString logsDir = GetEnv("LOG_PATH");
            daemonConfig.SetStdErr(logsDir + "/" + ToString(Now().MilliSeconds()) + ".std.err");
            daemonConfig.SetStdOut(logsDir + "/" + ToString(Now().MilliSeconds()) + ".std.out");
        }

        daemonConfig.InitLogs();

        TConfigPatchesStorage patchStorage(testConfigPatches);
        patchStorage.AddAlternatives(internalPatches);

        TConfigPatches patches;
        patchStorage.BuildAllCombinations(patches);

        TString absoluteDir;
        if (SafeResolveDir(resourceDir.data(), absoluteDir)) {
            clusterConf.PreprocessorVariables["RES_PATH"] = absoluteDir;
        }
        TFsPath configsRoot;
        if (!!clusterConfig) {
            clusterConf.Init(clusterConfig);
            configsRoot = TFsPath(clusterConfig).Parent().Fix();
            if (TFsPath(clusterConfig).GetName().StartsWith('_')){
                configsRoot = configsRoot.Parent().Fix();
                INFO_LOG << "using higher config root: " << configsRoot << Endl;
            }
            TVector<ui64> keys;
            keys.push_back(RandomNumber<ui64>());
            keys.push_back(Now().MicroSeconds());
            ui64 id = FnvHash<ui64>(&keys[0], keys.size() * sizeof(ui64)) / 2;
            TString zooRoot = "st_" + ToString(id) + "_" + ToString(keys[1]);
            clusterConf.PreprocessorPatches["DeployManager.Storage.Zoo.Root"] = "r" + zooRoot;
            clusterConf.PreprocessorPatches["DeployManager.QueueName"] = "q" + zooRoot;
            clusterConf.PreprocessorPatches["DeployManager.Storage.Saas.IndexPrefix"] = ToString(id);
            clusterConf.PreprocessorPatches["DeployManager.Storage.Mongo.DB"] = "dm_test_" + ToString(id) + "_" + ToString(keys[1]);
        } else {
            ConfigureAndStartBackend(rootDir.GetPath(), daemonConfig, protocolType, maxDocs);
            clusterConf.Nodes.push_back(NMiniCluster::TConfig::TNodeConfig(clusterConf));
            clusterConf.Nodes.back().MainConfigPath = "rtyserver.conf";
            clusterConf.Nodes.back().Name = "rtyserver";
            clusterConf.Nodes.back().Product = "rtyserver";
            configsRoot = rootDir / "def_cfg";
        }
        clusterConf.RootPath = rootDir.GetPath();
        clusterConf.BinPath = TFsPath(argv[0]).Parent().Fix().GetPath() + "/";
        TClusterCallback callback(clusterConf, configsRoot, parseOpt.Has("metaservices"));

        for(const auto& externalPatch : patches) {
            NUnitTest::TTestContext context;
            TTestSet tester(parseOpt, &callback);
            Cout << externalPatch->GetReadableString() << Endl;
            result += tester.Run(context, externalPatch);
        }
    } catch (yexception& e) {
        ERROR_LOG << e.what() << Endl;
        return -1;
    }
    return result;
}
