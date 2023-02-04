import itertools
import random
from typing import List
import torch
from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table, create_item
from ads_pytorch.core.psmodel import BaseParameterServerModule


############################################################
#                  TEST PARAMETER ITERATORS                #
############################################################


# Just for testing
class CrossNetworkEmbedding(BaseParameterServerModule):
    """
    CrossNetwork has the following recurrent update equations
    x_{i+1} = x_0(x_{i}.dot(w_{i}) + 1) + b_{i}

    For the DesiredOnlineProperty, we have to obtain zero prediction from zero initial initialization and - more
    important - keep model trainable

    Usually, we do not use the CrossNetworkEmbedding inside some other equations.
    Usually, we just put it with dot product
    """
    def __init__(self, embedding_dims: List[int], depth: int):
        super(CrossNetworkEmbedding, self).__init__()
        self.embedding_dims = embedding_dims
        self.depth = depth
        self.embeddings = torch.nn.ModuleList([HashEmbedding(create_hash_table("adam", d)) for d in self.embedding_dims])
        total_len = sum(self.embedding_dims)
        self.biases = torch.nn.ParameterList([torch.nn.Parameter(torch.FloatTensor(total_len, 1)) for _ in range(depth)])
        self.weights = torch.nn.ParameterList([torch.nn.Parameter(torch.FloatTensor(total_len, 1)) for _ in range(depth)])
        for x in itertools.chain(self.biases, self.weights):
            torch.nn.init.orthogonal_(x)


class CrossNetwork(CrossNetworkEmbedding):
    def __init__(self, embedding_dims: List[int], depth: int):
        super(CrossNetwork, self).__init__(embedding_dims, depth)
        total_dim = sum(self.embedding_dims)
        self.final_W = torch.nn.Linear(total_dim, 1, bias=True)
        torch.nn.init.orthogonal_(self.final_W.weight)
        self.final_W.bias.data.fill_(random.random())


class NestedCrossNetworkModel(BaseParameterServerModule):
    def __init__(self):
        super(NestedCrossNetworkModel, self).__init__()
        self.cn1 = CrossNetwork([30] * 10, 6)
        self.cn2 = CrossNetwork([30] * 10, 2)
        self.cn3 = CrossNetwork([30] * 10, 2)
        self.bias = torch.nn.Parameter(torch.FloatTensor([1]))
        self.bias.data.fill_(random.random())


def test_cross_network_parameters():
    cn = CrossNetwork([30, 30, 30], 3)
    params = list(itertools.chain(cn.final_W.parameters(), cn.biases.parameters(), cn.weights.parameters()))
    assert sorted(id(x) for x in cn.deep_parameters()) == sorted(id(x) for x in params)
    assert sorted(id(x) for x in cn.embeddings.parameters()) == sorted(id(x) for x in cn.hash_embedding_parameters())


def test_nested_model_parameters():
    nst = NestedCrossNetworkModel()
    params = list(
        itertools.chain.from_iterable(
            [
                itertools.chain(cn.final_W.parameters(), cn.biases.parameters(), cn.weights.parameters())
                for cn in [nst.cn1, nst.cn2, nst.cn3]
            ]
        )
    ) + [nst.bias]
    assert sorted(id(x) for x in nst.deep_parameters()) == sorted(id(x) for x in params)
    hash_embedding_params = list(
        itertools.chain.from_iterable(
            [
                cn.embeddings.parameters()
                for cn in [nst.cn1, nst.cn2, nst.cn3]
            ]
        )
    )
    assert sorted(id(x) for x in nst.hash_embedding_parameters()) == sorted(id(x) for x in hash_embedding_params)


# testing parameter server mode


def test_parameter_server_mode():
    model = NestedCrossNetworkModel()
    model.parameter_server_mode = True
    for m in model.modules():
        if isinstance(m, HashEmbedding):
            assert m.parameter_server_mode

    model.parameter_server_mode = False
    for m in model.modules():
        if isinstance(m, HashEmbedding):
            assert not m.parameter_server_mode


def test_dynamic_parameter_server_mode():
    model = NestedCrossNetworkModel()
    model.parameter_server_mode = True
    for m in model.modules():
        if isinstance(m, HashEmbedding):
            assert m.parameter_server_mode
    model.some_new_attr = HashEmbedding(create_hash_table("adam", 30))
    assert model.some_new_attr.parameter_server_mode

    model.parameter_server_mode = False
    for m in model.modules():
        if isinstance(m, HashEmbedding):
            assert not m.parameter_server_mode
    assert not model.some_new_attr.parameter_server_mode
    model.some_new_attr2 = HashEmbedding(create_hash_table("adam", 30))
    assert not model.some_new_attr2.parameter_server_mode
