import torch
import pytest
from deploy.production_model import safe_eval_model_on_cpu


class TrainEvalModel(torch.nn.Module):
    def __init__(self):
        super(TrainEvalModel, self).__init__()
        self.counter = 0

    def forward(self, dct):
        self.counter += 1
        value = 1 if self.training else 10
        return dct["1"] + value


@pytest.fixture
def run_safe_eval():
    tensor = torch.rand(10)
    model = TrainEvalModel()
    model.train()
    res = safe_eval_model_on_cpu(inputs={"1": tensor}, model=model)
    return tensor, model, res


def test_safe_eval_mode(run_safe_eval):
    tensor, _, res = run_safe_eval
    assert torch.allclose(res, tensor + 10)


def test_deepcopy(run_safe_eval):
    _, model, _ = run_safe_eval
    assert model.counter == 0
