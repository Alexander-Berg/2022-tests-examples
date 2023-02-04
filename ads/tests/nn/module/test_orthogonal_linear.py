from copy import deepcopy
import pytest
import torch
import torch.nn
import torch.nn.intrinsic as nni
import torch.nn.functional
import torch.quantization
import itertools
from ads_pytorch.tools.memory_profiler import CUDADeviceMemoryProfiler
from ads_pytorch.nn.module.orthogonal_linear import (
    OrthogonalLinear,
    SVDLinear,
    ALLOWED_METHODS,
    orthogonalize_cholesky,
    orthogonalize_symeig,
    OrthogonalLinearReLU,
    QATOrthogonalLinear, QATOrthogonalLinearReLU,
    QuantizedOrthogonalLinear, QuantizedOrthogonalLinearReLU,
    extend_inputs_orthogonal_linear,
    prepare_model_for_batched_orthogonalization
)
from ads_pytorch.nn.module.batched_cholesky_orthogonalization import (
    StackTensorsWithZeroPadding,
    BatchedCopyViewLike
)

"""
REMARKS:
1. We add atol to tests with forward_as_linear because
orthogonal layers have in-place preconditioning (for efficiency)
and when we several times call preconditioner, it's preconditioned several times. And
this cause some small floating point precision errors
"""


@pytest.fixture(params=["cholesky", "symeig"])
def method(request):
    assert request.param in ALLOWED_METHODS
    return request.param


def orthogonalize(
    tensor: torch.Tensor,
    eps: float = 1e-7,
    method: str = "cholesky"
):
    assert method in ALLOWED_METHODS
    if method == "cholesky":
        return orthogonalize_cholesky(tensor=tensor, eps=eps)
    elif method == "symeig":
        return orthogonalize_symeig(tensor=tensor, eps=eps)
    raise ValueError


def test_bad_eps(method):
    V = torch.nn.init.kaiming_normal_(torch.zeros(5, 5))
    with pytest.raises(ValueError):
        orthogonalize(V, eps=-1, method=method)


def test_fail_group_orthogonality(method):
    torch.manual_seed(756)
    V = torch.nn.init.kaiming_normal_(torch.zeros(20, 19))
    with pytest.raises(NotImplementedError):
        orthogonalize(tensor=V, method=method)


@pytest.mark.parametrize('size', [10, 100, 250, 1000])
def test_orthogonalize_square_matrix(size, method):
    torch.manual_seed(756)
    V = torch.nn.init.kaiming_normal_(torch.zeros(size, size))
    W = orthogonalize(tensor=V, method=method)
    identity = W @ W.t()
    assert torch.norm(identity - torch.eye(size)) / size ** 2 < 1e-3


@pytest.mark.parametrize('size', [10, 100, 250, 1000])
def test_orthogonalize_rectangular_matrix(size, method):
    torch.manual_seed(756)
    V = torch.nn.init.kaiming_normal_(torch.zeros(size, size * 2))
    W = orthogonalize(tensor=V, method=method)
    identity = W @ W.t()
    assert torch.norm(identity - torch.eye(size)) / size ** 2 < 1e-3


@pytest.mark.parametrize('square', [True, False], ids=['Square', 'Rectangular'])
def test_methods_relation(square):
    """
    Tests that both methods have fixed relation to each other. Yet another one bugchecker
    """
    torch.manual_seed(756)
    sizes = (10, 10) if square else (10, 20)
    tensor = torch.nn.init.kaiming_normal_(torch.zeros(*sizes))

    W_optimal = orthogonalize(tensor=tensor, method='symeig', eps=0)
    W_cholesky = orthogonalize(tensor=tensor, method='cholesky', eps=0)

    L = torch.linalg.cholesky(tensor @ tensor.t())
    U, S, Vh = torch.linalg.svd(L.t(), full_matrices=True)
    V = Vh.transpose(-2, -1).conj()
    W_after_correction = V @ U.t() @ W_cholesky
    assert torch.allclose(W_after_correction, W_optimal, atol=1e-5)


def test_instability_zeros(method):
    torch.manual_seed(756)
    orthogonalize(tensor=torch.zeros(500, 500), method=method, eps=1e-6)


