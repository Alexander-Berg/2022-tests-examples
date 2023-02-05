#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/cache.h>
#include <string>

namespace yandex {
namespace metrokit {

BOOST_AUTO_TEST_CASE(cache_with_initial_state__is_empty) {
    Cache<std::string, double> cache { 123 };

    BOOST_CHECK(cache.isEmpty());
    BOOST_CHECK_EQUAL(cache.size(), 0);
}

BOOST_AUTO_TEST_CASE(zero_size_cache__does_not_hold_values) {
    Cache<std::string, double> cache { 0 };
    
    cache.insert("key", 12.0);
    
    BOOST_CHECK(cache.value("key").isNone());
    BOOST_CHECK(!cache.hasValue("key"));
    BOOST_CHECK(cache.isEmpty());
    BOOST_CHECK_EQUAL(cache.size(), 0);
}

BOOST_AUTO_TEST_CASE(zero_size_cache_methods_are_available) {
    Cache<std::string, double> cache { 0 };
    
    BOOST_CHECK_NO_THROW(cache.insert("key1", 12.0));
    BOOST_CHECK_NO_THROW(cache.value("key2"));
    BOOST_CHECK_NO_THROW(cache.remove("key3"));
    BOOST_CHECK_NO_THROW(cache.clear());
    BOOST_CHECK_NO_THROW(cache.removeLastUsed(11));
}

BOOST_AUTO_TEST_CASE(after_inserting_the_value__value_method_shoult_return_it) {
    Cache<std::string, double> cache { 12 };
    
    cache.insert("key", 12.0);
    
    BOOST_CHECK_EQUAL(cache.value("key").value(), 12.0);
}

BOOST_AUTO_TEST_CASE(after_inserting_the_value__remove_method_shoult_remove_it) {
    Cache<std::string, double> cache { 12 };
    
    cache.insert("key", 12.0);
    cache.remove("key");
    
    BOOST_CHECK(cache.value("key").isNone());
}

BOOST_AUTO_TEST_CASE(remove_last_method_shoult_remove_last_n_elemenents) {
    Cache<std::string, double> cache { 12 };

    cache.insert("key", 12.0);
    cache.insert("key1", 2.0);
    cache.insert("key2", 232.0);
    cache.removeLastUsed(2);

    BOOST_CHECK(cache.hasValue("key2"));
    BOOST_CHECK_EQUAL(cache.size(), 1);
}

BOOST_AUTO_TEST_CASE(after_removing_last_more_than_size_elemenents__cache_should_be_empty) {
    Cache<std::string, double> cache { 12 };

    cache.insert("key", 12.0);
    cache.insert("key1", 2.0);
    cache.insert("key2", 232.0);
    cache.removeLastUsed(123);

    BOOST_CHECK(cache.isEmpty());
}

BOOST_AUTO_TEST_CASE(after_inserting_the_value__remove_method_with_different_key_shoult_not_remove_it) {
    Cache<std::string, double> cache { 12 };
    
    cache.insert("key1", 122.0);
    cache.remove("key2");
    
    BOOST_CHECK_EQUAL(cache.value("key1").value(), 122.0);
}

BOOST_AUTO_TEST_CASE(after_inserting_the_values__clear_method_shoult_remove_them) {
    Cache<std::string, double> cache { 12 };
    
    cache.insert("key1", 12.0);
    cache.insert("key2", 1121.0);
    
    cache.clear();
    
    BOOST_CHECK(cache.value("key1").isNone());
    BOOST_CHECK(cache.value("key2").isNone());
}

BOOST_AUTO_TEST_CASE(after_inserting_more_than_max_size_values__old_values_should_be_removed) {
    Cache<std::string, double> cache { 2 };
    
    cache.insert("key1", 12.0);
    cache.insert("key2", 1121.0);
    cache.insert("key3", 111.0);
    
    BOOST_CHECK(cache.value("key1").isNone());
    BOOST_CHECK_EQUAL(cache.value("key2").value(), 1121.0);
    BOOST_CHECK_EQUAL(cache.value("key3").value(), 111.0);
}

BOOST_AUTO_TEST_CASE(value_method_should_update_values) {
    Cache<std::string, double> cache { 2 };
    
    cache.insert("key1", 12.0);
    cache.insert("key2", 1121.0);
    
    cache.value("key1");
    
    cache.insert("key3", 111.0);
    
    BOOST_CHECK_EQUAL(cache.value("key1").value(), 12.0);
    BOOST_CHECK(cache.value("key2").isNone());
    BOOST_CHECK_EQUAL(cache.value("key3").value(), 111.0);
}

BOOST_AUTO_TEST_CASE(insert_method_with_the_same_key_should_rewrite_and_update_values) {
    Cache<std::string, double> cache { 2 };
    
    cache.insert("key1", 12.0);
    cache.insert("key2", 1121.0);
    
    cache.insert("key1", 19999.0);
    
    cache.insert("key3", 111.0);
    
    BOOST_CHECK_EQUAL(cache.value("key1").value(), 19999.0);
    BOOST_CHECK(cache.value("key2").isNone());
    BOOST_CHECK_EQUAL(cache.value("key3").value(), 111.0);
}

} }
