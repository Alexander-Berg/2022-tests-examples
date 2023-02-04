#include "backend_proxy.h"

#include <saas/library/daemon_base/actions_engine/controller_client.h>
#include <saas/rtyserver/controller/controller_actions/detach_action.h>
#include <saas/rtyserver/controller/controller_actions/download_action.h>
#include <saas/rtyserver/controller/controller_actions/shards_action.h>
#include <saas/deploy_manager/scripts/set_conf/action.h>
#include <saas/util/json/json.h>

#include <search/idl/meta.pb.h>
#include <search/session/compression/report.h>

#include <kernel/urlid/doc_handle.h>

#include <library/cpp/charset/ci_string.h>
#include <library/cpp/http/io/stream.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/string_utils/quote/quote.h>

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/coded_stream.h>

#include <util/charset/wide.h>
#include <util/memory/blob.h>
#include <util/network/socket.h>
#include <util/stream/str.h>
#include <util/stream/file.h>
#include <util/string/hex.h>
#include <util/string/vector.h>

bool ISearchSourceSelector::ParseAndCheck(const TString& docidStr, const TString& query, ui32 requiredMetaSearchLevel) {
    const TDocHandle docHandle(docidStr);
    DEBUG_LOG << "CHECK docid string " << docidStr << "/" << requiredMetaSearchLevel << Endl;

    if (!docHandle) {
        ERROR_LOG << "Empty docid string" << Endl;
        return false;
    }

    TDocHandle::THash docid;
    docHandle.TryIntHash(docid);
    DocId = docid;

    if (docHandle.DocRoute)
        SearcherId = docHandle.BaseSearchNum();
    else
        SearcherId = 0;

    const TVector<TString>& splitted = SplitString(docidStr, "-");
    return DoCheck(splitted, query, requiredMetaSearchLevel);
}

bool ISearchSourceSelector::HasSpecialSource() const {
    return Source != -1;
}

ui32 ISearchSourceSelector::GetParsedDocId() const {
    return DocId.GetRef();
}

ui32 ISearchSourceSelector::GetParsedSearcherId() const {
    return SearcherId.GetRef();
}

i32 ISearchSourceSelector::GetSource() const {
    return Source;
}

bool TDefaultSourceSelector::DoCheck(const TVector<TString>& splitted, const TString& query, ui32 requiredMetaSearchLevel) {
    if (splitted.size() < requiredMetaSearchLevel && !HasSpecialSource()) {
        if (query.find("broadcast_fetch") == TString::npos)
            return false;
    }
    return true;
}

bool TIndifferentSourceSelector::DoCheck(const TVector<TString>&, const TString&, ui32) {
    return true;
}

bool TLevelSourceSelector::DoCheck(const TVector<TString>& splitted, const TString&, ui32) {
    CHECK_WITH_LOG(splitted.size() == Level) << Level << " / " << splitted.size() << " / " << JoinStrings(splitted, ",");
    return true;
}

TBackendProxy::TBackendProxy(const TBackendProxyConfig& config)
    : Config(config)
    , RequiredMetaSearchLevel(2)
{
    for (ui32 i = 0; i < Config.Controllers.size(); ++i)
        ActiveBackends.insert(i);
}

void TBackendProxy::RestartServer(bool rigidStop, TRestartServerStatistics* statistics) {
    TJsonPtr result = ProcessCommand("restart&rigid=" + ToString(rigidStop));
    if (statistics) {
        statistics->StopTimeMilliseconds = (*result)[0]["stop_time"].GetInteger();
        statistics->StartTimeMilliseconds = (*result)[0]["start_time"].GetInteger();
    }
}

void TBackendProxy::StopBackends() {
     TJsonPtr result = ProcessCommand("stop");
    if ((*result)[0]["task_status"].GetStringRobust() != "FINISHED")
        ythrow yexception() << "failed to restart";
}

void TBackendProxy::StopBackend(ui32 backend) {
    TJsonPtr result = ProcessCommand("stop", backend);
    if ((*result)[0]["task_status"].GetStringRobust() != "FINISHED")
        ythrow yexception() << "failed to restart";
}

void TBackendProxy::RestartBackend(ui32 backend) {
    TJsonPtr result = ProcessCommand("restart", backend);
    if ((*result)[0]["task_status"].GetStringRobust() != "FINISHED")
        ythrow yexception() << "failed to restart";
}

void TBackendProxy::AbortBackends(TDuration waitStartTime, const TBackendSet& backends) {
    try {
        ProcessCommand("abort", backends, TString(), 1);
    } catch (...) {
    }
    if (waitStartTime != TDuration::Zero())
        WaitActiveServer(waitStartTime, backends);
}

void TBackendProxy::WaitEmptyIndexingQueues() const {
    ProcessCommand("wait_empty_indexing_queues");
}

void TBackendProxy::WaitActiveServer(const TDuration& duration, const TBackendSet& backends) const {
    DEBUG_LOG << "Tester: waiting for active server" << Endl;
    TInstant start = Now();
    bool result;
    while (true) {
        try {
            result = IsServerActive(backends);
            if (result || duration != TDuration::Zero() && Now() - start > duration)
                break;
        } catch (...) {
        }
        Sleep(TDuration::Seconds(1));
    }
    if (!result)
        ythrow yexception() << "server not start for " << duration;
    DEBUG_LOG << "Tester: server is active, continuing" << Endl;
}

bool TBackendProxy::IsServerActive(const TBackendSet& backends) const {
    TSimpleSharedPtr<NJson::TJsonValue> result(ProcessCommand("get_status", backends));
    const NJson::TJsonValue::TArray& statusArray = result->GetArray();
    bool allActive = true;
    for (NJson::TJsonValue::TArray::const_iterator i = statusArray.begin(); i != statusArray.end(); ++i) {
        const TCiString& status = (*i)["status"].GetString();
        allActive &= (status == "Active");
    }
    return allActive;
}

