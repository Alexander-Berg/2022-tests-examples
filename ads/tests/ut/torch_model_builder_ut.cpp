#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <ads/pytorch/deploy/eigen_lib/torch_model_builder.h>
#include <ads/pytorch/deploy/eigen_lib/torch_model_builder_impls.h>
#include <ads/yacontext/lib/dssm/inference/yml.h>


#include <util/system/tempfile.h>
#include <util/generic/xrange.h>

#include <library/cpp/iterator/zip.h>


using namespace NPytorchTransport;
using namespace NPytorchTransport::NTorchModule;
using namespace NNeural;


NInference::TLayerInput<float> MakeInput(const TVector<float>& vec) {
    return NInference::MakeInput(NInference::AsRowVector(vec.data(), vec.size()));
}


NInference::TLayerInput<float> MakeInput(const TVector<float>& vec, size_t nRows, size_t nCols) {
    Y_ENSURE(vec.size() == nRows * nCols);
    return NInference::MakeInput(NInference::AsDynamicMatrix(vec.data(), nRows, nCols));
}


void compareWithReference(
        const TString& configPath,
        const TVector<float>& inferenceReference,
        const TVector<TVector<float>>& inputs
) {
    auto&& modelConfig = YAML::LoadFile(configPath);
    auto layer = MakeModelBuilder(modelConfig)->BuildLayerModel(modelConfig, std::nullopt, NInference::MakeCpuLayerFactory());

    NInference::TLayerOutputs<float> outputs;
    NInference::TLayerInputs<float> modelInputs;
    for (const auto &input : inputs) {
        modelInputs.emplace_back(MakeInput(input, 1, input.size()));
    }
    layer->Inference(modelInputs, outputs);
    auto result = outputs.front();

    for (size_t i = 0; i < inferenceReference.size(); i++) {
        float value = (*std::get<NInference::TMatrixViewPtr<float>>(result.Matrix))(0, i);
        float ref_value = inferenceReference[i];
        UNIT_ASSERT(fabs(value - ref_value) <= 1e-4f);
    }
}


