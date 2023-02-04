import torch
import torch.nn.intrinsic.quantized as nniq
import torch.quantization
import pytest
import copy
from ads_pytorch.nn.module.densenet import (
    DenseNet,
    DenseNetEmbeddingNetwork,
    WeightNormalizedEmbeddingNetwork,
    OrthogonalLinearReLUDensenetFactory,
    IDenseNetLayerFactory
)
from ads_pytorch.nn.module.orthogonal_linear import OrthogonalLinearReLU
from ads_pytorch.nn.optim.shared_adam import SharedAdam
from ads_pytorch.tools.memory_profiler import CUDADeviceMemoryProfiler


@pytest.fixture(autouse=True)
def set_seed():
    torch.manual_seed(2952837)
    yield


def test_orthogonal_linear_factory_creation():
    linear_relu = OrthogonalLinearReLUDensenetFactory().create_layer(in_features=7, out_features=4)
    assert isinstance(linear_relu, OrthogonalLinearReLU)
    assert linear_relu.in_features == 7
    assert linear_relu.out_features == 4


def test_orthogonal_linear_factory_expand():
    factory = OrthogonalLinearReLUDensenetFactory()
    layer = factory.create_layer(in_features=7, out_features=4)
    optimizer = torch.optim.Adam(layer.parameters())

    # init optimizer state
    layer(torch.rand(10, 7)).sum().backward()
    optimizer.step()

    factory.extend_inputs_size(layer=layer, optimizer=optimizer, position=3, new_inputs_size=10)
    assert layer.in_features == 17
    assert layer.out_features == 4

    # Check optimizer ok
    layer(torch.rand(10, 17)).sum().backward()
    optimizer.step()


@pytest.fixture(scope='module', autouse=True)
def reset_seed():
    torch.manual_seed(756)


def test_densenet():
    inputs = torch.rand(100, 10)
    densenet = DenseNet(in_features=10, width=10, depth=10)
    res = densenet(inputs)
    assert res.size() == (100, densenet.out_features)


@pytest.mark.xfail
def test_quantize_densenet():
    inputs = torch.rand(100, 10)
    # densenet itself should not have quantize/dequantize stubs, but should work in quantized
    # models
    model = torch.nn.Sequential(
        torch.quantization.QuantStub(),
        DenseNet(in_features=10, width=10, depth=10),
        torch.quantization.DeQuantStub()
    )
    model.qconfig = torch.quantization.get_default_qat_qconfig("fbgemm")

    torch.quantization.prepare_qat(model, inplace=True)
    model(inputs)
    torch.quantization.convert(model, inplace=True)

    for layer in model[1].layers:
        assert isinstance(layer, nniq.LinearReLU)

    model(inputs)


def test_weight_normalized_network():
    inputs = torch.rand(100, 10)
    densenet = WeightNormalizedEmbeddingNetwork(in_features=10, width=10, depth=10, split_depth=3)
    res = densenet(inputs)
    assert res.size() == (100, densenet.out_features)


def test_weight_normalized_network_without_intermediate_densenet():
    inputs = torch.rand(100, 10)
    depth = 10
    densenet = WeightNormalizedEmbeddingNetwork(in_features=10, width=10, depth=depth, split_depth=depth)
    res = densenet(inputs)
    assert res.size() == (100, densenet.out_features)


def test_quantize_weight_normalized_network():
    pass


@pytest.mark.parametrize('position', [-1, 11])
def test_densenet_add_feature_bad_position(position):
    densenet = DenseNet(10, 10, 10)
    opt = torch.optim.Adam(densenet.parameters())
    with pytest.raises(AssertionError):
        densenet.add_feature(input_position=position, dim=10, optimizer=opt)


