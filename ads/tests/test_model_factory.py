import json
import pytest
import torch
from ads_pytorch import get_concatenated_field_name
from densenet_tsar_query_attention.model_factory import make_model_from_dict_config, TzarModel


@pytest.fixture
def tzar_model(model_config) -> TzarModel:
    with open(model_config, "rt") as f:
        cfg = json.load(f)
    return make_model_from_dict_config(parsed_model_config=cfg["model"])


def test_create_model(tzar_model):
    pass


def test_forward_pass(tzar_model, model_config):
    with open(model_config, "rt") as f:
        cfg = json.load(f)
    features = cfg["train_data"]["features"]
    features = [get_concatenated_field_name(x) for x in features]
    realvalue = [
        "CountersAggregatedValues",
        "QueryFactors",
        "VisitStatesFeatures",
    ]

    def _randcat():
        return [
            torch.randint(100, size=(3,), dtype=torch.int64),
            torch.IntTensor([3])
        ]

    inputs = {name: _randcat() for name in features if name not in realvalue}
    inputs["QueryFactors"] = torch.rand(1, 40, 5)
    inputs["CountersAggregatedValues"] = torch.rand(1, 256)
    inputs["VisitStatesFeatures"] = torch.rand(1, 7)

    tzar_model(inputs)
