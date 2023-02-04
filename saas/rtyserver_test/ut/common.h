#pragma once

#include <saas/indexerproxy/logging/rty_ipr.h>
#include <saas/rtyserver/logging/rty_index.h>
#include <saas/rtyserver/logging/rty_access.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/registar.h>

inline void DisableRTYServerLogging() {
    DoInitGlobalLog("null", 0, false, false);
    NLoggingImpl::InitLogImpl<TRTYAccessLog>("null", 0, false, false);
    NLoggingImpl::InitLogImpl<TRTYIndexLog>("null", 0, false, false);
    NLoggingImpl::InitLogImpl<TRTYIprLog>("null", 0, false, false);
}

