import pytest
import torch

from ads_pytorch.nn.module.standard_scaler import (
    StandardScalerParameterServerOptimizer,
    StandardScaler,
    validate_ps_standard_scaler,
)
from ads_pytorch.core import BaseParameterServerModule, ParameterServerOptimizer
from ads_pytorch.model_calcer.concat_wrapper import wrap_model_with_concat_wrapper
from ads_pytorch.model_calcer.factory import MinibatchPoolFactory
from ads_pytorch.model_calcer.calcer import CalcerResults
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.nn.module.float_normalizer import FloatInputsNormalizer


######################################################
#                PARAMETER SERVER MODE               #
######################################################


@pytest.mark.parametrize(
    "dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
@pytest.mark.parametrize(
    "shape_dims",
    [((5,), (0,)), ((5,), (0, 1)), ((5, 6), (0, 1))],
    ids=["batch_size", "batch_size_sequence", "batch_size_sequence_multi_dim"],
)
def test_standard_scaler_values(dtype, shape_dims):
    torch.manual_seed(765)
    feature_shape, dims_to_reduce = shape_dims
    array_shape = [*range(7, 7 + len(dims_to_reduce)), *feature_shape]
    arrays = [torch.rand(*array_shape) for _ in range(10)]

    standard_scaler = StandardScaler(
        *feature_shape, dims_to_reduce=dims_to_reduce, dtype=dtype
    )
    optim = StandardScalerParameterServerOptimizer(standard_scaler.parameters())
    for arr in arrays:
        standard_scaler.zero_grad()
        value = standard_scaler(arr)
        value.sum().backward()

        for p in standard_scaler.parameters():
            assert p.grad is not None

        optim.step()

    concatted = torch.cat(arrays, dim=0)
    mean = torch.mean(concatted, dim=dims_to_reduce)
    std = torch.std(concatted, dim=dims_to_reduce)

    assert torch.allclose(standard_scaler.mean.to(torch.float32), mean)
    assert torch.allclose(standard_scaler.std.to(torch.float32), std)


class MyModel(BaseParameterServerModule):
    def __init__(self):
        super(MyModel, self).__init__()
        self.scaler1 = StandardScaler(4)
        self.scaler2 = StandardScaler(4)

    def async_forward(self, inputs):
        return inputs

    def sync_forward(self, async_outputs):
        res = (self.scaler1(async_outputs[0]) + self.scaler2(async_outputs[1])).sum(
            dim=1
        )
        return res


@pytest.mark.asyncio
async def test_run_with_model_calcer():
    model = MyModel()
    model.parameter_server_mode = True
    optimizer = StandardScalerParameterServerOptimizer(model.parameters())

    worker_pool = MinibatchPoolFactory(
        model=model,
        loss=torch.nn.MSELoss(),
        deep_optimizers=[optimizer],
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        train_mode=True,
        get_predictions=True,
        devices=frozenset({torch.device("cpu")}),
        num_workers=10,
        num_cpu_update_threads=1,
    )()

    data_list = [(torch.rand(10, 4), torch.rand(10, 4)) for _ in range(10)]
    targets_list = [torch.rand(10) for _ in range(10)]

    async with worker_pool:
        for data, targets in zip(data_list, targets_list):
            record = MinibatchRecord(inputs=data, targets=targets)
            await worker_pool.assign_job(record)

    concatted_list = [
        torch.cat([x[0] for x in data_list], dim=0),
        torch.cat([x[1] for x in data_list], dim=0),
    ]
    for concatted, scaler in zip(concatted_list, (model.scaler1, model.scaler2)):
        mean = torch.mean(concatted, dim=0)
        std = torch.std(concatted, dim=0)

        assert torch.allclose(scaler.mean.to(torch.float32), mean)
        assert torch.allclose(scaler.std.to(torch.float32), std)


######################################################
#                     VALIDATION                     #
######################################################


def test_validator_none_optimized():
    model = MyModel()
    with pytest.raises(ValueError):
        validate_ps_standard_scaler(
            model=model, optimizer=ParameterServerOptimizer(), loss=torch.nn.MSELoss()
        )


def test_validator_not_all_optimized():
    model = MyModel()
    optimizer = StandardScalerParameterServerOptimizer(model.scaler1.parameters())
    with pytest.raises(ValueError):
        validate_ps_standard_scaler(
            model=model,
            optimizer=ParameterServerOptimizer(optimizer),
            loss=torch.nn.MSELoss(),
        )


@pytest.mark.parametrize("use_wrapper", [True, False], ids=["ConcatWrapper", "Usual"])
def test_validator_wrong_optimizer(use_wrapper):
    model = MyModel()
    if use_wrapper:
        model = wrap_model_with_concat_wrapper(model)
    model.parameter_server_mode = True
    params = (
        list(model.buffer_parameters()) if use_wrapper else list(model.parameters())
    )
    optimizer = torch.optim.Adam(params)
    with pytest.raises(ValueError):
        validate_ps_standard_scaler(
            model=model,
            optimizer=ParameterServerOptimizer(optimizer),
            loss=torch.nn.MSELoss(),
        )


@pytest.mark.parametrize("use_wrapper", [True, False], ids=["ConcatWrapper", "Usual"])
def test_validator_ok(use_wrapper):
    model = MyModel()
    if use_wrapper:
        model = wrap_model_with_concat_wrapper(model)
    model.parameter_server_mode = True
    params = (
        list(model.buffer_parameters()) if use_wrapper else list(model.parameters())
    )
    optimizer = StandardScalerParameterServerOptimizer(params)
    validate_ps_standard_scaler(
        model=model,
        optimizer=ParameterServerOptimizer(optimizer),
        loss=torch.nn.MSELoss(),
    )


@pytest.mark.parametrize("shape_dims", [(10, 10, 10, 10)], ids=["4dims"])
@pytest.mark.parametrize("reduce_dims", [(0, 1), (0, 1, 2)], ids=["2", "3"])
def test_float_normalizer_cpp_python_cuda(shape_dims, reduce_dims):
    if not torch.cuda.is_available():
        return
    device = torch.device("cuda")
    float_normalizer1 = FloatInputsNormalizer(*shape_dims).to(device)
    float_normalizer2 = FloatInputsNormalizer(*shape_dims).to(device)
    float_normalizer2.load_state_dict(float_normalizer1.state_dict())
    input = torch.randn((1,) + shape_dims).to(device)
    out_cpp = float_normalizer1(input)
    out_temp = float_normalizer2.standard_scaler(input).clamp(
        float_normalizer2._crop_boundaries_after_scaler[0],
        float_normalizer2._crop_boundaries_after_scaler[1],
    )
    out_py = (
        torch.tanh(float_normalizer2.realvalue_normalizers).broadcast_to(input.shape)
        * input
    )
    assert torch.allclose(out_cpp, out_py)


@pytest.mark.parametrize("device", ["cuda", "cpu"])
@pytest.mark.parametrize(
    "input_dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
@pytest.mark.parametrize(
    "standard_scaler_dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
def test_float_normalizer_dtypes(input_dtype, standard_scaler_dtype, device):
    if device == "cuda" and not torch.cuda.is_available():
        return
    device = torch.device(device)
    float_normalizer = FloatInputsNormalizer(
        1, standard_scaler_dtype=standard_scaler_dtype
    ).to(device)
    input = torch.randn((1, 1)).to(device, input_dtype)
    out = float_normalizer(input).sum()
    out.backward()

    assert input.dtype == out.dtype

    params_list = list(float_normalizer.parameters())

    assert standard_scaler_dtype == params_list[1].grad.dtype
    assert standard_scaler_dtype == params_list[2].grad.dtype
    assert standard_scaler_dtype == params_list[3].grad.dtype
    assert torch.float32 == params_list[0].grad.dtype


@pytest.mark.parametrize(
    "dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
def test_float_normalizer_no_contigious_dims_cuda(dtype):
    if not torch.cuda.is_available():
        return
    dtypes = [torch.float64, torch.float32]
    device = torch.device("cuda")
    float_normalizer = FloatInputsNormalizer(
        2, 4, dims_to_reduce=(1,), standard_scaler_dtype=dtype
    ).to(device)
    for dtype_input in dtypes:
        input = torch.randn((2, 3, 4)).to(device, dtype_input)
        float_normalizer(input)


@pytest.mark.parametrize("device", ["cuda", "cpu"])
@pytest.mark.parametrize(
    "dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
@pytest.mark.parametrize(
    "shape_dims", [(10, 10, 10, 10), (2, 2, 2, 2, 2, 2, 2)], ids=["4dims", "7dims"]
)
@pytest.mark.parametrize("reduce_dims", [(0, 1), (0, 1, 2)], ids=["2", "3"])
@pytest.mark.parametrize(
    "input_dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
def test_float_normalizer_fused_no_fused_forward(
    device, dtype, shape_dims, reduce_dims, input_dtype
):
    device = torch.device(device)
    float_normalizer1 = FloatInputsNormalizer(
        *shape_dims,
        dims_to_reduce=reduce_dims,
        standard_scaler_dtype=dtype,
        fused_mode=True
    ).to(device)
    float_normalizer2 = FloatInputsNormalizer(
        *shape_dims,
        dims_to_reduce=reduce_dims,
        standard_scaler_dtype=dtype,
        fused_mode=False
    ).to(device)
    float_normalizer1.load_state_dict(float_normalizer2.state_dict())
    input = torch.randn((1,) * len(reduce_dims) + shape_dims).to(device, input_dtype)
    assert torch.allclose(float_normalizer1(input), float_normalizer2(input))


@pytest.mark.parametrize("device", ["cuda", "cpu"])
@pytest.mark.parametrize(
    "dtype", [torch.float32, torch.float64], ids=["Float", "Double"]
)
def test_float_normalizer_fused_no_fused_backward(
    device, dtype
):
    device = torch.device(device)
    module1 = FloatInputsNormalizer(100, 100, fused_mode=True, standard_scaler_dtype=dtype).to(device)
    module2 = FloatInputsNormalizer(100, 100, fused_mode=False, standard_scaler_dtype=dtype).to(device)
    module2.load_state_dict(module1.state_dict())
    input_ = (torch.randn((5, 100, 100), dtype=dtype) + 10).to(device)

    optim1 = torch.optim.Adam(list(module1.parameters()) + list(module2.parameters()))
    for i in range(1):
        for i in range(10):
            optim1.zero_grad()
            module1(input_.clone()).sum().backward()
            module2(input_.clone()).sum().backward()
            optim1.step()
        params = list(module1.parameters())
        params1 = list(module2.parameters())
        for i in range(4):
            torch.allclose(params[i].grad, params1[i].grad)
