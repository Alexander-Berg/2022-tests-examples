#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/gzip.h>

#include "gzip_sample_inl.h"

namespace yandex {
namespace metrokit {
namespace gzip {

namespace {

const std::string SAMPLE_STRING = "This is gzip test\n";

} // namespace

BOOST_AUTO_TEST_CASE(ungzip_test) {
    const auto resultBinary = decompress(SAMPLE_GZIP_DATA);
    BOOST_ASSERT(resultBinary.isOk());
    const auto resultString = std::string { std::begin(resultBinary.okValue()), std::end(resultBinary.okValue()) };
    BOOST_CHECK_EQUAL(SAMPLE_STRING, resultString);
}

}}}
