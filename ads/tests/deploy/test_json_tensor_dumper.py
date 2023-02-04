import torch
import base64
import json
import numpy as np
from ads_pytorch.deploy.json_tensor_dumper import JsonTensorDumper
from ads_pytorch.nn.module.base_embedding_model import (
    BaseEmbeddingModel,
    EmbeddingDescriptor,
    embedding_descriptors_to_dim_dict
)


def test_json_dumper():
    embedding = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(name="text", features=["t1", "t2"], dim=19)
        ],
        external_factors=["rv0"]
    )

    inputs = {
        "t1": [torch.LongTensor([1, 2, 3]), torch.IntTensor([3])],
        "t2": [torch.LongTensor([1, 2, 3]), torch.IntTensor([3])],
        "rv0": torch.rand(1, 15)
    }
    outputs = [torch.rand(1, 3)]

    dumper = JsonTensorDumper()
    res = dumper.serialize(
        inputs=inputs,
        outputs=outputs,
        categorical_factors=["t1", "t2"],
        external_factors=["rv0"]
    )

    dct = json.loads(res.decode('utf-8'))
    # top-level meta
    assert set(dct.keys()) == {"inputs", "outputs"}
    assert set(dct["inputs"].keys()) == {"t1", "t2", "rv0"}
    assert len(dct["outputs"]) == 1

    # outputs
    out_tensor_dict = dct["outputs"][0]
    assert out_tensor_dict["compression"] == "identity"
    data_decomp = base64.b64decode(out_tensor_dict["data"])
    out_tensor = torch.from_numpy(np.frombuffer(data_decomp, dtype=np.float32)).view(out_tensor_dict["size"])
    assert torch.allclose(out_tensor, outputs[0])

    # inputs
    inputs_dict = dct["inputs"]