void TBackendProxy::WaitServerDesiredStatus(const THashSet<TCiString>& statuses, const TDuration& duration, const TBackendSet& backends) const {
    DEBUG_LOG << "Tester: waiting for server to have status any of " << JoinStrings(statuses.cbegin(), statuses.cend(), ",") << Endl;
    TInstant start = Now();
    bool result;
    while (true) {
        try {
            result = ServerHasStatus(statuses, backends);
            if (result || duration != TDuration::Zero() && Now() - start > duration)
                break;
        } catch (...) {
        }
        Sleep(TDuration::Seconds(1));
    }
    if (!result)
        ythrow yexception() << "server not start for " << duration;
    DEBUG_LOG << "Tester: servers are in desired statuses, continuing" << Endl;
}

bool TBackendProxy::ServerHasStatus(const THashSet<TCiString>& statuses, const TBackendSet& backends) const {
    TSimpleSharedPtr<NJson::TJsonValue> result(ProcessCommand("get_status", backends));
    const NJson::TJsonValue::TArray& statusArray = result->GetArray();
    bool allHaveDesiredStatus = true;
    for (NJson::TJsonValue::TArray::const_iterator i = statusArray.begin(); i != statusArray.end(); ++i) {
        const TCiString& status = (*i)["status"].GetString();
        allHaveDesiredStatus &= statuses.contains(status);
    }
    return allHaveDesiredStatus;
}

ui32 TBackendProxy::ProcessQuery(const TString& query, TString* result, TString host, ui16 port, bool appendService, const THashMap<TString, TString>* headers) const {
    try {
        if (!host && !port) {
            host = Config.Searcher.Host;
            port = Config.Searcher.Port;
        }
        TString service;
        if (appendService && query.find("service=") == TString::npos)
            service = "&service=tests";
        DEBUG_LOG << "REQUEST: " << host << ":" << port << "/" << query << service << Endl;

        TNetworkAddress addr(host, port);
        TSocket s(addr);
        s.SetSocketTimeout(50, 0);
        s.SetZeroLinger();

        TSocketOutput so(s);
        THttpOutput output(&so);

        output.EnableKeepAlive(false);
        output.EnableCompression(false);

        output << "GET " << query << service << " HTTP/1.1" << "\r\n";
        output << "Host: " << host << "\r\n";
        if (headers) {
            for (auto&& h : *headers) {
                output << h.first << ": " << h.second << "\r\n";
            }
        }
        output << "\r\n";
        output.Finish();

        TSocketInput si(s);
        THttpInput input(&si);
        unsigned http_code = ParseHttpRetCode(input.FirstLine());
        if (result)
            *result = input.ReadAll();
        return http_code;
    } catch (...) {
        ERROR_LOG << "Error on search: " << CurrentExceptionMessage() << Endl;
        return 0;
    }
}

namespace {
    inline ui64 HexToui64(const char* hex) {
        ui64 result = 0;
        int i = 0;
        int d;
        while (hex[i] && (d = Char2Digit(hex[i++])) >= 0) {
            result = result << 4;
            result |= d;
        }
        return result;
    }
    void SendPostHttpRequest(TSocket& s, const TStringBuf& host, const TStringBuf& request, const TStringBuf& data) {
        TSocketOutput so(s);
        const TString& msg =
            TString("POST ") + request + " HTTP/1.1\r\n"
            "Host: " + host + "\r\n"
            "Content-Length: " + ToString(data.size()) + "\r\n"
            "\r\n" + data;

        so << msg;
        so.Flush();
    }
}

void TBackendProxy::QueryPrint(const TString& query) const {
    DEBUG_LOG << "QueryPrint: " << query << " START" << Endl;
    TString searchResult;
    if (!ProcessQuery("/?ms=proto&hr=da&text=" + query, &searchResult)) {
        DEBUG_LOG << "Can't process query " << query << Endl;
        return;
    }
    DEBUG_LOG << searchResult << Endl;
    DEBUG_LOG << "QueryPrint: " << query << " FINISHED" << Endl;
}

