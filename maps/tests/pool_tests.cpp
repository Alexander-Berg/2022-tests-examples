#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/exception.h>

#include <maps/wikimap/mapspro/services/autocart/libs/utils/include/pool.h>

#include <vector>

namespace maps::wiki::autocart::tests {

Y_UNIT_TEST_SUITE(pool_tests)
{

Y_UNIT_TEST(test_squares_and_cubes)
{
    const std::vector<size_t> NUMS = {1, 2, 3, 4};
    const std::vector<size_t> SQUARES = {1, 4, 9, 16};
    const std::vector<size_t> CUBES = {1, 8, 27, 64};

    std::vector<size_t> testSquares;
    std::vector<size_t> testCubes;

    ThreadPool pool(2);
    pool.Add(
        [&]() {
            for (const size_t& num : NUMS) {
                testSquares.push_back(num * num);
            }
        }
    );
    pool.Add(
        [&]() {
            for (const size_t& num : NUMS) {
                testCubes.push_back(num * num * num);
            }
        }
    );
    pool.Wait();

    EXPECT_EQ(SQUARES, testSquares);
    EXPECT_EQ(CUBES, testCubes);
}

Y_UNIT_TEST(test_catch_exception)
{
    EXPECT_THROW(
        [&]() {
            ThreadPool pool(2);
            pool.Add(
                [&]() {
                    throw maps::RuntimeError("catch me if you can");
                }
            );
            pool.Add(
                [&]() {
                    size_t sum = 0;
                    for (size_t i = 0; i < 10; i++) {
                        sum += i * i;
                    }
                }
            );
            pool.Wait();
        }(),
        maps::RuntimeError
    );
}

} //Y_UNIT_TEST_SUITE(pool_tests)

} //namespace maps::wiki::autocart::tests


