#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/utils/serialization/document/node_utils.h>

namespace yandex {
namespace metrokit {
namespace utils {
namespace serialization {
namespace document {

BOOST_AUTO_TEST_CASE(after_merge_dictionaries__intersecting_keys_should_be_replaced) {
    auto base = Dictionary {
        { "num", Node { int64_t(12) } },
        { "str", Node { "12" } }
    };
    
    const auto diff = Dictionary {
        { "str", Node("hello") }
    };
    
    merge(base, diff);
    
    BOOST_CHECK_EQUAL(base.at("str").string().value(), "hello");
}

BOOST_AUTO_TEST_CASE(after_merge_dictionaries__unique_keys_should_be_replaced) {
    auto base = Dictionary {
        { "num", Node { int64_t(12) } },
        { "str", Node { "12" } }
    };
    
    const auto diff = Dictionary {
        { "str", Node { "hello" } }
    };
    
    merge(base, diff);
    
    BOOST_CHECK_EQUAL(base.at("num").intNumber().value(), 12);
}

BOOST_AUTO_TEST_CASE(after_merge_dictionaries__new_keys_should_be_inserted) {
    auto base = Dictionary {
        { "num", Node { int64_t(12) } },
        { "str", Node { "12" } }
    };
    
    const auto diff = Dictionary {
        { "new", Node { "33" } }
    };
    
    merge(base, diff);
    
    BOOST_CHECK_EQUAL(base.at("new").string().value(), "33");
}

BOOST_AUTO_TEST_CASE(after_merge_dictionaries__null_keys_should_be_inserted) {
    auto base = Dictionary {
        { "num", Node { int64_t(12) } },
        { "str", Node { "12" } }
    };
    
    const auto diff = Dictionary {
        { "str", Node {} }
    };
    
    merge(base, diff);
    
    BOOST_CHECK(base.at("str").isNull());
}

BOOST_AUTO_TEST_CASE(after_merge_dictionaries__keys_should_be_merged_recursively) {
    const auto baseShadow = Dictionary {
        { "x", Node { int64_t(0) } },
        { "x", Node { int64_t(10) } },
        { "color", Node { "FF0000" } },
        { "r", Node { int64_t(2) } }
    };

    auto base = Dictionary {
        { "stroke", Node { int64_t(12) } },
        { "shadow", Node { baseShadow } }
    };
    
    const auto diffShadow = Dictionary {
        { "x", Node {} },
        { "y", Node {} },
        { "r", Node { int64_t(121) } }
    };
    
    const auto diff = Dictionary {
        { "shadow", Node { diffShadow } }
    };
    
    merge(base, diff);
    
    BOOST_CHECK(base.at("shadow").dictionary().value().at("x").isNull());
    BOOST_CHECK(base.at("shadow").dictionary().value().at("y").isNull());
    BOOST_CHECK_EQUAL(base.at("shadow").dictionary().value().at("color").string().value(), "FF0000");
    BOOST_CHECK_EQUAL(base.at("shadow").dictionary().value().at("r").intNumber().value(), 121);
}

}}}}}
