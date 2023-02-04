#include "sandbox.h"
#include "utils.h"
#include <saas/util/logging/exception_process.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/charset/ci_string.h>
#include <util/network/socket.h>
#include <library/cpp/http/io/stream.h>
#include <util/string/cast.h>

using namespace NJson;
using namespace NXml;

namespace {
    void SendPostRequest(TSocket& s, const TStringBuf& host, const TStringBuf& request, const TStringBuf& data) {
        TSocketOutput so(s);
        THttpOutput output(&so);

        output.EnableKeepAlive(false);
        output.EnableCompression(false);
        TString cl("Content-length: " + ToString(data.size()));
        const IOutputStream::TPart parts[] = {
            IOutputStream::TPart(TStringBuf("POST ")),
            IOutputStream::TPart(request),
            IOutputStream::TPart(TStringBuf(" HTTP/1.1")),
            IOutputStream::TPart::CrLf(),
            IOutputStream::TPart(TStringBuf("Host: ")),
            IOutputStream::TPart(host),
            IOutputStream::TPart::CrLf(),
            IOutputStream::TPart(TStringBuf("Content-Type: text/xml")),
            IOutputStream::TPart::CrLf(),
            IOutputStream::TPart(cl),
            IOutputStream::TPart::CrLf(),
            IOutputStream::TPart::CrLf(),
            IOutputStream::TPart(data)
        };

        output.Write(parts, sizeof(parts) / sizeof(*parts));
        output.Finish();
    }

    bool ParseStruct(TJsonValue& result, xmlNode* node);
    bool ParseArray(TJsonValue& result, xmlNode* node);
    bool ParseValue(TJsonValue& result, xmlNode* node) {
        TRY
            xmlNode* valueTypeNode = node->children;
            while(valueTypeNode && (TCiString("text") == (char*)valueTypeNode->name)) valueTypeNode = valueTypeNode->next;
            if (!valueTypeNode)
                ythrow yexception() << "incorrect value node";
            const TCiString type((const char*)valueTypeNode->name);
            if (type == "int")
                result.SetValue(FromString<i64>((char*)GetChild("text", valueTypeNode)->content));
            else if (type == "string") {
                xmlNode* xmlText = GetChild("text", valueTypeNode);
                result.SetValue(xmlText ? (char*)xmlText->content : "");
            } else if (type == "boolean") {
                result.SetValue(FromString<bool>((char*)GetChild("text", valueTypeNode)->content));
            } else if (type == "struct") {
                return ParseStruct(result, valueTypeNode);
            } else if (type == "array") {
                return ParseArray(result, valueTypeNode);
            } else
                return false;
            return true;
        CATCH("While ParseValue");
        return false;
    }

    bool ParseMember(TJsonValue& result, xmlNode* node) {
        TRY
            if (TCiString("member") != (char*)node->name)
                return false;
            xmlNode* nameNode = GetChild("name", node);
            xmlNode* valueNode = GetChild("value", node);
            if (!nameNode || !valueNode)
                ythrow yexception() << "incorrect member node";
            xmlNode* xmlNameText = GetChild("text", nameNode);
            if (!xmlNameText)
                ythrow yexception() << "Incorrect name node";
            const char* name = (char*)xmlNameText->content;
            TJsonValue jValue;
            bool res = ParseValue(jValue, valueNode);
            if (res)
                result.InsertValue(name, jValue);
            return res;
        CATCH("While ParseMember");
        return false;
    }

    bool ParseStruct(TJsonValue& result, xmlNode* node) {
        bool res = false;
        result.SetType(JSON_MAP);
        for (xmlNode* xmlMember = node->children; xmlMember; xmlMember = xmlMember->next)
            if (TCiString("member") == (char*)xmlMember->name)
                res |= ParseMember(result, xmlMember);
        return res;
    }

    bool ParseArray(TJsonValue& result, xmlNode* node) {
        bool res = false;
        result.SetType(JSON_ARRAY);
        TJsonValue value;
        xmlNode* xmlData = GetChild("data", node);
        if (xmlData)
            for (xmlNode* xmlValue = xmlData->children; xmlValue; xmlValue = xmlValue->next)
                if (TCiString((char*)xmlValue->name) == "value" && ParseValue(value, xmlValue)) {
                    result.AppendValue(value);
                    res = true;
                };
        return res;
    }

    bool GetJsonFromXmlRpc(const TString& host,
        ui16 port,
        const TStringBuf& request,
        const TStringBuf& data,
        TJsonValue& result)
    {
        for (int i = 0; i < 5; ++i) {
            TRY
                TSocket s(TNetworkAddress(host, port), TDuration::Seconds(120));
                s.SetSocketTimeout(120);
                SendPostRequest(s, host + ":" + ToString(port), request, data);
                TSocketInput si(s);
                THttpInput input(&si);
                unsigned httpCode = ParseHttpRetCode(input.FirstLine());
                if (httpCode != 200)
                    ythrow yexception() << "Http code " << httpCode;
                TString xml = input.ReadAll();
                TXmlDoc doc(xml.data(), xml.size());
                xmlNode* xmlValue = GetChild("params.param.value", doc.GetRoot());
                if (!xmlValue)
                    ythrow yexception() << "Incorrect responce";
                ParseValue(result, xmlValue);
                return true;
             CATCH("While GetJsonFromXmlRpc try: " + ToString(i));
        };
        return false;
    }

    class TTaskRestarter {
        typedef THashMap<ui64, ui32> THistory;
        THistory History;
        TMutex Mutex;
        static const ui32 MaxCountRestart = 1;
    public:
        bool RestartTask(const TString& host, ui16 port, ui64 taskId) {
            TGuard<TMutex> g(Mutex);
            ui32& countRestart = History.insert(std::make_pair(taskId, 0)).first->second;
            if (countRestart >= MaxCountRestart)
                return false;
            NJson::TJsonValue result;
            const TString request = "<?xml version=\"1.0\"?><methodCall><methodName>restartTask</methodName><params><param><value><int>"
                + ToString(taskId)
                + "</int></value></param></params></methodCall>";
            if (!GetJsonFromXmlRpc(host, port, "/sandbox/xmlrpc", request, result) || !result.IsBoolean())
                return false;
            bool restarted = result.GetBoolean();
            if (restarted)
                ++countRestart;
            return restarted;
        }
        void TaskSuccess(ui64 taskId) {
            TGuard<TMutex> g(Mutex);
            History.erase(taskId);
        }
    };
}

namespace NSandbox {
    bool GetTask(const TString& host, ui16 port, ui64 taskId, NJson::TJsonValue& result) {
        if (taskId == 0){
            DEBUG_LOG << "found taskId == 0" << Endl;
            return false;
        }
        const TString request = "<?xml version=\"1.0\"?><methodCall><methodName>getTask</methodName><params><param><value><int>"
                             + ToString(taskId)
                             + "</int></value></param></params></methodCall>";
        if (!GetJsonFromXmlRpc(host, port, "/sandbox/xmlrpc", request, result)){
            DEBUG_LOG << "Error in GetJsonFromXmlRpc" << Endl;
            return false;
            }
        if (result.Has("status") && result["status"] == "UNKNOWN"){
            DEBUG_LOG << "restarting task " << taskId << Endl;
            return Singleton<TTaskRestarter>()->RestartTask(host, port, taskId);
            }
        return true;
    }
}
