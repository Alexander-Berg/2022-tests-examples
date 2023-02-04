import pytest
import torch
import io
from ads_pytorch.core import BaseParameterServerModule
from ads_pytorch.model_calcer.calcer import CalcerResults
from ads_pytorch.model_calcer.factory import MinibatchPoolFactory
from ads_pytorch.model_calcer.minibatch_worker import (
    AbstractCalcerResultsHandler
)
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.model_calcer.utils.create_optimizer_cache import preallocate_optimizer_buffers


@pytest.fixture(params=[True, False], ids=['Train', 'Eval'])
def train_mode(request):
    return request.param


_DEVICES = [
    torch.device("cpu"),
    pytest.param(torch.device("cuda", 0), marks=pytest.mark.requires_cuda)
]
_DEVICE_IDS = [
    "CPU",
    "CUDA"
]


@pytest.fixture(params=_DEVICES, ids=_DEVICE_IDS)
def device(request):
    return request.param


"""
WARNING!!!
This test takes into account a small hack we do to support multiprocessing:
optimizer buffer preallocation via one step with zero gradient
"""


class TestOptimizer(torch.optim.SGD):
    def step(self, closure=None):
        """Performs a single optimization step.

        Arguments:
            closure (callable, optional): A closure that reevaluates the model
                and returns the loss.
        """
        loss = None
        if closure is not None:
            loss = closure()

        for group in self.param_groups:
            weight_decay = group['weight_decay']
            momentum = group['momentum']
            dampening = group['dampening']
            nesterov = group['nesterov']

            for p in group['params']:
                if p.grad is None:
                    continue
                d_p = p.grad.data
                if weight_decay != 0:
                    d_p.add_(weight_decay, p.data)
                if momentum != 0:
                    param_state = self.state[p]
                    if 'momentum_buffer' not in param_state:
                        param_state['momentum_buffer'] = torch.clone(d_p).detach()
                    if torch.allclose(p.grad, torch.zeros_like(p.grad)):
                        continue
                    buf = param_state['momentum_buffer']
                    buf.mul_(momentum).add_(1 - dampening, d_p)
                    if nesterov:
                        d_p = d_p.add(momentum, buf)
                    else:
                        d_p = buf

                if not torch.allclose(p.grad, torch.zeros_like(p.grad)):
                    p.data.add_(-group['lr'], d_p)

        return loss


class LossCollector(AbstractCalcerResultsHandler):
    def __init__(self):
        self.calls = []

    async def __call__(self, worker_identity: int, calcer_res: CalcerResults, batch: MinibatchRecord):
        self.calls.append(calcer_res)


class PSModel(BaseParameterServerModule):
    def __init__(self):
        super(PSModel, self).__init__()
        self.net = torch.nn.Linear(10, 1)

    def async_forward(self, inputs):
        return inputs

    def sync_forward(self, async_outputs):
        return self.net(async_outputs)