def test_eps_zero(method):
    # generate "good matrix"
    torch.manual_seed(756)
    eigenvectors = torch.nn.init.orthogonal_(torch.zeros(4, 4))
    eigenvalues = torch.diag(torch.FloatTensor([1, 2, 3, 4]))
    tensor = eigenvectors @ eigenvalues @ eigenvectors.t()
    orthogonalize(tensor=tensor, method=method, eps=0)


def test_orthogonalize_big_eps(method):
    # generate "good matrix"
    torch.manual_seed(756)
    eigenvectors = torch.nn.init.orthogonal_(torch.zeros(4, 4))
    eigenvalues = torch.diag(torch.FloatTensor([1, 2, 3, 4]))
    tensor = eigenvectors @ eigenvalues @ eigenvectors.t()
    W = orthogonalize(tensor=tensor, method=method, eps=100)
    # with eps = 100 it will never be orthogonal so just check that we have some matrix
    assert torch.isnan(W).sum() == 0


###############################################
#              OrthogonalLinear               #
###############################################


@pytest.mark.parametrize('batch_size', [4, 100])
@pytest.mark.parametrize('in_features', [100, 10], ids=['Rectangular', 'Square'])
@pytest.mark.parametrize('precondition', [True, False], ids=['Precondition', 'Usual'])
def test_forward_no_rescale(in_features, batch_size, precondition):
    torch.manual_seed(756)
    data = torch.nn.init.normal_(torch.zeros(batch_size, in_features))
    linear = OrthogonalLinear(in_features, 10, bias=False, rescale=False, use_preconditioner=precondition)
    # Test proper forward with rotation
    forward_res = linear(data)
    orthogonal_matrix = orthogonalize_cholesky(tensor=linear.V, eps=linear.eps, precondition=precondition)
    assert torch.allclose(data @ orthogonal_matrix.t(), forward_res)


@pytest.mark.parametrize('batch_size', [4, 100])
@pytest.mark.parametrize('in_features', [100, 10], ids=['Rectangular', 'Square'])
@pytest.mark.parametrize('precondition', [True, False], ids=['Precondition', 'Usual'])
def test_scaled_forward(in_features, batch_size, precondition):
    torch.manual_seed(756)
    data = torch.nn.init.normal_(torch.zeros(100, in_features))
    linear = OrthogonalLinear(in_features, 10, bias=False, rescale=True, use_preconditioner=precondition)
    torch.nn.init.ones_(linear.rescale)
    # Test proper forward with rotation
    forward_res = linear(data)
    orthogonal_matrix = orthogonalize_cholesky(tensor=linear.V, eps=linear.eps, precondition=precondition)
    assert torch.allclose(data @ orthogonal_matrix.t(), forward_res)

    # Now test we use scaling properly
    torch.nn.init.normal_(linear.rescale)
    forward_res_after_scale = linear(data)
    assert torch.allclose(forward_res_after_scale, forward_res * linear.rescale, atol=1e-4)


_FWD_DEVICES = [torch.device("cpu")]
_FWD_DEVICES.append(pytest.param(torch.device("cuda", 0), marks=pytest.mark.requires_cuda))


@pytest.mark.parametrize('device', _FWD_DEVICES)
@pytest.mark.parametrize('batch_size', [4, 100])
@pytest.mark.parametrize('in_features', [100, 10, 3], ids=['Rectangular', 'Square', 'ReverseRectangular'])
def test_orthogonal_forward_same_as_linear(in_features, batch_size, device):
    torch.manual_seed(756)
    with torch.no_grad():
        data = torch.nn.init.normal_(torch.zeros(100, in_features).to(device))
        implicit_expand_transpose = in_features < 10
        ortho_linear = OrthogonalLinear(in_features, 10, bias=True, rescale=True,
                                        implicit_expand_transpose=implicit_expand_transpose).to(device=device)
        linear = torch.nn.Linear(in_features, 10, bias=True).to(device=device)
        torch.nn.init.normal_(ortho_linear.rescale, mean=1, std=1e-4)
        torch.nn.init.normal_(ortho_linear.bias, mean=0, std=1e-4)

        linear.bias.copy_(ortho_linear.bias)
        linear.weight.copy_(ortho_linear.weight)

        assert torch.allclose(ortho_linear(data), linear(data), atol=1e-5)


def test_backward():
    torch.manual_seed(756)
    data = torch.nn.init.normal_(torch.zeros(100, 10))
    linear = OrthogonalLinear(10, 10)
    (linear(data) ** 2).mean().backward()

    for grad in [linear.V.grad, linear.bias.grad, linear.rescale.grad]:
        assert grad is not None
        assert torch.isnan(grad).sum() == 0


