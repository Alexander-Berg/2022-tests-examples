import pytest
import torch


DEVICE_COUNTS = [1]
IDS = ["SingleGPU"]
if torch.cuda.device_count() > 1:
    DEVICE_COUNTS.append(2)
    IDS.append("MultiGPU")


@pytest.fixture(params=DEVICE_COUNTS, ids=IDS, scope="function")
def device_count(request):
    return request.param
