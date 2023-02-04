import itertools
import pytest
import torch
import torch.nn
from ads_pytorch.hash_embedding.hash_embedding import HashEmbeddingGradientWrapper
from ads_pytorch.cpp_lib import libcpp_lib

from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table,
    create_item
)
from ads_pytorch.hash_embedding.optim import create_optimizer
from ads_pytorch.hash_embedding.lazy_computation_context import LazyComputationContext as PyLazyComputationContext
from ads_pytorch.hash_embedding.lazy_computation_context_cpp import LazyComputationContext as CppLazyComputationContext


def EmbeddingAdamOptimizer(params, **kwargs):
    return create_optimizer(params, "adam", **kwargs)


def allclose_cpu(t1: torch.Tensor, t2: torch.Tensor, rtol=1e-5, atol=1e-8) -> bool:
    return torch.allclose(t1.cpu(), t2.cpu(), rtol=rtol, atol=atol)


@pytest.fixture(
    params=[
        torch.device("cpu"),
        pytest.param(torch.device("cuda", 0), marks=pytest.mark.requires_cuda)
    ], ids=["CPU", "CUDA"]
)
def device(request):
    return request.param


@pytest.fixture(params=[PyLazyComputationContext, CppLazyComputationContext], ids=["Python", "C++"])
def LazyComputationContext(request):
    return request.param


def _create_and_fill_hash_table(table_type, dim, shift=0):
    hash_embedding_table = HashEmbedding(create_hash_table(table_type=table_type, dim=dim))
    for i in range(10):
        item = create_item(table_type=table_type, dim=dim)
        item.w = torch.FloatTensor([shift + i] * dim)
        hash_embedding_table.insert_item(i, item)
    return hash_embedding_table


@pytest.fixture(
    params=[1, 10],
    ids=['SingleThread', 'Parallel']
)
def thread_pool(request):
    return libcpp_lib.ThreadPoolHandle(request.param)


@pytest.fixture(params=['adam'])
def hash_table(request):
    return _create_and_fill_hash_table(table_type=request.param, dim=10, shift=0)


@pytest.fixture
def ps_hash_table(hash_table):
    hash_table.parameter_server_mode = True
    return hash_table


def _manual_forward_pass(data, data_len, dim, shift):
    res = torch.zeros(data_len.size()[0], dim)
    shifted_data = data + shift
    cur_idx = 0
    for i, obj_size in enumerate(data_len):
        res[i, :] = shifted_data[cur_idx: cur_idx + obj_size].sum()
        cur_idx += obj_size

    return res


def test_manual_forward_pass():
    data = torch.LongTensor([5, 6, 7, 8, 9])
    data_len = torch.IntTensor([1, 1, 2, 1])

    dim = 10
    shift = 5

    reference = torch.FloatTensor(
        [
            [shift + 5] * dim,
            [shift + 6] * dim,
            [shift + 7 + shift + 8] * dim,
            [shift + 9] * dim,
        ]
    )

    assert allclose_cpu(_manual_forward_pass(data, data_len, dim=dim, shift=shift), reference)


################################################################
#                     FORWARD PASS TESTING                     #
################################################################


@pytest.mark.asyncio
async def test_context_dont_work_if_no_parameter_server_mode(hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = hash_table(data, data_len).to(device)
    assert allclose_cpu(reference, torch.FloatTensor([[6] * hash_table.dim(), [4] * hash_table.dim()]))

    async with LazyComputationContext(thread_pool=thread_pool, device=device):
        forward_tensor = hash_table(data, data_len)

    assert allclose_cpu(forward_tensor, reference)


@pytest.mark.asyncio
async def test_lazy_context_single_pass(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()]).to(device)

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len)

    # not real computation happened
    await ctx.run_forward_pass()
    assert forward_tensor.requires_grad
    assert allclose_cpu(forward_tensor, reference)


@pytest.mark.asyncio
async def test_lazy_context_multiple_pass_same_size(ps_hash_table, thread_pool, LazyComputationContext, device):
    # Use the same context for testing cache
    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()]).to(device)

    async with ctx:
        forward_tensor = ps_hash_table(data, data_len)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_tensor, reference)

    # Second pass async with other inputs
    second_reference = torch.FloatTensor([[5 + 6] * ps_hash_table.dim(), [7 + 8 + 9] * ps_hash_table.dim()])

    data = torch.LongTensor([5, 6, 7, 8, 9])
    data_len = torch.IntTensor([2, 3])

    async with ctx:
        second_forward_tensor = ps_hash_table(data, data_len)

    await ctx.run_forward_pass()
    assert allclose_cpu(second_forward_tensor, second_reference)

    # Check that these tensors share storage
    assert second_forward_tensor.numel() == forward_tensor.numel()


