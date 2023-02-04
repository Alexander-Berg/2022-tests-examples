#pragma once

#include <library/cpp/logger/global/global.h>

#include <util/generic/singleton.h>
#include <library/cpp/deprecated/atomic/atomic.h>
#include <util/system/rwlock.h>

class TBackendProxy;

struct TRTYGlobalTestOptions {
public:
    TRTYGlobalTestOptions()
        : UsingDistributor(false)
    {}

    bool GetUsingDistributor() const {
        return AtomicGet(UsingDistributor);
    }

    void SetUsingDistributor(bool use = true) {
        AtomicSet(UsingDistributor, use);
    }

    TBackendProxy* GetBackendProxy() const {
        TReadGuard guard(AccessLock);
        VERIFY_WITH_LOG(CurrentBackendProxy, "wrong usage: backend proxy is not set");
        return CurrentBackendProxy;
    }
    void SetBackendProxy(TBackendProxy* proxy) {
        TWriteGuard guard(AccessLock);
        CurrentBackendProxy = proxy;
    }

private:
    TAtomic UsingDistributor;
    TBackendProxy* CurrentBackendProxy;
    TRWMutex AccessLock;
};

inline TRTYGlobalTestOptions& GlobalOptions() {
    return * Singleton<TRTYGlobalTestOptions>();
}
