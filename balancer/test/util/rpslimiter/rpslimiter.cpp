#include "rpslimiter.h"

namespace NSrvKernel::NRpsLimiter {
    TMaybe<TPeerQuotas> ParsePeerQuotasPy(TString val) {
        using namespace NSrvKernel;
        using namespace NSrvKernel::NRpsLimiter;
        TPeerQuotas pq;
        Y_TRY(TError, err) {
            Y_PROPAGATE_ERROR(ParsePeerQuotas(val).AssignTo(pq));
            return {};
        } Y_CATCH {
            return Nothing();
        };
        return pq;
    }
}
