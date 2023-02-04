import pytest
import torch
import os
import psutil
import uvloop
uvloop.install()


@pytest.fixture(autouse=True)
def cleanup_cuda():
    yield

    # perform cleanup after test run to clean memory from GPU in xdist running
    current_process = psutil.Process(pid=os.getpid())
    for child in current_process.children(recursive=True):
        child.kill()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
