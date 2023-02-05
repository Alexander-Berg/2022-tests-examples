#include <yandex/maps/runtime/recovery/crashes_test.h>
#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/assert.h>

namespace yandex::maps::runtime::recovery {

void causeAssertFail()
{
    ASSERT(false, "Test ASSERT fail");
}

void causeRequireFail()
{
    REQUIRE(false, "Test REQUIRE fail");
}

void causeSegfault()
{
    int* p = reinterpret_cast<int*>(size_t(-1));
    *p = 0;
}

void causeSwallowedException()
{
    auto f = async::global()->async([] {
        throw RuntimeError("Test SwallowedException");
    });

    async::global()->spawn([](async::Future<void> f) {
        f.get();
    }, std::move(f)).detach();
}

} // namespace yandex::maps::runtime::recovery
