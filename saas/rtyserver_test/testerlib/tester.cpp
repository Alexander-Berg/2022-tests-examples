#include "tester.h"
#include "globals.h"
#include <saas/util/logging/exception_process.h>

#include <library/cpp/regex/pcre/regexp.h>

#include <util/digest/fnv.h>
#include <util/folder/dirut.h>

void CleanDir(const TString& dir) {
    if (NFs::Exists(dir)) {
        RemoveDirWithContents(dir);
    }
    TFsPath(dir).MkDirs();
}

TConfigFieldsPtr TTestSet::PrepareBackend(int backendNumber, const TString& rDir, const TString& diffPrefix) {
    TFsPath rootDir = rDir;
    if (!!Callback) {
        rootDir /= "${SERVER_NAME}";
    } else {
        rootDir /= ToString(backendNumber);
    }
    TConfigFieldsPtr diff(new TConfigFields);

    (*diff)[diffPrefix + "Merger.IndexSwitchSystemLockFile"] = TFsPath(rDir) / "indexswitchlock";
    const TFsPath indexDir = rootDir / "index";
    const TFsPath configsRoot = rootDir / "configs";
    const TFsPath stateRoot = rootDir / "state";
    (*diff)["DaemonConfig.MetricsStorage"] = stateRoot / "metrics";
    (*diff)["DaemonConfig.Controller.ConfigsRoot"] = configsRoot;
    (*diff)["DaemonConfig.Controller.StateRoot"] = stateRoot;
    (*diff)[diffPrefix + "IndexDir"] = indexDir;
    TFsPath detachPath = indexDir.Parent().Fix() / "detach";
    TFsPath suggestPath = indexDir.Parent().Fix() / "suggest";
    (*diff)[diffPrefix + "ModulesConfig.Synchronizer.DetachPath"] = detachPath;
    (*diff)["ServerConfig.Index.BaseDir"] = suggestPath;
    if (!Callback) {
        CleanDir(indexDir);
        CleanDir(detachPath.GetPath());
    }
    if (NotUseParsers) {
        (*diff)[diffPrefix + "indexer.common.XmlParserConfigFile"] = "";
        (*diff)[diffPrefix + "indexer.common.HtmlParserConfigFile"] = "";
    }
    if (!!RecognizerLibPath) {
        (*diff)[diffPrefix + "indexer.common.RecognizeLibraryFile"] = RecognizerLibPath + "/dict.dict";
        (*diff)[diffPrefix + "ComponentsConfig.Ann.RecognizerDataPath"] = RecognizerLibPath + "/queryrec/";
    }
    if (!!CacheDir) {
        (*diff)[diffPrefix + "searcher.QueryCache.UseCache"] = true;
        (*diff)[diffPrefix + "searcher.RearrangeOptions"] = "CacheSupporter";
        if (CacheDir == "memory") {
            (*diff)[diffPrefix + "searcher.QueryCache.Dir"] = "";
            (*diff)[diffPrefix + "searcher.QueryCache.MemoryLimit"] = 200000000;
        }
        else
            (*diff)[diffPrefix + "searcher.QueryCache.Dir"] = rootDir / "cache";

        if (!!CacheLifeTime)
            (*diff)[diffPrefix + "searcher.QueryCache.CacheLifeTime"] = CacheLifeTime;
    }
    if (!!ReArrangeOptions)
        (*diff)[diffPrefix + "searcher.ReArrangeOptions"] = ReArrangeOptions;
    return diff;
}

void TTestSet::PrepareTest(ITestCase::Ptr testCase, bool prefixed) {
    testCase->SetPrefixed(prefixed);
    testCase->SetResourcesDirectory(ResourcesDir);
    testCase->SetFactorsFileName(FactorsFileName);
    testCase->SetSendIndexReply(SendIndexReply);
    testCase->SetController(Proxy.Get());
    testCase->SetCallback(Callback);
    testCase->SetGenerateIndexedDoc(GenerateIndexedDoc);
    testCase->SetGetSaveResponses(GetSaveResponses);
    testCase->SetRunNDolbs(RunNDolbs);
    testCase->SetRootDirectory(RootDir);
    testCase->SetNoSearch(NoSearch);
    testCase->SetInfoCluster(&Callback->GetCluster());
    if (!testCase->InitConfig())
        ythrow yexception() << "Errors in InitConfig";
    if (!testCase->ApplyConfig())
        ythrow yexception() << "Errors in ApplyConfig";
    testCase->CheckConfig();
    if (!testCase->Prepare())
        ythrow yexception() << "Errors in Prepare";
    for (size_t i = 0; i < LogCheckers.size(); i++) {
        auto checker = LogCheckers[i];
        const TString logFileName = Proxy->GetConfigValue(checker->GetLoggerType(), "", TBackendProxy::TBackendSet(), checker->GetBinaryType());
        if (!logFileName)
            ythrow yexception() << "Logger type for " << checker->GetLoggerType() << " was not specified";

        checker->Prepare(logFileName, prefixed, testCase->Name(), ResourcesDir);
    }
    testCase->ResetGenerationShift();
}

