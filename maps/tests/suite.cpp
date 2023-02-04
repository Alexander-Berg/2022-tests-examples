#include "suite.h"

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

MainTestSuite* mainTestSuite()
{
    static MainTestSuite suite_;

    return &suite_;
}

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
