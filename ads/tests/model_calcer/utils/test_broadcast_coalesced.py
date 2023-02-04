import asyncio
from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.tools.cpp_async_threaded import cpp_threaded_worker
import pytest
import torch


@pytest.mark.requires_cuda
@pytest.mark.parametrize('buffer_size', [2, 2000, 2 ** 20])
def test_broadcast_coalesced(buffer_size):
    tensors = [
        torch.randn(10, 10, 10),
        torch.randn(10, 10, 10),
        torch.randn(10, 10, 10)
    ]
    tensors = [t.cuda() for t in tensors]

    bcf = libcpp_lib.TBroadcastCoalescedFunctor(buffer_size, [0, 1])
    for tensor in tensors:
        bcf.Push(tensor)
    worker = cpp_threaded_worker()
    asyncio.get_event_loop().run_until_complete(worker(bcf))
    results = bcf.GetResults()

    assert all(t.device.index == 0 for t in results[0])
    assert all(t.device.index == 1 for t in results[1])

    # results[0] is tensors itself
    for t1, t2 in zip(results[0], tensors):
        assert t1.storage().data_ptr() == t2.storage().data_ptr()

    for t1, t2 in zip(results[1], tensors):
        assert torch.allclose(t1.cpu(), t2.cpu())