@pytest.mark.asyncio
async def test_lazy_context_multiple_pass_increase_size(ps_hash_table, thread_pool, LazyComputationContext, device):
    # When size is increases, we have to create a new storage
    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)  # set size to 1 to enable resizing

    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()]).to(device)

    async with ctx:
        forward_tensor = ps_hash_table(data, data_len)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_tensor, reference)

    data = torch.LongTensor(list(range(1000)))
    data_len = torch.IntTensor([1] * 1000)

    async with ctx:
        second_forward_tensor = ps_hash_table(data, data_len)

    # We have changed the size, hence, currently we change the tensor: allzeros and different id
    await ctx.run_forward_pass()


@pytest.mark.asyncio
async def test_lazy_context_multiple_pass_decrease_size(ps_hash_table, thread_pool, LazyComputationContext, device):
    # When size of output tensor decreases, we must use the same shared storage
    # but different size on it
    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()]).to(device)

    async with ctx:
        forward_tensor = ps_hash_table(data, data_len)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_tensor, reference)

    # Second pass async with other inputs
    second_reference = torch.FloatTensor([[6] * ps_hash_table.dim()])

    data = torch.LongTensor([1, 2, 3])
    data_len = torch.IntTensor([3])

    async with ctx:
        second_forward_tensor = ps_hash_table(data, data_len)

    await ctx.run_forward_pass()
    assert allclose_cpu(second_forward_tensor, second_reference)


@pytest.mark.asyncio
async def test_different_hash_map_types_at_same_context(thread_pool, LazyComputationContext, device):
    class MyModels(torch.nn.Module):
        def __init__(self):
            super(MyModels, self).__init__()

            table1 = _create_and_fill_hash_table(table_type="adam", dim=10, shift=0)
            table2 = _create_and_fill_hash_table(table_type="adam", dim=100, shift=20)
            table3 = _create_and_fill_hash_table(table_type="adam_half", dim=10, shift=40)
            table4 = _create_and_fill_hash_table(table_type="adam_half", dim=100, shift=60)

            self._tables_list = torch.nn.ModuleList([table1, table2, table3, table4])
            for t in self._tables_list:
                t.parameter_server_mode = True

        def forward(self, datas):
            return [table(*data) for table, data in zip(self._tables_list, datas)]

    model = MyModels()

    datas = [
        [torch.LongTensor([5, 6, 7, 8, 9]), torch.IntTensor([1, 1, 2, 1])],
        [torch.LongTensor([0, 9, 1, 8, 5]), torch.IntTensor([1, 1, 3])],
        [torch.LongTensor(list(range(9))), torch.IntTensor([9])],
        [torch.LongTensor([5, 6, 7, 8, 9]), torch.IntTensor([1, 1, 2, 1])],
    ]

    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    async with ctx:
        forward_results = model(datas)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_results[0], _manual_forward_pass(*(datas[0]), dim=10, shift=0))
    assert allclose_cpu(forward_results[1], _manual_forward_pass(*(datas[1]), dim=100, shift=20))
    assert allclose_cpu(forward_results[2], _manual_forward_pass(*(datas[2]), dim=10, shift=40))
    assert allclose_cpu(forward_results[3], _manual_forward_pass(*(datas[3]), dim=100, shift=60))

    # test several changes in cache
    datas[0] = [torch.LongTensor([0, 9, 1, 8, 5]), torch.IntTensor([1, 3, 1])]
    datas[1][0] = torch.LongTensor(list(reversed([0, 9, 1, 8, 5])))
    datas[-1] = [torch.LongTensor(list(range(9))), torch.IntTensor([3, 2, 4])]

    async with ctx:
        new_forward_results = model(datas)

    await ctx.run_forward_pass()

    assert allclose_cpu(new_forward_results[0], _manual_forward_pass(*(datas[0]), dim=10, shift=0))
    assert allclose_cpu(new_forward_results[1], _manual_forward_pass(*(datas[1]), dim=100, shift=20))
    assert allclose_cpu(new_forward_results[2], _manual_forward_pass(*(datas[2]), dim=10, shift=40))
    assert allclose_cpu(new_forward_results[3], _manual_forward_pass(*(datas[3]), dim=100, shift=60))


