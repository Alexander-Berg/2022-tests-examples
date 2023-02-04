#include <gtest/gtest.h>
#include "sis/dims.h"

namespace NSis {
  TEST(TDims, Format)
  {
    TDims dims({1, 2, 3});
    ASSERT_EQ("[1, 2, 3]", dims.Format());
  }

  TEST(TDims, InitializerListConstruction)
  {
    TDims result({1, 2, 3});

    ASSERT_EQ(3, result.DimsCount);
    ASSERT_EQ(1, result.Dims[0]);
    ASSERT_EQ(2, result.Dims[1]);
    ASSERT_EQ(3, result.Dims[2]);
  }

  TEST(TDims, CollectionConstruction)
  {
    TDims test({1, 2, 3, 4});
    int dims4[4] = {1, 2, 3, 4};
    int dims2[2] = {1, 2};

    ASSERT_EQ(test, TDims(1, 2, 3, 4));
    ASSERT_EQ(test, TDims(dims4));
    ASSERT_EQ(test, TDims(dims2, 3, 4));

    TDims test2({1, 2, 3, 4});
    test2.SetInnerDims(-2);
    ASSERT_EQ(test2, TDims(TDims(1, 2), TDims(3, 4)));
    ASSERT_EQ(test2, TDims({1, 2}, {3, 4}));
    ASSERT_EQ(test2, TDims({1, 2}, TDims(3, 4)));
  }

  TEST(TDims, DefaultInnerSize)
  {
    TDims large(2, 3, 4);
    ASSERT_EQ(4, large.InnerSize());
    ASSERT_EQ(6, large.OuterSize());

    TDims small(2);
    ASSERT_EQ(1, small.InnerSize());
    ASSERT_EQ(2, small.OuterSize());
  }

  TEST(TDims, Reshape)
  {
    TDims dims(8, 8, 8, 8);
    ASSERT_EQ(TDims(64, 64), dims.Reshape(TDims(64, 64)));
    ASSERT_EQ(TDims(8, 8 * 8 * 8), dims.Reshape(TDims(8, -1)));
    EXPECT_ANY_THROW(dims.Reshape(TDims(1, -1, -1, 1)));
    EXPECT_ANY_THROW(dims.Reshape(TDims(7, 8, 9)));
  }

  TEST(TDims, InnerDim)
  {
    TDims dims(2, 3, 4, 5);
    ASSERT_EQ(5, dims.InnerSize());
    ASSERT_EQ(24, dims.OuterSize());

    dims.SetInnerDims(-2);
    ASSERT_EQ(20, dims.InnerSize());
    ASSERT_EQ(6, dims.OuterSize());

    dims.SetInnerDims(0);
    ASSERT_EQ(120, dims.InnerSize());
    ASSERT_EQ(1, dims.OuterSize());

    dims.SetInnerDims(4);
    ASSERT_EQ(1, dims.InnerSize());
    ASSERT_EQ(120, dims.OuterSize());

    TDims outIn({2, 3}, {4, 5});
    ASSERT_EQ(20, outIn.InnerSize());
  }

  TEST(TDims, SliceDefault)
  {
    TDims dims(1, 2, 3, 4);
    auto front = dims.Slice(1);
    auto back = dims.Slice(-1);

    ASSERT_EQ(TDims(2, 3, 4), front);
    ASSERT_EQ(4, front.InnerSize());
    ASSERT_EQ(6, front.OuterSize());

    TDims testBack(1, 2, 3);
    testBack.SetInnerDims(3);
    ASSERT_EQ(testBack, back);
    ASSERT_EQ(1, back.InnerSize());
    ASSERT_EQ(6, back.OuterSize());
  }