void TBackendProxy::ProcessReport(const TString& searchResult, const TString& query, TVector<TDocSearchInfo>& results, bool preffixedIndex, TQuerySearchContext& context) const {
    context.Errors.clear();
    context.EventLog.clear();
    NMetaProtocol::TReport report;
    if (context.HumanReadable) {
        report = NProtobufJson::Json2Proto<NMetaProtocol::TReport>(searchResult);
    } else {
        ::google::protobuf::io::CodedInputStream decoder((ui8*)searchResult.data(), searchResult.size());
        decoder.SetTotalBytesLimit(1 << 29);
        if (!report.ParseFromCodedStream(&decoder) || !decoder.ConsumedEntireMessage()) {
            ERROR_LOG << "proto not parsable: " << searchResult << Endl;
            ythrow yexception() << "Protobuf ParseFromArcadiaStream error ";
        }
    }
    if (report.GetErrorInfo().GetGotError() == NMetaProtocol::TErrorInfo::YES) {
        context.Errors.push_back(report.GetErrorInfo().GetText());
    }

    TString textReport;
    ::google::protobuf::TextFormat::PrintToString(report, &textReport);
    if (context.PrintResult) {
        TString infoLog;
        if (ProcessQuery("/broadcast/?info_server=yes", &infoLog)) {
            DEBUG_LOG << "---------------------------------------------INFO1=info_server" << query << Endl;
            DEBUG_LOG << infoLog << Endl;
            DEBUG_LOG << "---------------------------------------------" << Endl;
        }
        if (ProcessQuery("/?info_server=yes", &infoLog)) {
            DEBUG_LOG << "---------------------------------------------INFO2=info_server" << Endl;
            DEBUG_LOG << infoLog << Endl;
            DEBUG_LOG << "---------------------------------------------" << Endl;
        }
        DEBUG_LOG << "---------------------------------------------QUERY=" << query << Endl;
        DEBUG_LOG << textReport << Endl;
        DEBUG_LOG << "---------------------------------------------" << Endl;
    }

    if (context.CompressedReport) {
        if (!report.HasCompression()) {
            ythrow yexception() << "Data should be compressed";
        }
        NMetaProtocol::Decompress(report);
    }

    for (ui32 i = 0; i < report.EventLogSize(); ++i) {
        context.EventLog.push_back(report.GetEventLog(i));
    }

    if (context.SearchProperties) {
        context.SearchProperties->clear();
        for (size_t i = 0; i < report.SearcherPropSize(); ++i)
            context.SearchProperties->insert(std::make_pair(report.GetSearcherProp(i).GetKey(), report.GetSearcherProp(i).GetValue()));
        for (size_t i = 0; i < report.SearchPropertiesSize(); ++i) {
            for (ui32 prop = 0; prop < report.GetSearchProperties(i).PropertiesSize(); ++prop) {
                context.SearchProperties->insert(std::make_pair(report.GetSearchProperties(i).GetProperties(prop).GetKey(), report.GetSearchProperties(i).GetProperties(prop).GetValue()));
            }
        }
    }
    if (context.DocProperties)
        context.DocProperties->clear();
    if (report.GetGrouping().size() > 0) {
        const NMetaProtocol::TGrouping& grouping(report.GetGrouping(0));
        for (int i = 0; i < grouping.GetGroup().size(); i++) {
            const NMetaProtocol::TGroup& group(grouping.GetGroup(i));
            if (context.DocsPerCategory) {
                (*context.DocsPerCategory)[group.GetCategoryName()] = group.GetDocument().size();
            }
            for (int d = 0; d < group.GetDocument().size(); d++) {
                ui64 kps = 0;
                TSimpleSharedPtr<THashMultiMap<TString, TString> > props(context.DocProperties ? new THashMultiMap<TString, TString> : nullptr);
                for (unsigned a = 0; (context.DocProperties || preffixedIndex) && a < group.GetDocument(d).GetArchiveInfo().GtaRelatedAttributeSize(); ++a) {
                    const NMetaProtocol::TPairBytesBytes& attr = group.GetDocument(d).GetArchiveInfo().GetGtaRelatedAttribute(a);

                    if (!!props)
                        props->insert(std::make_pair(attr.GetKey(), attr.GetValue()));
                    if (attr.GetKey() == "prefix") {
                        kps = HexToui64(attr.GetValue().c_str());
                        if (!props)
                            break;
                    }
                }

                const TString& docidStr = group.GetDocument(d).GetDocId();

                if (!context.SourceSelector->ParseAndCheck(docidStr, query, RequiredMetaSearchLevel)) {
                    ythrow yexception() << "incorrect docid string " << docidStr << "/" << RequiredMetaSearchLevel;
                }

                ui32 docid = context.SourceSelector->GetParsedDocId();
                ui32 searcherid = context.SourceSelector->GetParsedSearcherId();

                results.push_back(TDocSearchInfo(
                    group.GetDocument(d).GetArchiveInfo().GetUrl(), docid, searcherid, kps, docidStr, group.GetCategoryName(), docidStr));
                results.back().SetRelevance(group.GetDocument(d).GetRelevance());

                if (!!props) {
                    for (unsigned a = 0; context.DocProperties && a < group.GetDocument(d).FirstStageAttributeSize(); ++a) {
                        const NMetaProtocol::TPairBytesBytes& attr = group.GetDocument(d).GetFirstStageAttribute(a);
                        props->insert(std::make_pair(attr.GetKey(), attr.GetValue()));
                    }

                    if (group.GetDocument(d).HasDocRankingFactors()) {
                        props->insert(std::make_pair("__docRankingFactors", group.GetDocument(d).GetDocRankingFactors()));
                    }

                    if (group.GetDocument(d).HasDocRankingFactorsSliceBorders()) {
                        props->insert(std::make_pair("__docRankingFactorsSliceBorders", group.GetDocument(d).GetDocRankingFactorsSliceBorders()));
                    }

                    TString psg = "";
                    for (size_t pass = 0; pass < group.GetDocument(d).GetArchiveInfo().PassageSize(); ++pass) {
                        const TString& snip = group.GetDocument(d).GetArchiveInfo().GetPassage(pass);
                        if (pass)
                            psg += " ";
                        psg += snip;
                    }
                    props->insert(std::make_pair("__passage", psg));

                    if (group.GetDocument(d).GetArchiveInfo().HasHeadline()) {
                        props->insert(std::make_pair("__headline", group.GetDocument(d).GetArchiveInfo().GetHeadline()));
                    }
                }
                if (context.DocProperties)
                    context.DocProperties->push_back(props);
            }
        }
    }
}


void TBackendProxy::CheckFormat(const TString& searchResult, TQuerySearchContext& context) const {
    if (context.ReportFormat == "json") {
        NJson::TJsonValue json;
        CHECK_WITH_LOG(NJson::ReadJsonFastTree(searchResult, &json, false));
        VERIFY_WITH_LOG(json.IsMap(), "No json object found");
    }
}

