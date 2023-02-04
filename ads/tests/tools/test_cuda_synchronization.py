import torch
from ads_pytorch.tools.cuda_synchronization import current_stream_sync_ctx


def test_double_enter():
    tt = torch.rand(2 ** 24).cuda()
    with current_stream_sync_ctx(devices=[tt.device]):
        for _ in range(20):
            tt.clone()
        with current_stream_sync_ctx(devices=[tt.device]):
            for _ in range(20):
                tt.clone()
