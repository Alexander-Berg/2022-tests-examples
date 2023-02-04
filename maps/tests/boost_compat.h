#pragma once

// Workaround for failed attempt of rvalue references support in boost.

#include <boost/config.hpp>
#undef BOOST_HAS_RVALUE_REFS
#define BOOST_NO_RVALUE_REFERENCES
