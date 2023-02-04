#include "mma_test_data.h"
#include "sis/data/fill_sin.h"
#include "sis/data/mma.h"
#include <cstdio>
#include <cstdint>

namespace NSis {
  namespace NTest {
    TMmaTestData::TMmaTestData(int innerSize)
    : InnerSize(innerSize)
    , Shape{InnerSize, 32, InnerSize}
    , ShapeA{Shape.M, Shape.K}
    , ShapeB{Shape.K, Shape.N}
    , ShapeD{Shape.M, Shape.N}
    {
    }

    void TMmaTestData::Prepare()
    {
      A.resize(ShapeA.Total());
      B.resize(ShapeB.Total());
      D.resize(ShapeD.Total());
      TestData.resize(ShapeD.Total());

      NData::FillSin(Shape.M, Shape.K, A.data());

      for (int64_t i = 0; i != ShapeB.Total(); ++i)
      {
        auto row = i / ShapeB.Column;
        auto col = i % ShapeB.Column;
        B[row * ShapeB.Column + col] = (row == col) ? 1.f : 0.f;
      }

      NData::MMA(Shape.M, Shape.N, Shape.K, A.data(), B.data(), TestData.data());
    }

    void TMmaTestData::Print(TMatrixShape shape, const float* data) const
    {
      for (size_t m = 0; m != std::min<size_t>(50, shape.Row); ++m)
      {
        for (size_t n = 0; n != std::min<size_t>(50, shape.Column); ++n)
        {
          fprintf(stderr, "%f ", data[m * shape.Column + n]);
        }
        fprintf(stderr, "\n");
      }
    }
  }
}
