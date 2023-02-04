#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <kernel/server/protos/serverconf.pb.h>
#include <kernel/server/server.h>
#include <util/stream/file.h>
#include <util/string/builder.h>
#include <util/system/info.h>
#include <util/system/mutex.h>
#include <google/protobuf/text_format.h>
#include <library/cpp/protobuf/util/pb_io.h>
#include <util/generic/ptr.h>
#include <util/network/address.h>

using TMockedResponse =  TAtomicSharedPtr<NJson::TJsonValue>;
using TReceivedRequests = THashMap<TString, NJson::TJsonValue>;


class TMockServerData {
public:
    TMockServerData(TString& mockedResponseFilePath, TString& backendIpsFilePath, bool saveRequests):
        MockedResponseFilePath(mockedResponseFilePath),
        BackendIpsFilePath(backendIpsFilePath),
        SaveRequests(saveRequests)
    {
        MockedResponse = MakeShared<NJson::TJsonValue, TAtomicCounter>(NJson::TJsonValue(NJson::EJsonValueType::JSON_MAP));
        NotFound["status"] = 404;
        if (!backendIpsFilePath.empty()) {
            TFileInput backendIpsFile(BackendIpsFilePath);
            Y_ENSURE(NJson::ReadJsonTree(&backendIpsFile, &Backend), "Cannot read backends data");
            Y_ENSURE(Backend.IsMap());
        }
    }

    const TString& ResolveBackend(TString& serverAddress) {
        if (!BackendIpsFilePath.empty())
            if (Backend.Has(serverAddress))
                return Backend[serverAddress].GetString();
        return serverAddress;
    }

    TString GetRequests(TString& serverAddress) {
        TGuard<TMutex> guard(Mutex);
        auto requests = ReceivedRequests[ResolveBackend(serverAddress)];
        if (!requests.IsArray())
            requests.SetType(NJson::EJsonValueType::JSON_ARRAY);
        return NJson::WriteJson(requests);
    }

    void AddRequest(TString& serverAddress, NJson::TJsonValue& request) {
        if (!BackendIpsFilePath.empty()) {
        }
        TGuard<TMutex> guard(Mutex);
        auto& requests = ReceivedRequests[ResolveBackend(serverAddress)];
        if (!requests.IsArray())
            requests.SetType(NJson::EJsonValueType::JSON_ARRAY);
        requests.AppendValue(request);
    }

    bool ReloadResponses() {
        TFileInput mockedResponseFile(MockedResponseFilePath);
        auto val = MakeShared<NJson::TJsonValue, TAtomicCounter>(NJson::TJsonValue());
        if (!NJson::ReadJsonTree(&mockedResponseFile, val.Get())) {
            return false;
        }
        MockedResponse = val;
        return true;
    }

public:

    TString MockedResponseFilePath;
    TReceivedRequests ReceivedRequests;
    TMockedResponse MockedResponse;
    NJson::TJsonValue NotFound;

    TString BackendIpsFilePath;
    NJson::TJsonValue Backend;

    TMutex Mutex;
    const bool SaveRequests;
};


class TMockRequest final : public NServer::TRequest {
public:
    explicit TMockRequest(NServer::TServer& server, TMockServerData &data)
        : NServer::TRequest{server}
        , Data(data)
    {
    }

    bool HandleAdmin(const TString& action, THttpResponse& response) override {
        TString serverAddress = NAddr::PrintHost(*RD.ServerAddress());
        if (action == "reload_static_responses") {
            int code = Data.ReloadResponses() ? 200 : 505;
            response.SetHttpCode((HttpCodes) code);
            return true;
        } else if (action == "get_all_requests") {
            response = TextResponse(Data.GetRequests(serverAddress));
            return true;
        }
        return false;
    }

