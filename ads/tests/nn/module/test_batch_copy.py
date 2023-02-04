import torch
import pytest
import itertools
from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.tools.cuda_synchronization import current_stream_sync_ctx


def test_empty_sequence():
    libcpp_lib.BatchCudaCopy([], [])


@pytest.mark.parametrize(
    ["t1", "t2"],
    [
        (torch.rand(1, 10), torch.rand(1, 1, 10)),
        (torch.rand(10), torch.rand(5)),
        (torch.rand(5).cuda(), torch.rand(5).cpu())
    ],
    ids=[
        "SameNumelDifferentShape",
        "DifferentNumel",
        "DifferentDevice"
    ]
)
def test_bad_inputs(t1, t2):
    with pytest.raises(RuntimeError):
        libcpp_lib.BatchCudaCopy([t1], [t2])


device = torch.device("cuda", 0)


@pytest.mark.parametrize(
    "sources",
    [
        [
            torch.rand(1, device=device),
        ],
        [
            torch.rand(1, 1, 1, 1, device=device),
        ],
        [
            torch.rand(1, device=device),
            torch.rand(1, device=device),
            torch.rand(1, device=device),
            torch.rand(1, device=device),
            torch.rand(1, device=device),
            torch.rand(1, device=device),
            torch.rand(1, device=device),
        ],
        [
            torch.rand(5, device=device),
            torch.rand(19, device=device),
            torch.rand(17, device=device),
            torch.rand(245, device=device)
        ],
        [torch.rand(int(x), device=device) for x in torch.linspace(1, 10000, 3700)],
        [
            torch.rand(5, device=device).half(),
            torch.rand(19, device=device).double(),
            torch.randint(0, 100500, (17, ), device=device),
            torch.rand(245, device=device).float()
        ]
    ]
)
@pytest.mark.parametrize("thread_job_size", [16, 128, None], ids=["Thread4", "Thread32", "ThreadNone"])
@pytest.mark.parametrize("batch_copy_size_limit", [None, 100], ids=["BatchCopyDefault", "BatchCopy100"])
def test_copy(sources, thread_job_size, batch_copy_size_limit):
    destinations = [torch.zeros_like(t) for t in sources]
    for src, dst in zip(sources, destinations):
        assert not torch.allclose(src, dst)

    kwargs = {}
    if batch_copy_size_limit is not None:
        kwargs.update(dict(batch_copy_size_limit=batch_copy_size_limit))
    if thread_job_size is not None:
        kwargs.update(dict(thread_job_size=thread_job_size))
    with current_stream_sync_ctx([torch.device("cuda", 0)]):
        libcpp_lib.BatchCudaCopy(sources, destinations, **kwargs)

    for i, (src, dst) in enumerate(zip(sources, destinations)):
        assert torch.allclose(src, dst)


def _make_strided_inputs(is_strided, device, sizes_list):
    with torch.no_grad():
        if is_strided:
            buf = torch.zeros(4, 245, 13, device=device)
            results = [
                buf[i, :s1, :s2]
                for i, (s1, s2) in enumerate(sizes_list)
            ]
            for res in results:
                torch.nn.init.uniform_(res, 0, 1)
            return results
        else:
            return [
                torch.rand(*size, device=device)
                for size in sizes_list
            ]


@pytest.mark.parametrize("thread_job_size", [16, 128, None], ids=["Thread4", "Thread32", "ThreadNone"])
@pytest.mark.parametrize("batch_copy_size_limit", [None, 100], ids=["BatchCopyDefault", "BatchCopy100"])
@pytest.mark.parametrize("src_strided", [True, False], ids=["SourceStrided", "SourceUsual"])
@pytest.mark.parametrize("dst_strided", [True, False], ids=["DestinationStrided", "DestinationUsual"])
def test_strided_copy(thread_job_size, batch_copy_size_limit, src_strided, dst_strided):
    # important case for batch matrix orthogonalization

    sizes_list = [
        (5, 7),
        (19, 2),
        (17, 4),
        (245, 13)
    ]
    device = torch.device("cuda", 0)

    sources      = _make_strided_inputs(is_strided=src_strided, device=device, sizes_list=sizes_list)
    destinations = _make_strided_inputs(is_strided=dst_strided, device=device, sizes_list=sizes_list)

    for src, dst in zip(sources, destinations):
        assert src.size() == dst.size()
        assert not torch.allclose(src, dst)

    kwargs = {}
    if batch_copy_size_limit is not None:
        kwargs.update(dict(batch_copy_size_limit=batch_copy_size_limit))
    if thread_job_size is not None:
        kwargs.update(dict(thread_job_size=thread_job_size))
    with current_stream_sync_ctx(devices=[device]):
        libcpp_lib.BatchCudaCopy(sources, destinations, **kwargs)

    for i, (src, dst) in enumerate(zip(sources, destinations)):
        assert torch.allclose(src, dst)


# Due to a bit complicated job splitting and offset calculation code,
# it's important to test ALL combinations
def test_different_strides_3d():
    torch.manual_seed(73856287356)
    device = torch.device("cuda", 0)
    maxsize = 100
    storage_shape = (maxsize, maxsize, maxsize ** 2)

    reference_storage = torch.zeros(*storage_shape, device=device)
    storage = torch.zeros_like(reference_storage)
    tensors = [
        torch.rand(s1, s2, device=device)
        for s1 in range(maxsize)
        for s2 in range(maxsize)
    ]
    views = [
        storage[:s1, :s2, i]
        for i, (s1, s2) in enumerate(itertools.product(range(maxsize), range(maxsize)))
    ]
    libcpp_lib.BatchCudaCopy(tensors, views)

    for i, (s1, s2) in enumerate(itertools.product(range(maxsize), range(maxsize))):
        reference_storage[:s1, :s2, i].copy_(tensors[i])

    # assert torch.allclose(reference_view, view)
    assert torch.allclose(reference_storage, storage)
