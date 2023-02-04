#include <maps/infopoint/lib/export/supplier_id_map.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <iostream>
#include <sstream>

using namespace infopoint;

TEST(supplier_generator_tests, test_get_id)
{
    SupplierIdMap gen;
    const int uriQuantity = 10;
    std::string ids[uriQuantity * 2];

    for (int i = 0; i < uriQuantity; ++i) {
        std::stringstream url;
        url << "http://partner/" << i;
        ids[i] = gen.getId(
            UserURI(url.str()), PublicUserURI(url.str() + "_public"));
    }
    for (int i = 0; i < uriQuantity; ++i) {
        std::stringstream url;
        url << "http://partner/" << (uriQuantity - 1 - i);
        ids[uriQuantity + i] = gen.getId(
            UserURI(url.str()), PublicUserURI(url.str() + "_public"));
    }
    // Equal URI should get equal ID's. Different URI should not.
    for (int i = 0; i < 20; i ++) {
        for (int j = i + 1; j < 20; j ++) {
            EXPECT_EQ(
                (ids[i] == ids[j]),
                (j == 2 * uriQuantity -1 - i)
            );
        }
    }
}
