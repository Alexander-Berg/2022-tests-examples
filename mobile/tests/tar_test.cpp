#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/tar.h>
#include <yandex/metrokit/container_utils.h>

#include "tar_sample_inl.h"

namespace yandex {
namespace metrokit {
namespace tar {

namespace {

auto asUint8Vector(const std::string& str) -> std::vector<uint8_t> {
    return { std::begin(str), std::end(str) };
}

const std::vector<Entry> SAMPLE_TAR_ENTRIES = {
    Entry { "sample/file1.txt", asUint8Vector("file1.txt contents\n") },
    Entry { "sample/file2.txt", asUint8Vector("file2.txt contents\n") },
    Entry { "sample/dir1/file3.txt", asUint8Vector("file3.txt contents\n") }
};

} // namespace

bool operator==(const Entry& lhs, const Entry& rhs) {
    return lhs.name == rhs.name && lhs.data == rhs.data;
}

std::ostream& operator<<(std::ostream& os, const Entry& entry) {
    return os << "name: \"" << entry.name << "\""
        << " data: \"" << std::string { std::begin(entry.data), std::end(entry.data) } << "\"";
}

std::ostream& operator<<(std::ostream& os, const std::vector<Entry>& entries) {
    size_t idx = 0;
    for (auto&& entry : entries) {
        os << "Entry " << idx << ": " << entry << "; ";
        ++idx;
    }
    return os;
}

BOOST_AUTO_TEST_CASE(untar_test) {
    const auto result = untar(SAMPLE_TAR_DATA);
    BOOST_CHECK(result.isOk());
    BOOST_CHECK_EQUAL(SAMPLE_TAR_ENTRIES, result.okValue());
}

}}}