@pytest.mark.parametrize('new_count', [0, 4])
@pytest.mark.parametrize('position', [0, 6, 12])
@pytest.mark.parametrize('opt_cls', [torch.optim.Adam, SharedAdam])
def test_densenet_add_feature(new_count, position, opt_cls):
    in_features = 12
    depth = 10
    width = 10
    model = DenseNet(in_features, depth, width)
    opt = opt_cls(model.parameters())
    inputs = torch.rand(100, in_features)
    targets = torch.cat([inputs, torch.rand(100, depth * width)], dim=1)
    loss = torch.nn.MSELoss()
    for _ in range(10):
        model.zero_grad()
        loss_val = loss(model(inputs), targets)
        loss_val.backward()
        opt.step()

    prev_output = model(inputs)
    model.add_feature(position, new_count, opt)
    assert model.in_features == in_features + new_count
    assert model.out_features == in_features + new_count + depth * width
    additional_input = torch.rand(100, new_count)
    new_inputs = torch.cat([inputs[:, :position], additional_input, inputs[:, position:]], dim=1)
    new_output = model(new_inputs)
    expected_output = torch.cat([prev_output[:, :position], additional_input, prev_output[:, position:]], dim=1)
    assert torch.allclose(new_output, expected_output, atol=1e-6)

    parameters_set = set(model.parameters())
    assert len(parameters_set.symmetric_difference(opt.param_groups[0]["params"])) == 0
    assert len(parameters_set.symmetric_difference(opt.__getstate__()["state"].keys())) == 0

    new_targets = torch.cat([new_inputs, torch.rand(100, depth * width)], dim=1)
    float_loss = float(loss(model(new_inputs), new_targets))
    for _ in range(10):
        model.zero_grad()
        loss_val = loss(model(new_inputs), new_targets)
        loss_val.backward()
        opt.step()
    assert float(loss_val) < float_loss


@pytest.mark.parametrize('new_count', [0, 4])
@pytest.mark.parametrize('position', [0, 6, 12])
@pytest.mark.parametrize('opt_cls', [torch.optim.Adam, SharedAdam])
def test_weight_normalized_network_add_feature(new_count, position, opt_cls):
    in_features = 12
    out_features = 20
    depth = 10
    width = 10
    model = WeightNormalizedEmbeddingNetwork(in_features, depth, width, out_features=out_features)
    opt = opt_cls(model.parameters())
    inputs = torch.rand(100, in_features)
    targets = torch.rand(100, out_features)
    loss = torch.nn.MSELoss()
    for _ in range(10):
        model.zero_grad()
        loss_val = loss(model(inputs), targets)
        loss_val.backward()
        opt.step()

    prev_output = model(inputs)
    model.add_feature(new_count, opt, position)
    assert model.in_features == in_features + new_count
    assert model.out_features == out_features
    additional_input = torch.rand(100, new_count)
    new_inputs = torch.cat([inputs[:, :position], additional_input, inputs[:, position:]], dim=1)
    new_output = model(new_inputs)
    assert torch.allclose(new_output, prev_output, atol=1e-6)

    parameters_set = set(model.parameters())
    assert len(parameters_set.symmetric_difference(opt.param_groups[0]["params"])) == 0
    assert len(parameters_set.symmetric_difference(opt.__getstate__()["state"].keys())) == 0

    float_loss = float(loss(model(new_inputs), targets))
    for _ in range(10):
        model.zero_grad()
        loss_val = loss(model(new_inputs), targets)
        loss_val.backward()
        opt.step()
    assert float(loss_val) < float_loss


########################################################################
#                                 FAST                                 #
########################################################################


@pytest.mark.parametrize(
    'inputs',
    [
        # must test separately because ordinary linear densenet has performance hacks for tensors
        torch.rand(10, 13),       # matrix case
        torch.rand(2, 3, 4, 13)   # arbirary tensor
    ],
    ids=["Matrix", "Tensor"]
)
@pytest.mark.parametrize('depth', [1, 10])
def test_densenet_fast_mode_no_inputs_backprop(inputs, depth):
    inputs.requires_grad = False
    common_kwargs = dict(
        in_features=inputs.size()[-1],
        depth=depth,
        width=2
    )
    slow = DenseNet(**common_kwargs, fast=False)
    fast = DenseNet(**common_kwargs, fast=True)
    fast.load_state_dict(slow.state_dict())

    slow_out = slow(inputs)
    fast_out = fast(inputs)

    assert torch.allclose(slow_out, fast_out)

    gradient = torch.rand_like(slow_out)
    torch.autograd.backward(tensors=slow_out, grad_tensors=[gradient])
    torch.autograd.backward(tensors=fast_out, grad_tensors=[gradient])

    for slow_p, fast_p in zip(slow.parameters(), fast.parameters()):
        assert torch.allclose(slow_p.grad, fast_p.grad)

    assert inputs.grad is None


