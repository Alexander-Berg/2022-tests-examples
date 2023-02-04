#include "suite.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

MainTestSuite* mainTestSuite()
{
    static MainTestSuite suite_;
    return &suite_;
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