################################################################
#                     FORWARD PASS TESTING WITH WEIGHTS        #
################################################################


@pytest.mark.asyncio
async def test_weighted_context_dont_work_if_no_parameter_server_mode(hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = hash_table(data, data_len, data_weight)
    assert allclose_cpu(reference, torch.FloatTensor([[32.1] * hash_table.dim(), [400] * hash_table.dim()]))

    async with LazyComputationContext(thread_pool=thread_pool, device=device):
        forward_tensor = hash_table(data, data_len, data_weight)

    assert allclose_cpu(forward_tensor, reference)


@pytest.mark.asyncio
async def test_weighted_lazy_context_single_pass(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = torch.FloatTensor([[32.1] * ps_hash_table.dim(), [400] * ps_hash_table.dim()])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    # not real computation happened
    await ctx.run_forward_pass()
    assert forward_tensor.requires_grad
    assert allclose_cpu(forward_tensor, reference)


@pytest.mark.asyncio
async def test_weighted_lazy_context_shared_memory(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    await ctx.run_forward_pass()


@pytest.mark.asyncio
async def test_weighted_lazy_context_multiple_pass_same_size(ps_hash_table, thread_pool, LazyComputationContext, device):
    # Use the same context for testing cache
    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = torch.FloatTensor([[32.1] * ps_hash_table.dim(), [400] * ps_hash_table.dim()])

    async with ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_tensor, reference)

    # Second pass async with other inputs
    second_reference = torch.FloatTensor([[6.5] * ps_hash_table.dim(), [79.8] * ps_hash_table.dim()])

    data = torch.LongTensor([5, 6, 7, 8, 9])
    data_len = torch.IntTensor([2, 3])
    data_weight = torch.FloatTensor([0.1, 1, 10, 0.1, 1])

    async with ctx:
        second_forward_tensor = ps_hash_table(data, data_len, data_weight)

    await ctx.run_forward_pass()
    assert allclose_cpu(second_forward_tensor, second_reference)

    # Check that these tensors share storage
    assert second_forward_tensor.numel() == forward_tensor.numel()


@pytest.mark.asyncio
async def test_weighted_lazy_context_multiple_pass_increase_size(ps_hash_table, thread_pool, LazyComputationContext, device):
    # When size is increases, we have to create a new storage
    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)  # set size to 1 to enable resizing

    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = torch.FloatTensor([[32.1] * ps_hash_table.dim(), [400] * ps_hash_table.dim()])

    async with ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_tensor, reference)

    data = torch.LongTensor(list(range(1000)))
    data_len = torch.IntTensor([1] * 1000)
    data_weight = torch.FloatTensor([1] * 1000)

    async with ctx:
        second_forward_tensor = ps_hash_table(data, data_len, data_weight)

    # We have changed the size, hence, currently we change the tensor: allzeros and different id
    await ctx.run_forward_pass()


@pytest.mark.asyncio
async def test_weighted_lazy_context_multiple_pass_decrease_size(ps_hash_table, thread_pool, LazyComputationContext, device):
    # When size of output tensor decreases, we must use the same shared storage
    # but different size on it
    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = torch.FloatTensor([[32.1] * ps_hash_table.dim(), [400] * ps_hash_table.dim()])

    async with ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    await ctx.run_forward_pass()
    assert allclose_cpu(forward_tensor, reference)

    # Second pass async with other inputs
    second_reference = torch.FloatTensor([[321] * ps_hash_table.dim()])

    data = torch.LongTensor([1, 2, 3])
    data_len = torch.IntTensor([3])
    data_weight = torch.FloatTensor([1, 10, 100])

    async with ctx:
        second_forward_tensor = ps_hash_table(data, data_len, data_weight)

    await ctx.run_forward_pass()
    assert allclose_cpu(second_forward_tensor, second_reference)


################################################################
#                    BACKWARD PASS TESTING                     #
################################################################


@pytest.mark.parametrize('gradient_type', [torch.float32, torch.float16], ids=['Float', 'Half'])
@pytest.mark.asyncio
async def test_single_hash_map_gradient(ps_hash_table, thread_pool, gradient_type, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len)

    # not real computation happened
    await ctx.run_forward_pass()
    assert forward_tensor.requires_grad
    assert allclose_cpu(forward_tensor, reference)
    partial_gradient = torch.ones_like(forward_tensor)
    ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=partial_gradient.to(gradient_type))
    await ctx.run_backward_pass()

    assert ps_hash_table.parameter_with_hash_table.grad is not None