ui16 TBackendProxy::QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, bool preffixedIndex, TQuerySearchContext& context) const {
    DEBUG_LOG << "QuerySearch: " << query << Endl;
    VERIFY_WITH_LOG(!!context.SourceSelector.Get(), "Uninitialized sourceSelector");
    results.clear();
    TString codedQuery = query;

    TString searchResult;
    TString sourceS = "";

    if (context.SourceSelector->HasSpecialSource()) {
        sourceS = ToString(context.SourceSelector->GetSource());
    }

    TString finalQuery = "/" + sourceS + "?" + codedQuery;
    if (context.PrintResult) {
        finalQuery += "&dump=eventlog";
    }

    if (context.HumanReadable) {
        finalQuery += "&hr=json";
    }

    if (context.ReportFormat) {
        CHECK_WITH_LOG(context.ReportFormat == "json");
        finalQuery += "&format=" + context.ReportFormat;
    } else {
        finalQuery += "&ms=proto";
    }

    ui32 code = ProcessQuery(finalQuery, &searchResult, context.Host, context.Port, context.AppendService, context.HttpHeaders.empty() ? nullptr : &context.HttpHeaders);

    if (context.CheckReportInAnyCase && !searchResult) {
        ythrow yexception() << "Incorrect report";
    }

    if (code != 200) {
        ERROR_LOG << "Errors while process query " << query << ", code=" << code << Endl;
        if (context.PrintResult) {
            ERROR_LOG << "code == " << code << " for " << searchResult << Endl;
        }
        if (code == 0)
            return code;
    }

    if (!context.ReportFormat) {
        ProcessReport(searchResult, query, results, preffixedIndex, context);
    } else {
        if (context.PrintResult) {
            DEBUG_LOG << searchResult << Endl;
        }
        CheckFormat(searchResult, context);
    }
    DEBUG_LOG << "QuerySearch: " << query << "... OK (" << results.size() << ")" << Endl;
    return code;
}

ui16 TBackendProxy::QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, bool preffixedIndex, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >* docProperties, THashMultiMap<TString, TString>* searchProperties, bool printResult, const TString& host, ui16 port, ISearchSourceSelector::TPtr sourceSelector, const THashMap<TString, TString>* headers) const {
    TQuerySearchContext context;
    context.DocProperties = docProperties;
    context.SearchProperties = searchProperties;
    context.PrintResult = printResult;
    context.Host = host;
    context.Port = port;
    context.SourceSelector = sourceSelector;
    if (!!headers)
        context.HttpHeaders = *headers;
    return QuerySearch(query, results, preffixedIndex, context);
}

bool TBackendProxy::IsFinalIndex(const TString& dir) const {
    TJsonPtr response = ProcessCommand("is_final_index&directory=" + dir);
    const NJson::TJsonValue::TArray& respArr = response->GetArray();
    for (NJson::TJsonValue::TArray::const_iterator i = respArr.begin(); i != respArr.end(); ++i){
        if ((*i)["result"].GetBoolean())
            return true;
        }
    return false;
}

const TSet<TString> TBackendProxy::GetFinalIndexes(bool stopServer) const {
    TSet<TString> result;
    TJsonPtr indexes = ProcessCommand("get_final_indexes&full_path=true");
    if (stopServer)
        ProcessCommand("stop");

    NJson::TJsonValue::TArray dirs;
    dirs.clear();

    const NJson::TJsonValue::TArray& respArr = indexes->GetArray();
    for (NJson::TJsonValue::TArray::const_iterator i = respArr.begin(); i != respArr.end(); ++i){
        NJson::TJsonValue::TArray dirsOne = (*i)["dirs"].GetArray();
        dirs.insert(dirs.end(), dirsOne.begin(), dirsOne.end());
        }

    for (NJson::TJsonValue::TArray::const_iterator i = dirs.begin(), e = dirs.end(); i != e; ++i)
        result.insert(i->GetString());
    return result;
}

void TBackendProxy::WaitIsRepairing() const {
    bool isRep = true;
    while(isRep){
        Sleep(TDuration::MilliSeconds(200));
        TJsonPtr response = ProcessCommand("is_repairing");
        isRep = false;
        const NJson::TJsonValue::TArray& respArr = response->GetArray();
        for (NJson::TJsonValue::TArray::const_iterator i = respArr.begin(); i != respArr.end(); ++i){
            isRep = isRep | (*i)["result"].GetBoolean();
        }
    }
}

TJsonPtr TBackendProxy::ProcessCommand(const TString& command, const TBackendSet& backends, const TString& data, ui32 attemps) const {

    NJson::TJsonValue* result = new NJson::TJsonValue(NJson::JSON_ARRAY);
    const TBackendSet* be = backends.empty() ? &ActiveBackends : &backends;
    DEBUG_LOG << "Command " << command << " processing..." << Endl;
    for (TBackendSet::const_iterator i = be->begin(); i != be->end(); ++i) {
        DEBUG_LOG << "Command " << command << " processing for " << Config.Controllers[*i].GetString() << Endl;
        if (*i >= Config.Controllers.size())
            ythrow yexception() << "backendNumber is greater that the number of backends: " << *i << " >= " << Config.Controllers.ysize();
        TJsonPtr oneResult = ProcessCommandOneHost(command, Config.Controllers[*i].Host, Config.Controllers[*i].Port, data, attemps);
        result->AppendValue(*oneResult.Get());
        DEBUG_LOG << "Command " << command << " processing for " << Config.Controllers[*i].GetString() << "... OK" << Endl;
    }
    return result;
}

