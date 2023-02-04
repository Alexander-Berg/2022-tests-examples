#include "load.h"

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/rtyserver_test/testerlib/system_signals.h>
#include <saas/rtyserver_test/util/tass_parsers.h>

#include <util/stream/file.h>
#include <util/system/env.h>

using namespace NRTYServer;

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestLoadSearchCommon, TestLoadCommon)
    ui16 SearchPort = 0;
    virtual void RunNodeIfPresents(TString nodeName, const TSet<TString>& scripts){
        if (scripts.contains(nodeName)){
            NOTICE_LOG << "starting " << nodeName << "..." << Endl;
            if (!Callback->RunNode(nodeName))
                ythrow yexception() << "fail to run node " << nodeName;
            NOTICE_LOG << nodeName << " done..." << Endl;
        }
        else {
            WARNING_LOG << nodeName << " script not found, continue without it" << Endl;
        }
    }
    virtual void RenameCurrentFiles(TString path, ui16 session, TString expression){
        TVector<TFsPath> files;
        TFsPath(path).List(files);
        for (TVector<TFsPath>::iterator f = files.begin(); f != files.end(); ++f){
            size_t pos = f->GetName().find(expression);
            if (pos != TString::npos){
                TString newName = f->GetName().replace(pos, expression.size(), ToString(session));
                f->RenameTo(f->Parent().Child(newName));
            }
        }
    }
    virtual void SetSearchPort(){
        SearchPort = Controller->GetConfig().Searcher.Port;
        SetEnv("SEARCH_PORT", ToString(SearchPort));
    }

    virtual void BackendPortAsSearch(){
        if (Controller->GetConfig().Controllers.size() > 1)
            ythrow yexception() << "more than one backend for backend-only loadtest";
        SearchPort = Controller->GetConfig().Controllers[0].Port - 3;
        SetEnv("SEARCH_PORT", ToString(SearchPort));
    }
    virtual void RunIndexingBeforeLoad(){
        ui16 port = Controller->GetConfig().Indexer.Port;
        SetEnv("INDEXER_PORT", ToString(port));
        if (!Callback->RunNode("run_indexing"))
            ythrow yexception() << "fail to run fake dolb" << Endl;
        INFO_LOG << "indexing started" << Endl;
    }
    virtual void StopIndexingAfterLoad(){
        Callback->StopNode("run_indexing");
        INFO_LOG << "indexing stopped" << Endl;
    }

    virtual void BeforeLoad(){
        try {
            TJsonPtr serverInfo(Controller->GetServerInfo());
            NJson::TJsonValue* info = &(*serverInfo)[0];
            ui64 docsFinal = (*info)["docs_in_final_indexes"].GetUInteger();
            if (docsFinal > 0) {
                RTY_MEM_LOG("mem_before_dolb_start");
            }
        } catch(...) {}
        DoBeforeLoad();
        SendProfSignals();
    };
    virtual void AfterLoad(){
        SendProfSignals();
        DoAfterLoad();
    };
    virtual void AfterWarmup(){};

    virtual void DoBeforeLoad(){};
    virtual void DoAfterLoad(){};

    bool Run() override{
        NOTICE_LOG << "Preparing for running loadtest, found " << RunNDolbs << " sessions" << Endl;
        RunNDolbs = (RunNDolbs > 0) ? RunNDolbs : 3;
        RunNDolbs = (RunNDolbs < 50) ? RunNDolbs : 3;

        TJsonPtr infos = Controller->GetServerInfo();
        TStringStream ss;
        NJson::WriteJson(&ss, infos.Get());
        NOTICE_LOG << ss.Str() << Endl;

        SetSearchPort();
        if (GetEnv("LOG_PATH") == TString())
            SetEnv("LOG_PATH", NFs::CurrentWorkingDirectory());
        TSet<TString> scripts = Callback->GetNodesNames(TRtyTestNodeType::TNODE_SCRIPT);

        RunNodeIfPresents("do_plan", scripts);

        BeforeLoad();

        bool success = RunDolb(scripts);

        AfterLoad();

        return success;
    }

    virtual bool RunDolb(const TSet<TString>& scripts) {
        SetEnv("CURRENT_SESSION", "warmup");
        RunNodeIfPresents("warmup", scripts);
        AfterWarmup();

        //sessions: load; dump short; dumf full; save
        for (ui16 i = 1; i <= RunNDolbs; ++i){
            SetEnv("CURRENT_SESSION", ToString(i));
            NOTICE_LOG << "Running dolb, session " << i << Endl;
            if (!Callback->RunNode("run_dolb"))
                ythrow yexception() << "fail to run node" << Endl;

            //process short
            RunNodeIfPresents("dolb_dump", scripts);
            RunNodeIfPresents("dolb_dump_full", scripts);
            //find&rename files
            RenameCurrentFiles(GetEnv("LOG_PATH"), i, "$CURRENT_SESSION");
            NOTICE_LOG << "dolb session " << i << " done" << Endl;
        }

        bool success = true;
        TRY
            TString tassResult = GetTassResult();
            INFO_LOG << "tass_signals: " << tassResult << Endl;

            success = CheckTassResult(tassResult);
            if (!success) {
                ERROR_LOG << "/tass data indicate a failure" << Endl;
            }
        CATCH("getting /tass");
        return success;
    }

    virtual TString GetTassResult() {
        TString tassResult;
        Controller->ProcessQuery("/tass", &tassResult, "localhost", SearchPort + 3, false);
        return tassResult;
    }

    virtual bool CheckTassResult(const TString&) {
        return true;
    };
};