@pytest.mark.asyncio
async def test_single_hash_map_None_gradient(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len)

    # not real computation happened
    await ctx.run_forward_pass()
    assert forward_tensor.requires_grad
    assert allclose_cpu(forward_tensor, reference)
    ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=None)
    await ctx.run_backward_pass()

    assert ps_hash_table.parameter_with_hash_table.grad is None


PARTIAL_GRADIENTS_FOR_REFERENCE = [
    torch.FloatTensor([
        [-1.4798,  0.4873, -3.0128,  0.4439, -1.0148, -0.5429,  0.4307, -1.9257, -0.2145, -1.2620],
        [-0.1282, -0.3737,  0.2058, -0.9300,  0.1142, -0.4450,  0.5224,  0.6058,  0.7472,  0.1542]
    ]),
    torch.FloatTensor([
        [ 0.8316, -0.7800,  1.5491, -2.5018,  0.3033,  0.7151,  1.4408, -1.0293, 0.4153, -0.2036],
        [-1.7423,  2.3303,  1.3145, -0.3961,  0.3589,  1.6166, -1.1585, -0.7145, 0.2891, -1.4571]
    ]),
    torch.FloatTensor([
        [ 0.1555,  1.0464,  1.6063, -0.2856, -0.2989,  0.5253, -0.7018,  0.8564, 0.2693, -0.4151],
        [-0.6616,  1.6264, -0.5764, -0.6060,  1.3400, -2.1391,  0.9819,  1.6378, -0.2963,  1.4026]
    ])
]


@pytest.mark.parametrize('gradient_type', [torch.float32, torch.float16], ids=['Float', 'Half'])
@pytest.mark.parametrize('compute_mode', ['mean', 'sum'])
@pytest.mark.parametrize('count', [1, 3], ids=['SingleGrad', 'SeveralGrads'])
@pytest.mark.asyncio
async def test_hash_map_gradient_calculated_property(ps_hash_table, count, thread_pool, compute_mode, gradient_type, LazyComputationContext, device):
    data_len = torch.IntTensor([3, 1])

    for grad_id, partial_gradient in enumerate(PARTIAL_GRADIENTS_FOR_REFERENCE[:count]):
        data = torch.LongTensor([i + grad_id * 10 for i in range(1, 5)])

        async with LazyComputationContext(thread_pool=thread_pool, max_data_size_per_forward_job=1) as ctx:
            forward_tensor = ps_hash_table(data, data_len, compute_mode=compute_mode)

        await ctx.run_forward_pass()
        ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=partial_gradient.to(gradient_type))
        await ctx.run_backward_pass()

        assert ps_hash_table.parameter_with_hash_table.grad is not None

        grad_wrapped = libcpp_lib.HashEmbeddingGradientWrapper(
            ps_hash_table.parameter_with_hash_table.grad,
            ps_hash_table.dim()
        )

        feaids, gradients = grad_wrapped.to_tensors()
        assert set(feaids.view(-1).numpy()) == set(data.view(-1).numpy())
        # we have fixed seed
        gradient_reference = torch.FloatTensor([
            list(partial_gradient[0, :].view(-1).numpy()),
            list(partial_gradient[0, :].view(-1).numpy()),
            list(partial_gradient[0, :].view(-1).numpy()),
            list(partial_gradient[1, :].view(-1).numpy())
        ])
        if compute_mode == "mean":
            gradient_reference[:3, :] /= 3

        assert allclose_cpu(gradients, gradient_reference, rtol=1e-3, atol=1e-3)