  TEST(TDims, SliceInnerBack)
  {
    TDims dims(2, 3, 4, 5);
    dims.SetInnerDims(-2);

    ASSERT_EQ(20, dims.InnerSize());
    ASSERT_EQ(6, dims.OuterSize());

    auto back1 = dims.Slice(-1);
    ASSERT_EQ(TDims(2, 3, 4), back1);
    ASSERT_EQ(4, back1.InnerSize());
    ASSERT_EQ(6, back1.OuterSize());

    auto back2 = dims.Slice(-2);
    TDims back2Test(2, 3);
    back2Test.SetInnerDims(2);
    ASSERT_EQ(back2Test, back2);
    ASSERT_EQ(1, back2.InnerSize());
    ASSERT_EQ(6, back2.OuterSize());

    auto back3 = dims.Slice(-3);
    ASSERT_EQ(TDims(2), back3);
    ASSERT_EQ(1, back3.InnerSize());
    ASSERT_EQ(2, back3.OuterSize());

    auto front1 = dims.Slice(1);
    TDims front1Test(3, 4, 5);
    front1Test.SetInnerDims(-2);
    ASSERT_EQ(front1Test, front1);
    ASSERT_EQ(20, front1.InnerSize());
    ASSERT_EQ(3, front1.OuterSize());

    auto front2 = dims.Slice(2);
    TDims front2Test(4, 5);
    front2Test.SetInnerDims(-2);
    ASSERT_EQ(front2Test, front2);
    ASSERT_EQ(20, front2.InnerSize());
    ASSERT_EQ(1, front2.OuterSize());

    auto front3 = dims.Slice(3);
    TDims front3Test(5);
    front3Test.SetInnerDims(-1);
    ASSERT_EQ(front3Test, front3);
    ASSERT_EQ(5, front3.InnerSize());
    ASSERT_EQ(1, front3.OuterSize());
  }

  TEST(TDims, SliceInnerFront)
  {
    TDims dims(2, 3, 4, 5);
    dims.SetInnerDims(2);

    ASSERT_EQ(20, dims.InnerSize());
    ASSERT_EQ(6, dims.OuterSize());

    auto back1 = dims.Slice(-1);
    ASSERT_EQ(TDims(2, 3, 4), back1);
    ASSERT_EQ(4, back1.InnerSize());
    ASSERT_EQ(6, back1.OuterSize());

    auto back2 = dims.Slice(-2);
    TDims back2Test(2, 3);
    back2Test.SetInnerDims(2);
    ASSERT_EQ(back2Test, back2);
    ASSERT_EQ(1, back2.InnerSize());
    ASSERT_EQ(6, back2.OuterSize());

    auto back3 = dims.Slice(-3);
    ASSERT_EQ(TDims(2), back3);
    ASSERT_EQ(1, back3.InnerSize());
    ASSERT_EQ(2, back3.OuterSize());

    auto front1 = dims.Slice(1);
    TDims front1Test(3, 4, 5);
    front1Test.SetInnerDims(1);
    ASSERT_EQ(front1Test, front1);
    ASSERT_EQ(20, front1.InnerSize());
    ASSERT_EQ(3, front1.OuterSize());

    auto front2 = dims.Slice(2);
    TDims front2Test(4, 5);
    front2Test.SetInnerDims(0);
    ASSERT_EQ(front2Test, front2);
    ASSERT_EQ(20, front2.InnerSize());
    ASSERT_EQ(1, front2.OuterSize());

    auto front3 = dims.Slice(3);
    TDims front3Test(5);
    front3Test.SetInnerDims(0);
    ASSERT_EQ(front3Test, front3);
    ASSERT_EQ(5, front3.InnerSize());
    ASSERT_EQ(1, front3.OuterSize());
  }

  TEST(TDims, RaggedSize)
  {
    TDims dims(2, 3, 4);
    dims.SetRaggedDimension(2);

    ASSERT_EQ(4, dims.InnerSize());
    ASSERT_EQ(6, dims.OuterSize());
  }

  TEST(TDims, SliceRagged)
  {
    TDims dims(2, 3, 4, 5);
    dims.SetRaggedDimension(2);

    ASSERT_EQ(2, dims.Slice(-1).GetLastRaggedDimension());
    ASSERT_FALSE(dims.Slice(-2).IsRagged());

    ASSERT_EQ(1, dims.Slice(1).GetLastRaggedDimension());
    ASSERT_EQ(0, dims.Slice(2).GetLastRaggedDimension());
    ASSERT_FALSE(dims.Slice(3).IsRagged());
  }
}