@pytest.mark.parametrize(
    ['layer_cls', 'kwargs'],
    [
        (OrthogonalLinear, dict(bias=b, rescale=r))
        for r, b in itertools.product([True, False], [True, False])
    ] + [
        (SVDLinear, dict(bias=b))
        for b in [True, False]
    ]
)
def test_layer_compatibility_with_ordinary_linear(layer_cls, kwargs):
    linear = layer_cls(4, 2, eps=0.01, **kwargs)
    assert linear.eps == 0.01
    assert linear.in_features == 4
    assert linear.out_features == 2
    # to be compatible with torch.nn.Linear
    assert linear.calc_weight().size() == torch.Size([2, 4])
    assert torch.allclose(linear.calc_weight(), linear.weight)
    if kwargs["bias"]:
        assert linear.bias.size() == torch.Size([2])
    else:
        assert linear.bias is None


@pytest.mark.requires_cuda
def test_gpu_forward_backward():
    torch.manual_seed(756)
    linear = OrthogonalLinear(10, 10).cuda()
    data = torch.nn.init.normal_(torch.zeros(100, 10)).cuda()
    (linear(data) ** 2).mean().backward()


@pytest.mark.requires_cuda
def test_to_device():
    torch.manual_seed(756)
    OrthogonalLinear(10, 10).cuda()


@pytest.mark.requires_cuda
def test_load_state_dict():
    torch.manual_seed(756)
    state_dict = OrthogonalLinear(10, 10).cuda().state_dict()
    linear = OrthogonalLinear(10, 10)
    linear.load_state_dict(state_dict)


def test_orthogonal_linear_numerical_stability():
    torch.manual_seed(678)
    data = torch.rand((100, 10))

    model = OrthogonalLinear(10, 10)
    torch.nn.init.normal_(model.V, mean=1e7, std=2)
    (model(data) ** 2).mean().backward()

    # check that our test fails without conditioner
    model = OrthogonalLinear(10, 10, use_preconditioner=False)
    torch.nn.init.normal_(model.V, mean=1e7, std=2)
    with pytest.raises(RuntimeError):
        (model(data) ** 2).mean().backward()


###############################################
#                  SVDLinear                  #
###############################################


# For svd linear, we test it can correctly compute forward-backward
# and can converge
# This is important test that our SVDLinear may approximate any matrix


@pytest.mark.parametrize('rank', [20, 10, 1, 0], ids=['Full', 'NotFull', 'RankOne', 'Zeros'])
def test_svd_matrix_approximate_square(rank):
    torch.manual_seed(12345)
    torch.set_num_threads(1)
    dim = 20

    linear = SVDLinear(dim, dim)

    # Generate random matrix of some rank
    U = torch.nn.init.orthogonal_(torch.randn(dim, dim))
    S = torch.randn(dim)
    S[rank:] = 0
    V = torch.nn.init.orthogonal_(torch.randn(dim, dim))

    matrix = U @ torch.diag(S) @ V.t()
    # sanity check
    assert torch.linalg.matrix_rank(matrix).item() == rank

    optim = torch.optim.LBFGS(linear.parameters(), lr=1)

    def closure():
        linear.zero_grad()
        loss = torch.nn.functional.mse_loss(linear.calc_weight().reshape(-1), matrix.view(-1))
        loss.backward()
        return loss

    losses = []
    for _ in range(50):
        loss = optim.step(closure)
        losses.append(float(loss))

    assert losses[-1] < 1e-4


@pytest.mark.parametrize('batch_size', [4, 100])
@pytest.mark.parametrize('in_features', [100, 10], ids=['Rectangular', 'Square'])
def test_svd_forward_same_as_linear(in_features, batch_size):
    torch.manual_seed(756)
    with torch.no_grad():
        data = torch.nn.init.normal_(torch.zeros(100, in_features))
        ortho_linear = SVDLinear(in_features, 10, bias=True)
        linear = torch.nn.Linear(in_features, 10, bias=True)
        torch.nn.init.normal_(ortho_linear.rescale)
        torch.nn.init.normal_(ortho_linear.bias)

        linear.bias.copy_(ortho_linear.bias)
        linear.weight.copy_(ortho_linear.weight)

        assert torch.allclose(ortho_linear(data), linear(data), atol=1e-4)


