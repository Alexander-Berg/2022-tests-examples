#define BOOST_TEST_MODULE best_segments_test
#define BOOST_AUTO_TEST_MAIN

#include <maps/analyzer/libs/signal_filters/include/circular_window.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/floating_point_comparison.hpp>

#include <iostream>

using namespace maps::analyzer::signal_filters;

BOOST_AUTO_TEST_CASE(has_next_has_prev_test) {
    CircularWindow<int> window(2);

    BOOST_CHECK_EQUAL(window.hasNext(1), false);
    BOOST_CHECK_EQUAL(window.hasPrev(1), false);
    BOOST_CHECK_EQUAL(window.empty(), true);

    window.push(1);
    window.push(2);

    BOOST_CHECK_EQUAL(window.hasNext(1), true);
    BOOST_CHECK_EQUAL(window.hasNext(2), false);
    BOOST_CHECK_EQUAL(window.hasPrev(1), false);
    BOOST_CHECK_EQUAL(window.empty(), false);

    window.advance();

    BOOST_CHECK_EQUAL(window.hasNext(1), false);
    BOOST_CHECK_EQUAL(window.hasPrev(1), true);
    BOOST_CHECK_EQUAL(window.hasPrev(2), false);
    BOOST_CHECK_EQUAL(window.empty(), false);
}

BOOST_AUTO_TEST_CASE(circularity_test) {
    CircularWindow<int> window(2);
    window.push(1);
    BOOST_CHECK_EQUAL(window.back(), 1);
    window.push(2);
    BOOST_CHECK_EQUAL(window.back(), 2);
    window.push(3);
    BOOST_CHECK_EQUAL(window.back(), 3);

    BOOST_CHECK_EQUAL(window[0], 2);
    BOOST_CHECK_EQUAL(window[1], 3);

    BOOST_CHECK_EQUAL(*window.begin(), 2);
    BOOST_CHECK_EQUAL(*(window.end() - 1), 3);
}

BOOST_AUTO_TEST_CASE(erase_test) {
    CircularWindow<int> window(3);
    window.push(1);
    window.push(2);
    window.push(3);
    BOOST_CHECK_EQUAL(window.advance(), 1);
    BOOST_CHECK_EQUAL(window.hasNext(1), true);
    BOOST_CHECK_EQUAL(window.hasNext(2), false);

    BOOST_CHECK_EQUAL(window.hasPrev(1), true);
    BOOST_CHECK_EQUAL(window.hasPrev(2), false);

    //  1 2 3
    //    ^

    window.erase(-1);

    // 2 3
    // ^

    BOOST_CHECK_EQUAL(window.hasNext(1), true);
    BOOST_CHECK_EQUAL(window.hasNext(2), false);
    BOOST_CHECK_EQUAL(window.hasPrev(1), false);

    window.erase(1);

    BOOST_CHECK_EQUAL(window.hasNext(1), false);
    BOOST_CHECK_EQUAL(window.hasPrev(1), false);

    BOOST_CHECK_EQUAL(window.advance(), 2);
}

BOOST_AUTO_TEST_CASE(clear_test) {
    CircularWindow<int> window(2);

    window.push(1);
    window.push(2);

    window.clear();

    BOOST_CHECK_EQUAL(window.empty(), true);
    BOOST_CHECK_EQUAL(window.hasNext(1), false);
    BOOST_CHECK_EQUAL(window.hasPrev(1), false);
}
