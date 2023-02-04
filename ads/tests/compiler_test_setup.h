#pragma once

#include "sis/builder.h"
#include "sis/network_definition.h"
#include "sis/inference.h"
#include "sis/inference_session.h"
#include "sis/cuda/compile_type.h"
#include "sis/cuda/gemm_type.h"
#include "sis/sis_assert.h"
#include "sis/data/data_cast.h"
#include "sis/network_calibration.h"
#include <list>
#include <map>
#include <set>
#include <vector>
#include <sstream>

namespace NSis {
  namespace NTest {

    enum struct EEngine : int
    {
      eSis,
      eTensorRT,
      eInvalid
    };

    inline EEngine DecodeEngine(const char* name);

    struct TTestSetup
    {
      static constexpr char eEngine[] = "engine";
      static constexpr char eCompilerType[] = "compiler_type";
      static constexpr char eGemmType[] = "gemm_type";
      static constexpr char eValueType[] = "value_type";
      static constexpr char eInnerDimension[] = "inner_dimension";
      static constexpr char eProfile[] = "profile";

      typedef std::vector<TTestSetup> TTestSetups;

      static TTestSetups Engines(std::initializer_list<const char*> engines);
      static TTestSetups Compilers(std::initializer_list<const char*> types);
      static TTestSetups Gemms(std::initializer_list<const char*> gemms);
      static TTestSetups InnerDims(std::initializer_list<int> dims);
      static TTestSetups ValueTypes(std::initializer_list<const char*> types);
      static TTestSetups Profile(std::initializer_list<bool> options);

      template <typename TDescription, typename T>
      static TTestSetups Custom(std::string name, TDescription description, std::initializer_list<T> options)
      {
        TTestSetups setups;
        for (auto opt: options)
        {
          auto strOpt = std::to_string(opt);
          setups.push_back(TTestSetup{
                  .Params={{name, strOpt}},
                  .CustomDescriptions={description(opt)}
          });
        }
        return setups;
      }

      template <typename T>
      static TTestSetups Custom(std::string name, std::initializer_list<T> options)
      {
        TTestSetups setups;
        for (auto opt: options)
        {
          auto strOpt = std::to_string(opt);
          setups.push_back(TTestSetup{
                  .Params={{name, strOpt}},
                  .CustomDescriptions={strOpt}
          });
        }
        return setups;
      }

      static TTestSetups Custom(std::string name, std::initializer_list<const char*> options)
      {
        TTestSetups setups;
        for (auto opt: options)
        {
          setups.push_back(TTestSetup{
                  .Params={{name, opt}},
                  .CustomDescriptions={opt}
          });
        }
        return setups;
      }

      bool IsValid() const;

      std::string Describe() const;

      struct Description
      {
        template<typename T>
        std::string operator()(const T &info) const
        {
          const TTestSetup &setup = info.param;
          return setup.Describe();
        }
      };

      typedef std::map<std::string, std::string> TParams;
      TParams Params;
      std::vector<std::string> CustomDescriptions;

      EEngine GetEngine() const;
      NCuda::ECompileType GetCompilerType() const;
      NCuda::EGemmType GetGemmType() const;
      DataType GetValueType() const;
      int GetInnerDim() const;
      bool GetProfile() const;

      const char* Get(const std::string& param) const;
      int GetInt(const std::string& params, int defaultValue = 0) const;

      TTestSetup AddParams(const TTestSetup& other) const;

      std::tuple<EEngine, NCuda::ECompileType, NCuda::EGemmType, DataType, int, bool> BaseTuple() const
      {
        return {GetEngine(), GetCompilerType(), GetGemmType(), GetValueType(), GetInnerDim(), GetProfile()};
      }

      bool operator<(const TTestSetup& r) const
      {
        auto base = BaseTuple();
        auto rBase = r.BaseTuple();
        if (base != rBase)
          return base < rBase;
        return Params < r.Params;
      }

      bool operator==(const TTestSetup& r) const
      {
        return Params == r.Params;
      }

    private:
      std::string GoogleName(std::string name) const;
    };

    typedef std::vector<TTestSetup> TTestSetups;

    inline
    std::set<TTestSetup> Add(std::set<TTestSetup> setups)
    {
      return setups;
    }

    template <typename... Targs>
    std::set<TTestSetup> Add(std::set<TTestSetup> u, const std::set<TTestSetup>& v, Targs... args)
    {
      for (auto s: v)
        u.insert(s);
      return Add(u, args...);
    }

    template <typename U, typename V>
    std::set<TTestSetup> Combine(const U& uSetups, const V& vSetups)
    {
      std::set<TTestSetup> result;
      for (const auto& u: uSetups)
      {
        for (const auto& v: vSetups)
        {
          auto merged = u.AddParams(v);
          if (merged.IsValid())
            result.insert(merged);
        }
      }
      return result;
    }

    inline std::set<TTestSetup> Combine()
    {
      return {};
    }

    template <typename U, typename V, typename... Targs>
    std::set<TTestSetup> Combine(const U& uSetups, const V& vSetups, Targs... args)
    {
      auto result = Combine(uSetups, vSetups);
      return Combine(result, args...);
    }

    struct TBaseTestPipeline : NData::TDataCast, ICalibrator
    {
      void InitSetup(const TTestSetup& setup);

      IInferencePtr CompileTestNetwork();
      void AugmentWithInt8();

      void SetDumpKernelSource(bool value);

      EEngine GetEngineType() const;
      NCuda::ECompileType GetCompilerType() const;
      NCuda::EGemmType GetGemmType() const;
      DataType GetValueType() const;
      int GetInnerDim() const;
      bool NeedProfiling() const;
      const char* GetCustom(const std::string& param) const;
      int GetInt(const std::string& param, int defaultValue = 0) const;

      float GetTestTypePrecision() const;

      IBuilderPtr Builder;
      INetworkDefinitionPtr Network;
      IInferencePtr Inference;
      ICalibrationResultPtr Calibration;

    protected:
      static bool IsValidValue(float v);

    private:
      INetworkDefinitionPtr CreateTestNetwork();
      IInferencePtr CompileGraph(INetworkDefinitionPtr network);
      void CompileTensorRT();
      DataType GetCastDataType() const override;

      std::vector<std::shared_ptr<void>> TestInputs;

      virtual void SetInputs(IInferencePtr inference, IInferenceSessionPtr session);
      void Feed(INetworkDefinitionPtr network, ICalibrationBatch& consumer) override;

    private:
      TTestSetup TestSetup2;
      bool DumpKernelSource = true;
    };
  }
}
