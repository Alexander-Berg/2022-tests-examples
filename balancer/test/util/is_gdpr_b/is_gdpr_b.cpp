#include "is_gdpr_b.h"

#include <balancer/kernel/cookie/gdpr/is_gdpr_b.h>

TMaybe<std::pair<int, TMaybe<int>>> ParseIsGdprB(TString val) {
    auto res = NGdprCookie::TIsGdprB::Parse(val);
    if (res) {
        return std::make_pair(res->IsGdpr, res->IsGdprNoVpn);
    }
    return Nothing();
}
