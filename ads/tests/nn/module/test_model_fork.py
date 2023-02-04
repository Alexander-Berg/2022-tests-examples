import torch
import pytest
from ads_pytorch.nn.module.model_fork import add_parameters_to_param_group
from ads_pytorch.nn.module.densenet import WeightNormalizedEmbeddingNetwork
from ads_pytorch.nn.optim.shared_adam import SharedAdam


@pytest.mark.parametrize('index', [-1, 1])
def test_model_add_parameters_bad_index(index):
    model = WeightNormalizedEmbeddingNetwork(10, 10, 10)
    opt = torch.optim.Adam(model.parameters())
    new_model = WeightNormalizedEmbeddingNetwork(10, 10, 10)
    with pytest.raises(AssertionError):
        add_parameters_to_param_group(param_group_index=index, parameters=list(new_model.parameters()), optimizer=opt)


def test_model_add_existing_parameters():
    model = WeightNormalizedEmbeddingNetwork(10, 10, 10)
    opt = torch.optim.Adam(model.parameters())
    opt.add_param_group({"params": []})
    with pytest.raises(AssertionError):
        add_parameters_to_param_group(1, list(model.parameters()), opt)


@pytest.mark.parametrize('opt_cls', [torch.optim.Adam, torch.optim.RMSprop, SharedAdam])
def test_model_add_parameters(opt_cls):
    in_features = 10
    out_features = 20
    model = WeightNormalizedEmbeddingNetwork(in_features, 10, 10, out_features=out_features)
    opt = opt_cls(model.parameters(), lr=0.001)
    new_model = WeightNormalizedEmbeddingNetwork(in_features, 10, 10, out_features=out_features)
    inputs = torch.rand(100, in_features)
    targets = torch.rand(100, out_features)
    loss = torch.nn.MSELoss()
    for _ in range(10):
        model.zero_grad()
        loss_val = loss(model(inputs), targets)
        loss_val.backward()
        opt.step()

    old_output = model(inputs)
    old_loss = float(loss(model(inputs), targets))
    output = new_model(inputs)

    add_parameters_to_param_group(0, list(new_model.parameters()), opt)
    assert torch.equal(old_output, model(inputs))

    state = opt.__getstate__()["state"]
    if len(state) > 0:
        for p in new_model.parameters():
            assert "step" in state[p]
            for k, v in state[p].items():
                if k == "step":
                    continue
                assert tuple(v.size()) == tuple(p.size())
                assert torch.equal(v, torch.zeros_like(p))

    for _ in range(50):
        model.zero_grad()
        loss_val = loss(model(inputs), targets)
        loss_val.backward()
        opt.step()
    assert float(loss(model(inputs), targets)) < old_loss
    assert torch.equal(output, new_model(inputs))

    float_loss = float(loss(new_model(inputs), targets))
    for _ in range(50):
        new_model.zero_grad()
        loss_val = loss(new_model(inputs), targets)
        loss_val.backward()
        opt.step()
    assert float(loss(new_model(inputs), targets)) < float_loss
    parameters_set = set(new_model.parameters())
    assert len(parameters_set.intersection(opt.param_groups[0]["params"])) == len(parameters_set)
