import pytest
import torch
from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table
from ads_pytorch.core.psmodel import BaseParameterServerModule
from ads_pytorch.core.deepcopy_model import deepcopy_model


class MyModel(BaseParameterServerModule):
    def __init__(self, dim):
        super(MyModel, self).__init__()
        self.embed = HashEmbedding(create_hash_table("adam", dim))
        self.some_parameter = torch.nn.Parameter(torch.FloatTensor([1, 1]))

    def async_forward(self, inputs):
        return self.embed(*inputs)

    def sync_forward(self, async_outputs):
        return async_outputs


def test_deepcopy_different_data_usual_parameters():
    dim = 1  # no difference here
    model = MyModel(dim)
    model2 = deepcopy_model(model)
    assert model.some_parameter is not model2.some_parameter
    model.some_parameter.data[1] = 2
    assert torch.all(torch.eq(model.some_parameter.data, torch.FloatTensor([1, 2])))
    assert torch.all(torch.eq(model2.some_parameter.data, torch.FloatTensor([1, 1])))


@pytest.mark.parametrize('dim', [1, 100])
def test_deepcopy_hash_tables(dim):
    model = MyModel(dim)
    model2 = deepcopy_model(model)
    assert model.embed.parameter_with_hash_table is not model2.embed.parameter_with_hash_table
    assert model.embed.parameter_with_hash_table.hash_table is model2.embed.parameter_with_hash_table.hash_table

    # check that after forward-pass we have no sharing in memory
    data = torch.LongTensor([1, 2, 3, 4, 5])
    data_len = torch.IntTensor([3, 2])
    res1 = model((data, data_len))
    res2 = model2((data, data_len))
    assert res1 is not res2


@pytest.mark.parametrize('dim', [1, 100])
def test_deepcopy_hash_tables_fwd_res_exists(dim):
    model = MyModel(dim)
    # check that after forward-pass we have no sharing in memory
    data = torch.LongTensor([1, 2, 3, 4, 5])
    data_len = torch.IntTensor([3, 2])
    res1 = model((data, data_len))

    model2 = deepcopy_model(model)
    assert model.embed.parameter_with_hash_table is not model2.embed.parameter_with_hash_table
    assert model.embed.parameter_with_hash_table.hash_table is model2.embed.parameter_with_hash_table.hash_table

    # check that after forward-pass we have no sharing in memory
    res2 = model2((data, data_len))
    assert res1 is not res2
