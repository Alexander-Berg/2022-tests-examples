#include "hostname.h"
#include <util/system/env.h>
#include <util/system/hostname.h>

const TString& TestsHostName() {
    const static TString localhost = "localhost";
    if (GetEnv("RTY_TESTS_USE_LOCALHOST") != TString()){
        return localhost;
    } else {
        return HostName();
    }
}
