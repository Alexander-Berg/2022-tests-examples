#include "sis/matrix_multiply_layer.h"
#include "sis/matrix_layout.h"
#include "mma_test_data.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <cmath>

namespace
{
  using namespace NSis;
  struct TestMatrixMultiply : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      InnerSize = GetInnerDim();

      Data.reset(new NTest::TMmaTestData(InnerSize));
    }

    void BuildNetwork()
    {
      auto A = Network->AddInput("A", GetValueType(), TDims2(Data->Shape.M, Data->Shape.K));
      auto B = Network->AddInput("B", GetValueType(), TDims2(Data->Shape.N, Data->Shape.K));
      auto layer = Network->AddMatrixMultiply(A, B);
      auto D = layer->GetOutput(0);
      EXPECT_EQ(GetValueType(), D->GetType());
      auto dimsD = D->GetDims();
      ASSERT_EQ(2, dimsD.DimsCount);
      ASSERT_EQ(Data->Shape.M, dimsD.OuterSize());
      ASSERT_EQ(Data->Shape.N, dimsD.InnerSize());
      Network->MarkOutput(D);
    }

    void ProfileGraph()
    {
      auto session = Inference->ProfileGraph();
      Evaluate(session);
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      Evaluate(session);
    }

    void Evaluate(IInferenceSessionPtr session)
    {
      session->SetInput(0, Data->A.size(), CastInput(Data->A));
      session->SetInput(1, Data->B.size(), CastInput(Data->B));
      session->Run();
      session->GetOutput(0, Data->D.size(), Data->D.data());
      session->Finalize();
      CastOutput(Data->D);
    }

    std::unique_ptr<NTest::TMmaTestData> Data;

    int InnerSize;
  };

  struct TestMatrixMultiplyEpilogue : TestMatrixMultiply
  {
    void ApplyEpilogue()
    {
      std::string_view epilogue = GetCustom("epilogue");
      if (epilogue == "identity")
        ApplyIdentity();
      else if (epilogue == "relu")
        ApplyRelu();
    }

    void ApplyIdentity()
    {
      auto& mma = dynamic_cast<IMatrixMultiplyLayer&>(*Network->GetLayer(0));
      mma.Epilogue()
              .Activation(NSis::EActivationType::eIdentity);
    }

    void ApplyRelu()
    {
      auto& mma = dynamic_cast<IMatrixMultiplyLayer&>(*Network->GetLayer(0));
      mma.Epilogue()
              .Activation(NSis::EActivationType::eRelu);
      for (auto&v : Data->TestData)
        v = std::max<float>(0, v);
    }

  };

  struct TestBatchMatrixMultiply : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
      const int M = 4;
      const int BatchN = 3;
      const int N = 4;
      const int K = 4;

      std::vector<float> A;
      std::vector<float> B;
      std::vector<float> D;
      std::vector<float> Test;

  protected:
    void SetUp() override
    {
      InitSetup(GetParam());

      PrepareData();
    }

    void PrepareData()
    {
      A.resize(M * K);
      B.resize(BatchN * N * K);
      D.resize(M * BatchN * N);
      Test.resize(M * BatchN * N);

      {
        int ld = K;
        auto ptr = A.data();
        for (int m = 0; m != M; ++m)
        {
          for (int k = 0; k != K; ++k)
          {
            if (m == k)
              ptr[k] = 1;
          }
          ptr += ld;
        }
      }

      {
        int ld = K;
        auto ptr = B.data();
        for (int b = 0; b != BatchN; ++b)
        {
          for (int n = 0; n != N; ++n)
          {
            for (int k = 0; k != K; ++k)
            {
              if (n == k)
                ptr[k] = 1 + b;
            }
            ptr += ld;
          }
        }
      }

      float alpha = GetAlpha();
      auto clamp = GetClamp();
      {
        int ld = N;
        auto ptr = Test.data();
        for (int b = 0; b != BatchN; ++b)
        {
          for (int m = 0; m != M; ++m)
          {
            for (int n = 0; n != N; ++n)
            {
              if (m == n)
                ptr[n] = clamp(alpha * (1 + b));
            }
            ptr += ld;
          }
        }
      }
    }

    void BuildNetwork()
    {
      auto A = Network->AddInput("A", GetValueType(), TDims2(M, K));
      auto B = Network->AddInput("B", GetValueType(), TDims2(N * BatchN, K));
      auto layer = Network->AddMatrixMultiply(A, B);
      layer->LayoutA()
        .Rows(M)
        .Cols(K)
        .LeadingDimension(K)
        .BatchCount(BatchN);
      layer->LayoutB()
        .Rows(N)
        .Cols(K)
        .LeadingDimension(K)
        .BatchCount(BatchN)
        .BatchStride(N * K);
      layer->LayoutD()
        .Rows(M)
        .Cols(N)
        .LeadingDimension(N)
        .BatchCount(BatchN)
        .BatchStride(M * N);

      layer->Epilogue()
        .Alpha(GetAlpha());

      auto D = layer->GetOutput(0);
      ASSERT_EQ(OutputDataType(), D->GetType());
      Network->MarkOutput(D);
    }

    DataType OutputDataType() const
    {
      switch (GetValueType())
      {
        case NSis::eFloat:
          return eFloat;
        case NSis::eHalf:
          return eHalf;
        case NSis::eInt8:
          return NSis::eInt8;
        default:
          SIS_FAIL("Type is not supported");
      }
    }

    std::function<float(float)> GetClamp() const
    {
      switch (GetValueType())
      {
        case NSis::eFloat:
          return [](float v) { return v;};
        case NSis::eHalf:
          return [](float v) { return v;};
        case NSis::eInt8:
          return [](float v){
            if (v < -128)
              v = -128;
            if (v > 127)
              v = 127;
            return (int8_t)v;
          };
        default:
          SIS_FAIL("Type is not supported");
      }
    }

    float GetAlpha() const
    {
      return GetCustom("alpha") ? 1e-2f : 1.f;
    }

    void ProfileGraph()
    {
      auto session = Inference->ProfileGraph();
      Evaluate(session);
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      Evaluate(session);
    }

    void Evaluate(IInferenceSessionPtr session)
    {
      session->SetInput(0, A.size(), CastInput(A));
      session->SetInput(1, B.size(), CastInput(B));
      session->Run();
      session->GetOutput(0, D.size(), D.data());
      session->Finalize();
      CastOutput(D, OutputDataType());
    }
  };

  TEST_P(TestMatrixMultiply, Correct)
  {
    BuildNetwork();
    CompileTestNetwork();
    Data->Prepare();

    if (NeedProfiling())
      ProfileGraph();

    PerformInference();

    auto precision = GetTestTypePrecision();
    for (size_t i = 0; i != Data->TestData.size(); ++i)
      ASSERT_NEAR(Data->TestData[i], Data->D[i], precision);
  }

  TEST_P(TestMatrixMultiplyEpilogue, Correct)
  {
    BuildNetwork();
    Data->Prepare();
    ApplyEpilogue();
    CompileTestNetwork();

    PerformInference();

    auto precision = GetTestTypePrecision();
    for (size_t i = 0; i != Data->TestData.size(); ++i)
     ASSERT_NEAR(Data->TestData[i], Data->D[i], precision);
  }

  TEST_P(TestBatchMatrixMultiply, Correct)
  {
    BuildNetwork();
    CompileTestNetwork();

    if (NeedProfiling())
      ProfileGraph();

    PerformInference();

    auto precision = GetTestTypePrecision();
    for (size_t i = 0; i != Test.size(); ++i)
      ASSERT_NEAR(Test[i], D[i], precision);
  }
}