void TTestSet::RunTest(const TString& name, NUnitTest::TTestContext& context, const TConfigFieldsPtr& externalPatch) {
    const int oldCountErrors = CountErrors;
    TInstant startTime = Now();
    TDuration initClusterTime;
    TDuration stopClusterTime;
    TDuration clearTime;
    bool notFound = false;
    Cout << "test=" << name << ";";
    Cout.Flush();
    ++CountErrors;
    bool ErrorOnRunning = false;
    bool ErrorOnFinishing = false;
    int attemption = 0;
    TRY
        for(; attemption < CountRunTest; ++attemption) {
            DEBUG_LOG << "Running test: ---" << name << Endl;
            ITestCase::Ptr testCase = Singleton<TRTYServerTestsFactory>()->Create(name);
            if (!testCase) {
                notFound = true;
                ythrow yexception() << "There in no such test";
            }
            TInstant initClusterStartTime = Now();
            const TString path = name + Now().ToString();
            const TString rootDir = RootDir + "/" + ToString(FnvHash<ui64>(path.data(), path.size())) + "_" + NLoggingImpl::GetLocalTimeSSimple() + "_";
            TConfigFieldsPtr diff = PrepareBackend(-1, rootDir, "Server.");
            diff->Patch(*externalPatch);
            Callback->RestartCluster(ProxyConfig, rootDir, diff);
            Proxy.Reset(new TBackendProxy(ProxyConfig));
            bool prefixed = externalPatch->contains("Server.IsPrefixedIndex") && FromString<bool>((*externalPatch)["Server.IsPrefixedIndex"].Value);
            PrepareTest(testCase, prefixed);
            if (testCase->NeedStartBackend())
                Proxy->RestartServer(false, nullptr);
            testCase->InitCluster();
            initClusterTime += Now() - initClusterStartTime;
            TInstant testStartTime = Now();
            if (!testCase->Run(context)) {
                ErrorOnRunning = true;
                ythrow yexception() << "Errors in Run";
            }
            if (!testCase->Finish()) {
                ErrorOnFinishing = true;
                CountErrors += 10;
                ythrow yexception() << "Errors in Finish";
            }
            clearTime += Now() - testStartTime;
            if (Callback) {
                TInstant stopClusterStartTime = Now();
                Callback->StopCluster();
                stopClusterTime += Now() - stopClusterStartTime;
            }
            for (size_t i = 0; i < LogCheckers.size(); i++)
                LogCheckers[i]->Check();
        }
        --CountErrors;
    CATCH("Failed test: ---" + TString(name))
    TRY
        ++CountErrors;
        if (!!Proxy && !Callback)
            Proxy->ProcessCommand("stop");
        if (Callback) {
            TInstant stopClusterStartTime = Now();
            Callback->StopCluster();
            stopClusterTime += Now() - stopClusterStartTime;
        }
        --CountErrors;
    CATCH("While stop server");

    TStringStream ss;
    ss << "full_time=" << (Now() - startTime) << ";init_cluster_time=" << initClusterTime << ";stop_cluster_time=" << stopClusterTime << ";clear_time=" << clearTime << ";tries=" << attemption << ";";

    if (CountErrors != oldCountErrors)
        Cout << "status=" << (notFound ? "NotFound;" : "Failed;") << (ErrorOnFinishing ? "when=finishing;" : "") << (ErrorOnRunning ? "when=running;" : "") << ss.Str() << Endl;
    else
        Cout << "status=OK;" << ss.Str() << Endl;

}

int TTestSet::Run(NUnitTest::TTestContext& context, const TConfigFieldsPtr& externalPatch) {
    CountErrors = 0;
    for (TSet<TString>::const_iterator i = ExecutableTests.begin(), e = ExecutableTests.end(); i != e; ++i) {
        RunTest(*i, context, externalPatch);
    }
    Proxy.Reset(nullptr);
    return CountErrors;
}

namespace {
    inline bool TagsDisallowed(const TRegExMatch* allowedTags, const TRegExMatch* disallowedTags, const TString& name) {
        if (!allowedTags && !disallowedTags)
            return false;
        const ITestCase::TTags* tags = Singleton<TRTYServerTestsFactory>()->GetTags(name);
        if (!tags)
            return false;
        bool result = allowedTags;
        for (ITestCase::TTags::const_iterator i = tags->begin(); i != tags->end(); ++i) {
            if (!!disallowedTags && disallowedTags->Match(i->data()))
                return true;
            if (!!allowedTags && allowedTags->Match(i->data()))
                result = false;
        }
        return result;
    }
}

