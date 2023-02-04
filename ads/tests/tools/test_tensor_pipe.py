"""
WARNING
These tests extensively use ads_pytorch.tools.nested_structure.apply function
All of them are written given that apply works correctly
apply has it's own test suites
"""

import asyncio
import pytest
import torch
from typing import Dict
import dataclasses
import multiprocessing
import traceback
import threading
import sys
from ads_pytorch.tools.tensor_pipe import TensorPipe, TensorPipeReader
from ads_pytorch.tools.multiprocessing import get_multiprocessing_context
from ads_pytorch.tools.nested_structure import apply


SUBPROCESS_WAIT_TIMEOUT = 60


def _asyncio_run(task):
    loop = asyncio.get_event_loop_policy().new_event_loop()
    loop.run_until_complete(task)


def _raise_if_ex(x):
    if isinstance(x, BaseException):
        raise x
    return x


def _clone_tensor_structure(tensor_structure):
    return apply(
        tensor_structure,
        fn=lambda t: t.clone() if t is not None else None
    )


def _compare_nested_tensor_structure(value, received) -> bool:
    # First, we test that structures are same
    assert type(value) == type(received)

    # We allow setting some elements of TensorPipe sended values to Nones
    # and these cases must be separated
    value_structure = apply(value, fn=lambda x: ... if x is not None else None)
    received_structure = apply(received, fn=lambda x: ... if x is not None else None)

    assert value_structure == received_structure

    # Okay, then we check that all tensors are equal
    all_closes = []

    def _cmp_fn(val: torch.Tensor, recv_val: torch.Tensor):
        if val is None and recv_val is None:
            return True
        if val.device != recv_val.device:
            return False
        if val.dtype != recv_val.dtype:
            return False
        return torch.allclose(val, recv_val)

    apply(
        value, received,
        fn=lambda x, y: all_closes.append(_cmp_fn(x, y)),
    )
    assert all(all_closes)


@dataclasses.dataclass
class NestedTensorKeeper:
    it1: Dict[str, torch.Tensor]
    t2: torch.Tensor


#######################################################
#                      PICKLABILITY                   #
#######################################################

# These tests check that we can send our tensor pipes to other processes
# This functionality is covered by other tests, but it may be a good indicator of failure


def _tensor_pipe_picklable_foo(sth, queue: multiprocessing.Queue):
    queue.put(True)


@pytest.mark.parametrize(
    'pipe',
    list(TensorPipe()),
    ids=[
        'UsualReader',
        'UsualWriter'
    ]
)
@pytest.mark.parametrize('mp_context', ['fork', 'spawn', 'forkserver', "pytorch"])
def test_fresh_tensor_pipe_picklable(pipe, mp_context):
    # will raise on unpicklability
    ctx = get_multiprocessing_context() if mp_context == "pytorch" else multiprocessing.get_context(mp_context)
    queue = ctx.Queue()
    proc = ctx.Process(target=_tensor_pipe_picklable_foo, args=(pipe, queue))
    proc.daemon = True
    proc.start()

    res = _raise_if_ex(queue.get(timeout=SUBPROCESS_WAIT_TIMEOUT))
    assert res is True


#######################################################
#                 SEND NESTED STRUCTURE               #
#######################################################


CPU_VALUE = [
    torch.FloatTensor([]),
    torch.FloatTensor([1, 1]),
    (),
    (torch.FloatTensor([1, 1]),),
    (torch.FloatTensor([1, 1]), torch.FloatTensor([2, 2])),
    {},
    {"tensor1": torch.FloatTensor([3, 3])},
    {"tensor1": torch.FloatTensor([3, 3]), "tensor2": torch.FloatTensor([4, 4])},
    NestedTensorKeeper(
        it1={1: torch.IntTensor([3, 4]), "2": torch.IntTensor([8, 9])},
        t2=torch.IntTensor([11, 12, 13])
    ),
    # Now test nested tensor structures with holes
    {"tensor1": torch.FloatTensor([3, 3]), "tensor2": None},
    [torch.FloatTensor([3, 3]), None],
    (torch.FloatTensor([3, 3]), None),
    NestedTensorKeeper(
        it1={1: None, "2": torch.IntTensor([8, 9])},
        t2=None
    )
]
CPU_VALUE_IDS = [
    'empty_tensor',
    'tensor',
    'tuple_empty',
    'tuple1',
    'tuple_many',
    'dict_empty',
    'dict1',
    'dict_many',
    'nested_structure',
    "dict_with_none",
    "list_with_none",
    "tuple_with_none",
    'nested_with_none'
]


def _mp_recv_for_test_send_recv(references, reader: TensorPipeReader, res_queue):
    reader.set_pickler_lock(threading.Lock())
    async def _main():
        res_queue.put(True)
        for reference in references:
            try:
                received = await reader.coro_recv()
                _compare_nested_tensor_structure(reference, received)
            except BaseException as ex:
                traceback.print_exc(file=sys.stderr)
                res_queue.put(ex)
                raise
            else:
                res_queue.put(True)

    _asyncio_run(_main())


