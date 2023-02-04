#include <balancer/kernel/http2/server/hpack/tests/common/hpack_test_common.h>

#include <library/cpp/scheme/scheme.h>

#include <util/stream/input.h>
#include <util/stream/output.h>

int main() {
    using namespace NSrvKernel::NHTTP2;
    Cout << NUt::TestHPackEncoder(
        NSc::TValue::FromJsonThrow(Cin.ReadAll()), NUt::THPackSettings()
    ).ToJsonPretty() << Endl;
}
