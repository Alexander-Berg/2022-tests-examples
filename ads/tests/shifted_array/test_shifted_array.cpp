#include <ads/bigkv/preprocessor_primitives/shifted_array/shifted_array.h>

#include <library/cpp/testing/unittest/registar.h>


namespace NShiftedArray {
    Y_UNIT_TEST_SUITE(ShiftedArrayTests) {
        Y_UNIT_TEST(EmptyArrayTest) {
            TShiftedArray<i32> arr;
            UNIT_ASSERT_VALUES_EQUAL(arr.GetSize(), 0);
            UNIT_ASSERT_VALUES_EQUAL(arr.GetMinVal(), 0);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(0, 1234), 1234);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(0, 12345), 12345);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(10, 12345), 12345);
        }

        Y_UNIT_TEST(NotEmptyArrayTest) {
            TShiftedArray<i32> arr(-5, 10, -123);
            UNIT_ASSERT_VALUES_EQUAL(arr[-3], -123);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(-1), -123);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(-1, 12345), -123);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(-6, 12345), 12345);
            UNIT_ASSERT_VALUES_EQUAL(arr.At(5, 12345), 12345);

            arr[4] = 10;
            UNIT_ASSERT_VALUES_EQUAL(arr.At(4, 12345), 10);
        }

        Y_UNIT_TEST(EmptySetTest) {
            TShiftedArraySet arr(TVector<i32>{});
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(0), false);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(10), false);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(-5), false);
        }

        Y_UNIT_TEST(NotEmptySetTest) {
            TShiftedArraySet arr(TVector<i32>{3, 4, -5});
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(0), false);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(10), false);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(-4), false);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(-10), false);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(-5), true);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(3), true);
            UNIT_ASSERT_VALUES_EQUAL(arr.Contains(4), true);
        }

        Y_UNIT_TEST(MergeSetsTest) {
            TShiftedArraySet arr1(TVector<i32>{3, 4, -10});
            TShiftedArraySet arr2(TVector<i32>{-5, 18, 4});

            auto arr = arr1.Merge(arr2);
            UNIT_ASSERT_VALUES_EQUAL(arr.GetSize(), 18 - (-10) + 1);

            for (i32 x: {-10, -5, 3, 4, 18}) {
                UNIT_ASSERT(arr.Contains(x));
            }

            for (i32 x: {-123, -6, 0, 2, 5, 20, 1234555}) {
                UNIT_ASSERT(!arr.Contains(x));
            }
        }
    }
}
