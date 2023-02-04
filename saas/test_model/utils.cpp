#include "utils.h"
#include <saas/util/logging/exception_process.h>
#include <saas/util/network/http_request.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/http/io/stream.h>
#include <library/cpp/charset/ci_string.h>
#include <util/network/socket.h>

bool GetJsonFromHttp(const TString& host, ui16 port, const TStringBuf& query, NJson::TJsonValue& result, const TDuration timeout, bool isHttps)
{
    for (int i = 0; i < 3; ++i) {
        TRY
            NUtil::THttpRequest request((TString(query)));
            request.SetTimeout(timeout.MilliSeconds()).SetIsHttps(isHttps);
            NUtil::THttpReply res = request.Execute(host, port);
            if (!res.IsSuccessReply())
                ythrow yexception() << res.Code() << " " << res.Content();
            TStringInput si(res.Content());
            if (NJson::ReadJsonTree(&si, &result))
                return true;

        CATCH("While GetJsonFromHttp try: " + ToString(i) + " " + query)
        Sleep(TDuration::Seconds(30));
    }
    return false;
}

bool GetTextFromHttp(const TString& host, ui16 port, const TStringBuf& query, TString& result, const TDuration timeout, bool isHttps)
{
    for (int i = 0; i < 3; ++i) {
        TRY
            NUtil::THttpRequest request((TString(query)));
            request.SetTimeout(timeout.MilliSeconds()).SetIsHttps(isHttps);
            NUtil::THttpReply res = request.Execute(host, port);
            if (!res.IsSuccessReply())
                ythrow yexception() << res.Code() << " " << res.Content();
            result = res.Content();
            if (!!result)
                return true;
            else
                DEBUG_LOG << "string for query " << query << " is empty" << Endl;
        CATCH("While GetTextFromHttp try: " + ToString(i) + " " + query)
        Sleep(TDuration::Seconds(10));
    }
    return false;
}

xmlNode* GetChild(const TCiString& path, xmlNode* parent, size_t beg) {
    if (!parent)
        return nullptr;
    size_t pos = path.find('.', beg);
    TCiString name = path.substr(beg, pos == TCiString::npos ? TCiString::npos : (pos - beg));
    for (xmlNode* result = parent->children; result; result = result->next)
        if (name == (char*)result->name)
            return pos == TCiString::npos ? result : GetChild(path, result, pos + 1);
    return nullptr;
}

void WriteRevisionInfo(const TRevisionInfo& revision, NJson::TJsonValue& element) {
    element.InsertValue("revision", revision.Revision);
    element.InsertValue("timestamp", revision.Timestamp);
    element.InsertValue("comment", revision.Comment);
    element.InsertValue("author", revision.Author);
    element.InsertValue("time", Strftime("%d %b %H:%M", localtime(&revision.Timestamp)));
}

void WriteRevisionInfo(const char* key, const TTestExecution& revision, NJson::TJsonValue& result, bool writeTaskInfo) {
    NJson::TJsonValue& element = result.InsertValue(key, NJson::JSON_MAP);
    WriteRevisionInfo(revision.GetData().Revision, element);
    if (writeTaskInfo)
        element.InsertValue("task_info", revision.GetData().TaskInfo);
}
