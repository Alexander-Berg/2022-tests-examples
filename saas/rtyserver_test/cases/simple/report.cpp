#include <saas/rtyserver/common/common_messages.h>
#include <saas/rtyserver_test/testerlib/search_checker.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>



SERVICE_TEST_RTYSERVER_DEFINE(TestFormatBase)
    void PrepareTest(TVector<NRTYServer::TMessage>& messages, const ui32 docsCount = 10) {
        GenerateInput(messages, docsCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (auto&& mes : messages) {
            if (GetIsPrefixed()) {
                mes.MutableDocument()->SetKeyPrefix(GetIsPrefixed());
            }
        }

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }

    TString GetReportString(const TString request, const TString& format, ui16 code, ui32 attempts = 3) {
        CHECK_WITH_LOG(attempts > 0);
        const TString plainQuery = "/?service=tests&text=" + request + "&kps=" + ::ToString(GetIsPrefixed())
            + "&sgkps=" + ::ToString(GetIsPrefixed());

        const TString queryWithFormat = plainQuery + "&format=" + format;
        const TString queryWithEvLog = plainQuery + "&ms=proto&hr=da&dump=eventlog";

        TString reportString;
        ui16 rc = ProcessQuery(queryWithFormat, &reportString);

        ui32 att = 1;
        while (att != attempts) {
            if (rc == code)
                break;

            TString evLogRes;
            const ui16 evLogRc = ProcessQuery(plainQuery, &evLogRes);
            DEBUG_LOG << "Code " << ::ToString(evLogRc) << " " << evLogRes << Endl;

            rc = ProcessQuery(queryWithFormat, &reportString);
            DEBUG_LOG << "Report: " << reportString << " att: " << ::ToString(att) << " code: " << ::ToString(rc) << Endl;
            att++;
        }

        if (rc != code) {
            ythrow yexception() << "Incorrect reply code " << rc << "/" << code;
        }

        return reportString;
    }

    NJson::TJsonValue GetReport(const TString request, const TString& format, ui16 code) {
        TString reportString = GetReportString(request, format, code);

        NJson::TJsonValue report;
        NJson::ReadJsonFastTree(reportString, &report, true);
        return report;
    }

    bool CheckJsonReportError(const NJson::TJsonValue& report) {
        if (!report.IsMap()) {
            ERROR_LOG << "Incorrect json root" << Endl;
            return false;
        }

        ui64 found = FromString<ui64>(report["response"]["found"]["all"].GetStringRobust());
        if (found != 0) {
            ERROR_LOG << "Incorrect found value " << found << Endl;
            return false;
        }

        TString error = report["errors"][0].GetString();
        if (error.empty()) {
            ERROR_LOG << "No error info in report " << found << Endl;
            return false;
        }

        return true;
    }

    bool CheckJsonReport(const NJson::TJsonValue& report, ui32 docsCount = 1) {
        if (!report.IsMap()) {
            ERROR_LOG << "Incorrect json root" << Endl;
            return false;
        }

        ui64 found = FromString<ui64>(report["response"]["found"]["all"].GetStringRobust());
        if (found != docsCount) {
            ERROR_LOG << "Incorrect found value " << found << Endl;
            return false;
        }

        ui64 results = report["response"]["results"].GetArray().size();
        if (results != 1) {
            ERROR_LOG << "Incorrect results size " << results << Endl;
            return false;
        }

        ui64 groups = report["response"]["results"][0]["groups"].GetArray().size();
        if (groups != docsCount) {
            ERROR_LOG << "Incorrect groups size " << groups << Endl;
            return false;
        }

        for (ui32 i = 0; i < groups; ++i) {
             const auto& docs = report["response"]["results"][0]["groups"][i]["documents"].GetArray();
             if (docs.size() != 1) {
                 ERROR_LOG << "Incorrect docs in group " << docs.size() << "/" << i << Endl;
                 return false;
             }

             if (docs[0]["url"].GetString().empty()) {
                 ERROR_LOG << "Empty doc url " << Endl;
                 return false;
             }
         }

         return true;
    }

    bool CheckYSuggestReport(const NJson::TJsonValue& report, ui32 docsCount, const TString& request, const TVector<TString>& requiredUrls) {
        NJson::TJsonValue::TArray globalArray;
        if (!report.GetArray(&globalArray)) {
            ERROR_LOG << "json root is not an array" << Endl;
            return false;
        }

        if (globalArray.size() != 2) {
            ERROR_LOG << "Incorrect global structure" << Endl;
            return false;
        }

        if (globalArray[0].GetStringSafe() != request) {
            ERROR_LOG << "Incorrect request " << globalArray[0].GetStringSafe() << "/" << request << Endl;
            return false;
        }

        const auto& urls = globalArray[1].GetArraySafe();
        if (urls.size() != docsCount) {
            ERROR_LOG << "Incorrect result docs count " << urls.size() << "/" << docsCount << Endl;
            return false;
        }
        for (auto&& url : requiredUrls) {
            Y_ENSURE(std::find(urls.begin(), urls.end(), url) != urls.end(), "cannot find " << url);
        }

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestReportFormat, TestFormatBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        PrepareTest(messages);

        CHECK_TEST_TRUE(CheckJsonReport(GetReport("body" ,"json", 200), 10));
        CHECK_TEST_TRUE(CheckJsonReport(GetReport("\"body\"","json", 200), 10));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestReportKVFormat, TestFormatBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        PrepareTest(messages);

        TString key = messages[0].GetDocument().GetUrl();
        CHECK_TEST_TRUE(CheckJsonReport(GetReport(key, "json", 200)));
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";

        (*SPConfigDiff)["Service.GlobalCgiParams.sp_meta_search"] = "multi_proxy";
        (*SPConfigDiff)["Service.CgiParams.meta_search"] = "first_found";
        (*SPConfigDiff)["Service.CgiParams.normal_kv_report"] = "da";

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestReportFormatSuggest, TestFormatBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        PrepareTest(messages);

        TVector<TString> urls;
        for (auto&& m : messages) {
            const TString& url = m.GetDocument().GetUrl();
            urls.push_back(url);
        }

        CHECK_TEST_TRUE(CheckYSuggestReport(GetReport("body", "ysuggest", 200), 10, "body", urls));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestReportKVFormatSuggest, TestFormatBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        PrepareTest(messages);

        TVector<TString> urls;
        for (auto&& m : messages) {
            const TString& url = m.GetDocument().GetUrl();
            urls.push_back(url);
        }

        CHECK_TEST_TRUE(CheckYSuggestReport(GetReport(urls[0], "ysuggest", 200), 1, urls[0], TVector<TString>(1, urls[0])));
        const TString request = JoinStrings(urls, ",");
        CHECK_TEST_TRUE(CheckYSuggestReport(GetReport(request, "ysuggest", 200), 10, request, urls));
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";

        (*SPConfigDiff)["Service.GlobalCgiParams.sp_meta_search"] = "multi_proxy";
        (*SPConfigDiff)["Service.CgiParams.meta_search"] = "first_found";
        (*SPConfigDiff)["Service.CgiParams.normal_kv_report"] = "da";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestReportFormatErrors, TestFormatBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        PrepareTest(messages);

        TString key = messages[0].GetDocument().GetUrl();
        CHECK_TEST_TRUE(CheckJsonReportError(GetReport("body&sp_meta_search=proxy", "json", 502)));
        CHECK_TEST_TRUE(!GetReportString("body","json_", 404).empty());
        return true;
    }
};