###############################################
#                 Quantization                #
###############################################


def test_orthogonal_linear_relu():
    torch.manual_seed(756)
    ortho = OrthogonalLinear(10, 7)
    ortho_relu = OrthogonalLinearReLU(10, 7)
    ortho_relu.load_state_dict(ortho.state_dict())

    inputs = torch.rand(100, 10)

    assert torch.allclose(torch.nn.functional.relu(ortho(inputs)), ortho_relu(inputs))


class QATSubstitutionSubmodule(torch.nn.Module):
    def __init__(self):
        super(QATSubstitutionSubmodule, self).__init__()
        self.linear = OrthogonalLinear(10, 10)
        self.linear_relu = OrthogonalLinearReLU(10, 10)


class QATSubstitutionModule(torch.nn.Module):
    def __init__(self):
        super(QATSubstitutionModule, self).__init__()
        self.linear = OrthogonalLinear(10, 10)
        self.linear_relu = OrthogonalLinearReLU(10, 10)
        self.submodule = QATSubstitutionSubmodule()


@pytest.mark.xfail
def test_prepare_qat():
    model = QATSubstitutionModule()
    qconfig = torch.quantization.get_default_qat_qconfig("fbgemm")
    model.qconfig = qconfig
    # prepare_qat does following things:
    # 1. Propagate qconfig to submodules
    # 2. Substitute modules with their qat twins
    qat_model = torch.quantization.prepare_qat(model, inplace=False)

    # 1. check qconfig has been propagated
    assert isinstance(qat_model.linear.qconfig, torch.quantization.QConfig)
    assert isinstance(qat_model.linear_relu.qconfig, torch.quantization.QConfig)
    assert isinstance(qat_model.submodule.linear.qconfig, torch.quantization.QConfig)
    assert isinstance(qat_model.submodule.linear_relu.qconfig, torch.quantization.QConfig)

    # 2. Test proper submodule substitution
    assert type(qat_model.linear) == QATOrthogonalLinear
    assert type(qat_model.linear_relu) == QATOrthogonalLinearReLU
    assert type(qat_model.submodule.linear) == QATOrthogonalLinear
    assert type(qat_model.submodule.linear_relu) == QATOrthogonalLinearReLU


@pytest.mark.xfail
def test_convert_qat():
    model = QATSubstitutionModule()
    qconfig = torch.quantization.get_default_qat_qconfig("fbgemm")
    model.qconfig = qconfig
    # prepare_qat does following things:
    # 1. Propagate qconfig to submodules
    # 2. Substitute modules with their qat twins
    torch.quantization.prepare_qat(model, inplace=True)
    torch.quantization.convert(model, inplace=True)

    # 2. Test proper submodule substitution
    assert type(model.linear) == QuantizedOrthogonalLinear
    assert type(model.linear_relu) == QuantizedOrthogonalLinearReLU
    assert type(model.submodule.linear) == QuantizedOrthogonalLinear
    assert type(model.submodule.linear_relu) == QuantizedOrthogonalLinearReLU


