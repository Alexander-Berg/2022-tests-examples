#include "lock.h"

#include <library/cpp/threading/future/future.h>
#include <util/thread/pool.h>

#include <util/string/cast.h>

#include <utility>

using namespace NThreading;

namespace NSolomon::NCoordination {
namespace {
    class TSlowMockLockClient: public TMockLockClient, public std::enable_shared_from_this<TSlowMockLockClient> {
    public:
        TSlowMockLockClient(ui32 id, TDuration acquireDelay, TDuration releaseDelay)
            : TMockLockClient{id}
            , Pool_{CreateThreadPool(2)}
            , AcquireDelay_{acquireDelay}
            , ReleaseDelay_{releaseDelay}
        {
        }

    private:
        NThreading::TFuture<void> Acquire(NSolomon::ILockStateListenerPtr listener) override {
            Listener_ = listener;
            return CompleteLater(AcquireDelay_);
        }

        NThreading::TFuture<void> Release() override {
            return CompleteLater(ReleaseDelay_);
        }

        TFuture<void> CompleteLater(TDuration delay) {
            auto p = NewPromise<void>();

            auto ok = Pool_->AddFunc([delay, p] () mutable {
                Sleep(delay);
                p.SetValue();
            });

            Y_VERIFY(ok);

            return p;
        }

    private:
        THolder<IThreadPool> Pool_;
        const TDuration AcquireDelay_;
        const TDuration ReleaseDelay_;
    };
} // namespace

    TMockLockClient::TMockLockClient(ui32 id)
        : Id_{ToString(id)}
    {
    }

    NThreading::TFuture<void> TMockLockClient::Acquire(NSolomon::ILockStateListenerPtr listener) {
        Listener_ = listener;
        return MakeFuture();
    }

    NThreading::TFuture<void> TMockLockClient::Release() {
        return MakeFuture();
    }

    void TMockLockClient::DoAcquire() {
        Listener_->OnLock();
    }

    void TMockLockClient::DoRelease() {
        Listener_->OnUnlock();
    }

    void TMockLockClient::DoError() {
        Listener_->OnError("error");
    }

    void TMockLockClient::DoChange(TString id, ui64 orderId) {
        Listener_->OnChanged({
            .Data = std::move(id),
            .OrderId = orderId,
        });
    }

    const TString& TMockLockClient::Id() const {
        return Id_;
    }

    TMockLock::~TMockLock() = default;

    IDistributedLockPtr TMockLock::CreateLockClient(ui32 id) {
        auto client = std::make_shared<TMockLockClient>(id);
        auto [it, isNew] = Clients_.emplace(id, client);
        Y_UNUSED(it);
        Y_VERIFY(isNew);

        return client;
    }

    IDistributedLockPtr TMockLock::CreateSlowLockClient(ui32 id, TDuration acquireDelay, TDuration releaseDelay) {
        auto client = std::make_shared<TSlowMockLockClient>(id, acquireDelay, releaseDelay);
        auto [it, isNew] = Clients_.emplace(id, client);
        Y_UNUSED(it);
        Y_VERIFY(isNew);

        return client;
    }

    void TMockLock::AcquireFor(ui32 id) {
        if (CurrentHolder_ && CurrentHolder_ != id) {
            Clients_.at(id)->DoRelease();
        }
        CurrentHolder_ = id;

        auto& owner = Clients_.at(id);
        owner->DoAcquire();
        for (auto&& [cid, client]: Clients_) {
            client->DoChange(owner->Id(), OrderId_);
        }

        OrderId_++;
    }

    void TMockLock::ReleaseFor(ui32 id) {
        Y_VERIFY(CurrentHolder_ && id == *CurrentHolder_);
        Clients_.at(id)->DoRelease();
        CurrentHolder_ = std::nullopt;
    }

    void TMockLock::ErrorFor(ui32 id) {
        Clients_.at(id)->DoError();
    }
} // namespace NSolomon::NCoordination