async def _tensor_pipe_send_recv_impl(values, pipe, ctx):
    cloned_values = [_clone_tensor_structure(v) for v in values]
    reader, writer = pipe

    res_queue = ctx.Queue()
    proc = ctx.Process(
        target=_mp_recv_for_test_send_recv,
        kwargs=dict(references=cloned_values, reader=reader, res_queue=res_queue),
        daemon=True
    )
    proc.start()

    res_queue.get(timeout=SUBPROCESS_WAIT_TIMEOUT)

    for value in values:
        await writer.coro_send(value)
        # writer currently user asyncio.StreamWriter API which does not immediately send
        # socket data to subprocess
        # So we should avoid blocking event loop to let it send data when it wants
        loop = asyncio.get_running_loop()
        res = _raise_if_ex(await loop.run_in_executor(None, res_queue.get))
        assert res
    proc.join()


# Only CPU tensors support send/recv semantic in same process


@pytest.mark.parametrize('value', CPU_VALUE, ids=CPU_VALUE_IDS)
@pytest.mark.asyncio
async def test_cpu_tensor_send_recv(value):
    ctx = get_multiprocessing_context()
    pipe = TensorPipe()
    await _tensor_pipe_send_recv_impl(
        values=[value],
        pipe=pipe,
        ctx=ctx
    )


"""
The reason we put gpu tests in separate test case - following code will segfault
(see https://pytorch.org/docs/stable/notes/multiprocessing.html#cuda-in-multiprocessing)
q = multiprocessing.Queue()
tensor # some GPU tensor
q.put(tensor)
q.get()  # Segfault - we can get() only from another process
"""


@pytest.mark.requires_cuda
@pytest.fixture(scope="module", params=["tensor", "nested", "mixed_state"])
def gpu_value(request):
    device = torch.device("cuda", 0)
    data_to_param = {
        "tensor": torch.FloatTensor([1, 1]).to(device),
        "nested": NestedTensorKeeper(
            it1={1: torch.IntTensor([3, 4]).to(device), "2": torch.IntTensor([8, 9]).to(device)},
            t2=torch.IntTensor([11, 12, 13]).to(device)
        ),
        "mixed_state": NestedTensorKeeper(
            it1={1: torch.IntTensor([3, 4]), "2": torch.IntTensor([8, 9]).to(device)},
            t2=torch.IntTensor([11, 12, 13]).to(device)
        )
    }
    return data_to_param[request.param]


@pytest.mark.requires_cuda
@pytest.mark.asyncio
async def test_gpu_tensor_send_recv(gpu_value):
    ctx = get_multiprocessing_context()
    pipe = TensorPipe()
    await _tensor_pipe_send_recv_impl(
        values=[gpu_value],
        pipe=pipe,
        ctx=ctx
    )


##############################################################
#                        REBUILD TESTS                       #
##############################################################


@pytest.mark.parametrize(
    "values",
    [
        [
            torch.rand(10),
            torch.rand(5),
            torch.rand(15),
            torch.rand(14),
            torch.rand(15),
            torch.rand(20),
        ],
        [
            torch.rand(10, device=torch.device("cuda", 0)),
            torch.rand(5, device=torch.device("cuda", 0)),
            torch.rand(15, device=torch.device("cuda", 0)),
            torch.rand(14, device=torch.device("cuda", 0)),
            torch.rand(15, device=torch.device("cuda", 0)),
            torch.rand(20, device=torch.device("cuda", 0)),
        ],
        [
            torch.rand(10),
            torch.rand(5, device=torch.device("cuda", 0)),
            torch.rand(15),
            torch.rand(14, device=torch.device("cuda", 0)),
            torch.rand(15, device=torch.device("cuda", 0)),
            torch.rand(20),
        ],
        [
            torch.rand(10, dtype=torch.half),
            torch.rand(5, dtype=torch.half),
            torch.rand(15, dtype=torch.half),
            torch.rand(14, dtype=torch.float),
            torch.rand(15, dtype=torch.double),
            torch.rand(20, dtype=torch.float)
        ],
        [
            torch.rand(10, device=torch.device("cuda", 0), dtype=torch.half),
            torch.rand(5, device=torch.device("cuda", 0), dtype=torch.half),
            torch.rand(15, device=torch.device("cuda", 0), dtype=torch.half),
            torch.rand(14, device=torch.device("cuda", 0), dtype=torch.float),
            torch.rand(15, device=torch.device("cuda", 0), dtype=torch.double),
            torch.rand(20, device=torch.device("cuda", 0), dtype=torch.float)
        ]
    ]
)
@pytest.mark.asyncio
async def test_gpu_tensor_send_recv_sequential(values):
    torch.manual_seed(23872)
    ctx = get_multiprocessing_context()
    pipe = TensorPipe()
    await _tensor_pipe_send_recv_impl(
        values=values,
        pipe=pipe,
        ctx=ctx
    )