@pytest.mark.xfail
def test_qat_training():
    # This test checks that qat works equally for our orthogonal layers
    # and ordinary (already torch-tested) Linear layers with same weights

    torch.manual_seed(72539)

    model = torch.nn.Sequential(
        torch.quantization.QuantStub(),
        OrthogonalLinear(10, 7),
        torch.nn.ReLU(),
        OrthogonalLinearReLU(7, 5),
        OrthogonalLinear(5, 3),
        torch.quantization.DeQuantStub(),
    )

    reference_model = torch.nn.Sequential(
        torch.quantization.QuantStub(),
        torch.nn.Linear(10, 7),
        torch.nn.ReLU(),
        nni.LinearReLU(torch.nn.Linear(7, 5), torch.nn.ReLU()),
        torch.nn.Linear(5, 3),
        torch.quantization.DeQuantStub(),
    )

    with torch.no_grad():
        for i in [1, 4]:
            reference_model[i].weight.copy_(model[i].weight)
            reference_model[i].bias.copy_(model[i].bias)

        reference_model[3][0].weight.copy_(model[3].weight)
        reference_model[3][0].bias.copy_(model[3].bias)

    inputs = torch.rand(10, 10)

    assert torch.allclose(model(inputs), reference_model(inputs))

    reference_out = model(inputs)

    # Reference models are equal - let's prepare both for qat-training, pass inputs
    # and compare outputs. They should be same (but not equal to float inputs)
    model.qconfig = torch.quantization.get_default_qat_qconfig("fbgemm")
    reference_model.qconfig = torch.quantization.get_default_qat_qconfig("fbgemm")
    torch.quantization.prepare_qat(model, inplace=True)
    torch.quantization.prepare_qat(reference_model, inplace=True)

    assert torch.allclose(model(inputs), reference_model(inputs))
    # Fast sanity check that our model really quantizes
    assert not torch.allclose(model(inputs), reference_out)

    # Check that quantization parameters in all observers are *exactly* same
    dct = {}
    torch.quantization.get_observer_dict(mod=model, target_dict=dct)
    reference_dct = {}
    torch.quantization.get_observer_dict(mod=reference_model, target_dict=reference_dct)

    assert list(reference_dct.keys()) == list(dct.keys())

    for key in reference_dct.keys():
        observer = dct[key]
        reference_observer = reference_dct[key]

        def _get_tensors(x):
            return dict(itertools.chain(x.named_buffers(), x.named_parameters()))

        observer_buffers = _get_tensors(observer)
        reference_buffers = _get_tensors(reference_observer)

        assert set(observer_buffers.keys()) == set(reference_buffers.keys())

        for buf_name in observer_buffers.keys():
            assert torch.allclose(observer_buffers[buf_name], reference_buffers[buf_name])

    # Finally - convert them to quantized variants and check outputs

    torch.quantization.convert(model, inplace=True)
    torch.quantization.convert(reference_model, inplace=True)

    assert torch.allclose(model(inputs), reference_model(inputs))


###########################################################
#                      EXTEND LAYER TEST                  #
###########################################################


@pytest.mark.parametrize('cls', [OrthogonalLinear, OrthogonalLinearReLU])
def test_extend_negative(cls):
    model = cls(5, 5)
    opt = torch.optim.Adam(model.parameters())
    with pytest.raises(ValueError):
        extend_inputs_orthogonal_linear(model, opt, 0, additional_inputs=-1)


@pytest.mark.parametrize('cls', [OrthogonalLinear, OrthogonalLinearReLU])
@pytest.mark.parametrize('position', [-1, 11])
def test_extend_bad_position(cls, position):
    model = cls(10, 1)
    opt = torch.optim.Adam(model.parameters())
    with pytest.raises(ValueError):
        extend_inputs_orthogonal_linear(model, opt, position, additional_inputs=5)


@pytest.mark.parametrize('new_count', [0, 4])
@pytest.mark.parametrize('cls', [OrthogonalLinear, OrthogonalLinearReLU])
@pytest.mark.parametrize('position', [0, 6, 11])
def test_extend_layer(cls, new_count, position):
    in_features = 11
    out_features = 5
    model = cls(in_features, out_features)

    opt = torch.optim.Adam(model.parameters())
    inputs = torch.rand(20, in_features)
    targets = torch.rand(20, out_features)
    loss = torch.nn.MSELoss()
    for _ in range(3):
        model.zero_grad()
        loss_val = loss(model(inputs), targets)
        loss_val.backward()
        opt.step()

    prev_weight = model.weight
    output = model(inputs)

    extend_inputs_orthogonal_linear(model=model, optimizer=opt, position=position, additional_inputs=new_count)

    new_weight = model.weight
    old_weight = torch.cat(
        [new_weight[:, :position], new_weight[:, position + new_count:]],
        dim=1
    )

    assert new_weight.size() == (out_features, in_features + new_count)

    assert torch.allclose(prev_weight, old_weight)
    assert torch.allclose(new_weight[:, position:position + new_count], torch.zeros(out_features, new_count))

    assert model.in_features == in_features + new_count
    assert torch.allclose(output, model(
        torch.cat([inputs[:, :position], torch.rand(20, new_count), inputs[:, position:]], dim=1)))

    assert model.V in set(opt.__getstate__()["state"].keys())
    for k, v in opt.__getstate__()["state"][model.V].items():
        if isinstance(v, torch.Tensor):
            assert torch.allclose(v[:, position:position + new_count], torch.zeros(out_features, new_count))