@pytest.mark.parametrize('count', [1, 3], ids=['SingleGrad', 'SeveralGrads'])
@pytest.mark.asyncio
async def test_hash_map_gradient_calculated_property_sequence(ps_hash_table, count, thread_pool, LazyComputationContext, device):
    torch.manual_seed(73878237)
    targets = torch.rand(2, ps_hash_table.dim()).to(device)
    # It is VERY important to have several zeros in data_len!
    # C++ code have optimization for CUDA forward pass: don't fill zeros
    # on cpu, instean send small data_len tensor to CUDA and fill zeros on CUDA
    # for sequence tensors async with very unbalances sequence lens in one minibatch this is
    # crutial optimization
    data_len = torch.IntTensor([[1, 1], [1, 0], [1, 0]])

    # Probably it's bad, but here we rely on previous test of sum mode correctness

    for grad_id, partial_gradient in enumerate(PARTIAL_GRADIENTS_FOR_REFERENCE[:count]):
        data = torch.LongTensor([i + grad_id * 10 for i in range(1, 5)])

        async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
            seq_tensor = ps_hash_table(data, data_len, compute_mode="sum")

        await ctx.run_forward_pass()
        torch.nn.functional.mse_loss(seq_tensor.sum(dim=0).view(-1), targets.view(-1)).sum().backward()
        ctx.assign_partial_gradient(forward_tensor=seq_tensor, gradient=seq_tensor.grad)
        await ctx.run_backward_pass()

        assert ps_hash_table.parameter_with_hash_table.grad is not None

        seq_grad = libcpp_lib.HashEmbeddingGradientWrapper(
            ps_hash_table.parameter_with_hash_table.grad,
            ps_hash_table.dim()
        )

        async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
            sum_tensor = ps_hash_table(data, data_len, compute_mode="sum")

        await ctx.run_forward_pass()
        torch.nn.functional.mse_loss(sum_tensor.sum(dim=0).view(-1), targets.view(-1)).sum().backward()
        ctx.assign_partial_gradient(forward_tensor=sum_tensor, gradient=sum_tensor.grad)
        await ctx.run_backward_pass()
        assert ps_hash_table.parameter_with_hash_table.grad is not None

        sum_grad = libcpp_lib.HashEmbeddingGradientWrapper(
            ps_hash_table.parameter_with_hash_table.grad,
            ps_hash_table.dim()
        )

        # cmp grads

        sum_feaids, sum_gradients = sum_grad.to_tensors()
        seq_feaids, seq_gradients = seq_grad.to_tensors()
        assert set(sum_feaids.view(-1).numpy()) == set(data.view(-1).numpy())
        assert set(seq_feaids.view(-1).numpy()) == set(data.view(-1).numpy())

        assert allclose_cpu(sum_gradients, seq_gradients, atol=1e-4)


@pytest.mark.asyncio
async def test_optimization_performed(thread_pool, LazyComputationContext, device):
    torch.manual_seed(12345)
    ps_hash_table = _create_and_fill_hash_table(table_type="adam", dim=100, shift=0)
    ps_hash_table.parameter_server_mode = True

    assert ps_hash_table.size() == 10
    # no such features in ps_hash_table
    data = torch.LongTensor([100, 200, 300, 400])
    data_len = torch.IntTensor([3, 1])

    optimizer = EmbeddingAdamOptimizer(ps_hash_table.parameters())

    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    async def _step():
        async with ctx:
            forward_tensor = ps_hash_table(data, data_len)

        await ctx.run_forward_pass()
        assert forward_tensor.requires_grad
        ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=torch.ones_like(forward_tensor))
        await ctx.run_backward_pass()

        optimizer.step()

    await _step()

    # here we tested that elements have been added
    assert ps_hash_table.size() == 14

    previous_weights = [ps_hash_table.lookup_item(x).w.clone() for x in range(100, 500, 100)]

    # Now we test that elements have been updated
    await _step()

    assert ps_hash_table.size() == 14
    weights = [ps_hash_table.lookup_item(x).w.clone() for x in range(100, 500, 100)]
    assert not any(allclose_cpu(w1, w2) for w1, w2 in zip(previous_weights, weights))


################################################################
#                    BACKWARD PASS TESTING WITH WEIGHTS        #
################################################################


@pytest.mark.parametrize('gradient_type', [torch.float32, torch.float16], ids=['Float', 'Half'])
@pytest.mark.asyncio
async def test_weighted_single_hash_map_gradient(ps_hash_table, thread_pool, gradient_type, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = torch.FloatTensor([[32.1] * ps_hash_table.dim(), [400] * ps_hash_table.dim()])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    # not real computation happened
    await ctx.run_forward_pass()
    assert forward_tensor.requires_grad
    assert allclose_cpu(forward_tensor, reference)
    partial_gradient = torch.ones_like(forward_tensor)
    ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=partial_gradient.to(gradient_type))
    await ctx.run_backward_pass()

    assert ps_hash_table.parameter_with_hash_table.grad is not None


