#include "console_client.h"

#include <library/cpp/getopt/modchooser.h>

#include <util/generic/yexception.h>
#include <util/stream/output.h>

int main(int argc, const char* argv[]) {
    TModChooser modChooser;

    modChooser.AddMode(
        "add",
        NInfra::NPodAgent::AddAddress,
        "Add ip6 address to device."
    );

    modChooser.AddMode(
        "del",
        NInfra::NPodAgent::RemoveAddress,
        "Remove ip6 address from device."
    );

    modChooser.AddMode(
        "list",
        NInfra::NPodAgent::ListAddress,
        "List ip6 addresses."
    );

    try {
        return modChooser.Run(argc, argv);
    } catch (...) {
        Cerr << CurrentExceptionMessage() << Endl;
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
