#pragma once

#include <vector>

namespace NSis {
  namespace NTest {
    struct TTensorShape
    {
      int M;
      int N;
      int K;
    };

    struct TMatrixShape
    {
      int Row;
      int Column;
      int Total() const
      {
        return Row * Column;
      }
    };

    struct TMmaTestData
    {
      TMmaTestData(int innerSize);

      const int InnerSize;

      const TTensorShape Shape;
      const TMatrixShape ShapeA;
      const TMatrixShape ShapeB;
      const TMatrixShape ShapeD;

      void Prepare();
      void Print(TMatrixShape shape, const float* data) const;

      std::vector<float> A;
      std::vector<float> B;
      std::vector<float> D;
      std::vector<float> TestData;
    };
  }
}