START_TEST_DEFINE_PARENT(TestReportFormatPaging, TestFormatBase)
    bool CheckReportsIdentical(const TString& query) {
        NJson::TJsonValue jsonReport = GetReport(query, "json", 200);

        if (!jsonReport.IsMap()) {
            ERROR_LOG << "Incorrect json root" << Endl;
            return false;
        }

        const ui64 jsonResults = jsonReport["response"]["results"].GetArray().size();
        if (jsonResults != 1) {
            ERROR_LOG << "Incorrect results size " << jsonResults << Endl;
            return false;
        }

        const ui64 jsonGroups = jsonReport["response"]["results"][0]["groups"].GetArray().size();

        TString protoReportStr;
        if (!ProcessQuery("/?service=tests&kps=" + ::ToString(GetIsPrefixed()) + "&sgkps=" + ::ToString(GetIsPrefixed()) +
                "&text=" + query + "&ms=proto", &protoReportStr)) {
            ERROR_LOG << "Incorrect ProcessQuery result" << Endl;
            return false;
        }

        NMetaProtocol::TReport report;
        if (!report.ParseFromString(protoReportStr)) {
            ERROR_LOG << "Failed to parse report" << Endl;
            return false;
        }
        DEBUG_LOG << report.DebugString() << Endl;

        const NMetaProtocol::TGrouping& grouping(report.GetGrouping(0));
        const ui64 protoGroups = grouping.GetGroup().size();
        if (protoGroups != jsonGroups) {
            ERROR_LOG << "Incorrect groups size " << grouping.GetGroup().size() << " vs " << jsonGroups << Endl;
            return false;

        }

        for (ui32 i = 0; i < jsonGroups; ++i) {
             const auto& jsonDocs = jsonReport["response"]["results"][0]["groups"][i]["documents"].GetArray();
             if (jsonDocs.size() != 1) {
                 ERROR_LOG << "Incorrect docs in json groups " << jsonDocs.size() << "/" << i << Endl;
                 return false;
             }

             const TString jsonUrl = jsonDocs[0]["url"].GetString();
             if (jsonUrl.empty()) {
                 ERROR_LOG << "Empty json doc url " << Endl;
                 return false;
             }

            if (grouping.GetGroup(i).GetDocument().size() != 1) {
                ERROR_LOG << "Incorrect results size " << jsonResults << Endl;
                return false;
            }
            const NMetaProtocol::TDocument& doc = grouping.GetGroup(i).GetDocument(0);
            CHECK_TEST_EQ(doc.GetArchiveInfo().GetUrl(), jsonUrl);
         }
         return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        PrepareTest(messages, 10);

        CHECK_TEST_TRUE(CheckReportsIdentical("body&p=0&numdoc=3"));
        CHECK_TEST_TRUE(CheckReportsIdentical("body&p=1&numdoc=3"));
        CHECK_TEST_TRUE(CheckReportsIdentical("body&p=2&numdoc=3"));
        return true;
    }
};