@pytest.mark.parametrize(
    'inputs',
    [
        # must test separately because ordinary linear densenet has performance hacks for tensors
        torch.rand(10, 13),       # matrix case
        torch.rand(2, 3, 4, 13)   # arbirary tensor
    ],
    ids=["Matrix", "Tensor"]
)
@pytest.mark.parametrize('depth', [1, 10])
def test_densenet_fast_mode_inputs_backprop(inputs, depth):
    inputs.requires_grad = False
    dim = inputs.size()[-1]

    # Insert network before densenets to check backpropagation
    slow_before_net = torch.nn.Sequential(
        torch.nn.Linear(dim, dim),
        torch.nn.Tanh(),
        torch.nn.Linear(dim, dim),
        torch.nn.Tanh()
    )
    fast_before_net = copy.deepcopy(slow_before_net)

    common_kwargs = dict(in_features=dim, depth=depth, width=2)
    fast = torch.nn.Sequential(fast_before_net, DenseNet(**common_kwargs, fast=True))
    slow = torch.nn.Sequential(slow_before_net, DenseNet(**common_kwargs, fast=False))

    fast.load_state_dict(slow.state_dict())

    slow_out = slow(inputs)
    fast_out = fast(inputs)

    assert torch.allclose(slow_out, fast_out)

    gradient = torch.rand_like(slow_out)
    torch.autograd.backward(tensors=slow_out, grad_tensors=[gradient])
    torch.autograd.backward(tensors=fast_out, grad_tensors=[gradient])

    for slow_p, fast_p in zip(slow.parameters(), fast.parameters()):
        assert torch.allclose(slow_p.grad, fast_p.grad)


@pytest.mark.parametrize(
    'inputs',
    [
        # must test separately because ordinary linear densenet has performance hacks for tensors
        torch.rand(10, 13),       # matrix case
        torch.rand(2, 3, 4, 13)   # arbirary tensor
    ],
    ids=["Matrix", "Tensor"]
)
@pytest.mark.parametrize("backprop_mode", ["sum", "separate"])
@pytest.mark.parametrize('depth', [1, 10])
def test_densenet_fast_mode_diamond_backprop(inputs, depth, backprop_mode):
    inputs.requires_grad = False
    dim = inputs.size()[-1]

    class DiamondDensenet(torch.nn.Module):
        def __init__(self, fast: bool):
            super(DiamondDensenet, self).__init__()
            self.net1 = torch.nn.Sequential(
                torch.nn.Linear(dim, dim),
                torch.nn.Tanh(),
                torch.nn.Linear(dim, dim),
                torch.nn.Tanh()
            )
            self.net2 = torch.nn.Sequential(
                torch.nn.Linear(dim, dim),
                torch.nn.ReLU(),
                torch.nn.Linear(dim, dim)
            )
            self.densenet = DenseNet(in_features=dim, depth=depth, width=2, fast=fast)

            self.out_net1 = torch.nn.Linear(self.densenet.out_features, 1)
            self.out_net2 = torch.nn.Linear(self.densenet.out_features, 1)

        def forward(self, tensor):
            net1 = self.out_net1(self.densenet(self.net1(tensor)))
            net2 = self.out_net2(self.densenet(self.net2(tensor)))
            return net1, net2

    # Insert network before densenets to check backpropagation
    fast = DiamondDensenet(fast=True)
    slow = DiamondDensenet(fast=False)

    fast.load_state_dict(slow.state_dict())

    loss = torch.nn.MSELoss()
    target1 = torch.rand(*inputs.size()[:-1], 1)
    target2 = torch.rand(*inputs.size()[:-1], 1)

    def _run_backprop(net_outs):
        out1, out2 = net_outs
        if backprop_mode == "sum":
            (loss(out1, target1) + loss(out2, target2)).backward()
        else:
            loss(out1, target1).backward(retain_graph=True)
            loss(out2, target2).backward(retain_graph=True)

    _run_backprop(slow(inputs))
    _run_backprop(fast(inputs))

    for slow_p, fast_p in zip(slow.parameters(), fast.parameters()):
        assert torch.allclose(slow_p.grad, fast_p.grad, atol=1e-5)


########################################################################
#                                 HALF                                 #
########################################################################


# We should properly sum all inputs after all nonlinearities
# If we have failed our *alignment*, this layer will indicate about it
class AllOnesFactor(IDenseNetLayerFactory):
    def create_layer(self, in_features: int, out_features: int) -> torch.nn.Module:
        layer = torch.nn.Linear(in_features=in_features, out_features=out_features, bias=False)
        torch.nn.init.constant_(layer.weight, 1)
        return torch.nn.Sequential(layer, torch.nn.Tanh())

    def extend_inputs_size(self, layer: torch.nn.Module, optimizer: torch.optim.Optimizer, position: int,
                           new_inputs_size: int) -> None:
        raise NotImplementedError