@pytest.mark.requires_cuda
def test_half():
    device = torch.device("cuda", 0)
    float_module = OrthogonalLinear(in_features=640, out_features=320)
    half_module = OrthogonalLinear(in_features=640, out_features=320)
    half_module.load_state_dict(float_module.state_dict())

    half_module.to(dtype=torch.half, device=device)
    float_module.to(device=device)

    # check precision issues
    assert half_module.V.dtype == torch.float

    float_inputs = torch.rand(1000, 640).to(device)
    half_inputs = float_inputs.clone().to(torch.half)

    with CUDADeviceMemoryProfiler(device=device) as half_stats:
        half_module(half_inputs)

    with CUDADeviceMemoryProfiler(device=device) as float_stats:
        float_module(float_inputs)

    float_max_ram = float_stats.get().allocated_bytes.all.peak
    half_max_ram = half_stats.get().allocated_bytes.all.peak

    assert half_max_ram < float_max_ram


###########################################################
#                 BATCH ORTHOGONALIZATION TEST            #
###########################################################

# StackTensorsWithZeroPadding
# BatchedCopyViewLike


def test_stack_tensors_zero_pad_grad():
    device = torch.device("cuda", 0)
    tensors = [
        torch.rand(10, 20, device=device, requires_grad=True),
        torch.rand(5, 3, device=device, requires_grad=True),
        torch.rand(3, 28, device=device, requires_grad=True)
    ]

    targets = torch.rand(3, 10, 28, device=device)
    loss = torch.nn.MSELoss()

    res = StackTensorsWithZeroPadding.apply(*tensors)
    assert res.size() == (3, 10, 28)
    loss(res.view(-1), targets.view(-1)).backward()

    gradients = [t.grad for t in tensors]
    for t in tensors:
        t.grad = None

    def _expand_zeros(p):
        ttt = torch.zeros(10, 28, device=device)
        ttt[:p.size()[0], :p.size()[1]] = p
        return ttt

    reference = torch.stack([_expand_zeros(p) for p in tensors])
    assert torch.allclose(res, reference)
    loss(reference.view(-1), targets.view(-1)).backward()

    reference_gradients = [t.grad for t in tensors]
    for g1, g2 in zip(gradients, reference_gradients):
        assert torch.allclose(g1, g2)


def test_batched_copy_view_like():
    torch.manual_seed(358275)
    # device = torch.device("cuda", 0)
    device = torch.device("cuda", 0)
    sizes = [
        (3, 10),
        (5, 3),
        (11, 18)
    ]
    batch = torch.rand(3, 11, 18, device=device, requires_grad=True)
    t1, t2, t3 = BatchedCopyViewLike.apply(batch, sizes)
    for tensor in [t1, t2, t3]:
        assert tensor.requires_grad

    t1.sum().backward()
    assert batch.grad is not None


class BatchOrthogonalModel(torch.nn.Module):
    def __init__(self):
        super(BatchOrthogonalModel, self).__init__()
        self.model = torch.nn.Sequential(
            OrthogonalLinearReLU(in_features=10, out_features=15, implicit_expand_transpose=True),
            OrthogonalLinearReLU(in_features=15, out_features=7, implicit_expand_transpose=True),
            OrthogonalLinearReLU(in_features=7, out_features=20, implicit_expand_transpose=True),
            OrthogonalLinearReLU(in_features=20, out_features=30, implicit_expand_transpose=True),
            OrthogonalLinear(in_features=30, out_features=1)
        )
        # batched orthogonalization should propagate gradients only for really used parameters
        self.unused_layer = OrthogonalLinearReLU(in_features=4, out_features=4)
        self.calculated_but_unused_layer = OrthogonalLinearReLU(in_features=10, out_features=9)

    def forward(self, tensor):
        uncalced = self.calculated_but_unused_layer(tensor)
        return self.model(tensor)


@pytest.mark.parametrize('device', _FWD_DEVICES)
@pytest.mark.parametrize("cpp_mode", [True, False], ids=["C++", "Python"])
def test_batch_orthogonalization_forward(device, cpp_mode):
    torch.manual_seed(32648767)
    model = BatchOrthogonalModel().to(device)
    batched_model = deepcopy(model)

    inputs = torch.rand(1, 10).to(device)

    reference = model(inputs)
    prepare_model_for_batched_orthogonalization(model=batched_model, cpp_mode=cpp_mode)
    batched = batched_model(inputs)

    assert torch.allclose(reference, batched)

    # Validate implementation of cleaning up after forward - all _batch_stuffs should be Nones
    for module in zip(model.modules(), batched_model.modules()):
        if isinstance(module, OrthogonalLinear):
            assert module._batch_stuff is None


