#pragma once

#include <solomon/libs/cpp/auth/tvm/tvm.h>

#include <util/generic/vector.h>
#include <util/generic/hash_set.h>

namespace NSolomon::NTesting {
    struct TMockTicketProvider: NAuth::NTvm::ITicketProvider {
        TTicketOrError GetTicket(ui32 clientId) const noexcept override {
            Y_UNUSED(clientId);
            return TTicketOrError::FromValue("ticket");
        }

        void AddDestinationId(ui32 id) noexcept override {
            Added.emplace(id);
        }

        void AddDestinationIds(TVector<ui32> ids) noexcept override {
            for (auto id: ids) {
                Added.emplace(id);
            }
        }

        THashSet<ui32> Added;
    };

    struct TFailingTicketProvider: TMockTicketProvider {
        TTicketOrError GetTicket(ui32) const noexcept override {
            return TTicketOrError::FromError("some error");
        }
    };
} // namespace NSolomon::NTesting
