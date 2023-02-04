#pragma once

#include <library/cpp/http/simple/http_client.h>

namespace NInfra::NPodAgent::NDaemonTest  {

class THttpPublicHandle {
public:
    THttpPublicHandle(ui32 port, const TDuration timeout):
        Client_("localhost", port, timeout, timeout)
    {
    }

    TString PodAttributesJson() {
        TStringStream output;
        Client_.DoGet("/pod_attributes", &output);
        return output.Str();
    }

    TString PodStatusJson() {
        TStringStream output;
        Client_.DoGet("/pod_status", &output);
        return output.Str();
    }

private:
    TSimpleHttpClient Client_;
};

} // namespace NInfra::NPodAgent::NDaemonTest
