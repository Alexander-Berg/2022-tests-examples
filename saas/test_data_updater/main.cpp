#include "dump_inddoc.h"

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/modchooser.h>

using namespace NLastGetopt;

int main(int argc, const char** argv) {
    TModChooser modChooser;
    modChooser.AddMode("dump2inddoc", main_dump2inddoc, "make inddocs from iproxy dump");
    try {
        return modChooser.Run(argc, argv);
    } catch (...) {
        Cerr << "An exception has occurred: " << CurrentExceptionMessage() << Endl;
        return EXIT_FAILURE;
    }
}
