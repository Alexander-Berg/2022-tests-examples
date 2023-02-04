import torch
import pytest
from ads_pytorch.tools.memory_profiler import (
    CUDADeviceMemoryProfiler,
    memory_stats
)


# We don't make assertions on exact amounts of memory. Torch developers can change pool sizes
# or any other details in CUDA Caching Allocator and invalidate results
@pytest.mark.requires_cuda
def test_memory_stats():
    gpu0 = torch.device("cuda", 0)
    gpu1 = torch.device("cuda", 1)

    # keep ref
    gpu0_tensor = torch.tensor(1).to(gpu0)

    gpu0_stats = memory_stats(device=gpu0)
    gpu1_stats = memory_stats(device=gpu1)
    assert gpu0_stats.active_bytes.all.current > 0
    assert gpu1_stats.active_bytes.all.current == 0

    gpu1_tensor = torch.rand(10000, 1000).to(gpu1)
    gpu1_stats = memory_stats(device=gpu1)
    assert gpu1_stats.active_bytes.all.current > 0


@pytest.mark.requires_cuda
def test_accurate_memory_estimation():
    device = torch.device("cuda", 0)

    model = torch.nn.Sequential(
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1)
    ).to(device)

    model.zero_grad()

    with CUDADeviceMemoryProfiler(device=device) as prof:
        inputs = torch.rand(300, 1000).to(device)
        model(inputs).sum().backward()

    prof.get()
