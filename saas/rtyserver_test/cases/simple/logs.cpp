
#include <saas/protos/rtyserver.pb.h>
#include <saas/rtyserver_test/log_checker/tskv_log_record.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/util/json/json.h>
#include <library/cpp/http/io/stream.h>
#include <kernel/search_daemon_iface/reqtypes.h>
#include <util/string/cast.h>
#include <util/system/fs.h>


//using namespace NRTY;


START_TEST_DEFINE(TestLogs)
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.AccessLog"] = Controller->GetConfigValue("LoggerType", "DaemonConfig") + "_access";
        (*ConfigDiff)["Indexer.Common.IndexLog"] = Controller->GetConfigValue("LoggerType", "DaemonConfig") + "_index";
        return true;
    }

    virtual bool GetLogLines(const TString serverLog, TVector<TString>& logLines){
        logLines.clear();
        CHECK_TEST_TRUE(TFsPath(serverLog).Exists());
        TUnbufferedFileInput file(serverLog);
        TString line;
        while (file.ReadLine(line)){
            logLines.push_back(line);
        }
        CHECK_TEST_TRUE(logLines.ysize());
        return true;
    }

    template <char c = ';'>
    bool GetLogRecords(const TString logFileName, TVector<NUtil::TParsedLogRecord::TValues>& records, TString filter="") {
        records.clear();
        TVector<TString> logLines;
        GetLogLines(logFileName, logLines);
        for (auto line : logLines){
            if (filter != "" && line.find(filter) == TString::npos) {
                continue;
            }
            NUtil::TTskvLogRecord<c> rec("");
            rec.ParseLine(line);
            records.push_back(rec.GetValues());
        }
        return (records.size() > 0);
    }

    virtual bool CheckAccessLog(TVector<NUtil::TParsedLogRecord::TValues>& records, int reqsCount, int docsPerReq, ui64 timeTotal) {
        TSet<int> ids;
        CHECK_TEST_TRUE((records.ysize() == reqsCount));
        ui64 minTimePerReq = timeTotal / (10 * reqsCount);
        ui64 maxTimePerReq = 3 * timeTotal / reqsCount;
        ui64 logTimeTotal = 0;
        for (auto& rec : records) {
            //id  -- different
            //ip, raw=ip
            //service == tests
            //query == body
            //reply_time -- 0 < time_mess/10 < x < time_mess
            //doccount -- == count_mess
            //cachehit -- == 0
            //timeouted -- == 0

            int id = FromString<int>(rec["id"]);
            CHECK_TEST_TRUE(ids.insert(id).second);

            CHECK_TEST_TRUE((rec["service"] == "tests"));
            CHECK_TEST_TRUE((rec["query"] == "body" || rec["query"].StartsWith("url:")));

            ui64 dur = FromString<ui64>(rec["reply_time"]);
            //CHECK_TEST_TRUE(dur > 0);
            CHECK_TEST_TRUE(dur > minTimePerReq);
            CHECK_TEST_TRUE(dur < maxTimePerReq);
            logTimeTotal += dur;

            CHECK_TEST_TRUE((FromString<int>(rec["doccount"]) == docsPerReq));
            CHECK_TEST_TRUE(rec["cachehit"].StartsWith('0'));
            CHECK_TEST_TRUE((rec["timeouted"] == "0"));
        }
        CHECK_TEST_TRUE(logTimeTotal > timeTotal / 15 && logTimeTotal <= timeTotal);
        return true;
    }

    virtual bool CheckAccessLogBackend(TVector<NUtil::TParsedLogRecord::TValues>& records, int reqsCount, int docsPerReq, ui64 timeTotal) {
        TSet<int> ids;
        CHECK_TEST_TRUE((records.ysize() == reqsCount));
        const ui64 maxTimePerReq = 3 * timeTotal / reqsCount;
        ui64 logTimeTotal = 0;
        ui64 procTimeTotal = 0;
        for (auto& rec : records) {
            //id  -- different
            //duration -- 0 < time_mess/10 < x < time_mess
            //process  -- 0 < x < dur;
            //result  -- == 1
            //count_docs -- == count_mess
            //type -- == Search
            //cachehit -- == 0
            const int id = FromString<int>(rec["id"]);
            CHECK_TEST_TRUE(ids.insert(id).second);

            const ui64 dur = FromString<ui64>(rec["duration"]);
            const ui64 proc = FromString<ui64>(rec["process"]);
            CHECK_TEST_GREATEREQ(dur, proc);
            CHECK_TEST_LESS(dur, maxTimePerReq);
            logTimeTotal += dur;
            procTimeTotal += proc;

            CHECK_TEST_EQ(rec["result"], "1");
            CHECK_TEST_EQ(FromString<int>(rec["count_docs"]), docsPerReq);
            CHECK_TEST_EQ(rec["type"], ToString(int(ERequestType::RT_Search)));
            CHECK_TEST_TRUE(rec["cachehit"].StartsWith('0'));
        }
        CHECK_TEST_GREATEREQ(logTimeTotal, timeTotal / 15);
        CHECK_TEST_LESSEQ(logTimeTotal, timeTotal);
        CHECK_TEST_GREATEREQ(procTimeTotal, timeTotal / 18);
        CHECK_TEST_LESSEQ(logTimeTotal, timeTotal);
        return true;
    }

    virtual bool CheckIndexLogBackend(TVector<NUtil::TParsedLogRecord::TValues>& records, int reqsCount, ui64 timeTotal, bool oneThread) {
        TSet<ui64> ids;
        DEBUG_LOG << "checking index log, records_size: " << records.ysize() << ", must be: " << reqsCount << Endl;
        CHECK_TEST_EQ(records.ysize(), reqsCount);
        TString okStatus = NRTYServer::TReply::TRTYStatus_Name(NRTYServer::TReply::TRTYStatus::TReply_TRTYStatus_OK);
        ui64 logTimeTotal = 0;
        for (auto& rec : records) {
            //id  -- different
            //reply -- ok
            //http_code -- 200
            //duration -- > 0

            const ui64 id = FromString<ui64>(rec["id"]);
            CHECK_TEST_TRUE(ids.insert(id).second);

            CHECK_TEST_EQ(rec["reply"], okStatus);
            CHECK_TEST_EQ(rec["http_code"], "200");

            const ui64 dur = FromString<ui64>(rec["duration"]);
            CHECK_TEST_GREATER(dur, 0u);
            CHECK_TEST_LESSEQ(dur, timeTotal);
            logTimeTotal += dur;
        }
        if (oneThread){
            CHECK_TEST_LESSEQ(logTimeTotal, timeTotal);
            CHECK_TEST_LESS(timeTotal / 3, logTimeTotal);
        }
        return true;
    }


    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        const int COUNT_MESSAGES = 10;
        TString indexLog = Controller->GetConfigValue("IndexLog", "Server.Indexer.Common");
        TString accessLog = Controller->GetConfigValue("AccessLog", "Server.Searcher");
        if (!accessLog || !indexLog)
            ythrow yexception() << "TestLogs must be started with access and index logs";
        TString accessLogSp;
        bool hasSproxy = Callback && Callback->GetNodesNames(TNODE_SEARCHPROXY).size();
        if (hasSproxy) {
            accessLogSp = Controller->GetConfigValue("InfoLog", "SearchProxy.Logger", TBackendProxy::TBackendSet(), TNODE_SEARCHPROXY);
            if (TFsPath(accessLogSp).Exists()) {
                NFs::Remove(accessLogSp);
                Controller->ProcessCommandOneHost("reopenlog", "localhost", Controller->GetConfig().Searcher.Port + 3);
            }
        }

        GenerateInput(messages, COUNT_MESSAGES, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        TInstant start = TInstant::Now();
        IndexMessages(messages, REALTIME, 1, 0, false);
        ui64 timeTotal = (TInstant::Now() - start).MilliSeconds();

        DEBUG_LOG << "callback: " << !!Callback << Endl;
        TVector<NUtil::TParsedLogRecord::TValues> records;
        CHECK_TEST_TRUE(GetLogRecords<'\t'>(indexLog, records, "http_code="));
        CHECK_TEST_TRUE(CheckIndexLogBackend(records, COUNT_MESSAGES, timeTotal, !Controller->GetConfig().Indexer.PackSend));

        //check backend-index-incoming here
        //check iproxy log here

        Sleep(TDuration::Seconds(1));
        start = TInstant::Now();
        TString sRes = Query("/?text=body&ms=proto&hr=da&numdoc=1");
        timeTotal = (TInstant::Now() - start).MilliSeconds();
        DEBUG_LOG << "timeTotal=" << timeTotal << " " << sRes << Endl;

        CHECK_TEST_TRUE(GetLogRecords(accessLog, records, "action=search"));
        CHECK_TEST_TRUE(CheckAccessLogBackend(records, 1, COUNT_MESSAGES, timeTotal));
        //check backend-search-incoming here
        if (hasSproxy) {
            CHECK_TEST_TRUE(GetLogRecords<'\t'>(accessLogSp, records, "tskv"));
            CHECK_TEST_TRUE(CheckAccessLog(records, 1, COUNT_MESSAGES, 1000 * (timeTotal + 1)));
        }

        NFs::Remove(accessLog);
        NFs::Remove(indexLog);
        Controller->ProcessCommand("reopenlog");
        if (hasSproxy){
            NFs::Remove(accessLogSp);
            Controller->ProcessCommandOneHost("reopenlog", "localhost", Controller->GetConfig().Searcher.Port + 3);
        }
        start = TInstant::Now();
        CheckSearchResults(messages);
        timeTotal = (TInstant::Now() - start).MilliSeconds();
        DEBUG_LOG << "timeTotal=" << timeTotal << " " << sRes << Endl;

        CHECK_TEST_TRUE(GetLogRecords(accessLog, records, "action=search"));
        CHECK_TEST_TRUE(CheckAccessLogBackend(records, COUNT_MESSAGES, 1, timeTotal));
        if (hasSproxy) {
            CHECK_TEST_TRUE(GetLogRecords<'\t'>(accessLogSp, records, "tskv"));
            CHECK_TEST_TRUE(CheckAccessLog(records, COUNT_MESSAGES, 1, 1000 * (timeTotal + 1)));
        }
        //check spr log here
        //check backend-search-incoming here

        //check some errors here

        //do tass here
        TString tassResult;
        Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);
        NOTICE_LOG << tassResult << Endl;

        return true;
    }
};
