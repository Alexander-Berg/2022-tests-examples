import torch


from ads_pytorch import BaseParameterServerModule
from densenet_tsar_query_attention.tzar_deployable_model import TzarModel
from densenet_tsar_query_attention.tensor_applicable_model import TzarTensorApplicableModel


class FixedEmbeddingModel(BaseParameterServerModule):
    def __init__(self, value: float, size):
        super(FixedEmbeddingModel, self).__init__()
        self.value = value
        self.size = size

    def async_forward(self, inputs):
        return torch.tensor([self.value], dtype=torch.float32)

    def sync_forward(self, async_outputs):
        # tests that async -> sync is passed correctly. We have many subnetworks here
        assert torch.allclose(async_outputs, torch.tensor([self.value], dtype=torch.float32))
        return torch.full(self.size, fill_value=self.value)


def test_fixed_model():
    size = (10, 5)
    user = FixedEmbeddingModel(value=1.0, size=size)
    user(None)


def test_tzar_model():
    size = (10, 5)
    user = FixedEmbeddingModel(value=1.0, size=size)
    banner = FixedEmbeddingModel(value=0.5, size=size)
    page = FixedEmbeddingModel(value=1.5, size=size)

    tensor = TzarTensorApplicableModel(5)
    model = TzarModel(user=user, banner=banner, page=page, tensor=tensor)

    torch.nn.init.constant_(tensor._tensor_model.bp, 1.1)
    torch.nn.init.constant_(tensor._tensor_model.pu, 2.2)
    torch.nn.init.constant_(tensor._tensor_model.ub, 3.3)
    torch.nn.init.constant_(tensor._tensor_model.triple, 4.4)
    torch.nn.init.constant_(tensor._tensor_model.bias, 5.5)

    # if no errors on this stage, then banner, user and page are not swapped
    res = model(None)

    # okay, let's manually compute the reference
    reference = tensor._tensor_model(
        banner=banner(None),
        user=user(None),
        page=page(None)
    )

    assert torch.allclose(reference, res)