@pytest.mark.parametrize(
    'model_type',
    ['torch_module', 'parameter_server']
)
@pytest.mark.asyncio
async def test_training_continue_with_preloaded_model(device, model_type):
    torch.manual_seed(12345)

    batch_size = 50
    iter_count = 8
    features_count = 10

    def _make_model():
        model = torch.nn.Linear(features_count, 1) if model_type == "torch_module" else PSModel()
        model.parameter_server_mode = True
        model = model.to(device)
        return model

    def _make_optim(model):
        return TestOptimizer(model.parameters(), lr=0.03, momentum=0.9)

    model = _make_model()
    loss = torch.nn.MSELoss()
    optim = _make_optim(model=model)

    inputs = torch.rand(batch_size, features_count)
    inputs.requires_grad = False
    targets = torch.rand(batch_size)

    inputs = inputs.to(device)
    targets = targets.to(device)

    reference_losses = []
    saved_model = None
    saved_optimizer = None
    snapshot_iter = 2
    snapshot_loss = None
    for i in range(iter_count):
        model.zero_grad()
        optim.zero_grad()

        if i == snapshot_iter:
            raw_save = io.BytesIO()
            torch.save(model.state_dict(), raw_save)
            saved_model = raw_save.getvalue()

            raw_save = io.BytesIO()
            torch.save(optim.state_dict(), raw_save)
            saved_optimizer = raw_save.getvalue()

        model.zero_grad()
        optim.zero_grad()
        output = loss(model(inputs), targets)
        if i == snapshot_iter:
            snapshot_loss = float(output)
        reference_losses.append(float(output))
        output.backward()
        optim.step()

    # Okay, not create worker with fresh(!) zero-initialized model
    # and check that load from snapshot works correctly
    # We load from snapshot before starting worker

    worker_model = _make_model()
    worker_loss = torch.nn.MSELoss()
    worker_optim = _make_optim(model=worker_model)
    for p in worker_model.parameters():
        torch.nn.init.zeros_(p)

    # sanity check
    for p1, p2 in zip(model.parameters(), worker_model.parameters()):
        assert not torch.allclose(p1, p2)

    worker_model.load_state_dict(torch.load(io.BytesIO(saved_model)))
    worker_optim.load_state_dict(torch.load(io.BytesIO(saved_optimizer)))

    loss_collector = LossCollector()
    worker_pool = MinibatchPoolFactory(
        model=worker_model,
        loss=worker_loss,
        deep_optimizers=[worker_optim],
        hash_embedding_optimizers=[],
        calcer_results_handlers=[loss_collector],
        train_mode=True,
        allow_async_gpu_update=False,
        devices=frozenset(({device})),
    )()

    async with worker_pool:
        for _ in range(iter_count - snapshot_iter):
            await worker_pool.assign_job(MinibatchRecord(inputs=inputs, targets=targets))

    pool_losses = [x.losses["Loss"] for x in loss_collector.calls]
    assert round(pool_losses[0], 6) == round(snapshot_loss, 6)
    for loss_val, reference_val in zip(pool_losses, reference_losses[snapshot_iter:]):
        assert round(loss_val, 6) == round(reference_val, 6)


@pytest.mark.parametrize(
    'model_type',
    ['torch_module', 'parameter_server']
)
@pytest.mark.asyncio
async def test_model_and_optimizer_are_shared(
    device,
    model_type,
):
    torch.manual_seed(12345)

    batch_size = 50
    iter_count = 15
    features_count = 10

    def _make_model():
        model = torch.nn.Linear(features_count, 1) if model_type == "torch_module" else PSModel()
        model.parameter_server_mode = True
        model = model.to(device)
        return model

    def _make_optim(model):
        return torch.optim.Adam(model.parameters())

    model = _make_model()
    worker_model = _make_model()
    worker_model.load_state_dict(model.state_dict())
    for p1, p2 in zip(worker_model.parameters(), model.parameters()):
        assert torch.allclose(p1.cpu(), p2.cpu())
    loss = torch.nn.MSELoss()
    optim = _make_optim(model=model)

    preallocate_optimizer_buffers(updated_parameters=list(model.parameters()), optimizers=[optim])

    inputs = torch.rand(batch_size, features_count)
    inputs.requires_grad = False
    targets = torch.rand(batch_size)

    inputs = inputs.to(device)
    targets = targets.to(device)

    for i in range(iter_count):
        model.zero_grad()
        optim.zero_grad()
        output = loss(model(inputs), targets)
        output.backward()
        optim.step()

    # Okay - now let's run the same thing through minibatch worker and test that
    # state dicts of the models match (this will mean that all changes during training
    # are reflected on out model and optimizer)

    for p1, p2 in zip(worker_model.parameters(), model.parameters()):
        assert not torch.allclose(p1, p2)

    worker_optim = torch.optim.Adam(worker_model.parameters())
    worker_loss = torch.nn.MSELoss()

    worker_pool = MinibatchPoolFactory(
        model=worker_model,
        loss=worker_loss,
        deep_optimizers=[worker_optim],
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        train_mode=True,
        devices=frozenset(({device})),
        allow_async_gpu_update=False,
    )()

    async with worker_pool:
        for i in range(iter_count):
            await worker_pool.assign_job(MinibatchRecord(inputs=inputs, targets=targets))

    # Test optimizer is shared
    for state_dct1, state_dict2 in zip(optim.state.values(), worker_optim.state.values()):
        for tensor1, tensor2 in zip(state_dct1.values(), state_dict2.values()):
            if isinstance(tensor1, torch.Tensor):
                assert torch.allclose(tensor1.cpu(), tensor2.cpu())

    # Test model is shared
    for p1, p2 in zip(model.parameters(), worker_model.parameters()):
        assert torch.allclose(p1.cpu(), p2.cpu())
