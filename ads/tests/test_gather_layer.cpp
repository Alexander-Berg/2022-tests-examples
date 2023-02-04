#include "sis/gather_layer.h"
#include "sis/element_wise_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <random>
#include <cmath>

namespace {
  using namespace NSis;
  struct TestGatherLayer : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
    }

    void BuildNetwork(TDims indicesDimension, TDims dataDimension)
    {
      indicesDimension.SetInnerDims(indicesDimension.DimsCount);
      dataDimension.SetInnerDims(1);

      PrepareData(false, indicesDimension, dataDimension);

      auto indices = Network->AddInput("indices", eInt32, indicesDimension);
      auto data = Network->AddInput("data", GetValueType(), dataDimension);

      auto layer = Network->AddGather(indices, data);
      auto output = layer->GetOutput(0);

      TDims expectedOutputDims(indicesDimension, dataDimension.Slice(1));
      ASSERT_EQ(expectedOutputDims, output->GetDims());

      Network->MarkOutput(output);
    }

    void BuildMaterializationNetwork(TDims indicesDimension, TDims dataDimension)
    {
      indicesDimension.SetInnerDims(indicesDimension.DimsCount);
      dataDimension.SetInnerDims(1);

      PrepareData(true, indicesDimension, dataDimension);

      auto indices = Network->AddInput("indices", eInt32, indicesDimension);
      auto data = Network->AddInput("data", GetValueType(), dataDimension);

      auto alteredData = Network->AddElementWise(data, data, EElementWiseOp::eSum)->GetOutput(0);
      auto output = Network->AddGather(indices, alteredData)->GetOutput(0);

      TDims expectedOutputDims(indicesDimension, dataDimension.Slice(1));
      ASSERT_EQ(expectedOutputDims, output->GetDims());

      Network->MarkOutput(output);
    }

    void PrepareData(bool alteredData, TDims indicesDimension, TDims dataDimension)
    {
      int vocabSize = dataDimension.Dims[0];
      Indices.resize(indicesDimension.TotalSize());
      for (size_t i = 0; i != Indices.size(); ++i)
        Indices[i] = i % vocabSize;

      Data.resize(dataDimension.TotalSize());
      for (size_t i = 0; i != Data.size(); ++i)
        Data[i] = i;

      if (alteredData)
      {
        AlteredData = Data;
        for (auto& v: AlteredData)
          v *= 2;
      }

      auto dataStride = dataDimension.InnerSize();
      Test.reserve(indicesDimension.TotalSize() * dataStride);
      for (auto index: Indices)
      {
        auto row = Data.data() + dataStride * index;
        if (alteredData)
        {
          for (int i = 0; i != dataStride; ++i)
            Test.push_back(row[i] + row[i]);
        }
        else
        {
          Test.insert(Test.end(), row, row + dataStride);
        }
      }

      ASSERT_EQ(indicesDimension.TotalSize() * dataDimension.InnerSize(), (int64_t)Test.size());

      Output.resize(Test.size());
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, Indices.size(), Indices.data());
      session->SetInput(1, Data.size(), CastInput(Data));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output);
    }

    std::vector<int> Indices;
    std::vector<float> Data;
    std::vector<float> AlteredData;
    std::vector<float> Test;
    std::vector<float> Output;
  };

  TEST_P(TestGatherLayer, Standalone)
  {
    BuildNetwork(TDims(4, 4), TDims(10, 8));
    CompileTestNetwork();
    PerformInference();

    ASSERT_EQ(Test, Output);
  }

  TEST_P(TestGatherLayer, MaterializeInputs)
  {
    BuildMaterializationNetwork(TDims(4, 4), TDims(10, 8));
    CompileTestNetwork();
    PerformInference();

    ASSERT_EQ(Test, Output);
  }
}

INSTANTIATE_TEST_SUITE_P(GatherLayerTest, TestGatherLayer,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                  NTest::TTestSetup::Engines({"sis", "tensorrt"}),
                                  NTest::TTestSetup::ValueTypes({"float", "half"}),
                                  NTest::TTestSetup::Compilers({"stage", "fusion", "kernel_barrier"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