TJsonPtr TBackendProxy::ProcessCommandOneHost(const TString& command, const TString& chost, const ui16 cport, const TString& data, ui32 attemps) const {
    TString allData;
    ui32 i = 0;
    while (true) {
        try {
            DEBUG_LOG << "Processing " << command << " started for " << chost << ":" << cport << " attempt " << i << Endl;
            TNetworkAddress addr(chost, cport);
            TSocket s(addr);
            s.SetZeroLinger();
            if (!data)
                SendMinimalHttpRequest(s, chost + ":" + ToString(cport), "/?command=" + command);
            else
                SendPostHttpRequest(s, chost + ":" + ToString(cport), "/?command=" + command, data);

            TSocketInput si(s);
            THttpInput input(&si);
            allData = input.ReadAll();
            unsigned httpCode = ParseHttpRetCode(input.FirstLine());
            if (httpCode != 200) {
                ERROR_LOG << "Processing " << command << " failed for " << chost << ":" << cport << " attempt " << i << "/" << allData << Endl;
            } else {
                INFO_LOG << "Processing " << command << " success for " << chost << ":" << cport << " attempt " << i << "/" << allData << Endl;
            }
            break;
        } catch (...) {
            ERROR_LOG << "Can't send command " << command << " for " << chost << ":" << cport << " attempt " << i << "/" << CurrentExceptionMessage() << Endl;
            if (++i < attemps)
                Sleep(TDuration::Seconds(1));
            else
                break;
        }
    }
    DEBUG_LOG << "Processing " << command << " finished for " << chost << ":" << cport << Endl;
    NJson::TJsonValue* result = new NJson::TJsonValue();
    TStringInput stri(allData);
    try {
        if (!NJson::ReadJsonTree(&stri, result, true))
            ythrow yexception() << "Incorrect output format: " << allData;
    } catch (...) {
        DEBUG_LOG << "Incoming JSON: " << allData << Endl;
        throw;
    }
    return result;
}

TJsonPtr TBackendProxy::GetServerInfo() const {
    TJsonPtr reply = ProcessCommand("get_info_server", ActiveBackends);
    NJson::TJsonValue* result = new NJson::TJsonValue(NJson::JSON_ARRAY);
    const NJson::TJsonValue::TArray& respArr = reply->GetArray();
    for (NJson::TJsonValue::TArray::const_iterator i = respArr.begin(); i != respArr.end(); ++i){
        result->AppendValue((*i)["result"]);
    }
    return result;
}

TJsonPtr TBackendProxy::GetDocInfo(const TString& fullDocId) const {
    TString fixedDocId = fullDocId;

    auto splitted = SplitString(fullDocId, "-");
    if (splitted.size() > 2) {
        const size_t size = splitted.size();
        fixedDocId = splitted[size - 2] + "-" + splitted[size - 1];
    }
    TJsonPtr reply = ProcessCommand("get_doc_info&docid=" + fixedDocId);
    DEBUG_LOG << "doc_info docid=" << fixedDocId << " multibackend response: " << Endl;
    DEBUG_LOG << NUtil::JsonToString(*reply) << Endl;
    NJson::TJsonValue* result = new NJson::TJsonValue();
    const NJson::TJsonValue::TArray& respArr = reply->GetArray();
    int count = 0;
    for (NJson::TJsonValue::TArray::const_iterator i = respArr.begin(); i != respArr.end(); ++i){
        if (count++ == 0) {
            *result = *i;
        } else {
            if ((*result)["info"] != (*i)["info"]) {
                WARNING_LOG << "doc_info from backend 0 and " << count << " differs" << Endl;
            }
        }
    }
    return result;
}

TJsonPtr TBackendProxy::GetDocInfo(ui32 searcherId, ui32 docId) const {
    return GetDocInfo(ToString(searcherId) + "-" + ToString(docId));
}

//returns /status?brief= if it is the same on all controllers, empty string otherwise
TString TBackendProxy::GetServerBrief() const {
    const TString query = "/status?brief=";

    TSet<TString> briefs;
    const TBackendSet* be = &ActiveBackends;

    DEBUG_LOG << "Query " << query << " processing" << Endl;
    for (TBackendSet::const_iterator i = be->begin(); i != be->end(); ++i) {
        if (*i >= Config.Controllers.size())
            ythrow yexception() << "backendNumber is greater that the number of backends: " << *i << " >= " << Config.Controllers.ysize();

        TString brief;
        ui32 httpCode = ProcessQuery(query, &brief, Config.Controllers[*i].Host, Config.Controllers[*i].Port, false, nullptr);
        if (httpCode != 200)
            return TString();

        briefs.insert(brief);
    }

    if (briefs.size() != 1)
        return TString();

    TString result = *briefs.begin();
    size_t idx = result.find_first_of(" \r\n");
    if (idx != TString::npos)
        result.resize(idx);
    return result;
}

NJson::TJsonValue SameResult(TJsonPtr response){
    const NJson::TJsonValue::TArray& respArr = response->GetArray();
    if (respArr.empty())
        return *response;
    TString status = respArr[0]["task_status"].GetStringRobust();
    NJson::TJsonValue result = respArr[0]["result"];
    for (NJson::TJsonValue::TArray::const_iterator i = respArr.begin(); i != respArr.end(); ++i){

        if (!((*i)["result"] == result)){
            ythrow yexception() << "Different results: " << (*i)["result"] << "and" << result;
        }
        if (!((*i)["task_status"].GetStringRobust() == status)){
            ythrow yexception() << "Different statuses: " << (*i)["task_status"].GetStringRobust() << " != " << status << "and" << result;
        }
    }
    return result;
}

