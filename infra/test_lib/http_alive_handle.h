#pragma once

#include <library/cpp/http/simple/http_client.h>

namespace NInfra::NPodAgent::NDaemonTest  {

class THttpAliveHandle {
public:
    THttpAliveHandle(ui32 port, const TDuration timeout):
        Client_("localhost", port, timeout, timeout)
    {
    }

    TString Alive() {
        TStringStream output;
        Client_.DoGet("/alive", &output);
        return output.Str();
    }

private:
    TSimpleHttpClient Client_;
};

} // namespace NInfra::NPodAgent::NDaemonTest
