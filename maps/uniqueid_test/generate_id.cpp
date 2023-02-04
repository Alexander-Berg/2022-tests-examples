#include <maps/mobile/server/init/lib/uniqueid.h>

#include <iostream>
#include <cstdlib>

namespace uid = maps::mobile::init;

int main(int argc, char** argv)
{
    unsigned count = 1;
    if (argc > 1) {
        count = strtoul(argv[1], NULL, 10);
    }

    for (unsigned i = 0; i < count; ++i) {
        auto id = uid::generateValidMiid(uid::generateUniqueData());
        std::cout << id << std::endl;
    }

    return 0;
}