START_TEST_DEFINE_PARENT(TestLoadSearch, TestLoadSearchCommon)
};

START_TEST_DEFINE_PARENT(TestLoadSearchBackend, TestLoadSearchCommon)
void SetSearchPort() override{
    BackendPortAsSearch();
}
};

START_TEST_DEFINE_PARENT(TestLoadSearchAndFake, TestLoadSearchCommon)
void DoBeforeLoad() override{
    if (!Callback->RunNode("run_dolb_fake"))
        ythrow yexception() << "fail to run fake dolb" << Endl;
    INFO_LOG << "fake dolb started" << Endl;
}
void DoAfterLoad() override{
    Callback->StopNode("run_dolb_fake");
    INFO_LOG << "fake dolb stopped" << Endl;
}
};

START_TEST_DEFINE_PARENT(TestLoadSearchAndIndex, TestLoadSearchCommon)
void DoBeforeLoad() override{
    RunIndexingBeforeLoad();
}
void DoAfterLoad() override{
    StopIndexingAfterLoad();
    CheckTassIndex();
}
virtual void CheckTassIndex(){
    TString tassResult;
    ui64 indexOk;
    Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Indexer.Port + 3, false);
    INFO_LOG << "tass_iproxy_signals: " << tassResult << Endl;
    if (!TRTYTassParser::GetTassValue(tassResult, "index-CTYPE-200_dmmm", &indexOk))
        ythrow (yexception() << "Failed to get index-200 from iproxy TUnistat data");
    if (indexOk <= 0)
        ythrow (yexception() << "No successful indexing in tass");
}
};

START_TEST_DEFINE_PARENT(TestLoadSearchAndIndexBackend, TestLoadSearchCommon)
void SetSearchPort() override{
    BackendPortAsSearch();
}
void DoBeforeLoad() override{
    RunIndexingBeforeLoad();
}
void DoAfterLoad() override{
    StopIndexingAfterLoad();
}
bool CheckTassResult(const TString& tassResult) override{
    ui64 indexOk;
    INFO_LOG << "Check for backend indexing signal" << Endl;
    if (!TRTYTassParser::GetTassValue(tassResult, "backend-index-CTYPE-200_dmmm", &indexOk))
        return false;
    return indexOk > 0;
}
};


namespace {
    static ui64 GetUnanwsersCount(const TString& tassResults){
        ui64 unanswered;
        if (!TRTYTassParser::GetTassValue(tassResults, "backend-source-CTYPE-unanswers_dmmm", &unanswered))
            ythrow (yexception() << "Failed to get unanswers count from TUnistat data");

        return unanswered;
    }
}

START_TEST_DEFINE_PARENT(TestLoadSearchBackendTass, TestLoadSearchCommon)
void SetSearchPort() override {
    BackendPortAsSearch();
}

void AfterWarmup() override {
    TString tassBefore = GetTassResult();
    UnansweredBeforeTest = GetUnanwsersCount(tassBefore);
}