Y_UNIT_TEST_SUITE(TPytorchTransportLinear) {
    Y_UNIT_TEST(Linear) {

        TVector<float> vec(10, 0);
        for (size_t c = 0; c < 10; ++c) { vec[c] = c; }

        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/linear.yaml",
                {-9.0958,  9.3356, -9.0957, -8.4669,  9.1589},
                {vec}
        );
    }

    Y_UNIT_TEST(LinearReLU) {

        TVector<float> vec(10, 0);
        for (size_t c = 0; c < 10; ++c) { vec[c] = c; }
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/linear_relu.yaml",
                {0,  9.3356, 0, 0,  9.1589},
                {vec}
        );
    }

    Y_UNIT_TEST(LayerNorm) {
        TVector<float> vec(10, 1);
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/layernorm.yaml",
                {-0.6454, -2.4222, -0.1823, -0.2561,  0.5053,  1.4061, -0.3221,  0.3947,
                 -0.6534,  0.2698},
                {vec}
        );
    }

    Y_UNIT_TEST(CommonNormalization) {
        TVector<float> vec(5, 1);
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/common_normalization.yaml",
                {0.6830,  1.6793,  1.2242, -0.3169,  0.9369},
                {vec}
        );
    }

    Y_UNIT_TEST(LogNormalization) {
        TVector<float> vec(5, 1);
        for (int i = 0; i < 5; i++) {
            vec[i] = i - 1;
        }
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/log_scale_normalization.yaml",
                {0,  0,  0.69315, 1.09861,  1.38629},
                {vec}
        );
    }

    Y_UNIT_TEST(Sequential) {
        TVector<float> vec(30, 1);
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/sequential.yaml",
                {0.0000, 0.4614, 0.0407, 0.0495, 0.0000},
                {vec}
        );
    }

    Y_UNIT_TEST(DenseNet) {
        TVector<float> vec(10, 1);
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/densenet.yaml",
                {
                        1.0000, 1.0000, 1.0000, 1.0000, 1.0000, 1.0000, 1.0000, 1.0000, 1.0000,
                        1.0000, 0.0000, 0.0446, 0.0000, 1.6332, 0.5712, 0.0000, 0.0000, 1.8387,
                        0.0000, 0.0000, 0.0000, 0.4574, 0.0000, 0.5892, 0.9795, 1.4605, 1.2638,
                        0.0000, 0.0000, 1.1591, 0.0000, 0.5680, 0.3561, 0.0000, 0.2951, 0.0000,
                        1.2096, 1.0028, 0.0000, 0.3814, 0.5961, 0.5734, 0.0909, 0.0000, 0.0167,
                        0.0000, 0.1877, 0.0000, 0.2399, 0.2266
                },
                {vec}
        );
    }

    void compareAttentionLayer(
            const TVector<float>& inputVec,
            const TVector<float>& inferenceReference,
            const TString& configPath,
            int inputs_count
    ) {
            auto&& modelConfig = NInference::YamlLoadFile(configPath);
            auto layer = MakeModelBuilder(modelConfig)->BuildLayerModel(modelConfig, std::nullopt, NInference::MakeCpuLayerFactory());

            NInference::TLayerOutputs<float> outputs;
            outputs.clear();
            const float *data_ptr = inputVec.data();
            auto&& data_view = NInference::ToViewNotAlignedWithCopy(data_ptr, 4, 4);
            NInference::TMatrix<float> data_copy = data_view->replicate(1, 1);

            NInference::TMatrixViewPtr<float> dynamic_input = NInference::ToView(std::move(data_copy));
            auto input = NInference::MakeInput(std::move(dynamic_input), {1, 4, 4});

            NInference::TLayerInputs<float> inputs;
            for (auto i = 0; i < inputs_count; i++) {
                inputs.emplace_back(input);
            }
            layer->Inference(inputs, outputs);
            auto result = outputs.front();

            for (auto i = 0; i < 4; i++) {
                for (auto j = 0; j < 4; j++) {
                    float value = (*std::get<NInference::TMatrixViewPtr<float>>(result.Matrix))(i, j);
                    float ref_value = inferenceReference[i * 4 + j];
                    UNIT_ASSERT(fabs(value - ref_value) <= 1e-4f);
                }
            }
    }

    Y_UNIT_TEST(MultiheadAttention) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                -0.4611, -1.5606,  0.7524,  2.1781,
                -0.4763, -1.5405,  0.7672,  2.1843,
                -0.4686, -1.5480,  0.7732,  2.1898,
                -0.4716, -1.5488,  0.7616,  2.1843
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/multi_head_attention.yaml",
                2
        );
    }

    Y_UNIT_TEST(MultiheadAttentionSoftmax) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                -0.4611, -1.5606,  0.7524,  2.1781,
                -0.4763, -1.5405,  0.7672,  2.1843,
                -0.4686, -1.5480,  0.7732,  2.1898,
                -0.4716, -1.5488,  0.7616,  2.1843
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/multi_head_attention_softmax.yaml",
                2
        );
    }

    Y_UNIT_TEST(MultiheadAttentionLinear) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                -0.0377, -0.0210, -0.0137, -0.0213,
                -0.0356, -0.0218, -0.0119, -0.0252,
                -0.0380, -0.0211, -0.0083, -0.0236,
                -0.0397, -0.0184, -0.0105, -0.0226
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/multi_head_attention_linear.yaml",
                2
        );
    }

    Y_UNIT_TEST(TransformerEncoderLayer) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                 0.2430,  0.8657,  0.5808, -1.6894,
                 1.3119,  0.1279, -1.5029,  0.0631,
                 1.3490, -1.3690,  0.4011, -0.3811,
                -1.3705,  0.2454, -0.2826,  1.4077
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/transformer_encoder_layer.yaml",
                1
        );
    }

    Y_UNIT_TEST(AdsPytorchTransformerEncoderLayer) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                 2.8676,  0.5893,  0.4181,  1.8409,
                 1.7016,  2.0817, -3.3689,  1.5283,
                 3.5298,  2.0825,  0.5783, -1.0311,
                -2.5821,  2.3809, -2.3650,  0.0760
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/ads_pytorch_transformer_encoder_layer.yaml",
                1
        );
    }

    Y_UNIT_TEST(TransformerEncoderLayerV2) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                0.2430,  0.8657,  0.5808, -1.6894,
                1.3119,  0.1279, -1.5029,  0.0631,
                1.3490, -1.3690,  0.4011, -0.3811,
                -1.3705,  0.2454, -0.2826,  1.4077
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/transformer_encoder_layer_v2.yaml",
                1
        );
    }

    Y_UNIT_TEST(TransformerEncoder) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                1.4242, -1.4041, -0.0033, -0.0168,
                1.4102, -1.4182, -0.0034,  0.0114,
                1.3921, -1.4357,  0.0252,  0.0184,
                1.3786, -1.4476,  0.0064,  0.0626
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/transformer_encoder.yaml",
                1
        );
    }

    Y_UNIT_TEST(AdsPytorchTransformerEncoder) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                 1.2335,  0.3954, -0.6110,  0.6371,
                 2.1515,  2.4685, -5.6027,  1.2507,
                 3.2976,  1.0432, -0.3395,  0.7500,
                -3.5245,  2.7538, -4.4999,  1.0759
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/ads_pytorch_transformer_encoder.yaml",
                1
        );
    }

    Y_UNIT_TEST(WeightNormalizedTanhNormalizer) {
        TVector<float> vec(5, 1);
        for (auto i = 0; i < 5; i++) {
            vec[i] = i;
        }
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/weight_normalized_network_tanh_normalizer.yaml",
                {1.0000, 1.7616, 1.9640, 1.9951, 1.9993},
                {vec}
        );
    }

    Y_UNIT_TEST(WeightNormalizedNetwork) {
        TVector<float> vec(10, 1);
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/weight_normalized_network.yaml",
                {0.0281,  0.1997, -0.0748, -0.0845, -0.0646},
                {vec}
        );
    }

    Y_UNIT_TEST(Tsar8GenericFeedForwardApplicableModel) {
            TVector<TVector<float>> inputs;
            inputs.emplace_back(TVector<float>({0.3003, 0.5772, 0.6318}));
            inputs.emplace_back(TVector<float>({0.2163, 0.9483, 0.8608}));
            inputs.emplace_back(TVector<float>({7.0273e-01, 5.4181e-01, 5.8377e-04}));
            inputs.emplace_back(TVector<float>({0.0237, 0.8544, 0.6984}));
            compareWithReference(
                    ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/tsar8_generic_feedforward_applicable.yaml",
                    {0.1098,  0.1721, -0.3715,  0.2309},
                    inputs
            );
    }

    Y_UNIT_TEST(Tsar8QueryAttentionNetwork) {
            TVector<TVector<float>> inputs;
            inputs.emplace_back(TVector<float>({0.3522, 0.8601, 0.9464, 0.1806, 0.8384, 0.5128, 0.5491, 0.6368, 0.3163, 0.6649}));
            inputs.emplace_back(TVector<float>({0.4838, 0.0066, 0.9214, 0.9200, 0.5104, 0.3350, 0.7649, 0.9170, 0.0092, 0.5771}));
            inputs.emplace_back(TVector<float>({0.0904, 0.9254, 0.6983, 0.7844, 0.0993, 0.4654, 0.7926, 0.6685, 0.8681, 0.5117}));
            inputs.emplace_back(TVector<float>({0.8173, 0.2497, 0.4030, 0.1290, 0.2545, 0.5088, 0.7752, 0.4909, 0.7837, 0.5798}));
            inputs.emplace_back(TVector<float>({0.0861, 0.2238, 0.4492, 0.0882, 0.3241, 0.0815, 0.6618, 0.6052, 0.2311, 0.6203}));
            inputs.emplace_back(TVector<float>({0.6292, 0.3083, 0.5647, 0.1928, 0.6565, 0.3066, 0.2477, 0.0612, 0.2050, 0.4129}));
            inputs.emplace_back(TVector<float>({
                0.1814, 0.4454, 0.5624, 0.5638, 0.1929, 0.8590, 0.4632, 0.6692, 0.0588,
                0.2194, 0.9009, 0.1444, 0.8293, 0.6700, 0.4927, 0.9524, 0.3689, 0.2800,
                0.1983, 0.4278, 0.3417, 0.9312, 0.9131, 0.7842, 0.9217, 0.7545, 0.5909,
                0.1415, 0.8252, 0.1737, 0.8806, 0.5881, 0.2429, 0.3252, 0.0757, 0.6127,
                0.4958, 0.3526, 0.7209, 0.0675, 0.5436, 0.4919, 0.4655, 0.5943, 0.4183,
                0.0243, 0.1168, 0.9792, 0.8698, 0.8120, 0.5514, 0.1478, 0.5660, 0.7798,
                0.4214, 0.2832, 0.5097, 0.8188, 0.8604, 0.1358, 0.6509, 0.0405, 0.0393,
                0.1591, 0.3880, 0.4564, 0.6323, 0.6437, 0.7355, 0.6513, 0.0124, 0.6942,
                0.2809, 0.4104, 0.8208, 0.5179, 0.8790, 0.7688, 0.5642, 0.5624, 0.1858,
                0.5292, 0.7953, 0.7047, 0.4480, 0.9804, 0.2961, 0.1241, 0.5097, 0.0438,
                0.7459, 0.8326, 0.0662, 0.1837, 0.7635, 0.6931, 0.8669, 0.1813, 0.5900,
                0.4535, 0.1546, 0.2004, 0.2639, 0.6917, 0.3536, 0.6345, 0.0403, 0.4263,
                0.8471, 0.6742, 0.3730, 0.6059, 0.5543, 0.4173, 0.1982, 0.7707, 0.8614,
                0.9240, 0.7633, 0.3647, 0.5341, 0.2659, 0.8849, 0.1420, 0.6995, 0.9144,
                0.8142, 0.8251, 0.9380, 0.2575, 0.6989, 0.4998, 0.0177, 0.0799, 0.7635,
                0.9542, 0.4745, 0.2807, 0.7861, 0.2479, 0.0340, 0.6205, 0.6345, 0.9815,
                0.3239, 0.3721, 0.9404, 0.0976, 0.1792, 0.9593, 0.6895, 0.8504, 0.0293,
                0.0918, 0.9000, 0.7677, 0.2035, 0.9137, 0.0555, 0.1633, 0.0811, 0.0508,
                0.7063, 0.5445, 0.9321, 0.7032, 0.4633, 0.0282, 0.0761, 0.6580, 0.5539,
                0.0191, 0.1269, 0.8461, 0.8446, 0.7711, 0.3576, 0.9936, 0.0956, 0.7358,
                0.0254, 0.9444, 0.4375, 0.4190, 0.3584, 0.9890, 0.3709, 0.8454, 0.2023,
                0.4268, 0.9355, 0.1485, 0.8190, 0.1993, 0.6939, 0.3928, 0.2271, 0.8282,
                0.5862, 0.6067}));
            inputs.emplace_back(TVector<float>({1}));
            inputs.emplace_back(TVector<float>({0.3924, 0.7949, 0.1477, 0.3009, 0.2282, 0.8644, 0.1954, 0.1767, 0.1335, 0.1831}));
            inputs.emplace_back(TVector<float>({0.5858, 0.8920, 0.3636, 0.5659, 0.3623, 0.3008, 0.1426, 0.8891, 0.6971, 0.1388}));
            compareWithReference(
                    ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/tsar8_query_attention_network.yaml",
                    {-0.3574, -0.3746, -0.4998,  1.1286,  1.3701,  1.3222,  1.8248, -0.6851,
                     -0.8093, -0.3060, -0.0749, -1.8583, -0.2264, -0.1317, -0.3221},
                    inputs
            );
    }

    Y_UNIT_TEST(PlusOneModelWrapper) {
            TVector<float> inputs(12, 1.);
            compareWithReference(
                    ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/plus_one_wrapped_before.yaml",
                    {0.1453,  0.0942, -0.4129,  0.3251},
                    {inputs}
            );
            compareWithReference(
                    ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/plus_one_wrapped_after.yaml",
                    {1., 0.1453,  0.0942, -0.4129,  0.3251},
                    {inputs}
            );
    }

    Y_UNIT_TEST(TFillTensorTest) {
        // also checks that we can fill with zeros any size
        TVector<float> inputs(4, 12.);
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/fill_tensor.yaml",
                {1.5, 1.5, 1.5, 1.5},
                {inputs}
        );
    }

    Y_UNIT_TEST(TClampTensorTest) {
        TVector<float> inputs = {-13, 2, 15};
        TVector<TString> clampers = {
                "{_deploy_module_name_: clamp_layer, _deploy_version_: 1, children: {}, max_value: .inf, min_value: -.inf, parameters: {}}",
                "{_deploy_module_name_: clamp_layer, _deploy_version_: 1, children: {}, max_value: 10.0, min_value: -.inf, parameters: {}}",
                "{_deploy_module_name_: clamp_layer, _deploy_version_: 1, children: {}, max_value: .inf, min_value: -10.0, parameters: {}}",
                "{_deploy_module_name_: clamp_layer, _deploy_version_: 1, children: {}, max_value: 10.0, min_value: -10.0, parameters: {}}"
        };
        TVector<TVector<float>> references = {
                {-13, 2, 15},
                {-13, 2, 10},
                {-10, 2, 15},
                {-10, 2, 10}
        };
        for(const auto &[serializedClamper, reference] : Zip(clampers, references)) {
            TTempFile tempFile("clamp_layer.yaml");
            TFileOutput outputStream(tempFile.Name());
            outputStream << serializedClamper;
            outputStream.Finish();

            compareWithReference(tempFile.Name(), reference, {inputs});
        }
    }

    Y_UNIT_TEST(TTanhActivationLayerTest) {
        // also checks that we can fill with zeros any size
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/tanh_activation_layer.yaml",
                {
                    -0.1038,  0.5790,  0.3714, -0.5597,  0.3675,  0.3835, -0.6733, -0.1439,
                    0.3811, -0.5395,  0.0129, -0.5973, -0.6774, -0.1861, -0.4011, -0.3124
                },
                {vec}
        );
    }

    Y_UNIT_TEST(TInFeatureProjectorTest) {
        TVector<float> vec(10, 0);
        for (size_t c = 0; c < 10; ++c) { vec[c] = c; }

        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/in_feature_projector_dump.yaml",
                {0.4699, -3.6750,  7.0000,  8.0000,  9.0000},
                {vec}
        );
    }

    Y_UNIT_TEST(TDensenetEmbeddingLayerNormalize) {
        // also checks that we can fill with zeros any size
        TVector<float> vec = {-0.1042, 0.6610, 0.3901, -0.6324};
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/densenet_embedding_normalize.yaml",
                {-0.6433, -0.5702, -0.2261, -0.3905, -0.1628,  0.1759},
                {vec}
        );
    }

    Y_UNIT_TEST(TDensenetEmbeddingLayerNoNorm) {
        // also checks that we can fill with zeros any size
        TVector<float> vec = {-0.1042, 0.6610, 0.3901, -0.6324};
        compareWithReference(
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/densenet_embedding_unnorm.yaml",
                {-0.4261, -0.3776, -0.1497, -0.2586, -0.1078,  0.1165},
                {vec}
        );
    }

    Y_UNIT_TEST(TLinearSymmetricSelfAttention) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                1.9916,  10.1912, -10.7226,  19.9420,
                2.0442,  10.1817, -10.8173,  19.9480,
                2.0226,  10.1871, -10.6780,  19.9400,
                2.0485,  10.1801, -10.8768,  19.9513
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/symmetric_linear_attention.yaml",
                2
        );
    }

    // nokey
    Y_UNIT_TEST(TLinearSymmetricSelfAttentionNoKey) {
        TVector<float> vec = {
                -0.1042,  0.6610,  0.3901, -0.6324,
                0.3855,  0.4042, -0.8167, -0.1449,
                0.4013, -0.6035,  0.0129, -0.6889,
                -0.8243, -0.1883, -0.4250, -0.3232
        };

        TVector<float> inferenceReference = {
                1.9916,  10.1912, -10.7226,  19.9420,
                2.0442,  10.1817, -10.8173,  19.9480,
                2.0226,  10.1871, -10.6780,  19.9400,
                2.0485,  10.1801, -10.8768,  19.9513
        };

        compareAttentionLayer(
                vec,
                inferenceReference,
                ArcadiaSourceRoot() + "/ads/pytorch/deploy/tests/ut/fixture/symmetric_linear_attention_nokey.yaml",
                2
        );
    }

    Y_UNIT_TEST(TFloatDecompositionTensorTest) {
      TVector<float> inputs = {-13.1, 20.4, 15.67};
      TVector<TString> clampers = {
          "{_deploy_module_name_: float_decomposition, _deploy_version_: 1, children: {}, val_idx: 0, additive_idx: 1, compression_val: 20,  parameters: {}}",
          "{_deploy_module_name_: float_decomposition, _deploy_version_: 1, children: {}, val_idx: 1, additive_idx: 2, compression_val: 20, parameters: {}}",
          "{_deploy_module_name_: float_decomposition, _deploy_version_: 1, children: {}, val_idx: 0, additive_idx: 2, compression_val: 20, parameters: {}}",
          "{_deploy_module_name_: float_decomposition, _deploy_version_: 1, children: {}, val_idx: 2, additive_idx: 0, compression_val: 40, parameters: {}}"
      };
      TVector<TVector<float>> references = {
          {-0.7, 0.9, 15.67},
          {-13.1, 1, 0.4},
          {-0.7, 20.4, 0.9},
          {0.67, 20.4, 0.375}
      };
      for(const auto &[serializedClamper, reference] : Zip(clampers, references)) {
        TTempFile tempFile("float_decomposition_layer.yaml");
        TFileOutput outputStream(tempFile.Name());
        outputStream << serializedClamper;
        outputStream.Finish();

        compareWithReference(tempFile.Name(), reference, {inputs});
      }
    }

};
