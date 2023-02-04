import torch
import random
from densenet_tsar_query_attention.query_attention import (
    QueryFactorsProcessor,
    QueryEmbeddingsGrouper,
    QueryAttentionNetwork
)


def test_embeddings_grouper():
    query_count = 20
    queries = [f"QueryText{i}" for i in range(query_count)]
    select_types = [f"SelectType{i}" for i in range(query_count)]
    factors_list = queries + select_types + [f"other{i}" for i in range(30)]
    factor_ids = list(range(len(factors_list)))
    random.shuffle(factor_ids)
    feature_to_id = {name: i for name, i in zip(factors_list, factor_ids)}
    feature_to_id_reversed = {v: k for k, v in feature_to_id.items()}
    feature_order = [feature_to_id_reversed[i] for i in range(len(factors_list))]

    model = QueryEmbeddingsGrouper(
        feature_to_id=feature_to_id,
        features={"Text": queries, "SelectType": select_types}
    )

    inputs = {key: torch.rand(5, 1) for key in factors_list}
    inputs_list = [inputs[feature] for feature in feature_order]

    res = model(inputs_list)

    for tensor, q_id, s_id in zip(res, queries, select_types):
        reference = (inputs[q_id] + inputs[s_id]) / 2
        assert torch.allclose(tensor, reference)


def test_query_factors_processor():
    batch_size = 1
    total_query_count = 13
    model = QueryFactorsProcessor(
        query_factors_id=3,
        total_query_count=total_query_count
    )

    inputs = [torch.rand(1, 1)] * 3
    inputs += [torch.rand(batch_size, total_query_count, 5)]
    inputs += [torch.rand(1, 1)]
    inputs += [torch.rand(1, 1)] * 5

    # todo check output
    model(inputs)


def test_query_attention():
    query_count = 20
    query_factors_count = 40
    embedding_dim = 30
    queries = [f"QueryText{i}" for i in range(query_count)]
    select_types = [f"SelectType{i}" for i in range(query_count)]
    factors_list = queries + select_types + [f"other{i}" for i in range(30)] + ["QueryFactors"]
    factor_ids = list(range(len(factors_list)))
    random.shuffle(factor_ids)
    feature_to_id = {name: i for name, i in zip(factors_list, factor_ids)}
    feature_to_id_reversed = {v: k for k, v in feature_to_id.items()}
    feature_order = [feature_to_id_reversed[i] for i in range(len(factors_list))]

    transformer = torch.nn.TransformerEncoder(
        encoder_layer=torch.nn.TransformerEncoderLayer(
            d_model=embedding_dim + 5, nhead=1, dim_feedforward=30, dropout=0
        ),
        num_layers=3
    )

    model = QueryAttentionNetwork(
        feature_to_id=feature_to_id,
        features={"Text": queries, "SelectType": select_types},
        transformer=transformer,
        query_factors_count=query_factors_count
    )

    batch_size = 20
    inputs = {key: torch.rand(batch_size, embedding_dim) for key in factors_list}
    inputs["QueryFactors"] = torch.rand(batch_size, query_factors_count, 5)

    inputs_list = [inputs[feature] for feature in feature_order]
    res = model(inputs_list)

    # Okay, let's calc manually using dicts, where we 100% sure in solution
    model.eval()
    embeds = [(inputs[q_id] + inputs[s_id]) / 2 for q_id, s_id in zip(queries, select_types)]
    embeds = torch.stack(embeds, dim=0)
    query_factors = model.query_factors_network(inputs_list)
    query_factors = query_factors.narrow(1, 0, query_count).permute(1, 0, 2)
    embeds = torch.cat([embeds, query_factors], dim=2)
    reference = torch.mean(transformer(embeds), dim=0)

    assert torch.allclose(res, reference)