@pytest.mark.asyncio
async def test_weighted_single_hash_map_None_gradient(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    reference = torch.FloatTensor([[32.1] * ps_hash_table.dim(), [400] * ps_hash_table.dim()])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        forward_tensor = ps_hash_table(data, data_len, data_weight)

    # not real computation happened
    await ctx.run_forward_pass()
    assert forward_tensor.requires_grad
    assert allclose_cpu(forward_tensor, reference)
    ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=None)
    await ctx.run_backward_pass()

    assert ps_hash_table.parameter_with_hash_table.grad is None


@pytest.mark.parametrize('gradient_type', [torch.float32, torch.float16], ids=['Float', 'Half'])
@pytest.mark.parametrize('compute_mode', ['mean', 'sum'])
@pytest.mark.parametrize('count', [1, 3], ids=['SingleGrad', 'SeveralGrads'])
@pytest.mark.asyncio
async def test_weighted_hash_map_gradient_calculated_property(ps_hash_table, count, thread_pool, compute_mode, gradient_type, LazyComputationContext, device):
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    for grad_id, partial_gradient in enumerate(PARTIAL_GRADIENTS_FOR_REFERENCE[:count]):
        data = torch.LongTensor([i + grad_id * 10 for i in range(1, 5)])

        async with LazyComputationContext(thread_pool=thread_pool, max_data_size_per_forward_job=1) as ctx:
            forward_tensor = ps_hash_table(data, data_len, data_weight, compute_mode=compute_mode)

        await ctx.run_forward_pass()
        ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=partial_gradient.to(gradient_type))
        await ctx.run_backward_pass()

        assert ps_hash_table.parameter_with_hash_table.grad is not None

        grad_wrapped = libcpp_lib.HashEmbeddingGradientWrapper(
            ps_hash_table.parameter_with_hash_table.grad,
            ps_hash_table.dim()
        )

        feaids, gradients = grad_wrapped.to_tensors()
        assert set(feaids.view(-1).numpy()) == set(data.view(-1).numpy())
        # we have fixed seed
        gradient_reference = torch.FloatTensor([
            list(partial_gradient[0, :].view(-1).numpy()),
            list(partial_gradient[0, :].view(-1).numpy()),
            list(partial_gradient[0, :].view(-1).numpy()),
            list(partial_gradient[1, :].view(-1).numpy())
        ])
        if compute_mode == "mean":
            gradient_reference[:3, :] /= 3

        assert allclose_cpu(gradients, gradient_reference * data_weight.unsqueeze(1), rtol=1e-3, atol=1e-3)


@pytest.mark.parametrize('count', [1, 3], ids=['SingleGrad', 'SeveralGrads'])
@pytest.mark.asyncio
async def test_weighted_hash_map_gradient_calculated_property_sequence(ps_hash_table, count, thread_pool, LazyComputationContext, device):
    torch.manual_seed(73878237)
    targets = torch.rand(2, ps_hash_table.dim()).to(device)
    data_len = torch.IntTensor([[1, 1], [1, 0], [1, 0]])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    # Probably it's bad, but here we rely on previous test of sum mode correctness

    for grad_id, partial_gradient in enumerate(PARTIAL_GRADIENTS_FOR_REFERENCE[:count]):
        data = torch.LongTensor([i + grad_id * 10 for i in range(1, 5)])

        async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
            seq_tensor = ps_hash_table(data, data_len, data_weight, compute_mode="sum")

        await ctx.run_forward_pass()
        torch.nn.functional.mse_loss(seq_tensor.sum(dim=0).view(-1), targets.view(-1)).sum().backward()
        ctx.assign_partial_gradient(forward_tensor=seq_tensor, gradient=seq_tensor.grad)
        await ctx.run_backward_pass()

        assert ps_hash_table.parameter_with_hash_table.grad is not None

        seq_grad = libcpp_lib.HashEmbeddingGradientWrapper(
            ps_hash_table.parameter_with_hash_table.grad,
            ps_hash_table.dim()
        )

        async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
            sum_tensor = ps_hash_table(data, data_len, data_weight, compute_mode="sum")

        await ctx.run_forward_pass()
        torch.nn.functional.mse_loss(sum_tensor.sum(dim=0).view(-1), targets.view(-1)).sum().backward()
        ctx.assign_partial_gradient(forward_tensor=sum_tensor, gradient=sum_tensor.grad)
        await ctx.run_backward_pass()
        assert ps_hash_table.parameter_with_hash_table.grad is not None

        sum_grad = libcpp_lib.HashEmbeddingGradientWrapper(
            ps_hash_table.parameter_with_hash_table.grad,
            ps_hash_table.dim()
        )

        # cmp grads

        sum_feaids, sum_gradients = sum_grad.to_tensors()
        seq_feaids, seq_gradients = seq_grad.to_tensors()
        assert set(sum_feaids.view(-1).numpy()) == set(data.view(-1).numpy())
        assert set(seq_feaids.view(-1).numpy()) == set(data.view(-1).numpy())

        assert allclose_cpu(sum_gradients, seq_gradients, atol=1e-4)