TBackendProxy::TCachePolicy TBackendProxy::GetCachePolicy() {
    TJsonPtr response = ProcessCommand("get_cache_policy");
    TString res = SameResult(response).GetString();

    if (res == "NOCACHE")
        return NO_CACHE;
    if (res == "CACHE_WITH_LIVE_TIME")
        return LIFE_TIME;
    if (res == "CACHE_WITH_LIVE_TIME_MODIFICABLE")
        return LIFE_TIME_MODIFIED;
    ythrow yexception() << "unknown cache policy";
}

const TBackendProxyConfig& TBackendProxy::GetConfig() {
    return Config;
}

TConfigFieldsPtr TBackendProxy::ApplyConfigDiff(TConfigFieldsPtr diff, const TBackendSet& backends, const TCiString& prefix) const {
    TConfigFieldsPtr responseFields;
    if (Config.ControllingDeployManager) {
        responseFields = PatchConfigDiff(diff);
    } else {
        responseFields.Reset(new TConfigFields);
        TJsonPtr response = ProcessCommand("set_config&fields=" + CGIEscapeRet(diff->Serialize()) + "&prefix=" + prefix, backends);
        TStringStream stringStream;
        NJson::WriteJson(&stringStream, response.Get(), true);
        DEBUG_LOG << stringStream.Str() << Endl;
        responseFields->Deserialize(SameResult(response));
        NJson::TJsonValue::TArray arr;
        if (!response->GetArray(&arr))
            ythrow yexception() << "incorrect reply format";
        for (auto&& i : arr) {
            if (i["task_status"].GetStringRobust() != "FINISHED")
                ythrow yexception() << "incorrect set_config execution";
        }
    }
    return responseFields;
}

NJson::TJsonValue TBackendProxy::GetInfoServerProxy(const TCiString& proxyKind) const {
    TConfigFieldsPtr responseFields;
    TBackendProxyConfig::TAddress proxyAddr;
    if (proxyKind == "search") {
        proxyAddr = Config.Searcher;
    } else if (proxyKind == "indexer") {
        proxyAddr = Config.Indexer;
    } else {
        ythrow yexception() << "unknown proxy kind '" << proxyKind << "', use 'search' or 'indexer'" << Endl;
    }
    TJsonPtr response = ProcessCommandOneHost("get_info_server", proxyAddr.Host, proxyAddr.Port + 3);
    return *response;
}

TConfigFieldsPtr TBackendProxy::ApplyProxyConfigDiff(TConfigFieldsPtr diff, const TCiString& proxyKind, const TCiString& prefix) const {
    TConfigFieldsPtr responseFields;
    if (Config.ControllingDeployManager) {
        ythrow yexception() << "not implemented for dm tests" << Endl;
    } else {
        responseFields.Reset(new TConfigFields);
        TBackendProxyConfig::TAddress proxyAddr;
        if (proxyKind == "search"){
            proxyAddr = Config.Searcher;
        } else if (proxyKind == "indexer") {
            proxyAddr = Config.Indexer;
        } else if (proxyKind == "common") {
            proxyAddr = Config.CommonProxy;
        } else {
            ythrow yexception() << "unknown proxy kind '" << proxyKind << "', use 'search' or 'indexer' or 'common'" << Endl;
        }
        TJsonPtr response = ProcessCommandOneHost("set_config&fields=" + CGIEscapeRet(diff->Serialize()) + "&prefix=" + prefix, proxyAddr.Host, proxyAddr.Port + 3);
        TStringStream stringStream;
        NJson::WriteJson(&stringStream, response.Get(), true);
        DEBUG_LOG << stringStream.Str() << Endl;
        responseFields->Deserialize(*response);
    }
    return responseFields;
}

TConfigFieldsPtr TBackendProxy::GetConfigValues(TConfigFieldsPtr diff, const TCiString& prefix, const TBackendSet& backends, const TRtyTestNodeType binary) const {
    TStringStream requestFields;
    NJson::TJsonWriter writer(&requestFields, false);
    writer.OpenArray();
    for (TConfigFields::const_iterator i = diff->begin(), e = diff->end(); i != e; ++i) {
        writer.Write(i->first);
    }
    writer.CloseArray();
    writer.Flush();
    TJsonPtr response;
    NJson::TJsonValue fieldsResult;
    if (binary == TNODE_RTYSERVER) {
        response = ProcessCommand("get_config&fields=" + requestFields.Str() + "&prefix=" + prefix, backends);
        fieldsResult = SameResult(response);
    }
    else {
        TBackendProxyConfig::TAddress proxyAddr;
        if (binary == TNODE_SEARCHPROXY)
            proxyAddr = Config.Searcher;
        else if (binary == TNODE_INDEXERPROXY)
            proxyAddr = Config.Indexer;
        else
            ythrow yexception() << "not implemented for this binary type: " << int(binary) << Endl;
        response = ProcessCommandOneHost("get_config&fields=" + requestFields.Str() + "&prefix=" + prefix, proxyAddr.Host, proxyAddr.Port + 3);
        fieldsResult = (*response)["result"];
    }

    TConfigFieldsPtr responseFields(new TConfigFields);
    const NJson::TJsonValue::TMapType& respFieldsMap = fieldsResult.GetMap();
    for (NJson::TJsonValue::TMapType::const_iterator i = respFieldsMap.begin(), e = respFieldsMap.end(); i != e; ++i) {
        (*responseFields)[i->first] = i->second.GetString();
    }
    return responseFields;
}

TString TBackendProxy::GetConfigValue(const TCiString& key, const TCiString& prefix, const TBackendSet& backends, const TRtyTestNodeType binary) const {
    TConfigFieldsPtr request(new TConfigFields);
    (*request)[key] = TConfigField();
    TConfigFieldsPtr responce = GetConfigValues(request, prefix, backends, binary);
    TConfigFields::const_iterator i = responce->find(key);
    return i == responce->end() ? TString() : i->second.Value;
}