START_TEST_DEFINE(TestFullArchiveGrouping)
    bool QueryDoc(const TVector<NRTYServer::TMessage>& messages, const TString& groupingParam, const TString& extraParams = "") {
        const ui32 prefix = messages[0].GetDocument().GetKeyPrefix();
        TString keys;
        for (const auto& message: messages) {
            if (keys) {
                keys += ",";
            }
            keys += message.GetDocument().GetUrl();
        }

        TString query = "/?text=" + keys + "&sp_meta_search=multi_proxy&meta_search=first_found&normal_kv_report=1&sgkps=" + ToString(prefix);
        if (groupingParam) {
            query += "&g=" + groupingParam;
        }
        query += extraParams;

        TString searchResult = Query(query);
        NMetaProtocol::TReport report;
        Y_PROTOBUF_SUPPRESS_NODISCARD report.ParseFromString(searchResult);
        DEBUG_LOG << report.DebugString() << Endl;
        CHECK_TEST_EQ(report.GetGrouping().size(), 1);
        const NMetaProtocol::TGrouping& grouping = report.GetGrouping(0);
        if (groupingParam) {
            CHECK_TEST_EQ(grouping.GetGroup().size(), 2);
            {
                NMetaProtocol::TGroup grp = grouping.GetGroup(0);
                CHECK_TEST_EQ(grp.GetCategoryName(), "1");
                CHECK_TEST_EQ(grp.GetDocument().size(), 2);
                TVector<TString> keys;
                for (int i = 0; i < grp.GetDocument().size(); ++i) {
                    keys.push_back(grp.GetDocument(i).GetUrl());
                }
                Sort(keys);
                CHECK_TEST_EQ(keys[0], messages[0].GetDocument().GetUrl());
                CHECK_TEST_EQ(keys[1], messages[1].GetDocument().GetUrl());
            }
            {
                NMetaProtocol::TGroup grp = grouping.GetGroup(1);
                CHECK_TEST_EQ(grp.GetCategoryName(), "2");
                CHECK_TEST_EQ(grp.GetDocument().size(), 1);
                CHECK_TEST_EQ(grp.GetDocument(0).GetUrl(), messages[2].GetDocument().GetUrl());
            }
        } else {
            CHECK_TEST_EQ(grouping.GetGroup().size(), int(messages.size()));
            TVector<TString> keys;
            for (int i = 0; i < grouping.GetGroup().size(); ++i) {
                NMetaProtocol::TGroup grp = grouping.GetGroup(i);
                CHECK_TEST_EQ(grp.GetDocument().size(), 1);
                keys.push_back(grp.GetDocument(0).GetUrl());
            }
            Sort(keys);
            CHECK_TEST_EQ(keys[0], messages[0].GetDocument().GetUrl());
            CHECK_TEST_EQ(keys[1], messages[1].GetDocument().GetUrl());
            CHECK_TEST_EQ(keys[2], messages[2].GetDocument().GetUrl());
        }
        return true;
    }

    bool Run() override {
        const ui32 messagesCount = 3;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        const ui64 ts = TInstant::Now().Seconds();
        const ui32 prefix = messages[0].MutableDocument()->GetKeyPrefix();
        for (ui32 i = 0; i < messagesCount; ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(prefix);
            messages[i].MutableDocument()->SetUrl("some_key_" + ToString(i));
            messages[i].MutableDocument()->SetModificationTimestamp(ts);
            messages[i].MutableDocument()->SetModificationTimestamp(ts);
        }
        AddProperty(messages[0], "zzzz", "1");
        AddProperty(messages[1], "zzzz", "1");
        AddProperty(messages[2], "zzzz", "2");

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_TRUE(QueryDoc(messages, "1.zzzz.1.1.1.1"));
        CHECK_TEST_TRUE(QueryDoc(messages, ""));
        CHECK_TEST_TRUE(QueryDoc(messages, "1.zzzz.1.1.1.1", "&gta=_UrlOnly"));
        CHECK_TEST_TRUE(QueryDoc(messages, "", "&gta=_UrlOnly"));

        return true;
 }

 bool InitConfig() override {
     SetIndexerParams(DISK, 100, 1);
     (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
     (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
     (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
     (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
     (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
     (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
     return true;
 }
};
