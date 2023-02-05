#pragma once

#include <yandex/maps/runtime/async/future.h>

namespace yandex::maps::navi::ui::menu {

class CrashTest {
public:
    static void crashNow();
    static void crashAssert();
    static void anr();

    void crashAsyncInMain();
    void crashAsyncInGlobal();

private:
    runtime::async::Handle handle_;
};

}