bool TBackendProxy::IsDistributorExhausted() const {
    TSimpleSharedPtr<NJson::TJsonValue> result(ProcessCommand("get_docfetcher_status"));
    DEBUG_LOG << "get_docfetcher_status RESULT: " << NUtil::JsonToString(*result) << Endl;
    const NJson::TJsonValue::TArray& statusArray = result->GetArray();
    bool allExhausted = true;
    for (NJson::TJsonValue::TArray::const_iterator i = statusArray.begin(); i != statusArray.end(); ++i) {
        bool hasDocsInDistributor = (*i)["has_docs_in_distributor"].GetBoolean();
        ui64 queueSize = (*i)["queue_size"].GetUInteger();
        ui64 docsIndexing = (*i)["docs_indexing"].GetUInteger();
        allExhausted &= (!hasDocsInDistributor && queueSize + docsIndexing == 0);
    }
    return allExhausted;
}

NJson::TJsonValue TBackendProxy::GetDistributorReplies() const {
    TSimpleSharedPtr<NJson::TJsonValue> result(ProcessCommand("get_docfetcher_status"));
    const NJson::TJsonValue::TArray& statusArray = result->GetArray();
    for (NJson::TJsonValue::TArray::const_iterator i = statusArray.begin(); i != statusArray.end(); ++i) {
        return (*i)["replies"];
    }
    return NJson::TJsonValue();
}

ui64 TBackendProxy::GetMetric(const TString& name, const TBackendSet& backends) const {
    TJsonPtr result = ProcessCommand("get_metric&name=" + name + "&update=1", backends);
    for (auto&& e : result->GetArray()) {
        if (e.Has("value")) {
            return e["value"].GetUInteger();
        }
    }
    return 0;
}

void TBackendProxy::WaitDistributorExhausted() const {
    DEBUG_LOG << "Tester: waiting for distributor exhaustion" << Endl;
    Sleep(TDuration::Seconds(10)); // wait before newly sent docs become available in distributor
    while (!IsDistributorExhausted())
        Sleep(TDuration::Seconds(1));
    DEBUG_LOG << "Tester: distributor is exhausted, continuing" << Endl;
}

bool TBackendProxy::SynchronizeInternal(const TString& idRes, const NRTYServer::EConsumeMode* consumeMode, TString& result) {
    CHECK_WITH_LOG(Config.Controllers.size() == 1);
    for (ui32 i = 0; i < Config.Controllers.size(); ++i) {
        NDaemonController::TDownloadAction da = (
            consumeMode == nullptr ?
            NDaemonController::TDownloadAction(idRes, NDaemonController::apStartAndWait) :
            NDaemonController::TDownloadAction(idRes, NDaemonController::apStartAndWait, *consumeMode)
        );
        NDaemonController::TControllerAgent(Config.Controllers[i].Host, Config.Controllers[i].Port).ExecuteAction(da);
        if (da.IsFailed())
            return false;
        result = da.GetInfo();
    }
    return true;
}

bool TBackendProxy::Synchronize(const TString& idRes, TString& reply) {
    return SynchronizeInternal(idRes, nullptr, reply);
}

bool TBackendProxy::Synchronize(const TString& idRes, NRTYServer::EConsumeMode consumeMode, TString& reply) {
    return SynchronizeInternal(idRes, &consumeMode, reply);
}

bool TBackendProxy::Detach(NSearchMapParser::TShardIndex shardMin, NSearchMapParser::TShardIndex shardMax, const NSaas::TShardsDispatcher::TContext& sharding, TString& result) {
    CHECK_WITH_LOG(Config.Controllers.size() == 1);
    for (ui32 i = 0; i < Config.Controllers.size(); ++i) {
        NDaemonController::TDetachAction da(shardMin, shardMax, sharding, NDaemonController::apStartAndWait);
        NDaemonController::TControllerAgent(Config.Controllers[i].Host, Config.Controllers[i].Port).ExecuteAction(da);
        if (da.IsFailed())
            return false;
        NJson::TJsonValue value;
        const TString& daInfo = da.GetInfo();
        TStringInput si(daInfo);
        if (!NJson::ReadJsonTree(&si, &value))
            return false;
        result = value["id_res"].GetStringRobust();
    }
    return true;
}

bool TBackendProxy::Detach(TVector<NSearchMapParser::TShardIndex> shardMin, TVector<NSearchMapParser::TShardIndex> shardMax, const NSaas::TShardsDispatcher::TContext& sharding, TVector<TString>& result) {
    result.clear();
    CHECK_WITH_LOG(shardMin.size() == shardMax.size());
    CHECK_WITH_LOG(Config.Controllers.size() == 1);
    for (ui32 i = 0; i < Config.Controllers.size(); ++i) {
        NDaemonController::TDetachAction da(shardMin, shardMax, sharding, NDaemonController::apStartAndWait);
        NDaemonController::TControllerAgent(Config.Controllers[i].Host, Config.Controllers[i].Port).ExecuteAction(da);
        if (da.IsFailed())
            return false;
        NJson::TJsonValue value;
        result = da.GetResults();
    }
    return true;
}

bool TBackendProxy::ShardsAction(NSearchMapParser::TShardIndex shardMin, NSearchMapParser::TShardIndex shardMax, const NSaas::TShardsDispatcher::TContext& sharding, TString& result, const TString& action) {
    CHECK_WITH_LOG(Config.Controllers.size() == 1);
    for (ui32 i = 0; i < Config.Controllers.size(); ++i) {
        NDaemonController::TShardsAction::ActionType actType = NDaemonController::TShardsAction::satRemove;
        if (action == "check")
            actType = NDaemonController::TShardsAction::satCheck;
        NDaemonController::TShardsAction sa(shardMin, shardMax, sharding, actType, NDaemonController::apStartAndWait);
        NDaemonController::TControllerAgent(Config.Controllers[i].Host, Config.Controllers[i].Port).ExecuteAction(sa);
        if (sa.IsFailed())
            return false;
        result = sa.GetInfo();
    }
    return true;
}

