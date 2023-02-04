import torch
import pytest
from ads_pytorch.nn.module.named_matrix_output_mixin import (
    NamedMatrixListOutputMixin,
    SingleMatrixToNamedMatrixList,
    SingleMatrixOutputMixin
)


class _HelperNet(torch.nn.Module, SingleMatrixOutputMixin):
    def __init__(self, net, out: int):
        super(_HelperNet, self).__init__()
        self.net = net
        self.out = out

    def forward(self, tensor):
        return self.net(tensor)

    def get_out_features(self) -> int:
        return self.out


def test_single_matrix_to_named_matrix():
    torch.manual_seed(12345)
    network = _HelperNet(net=torch.nn.Linear(100, 10), out=10)
    wrapped = SingleMatrixToNamedMatrixList(net=network, head_name="LOL")

    input_tensor = torch.rand(5, 100)
    res = wrapped(input_tensor)
    assert len(res) == 1
    assert torch.allclose(res[0], network(input_tensor))

    assert wrapped.get_output_dims() == {"LOL": network.get_out_features()}
    assert wrapped.get_names_order() == ["LOL"]
