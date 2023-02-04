#include "router.h"
#include "service.h"

#include <library/cpp/getopt/small/last_getopt.h>

#include <util/string/cast.h>

int main(int argc, char* argv[]) {
    NLastGetopt::TOpts opts;

    TString port;
    opts.AddLongOption(
            'p', "port")
        .Required()
        .StoreResult(&port);
    NLastGetopt::TOptsParseResult parsedOpts(&opts, argc, argv);

    NInfra::THttpServiceConfig cfg;
    cfg.SetPort(FromString<i32>(port));

    NInfra::NPodAgent::TYpTestService service(cfg);
    service.Start();
    service.Wait();

    return EXIT_SUCCESS;
}
