#include "console_client.h"

#include <infra/pod_agent/libs/ip_client/simple_client.h>

#include <library/cpp/getopt/small/last_getopt.h>

#include <util/stream/file.h>

namespace NInfra::NPodAgent {

int AddAddress(int argc, const char* argv[]) {
    NLastGetopt::TOpts opts;

    opts.SetFreeArgsNum(3);
    opts.SetFreeArgTitle(0, "<device>", "device");
    opts.SetFreeArgTitle(1, "<ip6>", "ip in ip6 format");
    opts.SetFreeArgTitle(2, "<subnet>", "subnet");

    NLastGetopt::TOptsParseResult parsedOpts(&opts, argc, argv);

    TString device = parsedOpts.GetFreeArgs()[0];
    TString ip6 = parsedOpts.GetFreeArgs()[1];
    ui32 subnet = FromString<ui32>(parsedOpts.GetFreeArgs()[2]);

    TSimpleIpClientPtr ipClient = new TSimpleIpClient();

    auto result = ipClient->AddAddress(device, TIpDescription(ip6, subnet));

    if (!result) {
        Cout << ToString(result.Error().Errno) << ": " << result.Error().Message << Endl;
        return EXIT_FAILURE;
    }

    Cout << "Success" << Endl;

    return EXIT_SUCCESS;
}

int RemoveAddress(int argc, const char* argv[]) {
    NLastGetopt::TOpts opts;

    opts.SetFreeArgsNum(3);
    opts.SetFreeArgTitle(0, "<device>", "device");
    opts.SetFreeArgTitle(1, "<ip6>", "ip in ip6 format");
    opts.SetFreeArgTitle(2, "<subnet>", "subnet");

    NLastGetopt::TOptsParseResult parsedOpts(&opts, argc, argv);

    TString device = parsedOpts.GetFreeArgs()[0];
    TString ip6 = parsedOpts.GetFreeArgs()[1];
    ui32 subnet = FromString<ui32>(parsedOpts.GetFreeArgs()[2]);

    TSimpleIpClientPtr ipClient = new TSimpleIpClient();

    auto result = ipClient->RemoveAddress(device, TIpDescription(ip6, subnet));

    if (!result) {
        Cout << ToString(result.Error().Errno) << ": " << result.Error().Message << Endl;
        return EXIT_FAILURE;
    }

    Cout << "Success" << Endl;

    return EXIT_SUCCESS;
}

int ListAddress(int argc, const char* argv[]) {
    NLastGetopt::TOpts opts;

    opts.SetFreeArgsNum(1);
    opts.SetFreeArgTitle(0, "<device>", "device");

    NLastGetopt::TOptsParseResult parsedOpts(&opts, argc, argv);

    TString device = parsedOpts.GetFreeArgs()[0];

    TSimpleIpClientPtr ipClient = new TSimpleIpClient();

    auto result = ipClient->ListAddress(device);

    if (!result) {
        Cout << ToString(result.Error().Errno) << ": " << result.Error().Message << Endl;
        return EXIT_FAILURE;
    }

    Cout << "List result: " << Endl;
    for (const TIpDescription& ip : result.Success()) {
        Cout << ip.ToString() << Endl;
    }

    return EXIT_SUCCESS;
}

} // namespace NInfra::NPodAgent