@pytest.mark.parametrize("cpp_mode", [True, False], ids=["C++", "Python"])
@pytest.mark.parametrize('device', _FWD_DEVICES)
def test_batch_orthogonalization_backward(device, cpp_mode):
    torch.manual_seed(32648767)
    model = BatchOrthogonalModel().to(device)
    batched_model = deepcopy(model)

    model_dict = dict(model.named_parameters())
    batched_model_dict = dict(batched_model.named_parameters())

    inputs = torch.rand(1, 10).to(device)

    model(inputs).sum().backward()
    prepare_model_for_batched_orthogonalization(model=batched_model, cpp_mode=cpp_mode)
    batched_model(inputs).sum().backward()

    for p in itertools.chain(
        model.unused_layer.parameters(),
        model.calculated_but_unused_layer.parameters(),
        batched_model.unused_layer.parameters(),
        batched_model.calculated_but_unused_layer.parameters()
    ):
        assert p.grad is None

    for n, p in batched_model.model.named_parameters():
        print(f"{n}: {p.grad is None}")

    for p in itertools.chain(
        model.model.parameters(),
        batched_model.model.parameters()
    ):
        assert p.grad is not None

    for name in model_dict.keys():
        if model_dict[name].grad is None and batched_model_dict[name].grad is None:
            continue
        if not torch.allclose(model_dict[name].grad, batched_model_dict[name].grad, atol=1e-6):
            raise RuntimeError(f"Different for {name}")

    # Validate implementation of cleaning up after forward - all _batch_stuffs should be Nones
    for module in zip(model.modules(), batched_model.modules()):
        if isinstance(module, OrthogonalLinear):
            assert module._batch_stuff is None


@pytest.mark.parametrize("cpp_mode", [True, False], ids=["C++", "Python"])
@pytest.mark.parametrize('device', _FWD_DEVICES)
def test_batch_orthogonalization_training(device, cpp_mode):
    torch.manual_seed(2875823)
    iter_count = 10

    model = BatchOrthogonalModel().to(device)
    batched_model = deepcopy(model)

    model_dict = dict(model.named_parameters())
    batched_model_dict = dict(batched_model.named_parameters())

    inputs_list = [torch.rand(10, 10).to(device) for _ in range(iter_count)]
    targets_list = [torch.rand(10).to(device) for _ in range(iter_count)]
    loss = torch.nn.MSELoss()

    optimizer = torch.optim.SGD(list(model.parameters()) + list(batched_model.parameters()), lr=0.1)

    usual_losses = []
    batched_losses = []
    for inputs, targets in zip(inputs_list, targets_list):
        for module in zip(model.modules(), batched_model.modules()):
            if isinstance(module, OrthogonalLinear):
                assert module._batch_stuff is None

        model.zero_grad(set_to_none=True)
        batched_model.zero_grad(set_to_none=True)

        _loss = loss(model(inputs), targets)
        usual_losses.append(_loss.item())
        _loss.backward()

        prepare_model_for_batched_orthogonalization(model=batched_model, cpp_mode=cpp_mode)
        _loss = loss(batched_model(inputs), targets)
        batched_losses.append(_loss.item())
        _loss.backward()

        for p in itertools.chain(
            model.unused_layer.parameters(),
            model.calculated_but_unused_layer.parameters(),
            batched_model.unused_layer.parameters(),
            batched_model.calculated_but_unused_layer.parameters()
        ):
            assert p.grad is None

        for name in model_dict.keys():
            if model_dict[name].grad is None and batched_model_dict[name].grad is None:
                continue
            nonecount = (batched_model_dict[name].grad is None) + (model_dict[name].grad is None)
            if nonecount == 1:
                raise RuntimeError(name)
            assert torch.allclose(model_dict[name].grad, batched_model_dict[name].grad, atol=1e-6)

        # Validate implementation of cleaning up after forward - all _batch_stuffs should be Nones
        for module in zip(model.modules(), batched_model.modules()):
            if isinstance(module, OrthogonalLinear):
                assert module._batch_stuff is None

        optimizer.step()

    for _loss, _batched_loss in zip(usual_losses, batched_losses):
        assert abs(round(_loss - _batched_loss, 6)) == 0.