@pytest.mark.asyncio
async def test_weighted_optimization_performed(thread_pool, LazyComputationContext, device):
    torch.manual_seed(12345)
    ps_hash_table = _create_and_fill_hash_table(table_type="adam", dim=100, shift=0)
    ps_hash_table.parameter_server_mode = True

    assert ps_hash_table.size() == 10
    # no such features in ps_hash_table
    data = torch.LongTensor([100, 200, 300, 400])
    data_len = torch.IntTensor([3, 1])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])

    optimizer = EmbeddingAdamOptimizer(ps_hash_table.parameters())

    ctx = LazyComputationContext(thread_pool=thread_pool, device=device)

    async def _step():
        async with ctx:
            forward_tensor = ps_hash_table(data, data_len, data_weight)

        await ctx.run_forward_pass()
        assert forward_tensor.requires_grad
        ctx.assign_partial_gradient(forward_tensor=forward_tensor, gradient=torch.ones_like(forward_tensor))
        await ctx.run_backward_pass()

        optimizer.step()

    await _step()

    # here we tested that elements have been added
    assert ps_hash_table.size() == 14

    previous_weights = [ps_hash_table.lookup_item(x).w.clone() for x in range(100, 500, 100)]

    # Now we test that elements have been updated
    await _step()

    assert ps_hash_table.size() == 14
    weights = [ps_hash_table.lookup_item(x).w.clone() for x in range(100, 500, 100)]
    assert not any(allclose_cpu(w1, w2) for w1, w2 in zip(previous_weights, weights))


##################################################################
#                      PARALLEL COMPUTATION                      #
##################################################################


@pytest.mark.parametrize(
    'num_threads',
    [1, 3, 10],
    ids=['Single', 'Parallel', 'Overcommit']
)
@pytest.mark.asyncio
async def test_thread_pool(num_threads, LazyComputationContext, device):
    torch.manual_seed(1234567)
    # This test checks that threaded computation is the same as non-threaded
    # non-threaded has been excessively tested above
    # 4 hash tables
    iterables = [["adam"], [10, 100]]
    hash_tables = [
        HashEmbedding(
            create_hash_table(table_type=table_type, dim=dim),
        )
        for table_type, dim in itertools.product(*iterables)
    ]

    lazy_hash_tables = [
        HashEmbedding(
            create_hash_table(table_type=table_type, dim=dim)
        )
        for table_type, dim in itertools.product(*iterables)
    ]
    for h in lazy_hash_tables:
        h.parameter_server_mode = True

    # we must insert equally initialized items
    for h1, h2 in zip(hash_tables, lazy_hash_tables):
        value = torch.randn(h1.dim())
        for i in range(100):
            item = create_item("adam", h1.dim())
            item.w = value
            h1.insert_item(i, item)
            item = create_item("adam", h2.dim())
            item.w = value
            h2.insert_item(i, item)

    data = torch.LongTensor(list(range(100)))
    data_len = torch.IntTensor([10] * 10)
    targets = torch.randn(10)
    targets.requires_grad = False
    loss = torch.nn.MSELoss()

    def _deep_calc(tensors):
        tensors = [t.sum(1) for t in tensors]
        res = torch.zeros_like(tensors[0])
        res.requires_grad = False
        for t in tensors:
            res.add_(t)
        loss(res, targets).backward()

    epoch_count = 4
    ctx = LazyComputationContext(libcpp_lib.ThreadPoolHandle(num_threads))
    for _ in range(epoch_count):
        tensors = [hash_table(data, data_len) for hash_table in hash_tables]
        _deep_calc(tensors)

        async with ctx:
            lazy_tensors = [hash_table(data, data_len) for hash_table in lazy_hash_tables]
        await ctx.run_forward_pass()
        _deep_calc(lazy_tensors)
        # assign gradients
        for tensor, hash_table in zip(lazy_tensors, lazy_hash_tables):
            ctx.assign_partial_gradient(
                forward_tensor=tensor,
                gradient=tensor.grad
            )
        await ctx.run_backward_pass()

        # Check all items in the hash table are equal
        for h1, h2 in zip(hash_tables, lazy_hash_tables):
            for i in range(100):
                item1 = h1.lookup_item(i)
                item2 = h2.lookup_item(i)
                assert allclose_cpu(item1.w, item2.w)


