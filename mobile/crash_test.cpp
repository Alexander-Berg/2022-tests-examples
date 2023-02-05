#include "crash_test.h"

#include <yandex/maps/runtime/assert.h>
#include <yandex/maps/runtime/async/dispatcher.h>

#include <cstdlib>

namespace yandex::maps::navi::ui::menu {

void CrashTest::crashNow()
{
    abort();
}

void CrashTest::crashAssert()
{
    ASSERT(false);
}

void CrashTest::crashAsyncInMain()
{
    handle_ = runtime::async::ui()->spawn([] { crashNow(); });
}

void CrashTest::crashAsyncInGlobal()
{
    handle_ = runtime::async::global()->spawn([] { crashNow(); });
}

void CrashTest::anr()
{
    sleep(60 * 1000);
}

}