TTestSet::TTestSet(const TString& root, const TRtyServerTestEnv& env, ICallback* callback)
    : RootDir(root)
    , ResourcesDir(env.ResourceDir)
    , CacheDir(env.CacheDir)
    , CacheLifeTime(env.CacheLifeTime)
    , NoSearch(env.NoSearch)
    , Callback(callback)
{
    ExecutableTests.insert(env.TestName);
}

TTestSet::TTestSet(const NLastGetopt::TOptsParseResult& parseOpt, ICallback* callback)
    : RootDir(parseOpt.Get('r'))
    , ResourcesDir(parseOpt.GetOrElse('d', ""))
    , StartTest(parseOpt.GetOrElse('f', ""))
    , RecognizerLibPath(parseOpt.GetOrElse('o', ""))
    , CacheDir(parseOpt.Has('M') ? "memory" : parseOpt.GetOrElse('F', ""))
    , ReArrangeOptions(parseOpt.GetOrElse('E', ""))
    , CacheLifeTime(parseOpt.GetOrElse('L', ""))
    , SendIndexReply(!parseOpt.Has('n'))
    , NoSearch(parseOpt.Has('R'))
    , NotUseParsers(parseOpt.Has('S'))
    , CountRunTest(parseOpt.Get<ui16>('A'))
    , GenerateIndexedDoc(parseOpt.Has('Z'))
    , GetSaveResponses(parseOpt.Has('G'))
    , RunNDolbs(parseOpt.Get<ui16>('w'))
    , Callback(callback)
{

    TString confFolderName = Callback ? "copy" : "def_cfg";
    FactorsFileName = parseOpt.GetOrElse('T', RootDir + GetDirectorySeparator() + confFolderName + GetDirectorySeparator() + "factors.cfg");


    TString regularExpressionDoRun = parseOpt.GetOrElse('x',"");
    TString regularExpressionExclude = parseOpt.GetOrElse('c', "");

    if (parseOpt.Has('D')) {
        GlobalOptions().SetUsingDistributor();
    }

    if (parseOpt.Has('t')) {
        TVector<TString> tasksVector;
        Split(parseOpt.Get('t'), ",", tasksVector);
        for (TVector<TString>::const_iterator i = tasksVector.begin(), e = tasksVector.end(); i != e; ++i)
            ExecutableTests.insert(*i);
    } else {
        Singleton<TRTYServerTestsFactory>()->GetKeys(ExecutableTests);
    }

    TSet<TString> excludedTests;
    if (parseOpt.Has('e')) {
        TVector<TString> tasksVector;
        Split(parseOpt.Get('e'), ",", tasksVector);
        for (TVector<TString>::const_iterator i = tasksVector.begin(), e = tasksVector.end(); i != e; ++i)
            excludedTests.insert(*i);
    }

    VERIFY_WITH_LOG(!CacheLifeTime || !!CacheDir, "Cannot be CacheLifeTime(-L) without Cache(-M or -F)");
    TRegExMatch regExp(regularExpressionDoRun.data());
    TRegExMatch regExpExclude(regularExpressionExclude.data());

    TString allowedTags = parseOpt.GetOrElse('X', "");
    TString disallowedTags = parseOpt.GetOrElse('B', "");
    THolder<TRegExMatch> allowedTagsReg(!!allowedTags ? new TRegExMatch(allowedTags.data()) : nullptr);
    THolder<TRegExMatch> disallowedTagsReg(!!disallowedTags ? new TRegExMatch(disallowedTags.data()) : nullptr);

    for (TSet<TString>::const_iterator i = ExecutableTests.begin(), e = ExecutableTests.end(); i != e; ++i)
        if ((!!regularExpressionDoRun && !regExp.Match(i->data()))
            || (!!regularExpressionExclude && regExpExclude.Match(i->data()))
            || (!!StartTest && (*i < StartTest))
            || TagsDisallowed(allowedTagsReg.Get(), disallowedTagsReg.Get(), i->data())) {
                excludedTests.insert(*i);
                continue;
        }

    for (TSet<TString>::const_iterator i = excludedTests.begin(), e = excludedTests.end(); i != e; ++i)
        ExecutableTests.erase(*i);

    if (parseOpt.Has('O')) {
        for (TSet<TString>::const_iterator i = ExecutableTests.begin(), e = ExecutableTests.end(); i != e; ++i)
            Cout << *i << Endl;
        exit(0);
    }

    if (parseOpt.Has('C')) {
        TVector<TString> logsToCheck;
        Split(parseOpt.Get('C'), ",", logsToCheck);
        for (size_t i = 0; i < logsToCheck.size(); i++) {
            const TString& logType = logsToCheck[i];
            TAutoPtr<TLogChecker> checker = TLogChecker::TFactory::Construct(logType, logType);
            VERIFY_WITH_LOG(checker, "Failed to construct log checker for log type %s", logType.data());
            LogCheckers.push_back(checker.Release());
        }
    }
}

TBackendProxyConfig& TTestSet::GetProxyConfig() {
    return ProxyConfig;
}
