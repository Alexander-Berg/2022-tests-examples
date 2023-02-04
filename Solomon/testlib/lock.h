#pragma once

#include <solomon/libs/cpp/distributed_lock/lock.h>

#include <library/cpp/threading/future/future.h>

#include <util/generic/hash_set.h>
#include <util/generic/ptr.h>

#include <optional>
#include <utility>

namespace NSolomon::NCoordination {
    class TMockLockClient: public NSolomon::IDistributedLock {
        friend class TMockLock;
    public:
        TMockLockClient(ui32 id);

    protected:
        NThreading::TFuture<void> Acquire(NSolomon::ILockStateListenerPtr listener) override;
        NThreading::TFuture<void> Release() override;

        void DoAcquire();
        void DoRelease();
        void DoError();
        void DoChange(TString id, ui64 orderId);

        const TString& Id() const;

    protected:
        TString Id_;
        NSolomon::ILockStateListenerPtr Listener_{nullptr};
    };

    using TMockLockClientPtr = std::shared_ptr<TMockLockClient>;

    struct TLockCallProxy: IDistributedLock {
        TLockCallProxy(IDistributedLockPtr impl)
            : Impl_{std::move(impl)}
        {
        }

        NThreading::TFuture<void> Acquire(NSolomon::ILockStateListenerPtr listener) override {
            ++AcquireCalls;
            return Impl_->Acquire(listener);
        }

        NThreading::TFuture<void> Release() override {
            ++ReleaseCalls;
            return Impl_->Release();
        }

        ui32 AcquireCalls{0};
        ui32 ReleaseCalls{0};

    private:
        IDistributedLockPtr Impl_;
    };


    class TMockLock {
    public:
        ~TMockLock();
        IDistributedLockPtr CreateLockClient(ui32 id);
        IDistributedLockPtr CreateSlowLockClient(ui32 id, TDuration acquireDelay, TDuration releaseDelay);

        void AcquireFor(ui32 id);
        void ReleaseFor(ui32 id);
        void ErrorFor(ui32 id);

        ui64 OrderId() const {
            Y_VERIFY(OrderId_ > 1);
            return OrderId_ - 1;
        }

        std::optional<ui32> CurrentHolder() const {
            return CurrentHolder_;
        }

    private:
        std::optional<ui32> CurrentHolder_;
        THashMap<ui32, TMockLockClientPtr> Clients_;
        ui64 OrderId_{1};
    };
} // namespace NSolomon::NCoordination