TString TBackendProxy::SendCommandToDeployManager(const TString& command, const ui16 shiftPort, const TString& data) const {
    if (!Config.DeployManager.Port)
        ythrow yexception() << "there is no deploy_mananger";
    DEBUG_LOG << "try to SendCommandToDeployManager: " << Config.DeployManager.Host << ":" << Config.DeployManager.Port + shiftPort << "/" << command << Endl;
    TNetworkAddress addr(Config.DeployManager.Host, Config.DeployManager.Port + shiftPort);
    TSocket s(addr);
    s.SetZeroLinger();
    if (!data) {
        SendMinimalHttpRequest(s, Config.DeployManager.Host + ":" + ToString(Config.DeployManager.Port + shiftPort), (shiftPort ? "" : Config.DeployManager.UriPrefix) + "/" + command);
    } else {
        SendPostHttpRequest(s, Config.DeployManager.Host + ":" + ToString(Config.DeployManager.Port + shiftPort), (shiftPort ? "" : Config.DeployManager.UriPrefix) + "/" + command, data);
    }

    TSocketInput si(s);
    THttpInput input(&si);
    ui32 http_code = ParseHttpRetCode(input.FirstLine());
    if (http_code != 200)
        ythrow yexception() << "error while process command to deploy manager: " << command << ", http_code:" << http_code << ", error: " << input.ReadAll();
    return input.ReadAll();

}

void TBackendProxy::UploadDataToDeployManager(const TString& data, const TString& path) const {
    TString writedFile;
    NDaemonController::TSetConfAction action(path, data);
    ExecuteActionOnDeployManager(action);
    do {
        try {
            writedFile = SendCommandToDeployManager("process_storage?path=" + path + "&download=yes&action=get");
            if (writedFile != data) {
                WARNING_LOG << "Additional attempt for read deployed file " << path << Endl;
            } else {
                if (data.length() < 30000)
                    INFO_LOG << "Saved data for " << path << " : " << data << Endl;
                else
                    INFO_LOG << "Saved data for " << path << " : <" << data.length() << " length>" << Endl;
            }
        } catch (...) {
            continue;
        }
    } while (writedFile != data);
}

void TBackendProxy::UploadFileToDeployManager(const TString& filename, const TString& path) const {
    TBlob data(TBlob::FromFile(filename));
    UploadDataToDeployManager(TString(data.AsCharPtr(), data.Size()), path);
}

TString TBackendProxy::Deploy(const TString& command) const {
    const TString& result = SendCommandToDeployManager(command);
    TString taskId = TStringInput(result).ReadLine();
    DEBUG_LOG << "Deploy started, taskId: " << taskId << Endl;
    return taskId;
}

void TBackendProxy::WaitDeploy(const TString& taskId) const {
    for (int i = 0; i < 150; ++i) {
        TString repl = SendCommandToDeployManager("deploy_info?id=" + taskId);
        TStringInput si(repl);
        NJson::TJsonValue json;
        if (!NJson::ReadJsonTree(&si, &json))
            ythrow yexception() << "invalid reply from deploy_info: " << repl;
        TString code = json["result_code"].GetString();
        if (code == "FINISHED")
            return;
        if (code == "ENQUEUED" || code == "IN_PROGRESS" || code == "SAVED") {
            DEBUG_LOG << code << Endl;
            Sleep(TDuration::Seconds(1));
            continue;
        }
        ythrow yexception() << "task failed: " << code << ", " << taskId;
    }
    ythrow yexception() << "task failed: TIMEOUT, " << taskId;
}

TConfigFieldsPtr TBackendProxy::PatchConfigDiff(TConfigFieldsPtr diff, const TString& /*configType*/, const TString& service) const {
    TConfigFields newDiff;
    bool loaded = false;
    TString suffix;
    if (!loaded)
        try {
            newDiff.Deserialize(SendCommandToDeployManager("get_conf?service=" + service + "&filename=rtyserver.diff-" + service +"&orig=da"));
        } catch (const yexception& e) {
            DEBUG_LOG << "Cannot download old diff rtyserver.diff-" <<  service << ": " << e.what() << Endl;
            newDiff.clear();
        }
    newDiff.Patch(*diff);
    TString newDiffStr = newDiff.Serialize();
    UploadDataToDeployManager(newDiffStr, "/configs/" + service + "/rtyserver.diff-"  + service + suffix);
    return diff;
}

void TBackendProxy::PatchConfigDiff(const TString& filename, const TString& configType, const TString& service) const {
    TConfigFieldsPtr diff(new TConfigFields);
    diff->Deserialize(TUnbufferedFileInput(filename).ReadAll());
    PatchConfigDiff(diff, configType, service);
}

void TBackendProxy::ExecuteActionOnDeployManager(NDaemonController::TAction& action) const {
    if (!Config.DeployManager.Port)
        ythrow yexception() << "there is no deploy_mananger";
    NDaemonController::TControllerAgent agent(Config.DeployManager.Host, Config.DeployManager.Port, nullptr, Config.DeployManager.UriPrefix + "/");
    if (!agent.ExecuteAction(action))
        ythrow yexception() << "error while execute task " << action.ActionId();
    if (action.IsFinished() && action.IsFailed())
        ythrow yexception() << "task " << action.ActionId() << " failed: " << action.GetInfo();
}
