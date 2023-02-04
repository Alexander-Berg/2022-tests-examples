#pragma once

#include <util/generic/hash.h>

namespace NSaas {
    using TId2Url = THashMap<ui32, TString>;

    THolder<TId2Url> LoadDocUrls(TString path);
}
