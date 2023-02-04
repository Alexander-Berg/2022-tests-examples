from pclick_tsar4 import make_model
import torch


def make_fake_inputs(model_config):
    res = {}
    for names in model_config["namespaces"].values():
        for key in names:
            key = ",".join(key) if isinstance(key, (tuple, list)) else key
            res[key] = [
                torch.randint(10000, (20, ), dtype=torch.int64),
                torch.IntTensor([15, 5])
            ]
    return res


def test_forward_pass(model_config):
    model = make_model(model_conf=model_config)
    inputs = make_fake_inputs(model_config=model_config)
    res = model(inputs)
    for item in res.values():
        assert item.dtype == torch.float32
        assert item.numel() == 2