bool CheckTassResult(const TString& tassResult) override {
    ui64 unasweredAfterTest = GetUnanwsersCount(tassResult);
    return UnansweredBeforeTest == unasweredAfterTest;
}

protected:
ui64 UnansweredBeforeTest;
};


START_TEST_DEFINE_PARENT(TestMeasureLoadSearchBackend, TestLoadSearchCommon)
enum TDumpMode {
    NoDump,
    DumpIteration,
    DumpFinal
};

struct TStats {
    ui64 RequestsNum;
    ui64 ErrorsNum;
    TDuration FullTime;
    ui64 SourceUnanswers;
};

public:
void SetSearchPort() override{
    BackendPortAsSearch();
}

virtual bool RunDolb(const TSet<TString>& scripts, ui32 sessionNo, ui32 rps, TDumpMode dumpMode) {
    SetEnv("CURRENT_SESSION", ToString(sessionNo));
    SetEnv("CURRENT_RPS", ToString(rps));

    auto unanswers = GetUnanwsersCount(GetTassResult());

    NOTICE_LOG << "Running dolb, session " << sessionNo << ", rps " << rps << Endl;
    if (!Callback->RunNode("run_dolb"))
        ythrow yexception() << "failed to run node" << Endl;

    unanswers = GetUnanwsersCount(GetTassResult()) - unanswers;

    TString filePrefix;
    if (dumpMode == DumpIteration) {
        //process intermediate results
        RunNodeIfPresents("dolb_dump_iter", scripts);
        filePrefix = "dolb_iter_results_";
    } else if (dumpMode == DumpFinal) {
        //process short & full
        RunNodeIfPresents("dolb_dump", scripts);
        RunNodeIfPresents("dolb_dump_full", scripts);
        filePrefix = "dolb_results_";
    }

    //find&rename files
    RenameCurrentFiles(GetEnv("LOG_PATH"), sessionNo, "$CURRENT_SESSION");

    bool statsOk = true;
    if (filePrefix) {
        TString fileName = GetDolbOutput(sessionNo, filePrefix);

        if (!TFsPath(fileName).Exists())
            ythrow (yexception() << "File " << fileName << " is not found");

        TStats stats;
        stats.SourceUnanswers = unanswers;
        ParseDolbStats(fileName, stats);

        statsOk = CheckDolbStats(stats, rps);
    }

    NOTICE_LOG << "dolb session " << sessionNo << " done" << Endl;
    return statsOk;
}

bool RunDolb(const TSet<TString> &scripts) override {
    ui32 minRps, maxRps, precision, expectedResult;
    const auto& vars = Cluster->GetConfig().PreprocessorVariables;

    minRps = ReadIntVar(vars, "BINARY_RPS_MIN", 100);
    maxRps = ReadIntVar(vars, "BINARY_RPS_MAX", 5000);
    precision = ReadIntVar(vars, "BINARY_RPS_PREC", 10);
    expectedResult = ReadIntVar(vars, "RPS_REQUIRED", 0);

    precision = ClampVal<ui32>(precision, 5, 1000);
    minRps = ClampVal<ui32>(minRps, precision, 100000);
    maxRps = ClampVal<ui32>(maxRps, minRps, 100000);

    ui32 sessionNo = 1;
    ui32 curRps = maxRps;

    SetEnv("CURRENT_SESSION", "warmup");
    RunNodeIfPresents("warmup", scripts);
    AfterWarmup();

    NOTICE_LOG << "Starting binary search in [" << minRps << ", " << maxRps << "] rps range, precision=" << precision << Endl;

    while (minRps + precision < maxRps) {
        curRps = (minRps + maxRps) / 2;
        if (curRps == minRps)
            break;

        bool success = false;
        for (ui16 i = 1; i <= RunNDolbs; ++i) {
            success = RunDolb(scripts, sessionNo++, curRps, DumpIteration);
            if (success)
                break;
        }

        if (success)
            minRps = curRps;
        else
            maxRps = curRps;
    }

    //We'd better decrease the RPS a little to guarantee the stability of the test
    curRps -= Max(curRps / 50, curRps % precision); // -2% to suppress the natural instability
    curRps -= curRps % precision;                   // Round the number as specified in BINARY_RPS_PREC
    curRps = Max(curRps, precision);                // Ensure the correctness of the result

    bool goodEnough = curRps >= expectedResult;

    //Do the last run
    NOTICE_LOG << "Running the final dolb session" << Endl;
    bool ok = RunDolb(scripts, sessionNo, curRps, DumpFinal);
    if (ok)
        NOTICE_LOG << "Binary search results: stable at " << curRps << " rps" << Endl;
    else
        ERROR_LOG << "Binary search failure: no stable rps rate was found" << Endl;
    if (ok && !goodEnough)
        ERROR_LOG << "Expected " << expectedResult << " rps, got " << curRps << " rps" << Endl;

    return ok && goodEnough;
}