##################################################################
#               ONE HASH TABLE WITH MULTIPLE CALC                #
##################################################################


@pytest.mark.asyncio
async def test_shared_forward_pass(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])
    reference = torch.FloatTensor([[6] * ps_hash_table.dim(), [4] * ps_hash_table.dim()])

    second_data = torch.LongTensor([5, 6, 7, 8, 9])
    second_data_len = torch.IntTensor([2, 3])
    second_reference = torch.FloatTensor([[5 + 6] * ps_hash_table.dim(), [7 + 8 + 9] * ps_hash_table.dim()])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        tensor = ps_hash_table(data, data_len)
        second_tensor = ps_hash_table(second_data, second_data_len)

    await ctx.run_forward_pass()

    assert allclose_cpu(tensor, reference)
    assert allclose_cpu(second_tensor, second_reference)


@pytest.mark.asyncio
async def test_shared_backward(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    second_data = torch.LongTensor([5, 6, 7, 8, 9])
    second_data_len = torch.IntTensor([2, 3])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        tensor = ps_hash_table(data, data_len)
        second_tensor = ps_hash_table(second_data, second_data_len)

    await ctx.run_forward_pass()
    ctx.assign_partial_gradient(tensor, torch.ones_like(tensor))
    ctx.assign_partial_gradient(second_tensor, torch.ones_like(second_tensor))
    await ctx.run_backward_pass()

    wrapped = HashEmbeddingGradientWrapper(ps_hash_table.dim(), ps_hash_table.parameter_with_hash_table.grad)
    features, gradients = wrapped.to_tensors()
    features = features.clone()
    gradients = gradients.clone()

    assert set(list(features.numpy())) == set(range(1, 10))

    # Okay - now calculate the reference. In our case, it should be just as if
    # we concatenated inputs and calced them at once
    cat_data = torch.cat([data, second_data])
    cat_data_len = torch.cat([data_len, second_data_len])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        tensor = ps_hash_table(cat_data, cat_data_len)

    await ctx.run_forward_pass()
    ctx.assign_partial_gradient(tensor, torch.ones_like(tensor))
    await ctx.run_backward_pass()

    wrapped = HashEmbeddingGradientWrapper(ps_hash_table.dim(), ps_hash_table.parameter_with_hash_table.grad)
    cat_features, cat_gradients = wrapped.to_tensors()

    cat_features = cat_features.clone()
    cat_gradients = cat_gradients.clone()

    assert set(list(features.numpy())) == set(range(1, 10))

    assert allclose_cpu(features, cat_features)
    assert allclose_cpu(gradients, cat_gradients)


@pytest.mark.asyncio
async def test_shared_backward_with_one_no_grad(ps_hash_table, thread_pool, LazyComputationContext, device):
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    second_data = torch.LongTensor([5, 6, 7, 8, 9])
    second_data_len = torch.IntTensor([2, 3])

    async with LazyComputationContext(thread_pool=thread_pool, device=device) as ctx:
        tensor = ps_hash_table(data, data_len)
        second_tensor = ps_hash_table(second_data, second_data_len)

    await ctx.run_forward_pass()
    ctx.assign_partial_gradient(tensor, torch.ones_like(tensor))
    ctx.assign_partial_gradient(second_tensor, None)
    await ctx.run_backward_pass()

    wrapped = HashEmbeddingGradientWrapper(ps_hash_table.dim(), ps_hash_table.parameter_with_hash_table.grad)
    features, gradients = wrapped.to_tensors()

    assert set(list(features.numpy())) == {1, 2, 3, 4}


@pytest.mark.asyncio
async def test_external_tensor_copy_with(ps_hash_table, thread_pool, device):
    # c++-only feature
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([3, 1])

    ctx = CppLazyComputationContext(thread_pool=thread_pool, device=device)

    async with ctx:
        ps_hash_table(data, data_len)
    externals = [
        torch.rand(10, 10),
        torch.rand(10, 20).half(),
        torch.rand(5, 7)
    ]
    cuda_externals = [ctx.add_copy_tensor(x) for x in externals]

    await ctx.run_forward_pass()

    for reference, copied in zip(externals, cuda_externals):
        assert copied.device == device
        assert allclose_cpu(reference, copied)
