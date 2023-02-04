#include "sis/concatenation_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>

namespace
{
  using namespace NSis;

  struct ConcatenationLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    using TAlgorithm = std::function<float(float, float)>;

  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      DimsA = TDims(3, 3, 4);
      DimsA.SetRaggedDimension(1);

      DimsB = TDims(3, 4, 4);
      DimsB.SetRaggedDimension(1);

      BuildNetwork();
      CompileTestNetwork();
      PrepareData();
      PerformInference();
    }

    void BuildNetwork()
    {
      auto A = Network->AddInput("A", GetValueType(), DimsA);
      auto B = Network->AddInput("B", GetValueType(), DimsB);
      auto layer = Network->AddConcatenation(A, B, 1);
      auto output = layer->GetOutput(0);
      TDims expected(3, 7, 4);
      expected.SetRaggedDimension(1);
      ASSERT_EQ(expected, output->GetDims());
      Network->MarkOutput(output);
    }

    using TStructure = std::vector<std::vector<std::vector<float>>>;

    void PrepareData()
    {
      int counter = 0;
      TStructure structureA;
      TStructure structureB;
      OffsetsA = FillStructure(counter, DimsA, structureA);
      OffsetsB = FillStructure(counter, DimsB, structureB);
      A = Flatten(structureA);
      B = Flatten(structureB);

      auto concat = ConcatenateRaggedDim(structureA, structureB);
      Test = Flatten(concat);
      Output.resize(Test.size());
    }

    std::vector<int> FillStructure(int& offset, TDims dims, TStructure & structure)
    {
      std::vector<int> offsets(1, 0);
      structure.resize(dims.Dims[0]);
      for (int i = 0; i != dims.Dims[0]; ++i)
      {
        const int count = i + 1;
        offsets.push_back(offsets.back() + count);

        auto& raggedDim = structure[i];
        raggedDim.resize(count);
        for (auto& innerDim: raggedDim)
        {
          innerDim.resize(dims.Dims[2]);
          std::iota(innerDim.begin(), innerDim.end(), offset);
          offset += dims.Dims[2];
        }
      }

      return offsets;
    }

    std::vector<float> Flatten(const TStructure & data)
    {
      std::vector<float> r;
      for (auto& ragged: data)
      {
        for (auto& inner: ragged)
        {
            r.insert(r.end(), inner.begin(), inner.end());
        }
      }
      return r;
    }

    TStructure ConcatenateRaggedDim(TStructure a, const TStructure& b)
    {
      SIS_VERIFY(a.size()== b.size());
      for (size_t i = 0; i != a.size(); ++i)
      {
        auto& target = a[i];
        auto& source = b[i];
        target.insert(target.end(), source.begin(), source.end());
      }

      return a;
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetRaggedInput(0, OffsetsA.size(), OffsetsA.data(), A.size(), CastInput(A));
      session->SetRaggedInput(1, OffsetsB.size(), OffsetsB.data(), B.size(), CastInput(B));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output);
    }

    TDims DimsA;
    TDims DimsB;
    std::vector<float> A;
    std::vector<float> B;
    std::vector<int> OffsetsA;
    std::vector<int> OffsetsB;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(ConcatenationLayerTest, RaggedAxis)
{
  auto precision = GetTestTypePrecision();

  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision) << i;
}

INSTANTIATE_TEST_SUITE_P(ConcatenationLayerTest, ConcatenationLayerTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Compilers({"stage", "kernel barrier"})
                                         )
                         ),
                         NTest::TTestSetup::Description()
);
