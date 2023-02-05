#pragma once

#include <maps/renderer/libs/base/include/string_convert.h>
#include <boost/version.hpp>

namespace boost {
namespace test_tools {

#if BOOST_VERSION >= 106000
namespace tt_detail {
#endif

// for use wstring in boost test macros
template<typename T>
struct print_log_value;

template<>
struct print_log_value<std::wstring> {
    void operator()(std::ostream& os, std::wstring const& str)
    {
        os << maps::renderer::base::ws2s(str);
    }
};

#if BOOST_VERSION >= 106000
} // namespace tt_detail
#endif

} // namespace test_tools
} // namespace boost