INSTANTIATE_TEST_SUITE_P(BasicChecks, TestMatrixMultiply,
                        ::testing::ValuesIn(
                          NTest::Add(
                            NTest::Combine(
                              NTest::TTestSetup::InnerDims({32}),
                              NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                              NTest::TTestSetup::Compilers({"stage", "kernel barrier"}),
                              NTest::TTestSetup::Gemms({"cublas_lt"}),
                              NTest::TTestSetup::ValueTypes({"float", "half"})
                            )
                          )
                          ),
                          NTest::TTestSetup::Description()
);

INSTANTIATE_TEST_SUITE_P(Epilogue, TestMatrixMultiplyEpilogue,
                         ::testing::ValuesIn(
                                 NTest::Add(
                                         NTest::Combine(
                                                 NTest::TTestSetup::InnerDims({32}),
                                                 NTest::TTestSetup::Engines({"sis"}),
                                                 NTest::TTestSetup::Compilers({"stage"}),
                                                 NTest::TTestSetup::Gemms({"cublas_lt"}),
                                                 NTest::TTestSetup::ValueTypes({"float", "half"}),
                                                 NTest::TTestSetup::Custom(
                                                         "epilogue",
                                                         {
                                                           "identity",
                                                           "relu"
                                                         })
                                         )
                                 )
                         ),
                         NTest::TTestSetup::Description()
);

INSTANTIATE_TEST_SUITE_P(ProfileTest, TestBatchMatrixMultiply,
                         ::testing::ValuesIn(
                                         NTest::Combine(
                                                 NTest::TTestSetup::Engines({"sis"}),
                                                 NTest::TTestSetup::Compilers({"stage", "kernel_barrier"}),
                                                 NTest::TTestSetup::Gemms({"cublas_lt"}),
                                                 NTest::TTestSetup::Profile({true, false}),
                                                 NTest::TTestSetup::Custom("batched", [](auto){return "batched";}, {true}),
                                                 NTest::TTestSetup::ValueTypes({"float", "half", "int8"})
                                         )
                         ),
                         NTest::TTestSetup::Description()
);

INSTANTIATE_TEST_SUITE_P(AlphaBetaTest, TestBatchMatrixMultiply,
                         ::testing::ValuesIn(
                           NTest::Add(
                             NTest::Combine(
                               NTest::TTestSetup::Engines({"sis"}),
                               NTest::TTestSetup::Compilers({"stage", "kernel_barrier"}),
                               NTest::TTestSetup::Gemms({"cublas_lt"}),
                               NTest::TTestSetup::ValueTypes({"float", "half", "int8"}),
                               NTest::TTestSetup::Custom(
                                       "alpha",
                                       [](auto){return "alpha";},
                                       {1})
                             )
                           )
                         ),
                         NTest::TTestSetup::Description()
);