    bool DoReply(const TString& script, THttpResponse& response) override {
        Y_UNUSED(script);

        TString serverAddress = NAddr::PrintHost(*RD.ServerAddress());

        TString reqid = "";

        if (Data.SaveRequests) {
            NJson::TJsonValue request(NJson::EJsonValueType::JSON_MAP);
            request["request_line"] = RequestString;
            request["headers"] = NJson::TJsonValue(NJson::EJsonValueType::JSON_MAP);
            for (const auto& header: ParsedHeaders) {
                request["headers"][header.first] = header.second;
                TString headerKey = to_lower(header.first);
                if (headerKey == "x-req-id") reqid = header.second;
            }
            request["data"] = TString(Buf.AsCharPtr(), Buf.Size());
            Data.AddRequest(serverAddress, request);
        } else {
            for (const auto& header: ParsedHeaders) {
                TString headerKey = to_lower(header.first);
                if (headerKey == "x-req-id") reqid = header.second;
            }
        }

        auto respPtrCopy = Data.MockedResponse;

        bool redirected = false;
        if ((*respPtrCopy).Has("internal_redirect")) {  // saves requests to redirected destination
            const NJson::TJsonValue& redirect = (*respPtrCopy)["internal_redirect"];\
            while (redirect.Has(serverAddress)) {
                redirected = true;
                serverAddress = redirect[serverAddress].GetString();
            }
        }

        TString serverAndReqid = serverAddress + ":" + reqid;

        const NJson::TJsonValue& mockedResponseReqid = (reqid.size() && (*respPtrCopy).Has(serverAndReqid)) ? (*respPtrCopy)[serverAndReqid] : Data.NotFound;
        const NJson::TJsonValue& mockedResponseForAllReqids = (*respPtrCopy).Has(serverAddress) ? (*respPtrCopy)[serverAddress] : Data.NotFound;
        const NJson::TJsonValue& mockedResponse = (mockedResponseReqid == Data.NotFound) ?  mockedResponseForAllReqids : mockedResponseReqid;

        if (mockedResponse["is_text"] == "1")
            response = TextResponse("");

        if (mockedResponse.Has("content"))
            response.SetContent(mockedResponse["content"].GetString());

        if (mockedResponse.Has("status")) {
            ui32 status = mockedResponse["status"].IsString() ? atoi(mockedResponse["status"].GetString().c_str()) : mockedResponse["status"].GetInteger();
            response.SetHttpCode((HttpCodes) status);
        }

        for (auto& header : mockedResponse["headers"].GetArray()) {
            response.AddHeader(header[0].GetString(), header[1].GetString());
        }
        return true;
    }


private:
    TMockServerData& Data;
};

class TMockServer final : public NServer::TServer {
public:
    explicit TMockServer(const NServer::THttpServerConfig& config, TMockServerData &data)
        : NServer::TServer{config}
        , MockData(data)
    {
    }

    TClientRequest* CreateClient() override {
        return new TMockRequest(*this, MockData);
    }

    TMockServerData& MockData;
};

int main(int argc, char **argv) {
    NLastGetopt::TOpts options;

    TString configFilePath;
    options
        .AddLongOption('c', "config", "Path to serverconf proto file")
        .Required()
        .RequiredArgument("CONFIG")
        .StoreResult(&configFilePath);

    TString staticResponseFilePath;
    options
        .AddLongOption('r', "response", "Path to static response json file")
        .Required()
        .RequiredArgument("RESPONSE")
        .StoreResult(&staticResponseFilePath);

    TString backendIpsFilePath;
    options
        .AddLongOption('b', "backend", "Path to backend ips json file")
        .Optional()
        .RequiredArgument("BACKEND")
        .StoreResult(&backendIpsFilePath);

    bool saveRequests;
    options
        .AddLongOption('s', "save_requests", "Whether to save requests")
        .NoArgument()
        .SetFlag(&saveRequests);

    options.AddHelpOption('h');
    NLastGetopt::TOptsParseResult optParseResult(&options, argc, argv);
    TFileInput configFile(configFilePath);

    NServer::THttpServerConfig config;
    config = ParseFromTextFormat<decltype(config)>(configFilePath);

    TMockServerData data(staticResponseFilePath, backendIpsFilePath, saveRequests);

    TMockServer server(config, data);
    server.Start();
    server.Wait();
    return 0;
}