@pytest.mark.requires_cuda
@pytest.mark.parametrize('fast', [True, False], ids=['Fast', 'Usual'])
@pytest.mark.parametrize('in_features', [20, 24], ids=["InUnaligned", "InAligned"])
@pytest.mark.parametrize('dtype', [torch.float], ids=["Float"])
def test_densenet_compute_alignment(dtype, fast, in_features):
    device = torch.device("cuda", 0)

    aligned_model = DenseNet(
        in_features=in_features, depth=3, width=16, fast=fast,
        compute_alignment=8, factory=AllOnesFactor()
    ).to(device=device, dtype=dtype)
    usual_model = DenseNet(
        in_features=in_features, depth=3, width=16, fast=fast,
        compute_alignment=None, factory=AllOnesFactor()
    ).to(device=device, dtype=dtype)

    # Check forward
    with torch.no_grad():
        inputs = torch.rand(128, in_features).to(device=device, dtype=dtype)
        assert torch.allclose(aligned_model(inputs), usual_model(inputs))

    # Check training with backward pass
    # Adding beforenet will also check backpropagation beyond densenet
    aligned_before_net = torch.nn.Sequential(
        torch.nn.Linear(in_features, in_features),
        torch.nn.Tanh(),
        torch.nn.Linear(in_features, in_features),
        torch.nn.Tanh(),
    ).to(device=device, dtype=dtype)
    usual_before_net = copy.deepcopy(aligned_before_net)

    # again, use twice backprop
    inputs = torch.rand(128, in_features).to(device=device, dtype=dtype)
    target1 = torch.rand(128).to(device=device, dtype=dtype)
    target2 = torch.rand(128).to(device=device, dtype=dtype)
    loss = torch.nn.MSELoss(reduction="mean")

    def _onestep(model, optim):
        model.zero_grad()
        output = model(inputs).mean(dim=-1)
        loss(output, target1).backward(retain_graph=True)
        loss(output, target2).backward(retain_graph=True)
        optim.step()
        return output

    aligned_full_model = torch.nn.Sequential(aligned_before_net, aligned_model)
    aligned_optim = torch.optim.Adam(aligned_full_model.parameters())

    usual_full_model = torch.nn.Sequential(usual_before_net, usual_model)
    usual_optim = torch.optim.Adam(usual_full_model.parameters())

    for _ in range(10):
        aligned_out = _onestep(model=aligned_full_model, optim=aligned_optim)
        usual_out = _onestep(model=usual_full_model, optim=usual_optim)
        assert torch.allclose(aligned_out, usual_out, atol=3e-4)

    for p1, p2 in zip(usual_before_net.parameters(), aligned_before_net.parameters()):
        assert torch.allclose(p1, p2, atol=3e-4)


@pytest.mark.parametrize(
    ['cls', 'half_memory', 'float_memory'],
    [
        (DenseNet, 143872, 249856),
        (DenseNetEmbeddingNetwork, 244736, 399872),
        (WeightNormalizedEmbeddingNetwork, 438272, 609280)
    ],
    ids=[
        "DenseNet",
        "DenseNetEmbeddingNetwork",
        "WeightNormalizedEmbeddingNetwork",
    ]
)
@pytest.mark.requires_cuda
def test_half(cls, half_memory, float_memory):
    in_features = 20
    half_model = cls(
        in_features=in_features,
        depth=3,
        width=16,
        fast=True,
        compute_alignment=8
    ).cuda().to(torch.half)
    half_inputs = torch.rand(128, in_features).cuda().half()
    with CUDADeviceMemoryProfiler(device=torch.device("cuda", 0)) as half_stats:
        half_model(half_inputs).sum().backward()
    half_stats = half_stats.get()

    float_model = half_model.to(torch.float)
    float_inputs = half_inputs.to(torch.float)
    with CUDADeviceMemoryProfiler(device=torch.device("cuda", 0)) as float_stats:
        float_model(float_inputs).sum().backward()

    float_stats = float_stats.get()

    float_max_ram = float_stats.allocated_bytes.all.peak
    half_max_ram = half_stats.allocated_bytes.all.peak

    assert half_max_ram < half_memory * 1.1
    assert float_max_ram < float_memory * 1.1