protected:
static ui32 ReadIntVar(const NMiniCluster::TConfig::TKeyValues& map, const TString& key, const ui32 defVl) {
    auto iterator = map.find(key);
    if (iterator == map.end())
        return defVl;

    ui32 result = defVl;
    TryFromString(iterator->second, result);
    return result;
}

static ui64 ParseDolbLine(const TString& line, ui64 defVl) {
    TVector<TString> v;
    StringSplitter(line).SplitByString("  ").SkipEmpty().Limit(2).Collect(&v);
    if (v.size() < 2)
        return defVl;

    TString& s = v[1];
    size_t b = s.find_first_not_of(' ');
    size_t e = s.find_first_of(", ", b);
    if (b == TString::npos)
        b = 0;
    if (e == TString::npos)
        e = s.size();
    s = s.substr(b, e - b);

    ui64 vl;
    if (!TryFromString<ui64>(v[1], vl))
        return defVl;
    return vl;
}

static void ParseDolbStats(const TString& fileName, TStats& stats) {
    TFileInput results(fileName);
    TString line;

    const ui64 Unknown = (ui64)-1;
    stats.FullTime = TDuration::Max();
    stats.RequestsNum = Unknown;
    stats.ErrorsNum = 0;

    while (results.ReadLine(line)) {
        if (line.StartsWith(TStringBuf("requests")))
            stats.RequestsNum = ParseDolbLine(line, stats.RequestsNum);
        else if (line.StartsWith(TStringBuf("error requests")))
            stats.ErrorsNum += ParseDolbLine(line, stats.ErrorsNum);
        else if (line.StartsWith(TStringBuf("Internal server error")))
            stats.ErrorsNum += ParseDolbLine(line, stats.ErrorsNum);
        else if (line.StartsWith(TStringBuf("full time")))
            stats.FullTime = TDuration::MicroSeconds(ParseDolbLine(line, stats.FullTime.MicroSeconds()));
    }

    if (stats.FullTime == TDuration::Max() || stats.RequestsNum == Unknown)
        ythrow (yexception() << fileName << ": invalid file format (failed to parse)");
}

static TString GetDolbOutput(ui32 sessionNo, const TString& filePrefix) {
    return GetEnv("LOG_PATH") + "/" + filePrefix + ToString(sessionNo) + ".out";
}

static bool CheckDolbStats(TStats& stats, ui32 expectedRps) {
    CHECK_WITH_LOG(stats.RequestsNum != 0);

    INFO_LOG << "stats: errors=" << stats.ErrorsNum << ";source_unanswers=" << stats.SourceUnanswers << ";requests_num=" << stats.RequestsNum << Endl;

    //Check if the rps was actually obtained
    TDuration adjFullTime = stats.FullTime;
    if (adjFullTime > TDuration::Seconds(MaxDolbShutdownTime))
        adjFullTime -= TDuration::Seconds(MaxDolbShutdownTime);
    if ((double) stats.RequestsNum * TDuration::Seconds(1).MicroSeconds() / adjFullTime.MicroSeconds() < expectedRps) {
        WARNING_LOG << "DEXECUTOR failed to obtain " << expectedRps << " rps (not enough routines?)" << Endl;
        return false;
    }

    double errors = (double) (stats.ErrorsNum + stats.SourceUnanswers) / stats.RequestsNum;
    if (errors >= MaxErrorRequests) {
        INFO_LOG << "Check failed: (erros+source_unanswers)/requests_num=" << errors << Endl;
        return false;
    } else {
        return true;
    }

}

protected:
static constexpr double MaxErrorRequests = 0.005; //Errors + SourceUnanswers should be less than this
static constexpr ui32 MaxDolbShutdownTime = 6;
};
