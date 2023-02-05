#pragma once

#include <boost/test/test_tools.hpp>


#define CHECK_EQUAL_COLLECTIONS(value, ...)             \
    do {                                                \
        const auto _left = (value);                     \
        const auto _expected = __VA_ARGS__;             \
        BOOST_CHECK_EQUAL_COLLECTIONS(                  \
            std::begin(_left), std::end(_left),         \
            std::begin(_expected), std::end(_expected)); \
    } while (false)


#define REQUIRE_EQUAL_COLLECTIONS(value, ...)           \
    do {                                                \
        const auto _left = (value);                     \
        const auto _expected = __VA_ARGS__;             \
        BOOST_REQUIRE_EQUAL_COLLECTIONS(                \
            std::begin(_left), std::end(_left),         \
            std::begin(_expected), std::end(_expected)); \
    } while (false)
