from typing import Any, List, Dict, Union

import torch
import json
import asyncio

from ads_pytorch.deploy.deployable_model import (
    IDeployableModel,
    ParameterServerModelDeployDescriptor,
)

from ads_pytorch.deploy.deployable_model_serialization import (
    save_ideployable_model,
    IValidationDataSerializer
)

from ads_pytorch.nn.module.base_embedding_model import BaseEmbeddingModel
from ads_pytorch.tools.progress import ProgressLogger, TimeTracker
from ads_pytorch.core.model_serializer import StdoutModelSaverProgressLogger
from ads_pytorch.core.disk_adapter import DiskFileSystemAdapter, DiskSavePool

from densenet_tsar_query_attention_v2 import (
    build_network,
    build_embedding_model,
)

import json
import torch.nn
import torch.nn.functional

from ads_pytorch import (
    ModelFactory,
    ParameterServerOptimizer,
    BaseParameterServerModule,
    create_optimizer,
    wrap_model_with_concat_wrapper,
)

from ads_pytorch.nn.optim.shared_adam import SharedAdam
from ads_pytorch.nn.module.standard_scaler import (
    StandardScalerParameterServerOptimizer,
    is_standard_scaler_param
)
from ads_pytorch.nn.module.tzar_tensor_convolution import TzarDoubleTopLevelModel
from densenet_tsar_query_attention_v2 import (
    build_network,
    build_embedding_model,
    read_embedding_descriptors
)
from ads_pytorch.nn.module.base_embedding_model import BaseEmbeddingModel
from ads_pytorch.deploy.deployable_model import (
    IDeployableModel,
    TorchModuleDeployDescriptor,
    ParameterServerModelDeployDescriptor
)

import torch
from typing import List, Optional
import math


class SimpleDSSM(IDeployableModel):
    def __init__(
        self,
        user: torch.nn.Module,
        document: torch.nn.Module,
        embeddings: BaseEmbeddingModel
    ):
        super(SimpleDSSM, self).__init__(embedding_model=embeddings)
        self.user = user.net
        self.document = document.net
        self.embeddings = embeddings
        self.top_level = TzarDoubleTopLevelModel()
        self.user_features = list(user.features)
        self.document_features = list(document.features)

    def async_forward(self, inputs):
        return self.embeddings.async_forward(inputs)

    def sync_forward(self, async_outputs) -> torch.Tensor:
        seq_len = async_outputs["BigBQueryTexts"].shape[1]
        async_outputs["BigBQueryFactors"] = async_outputs["BigBQueryFactors"][:, :seq_len * 5]

        embedded = self.embeddings.sync_forward(async_outputs)
        user = self.user(embedded)
        document = self.document(embedded)
        return self.top_level(user, document)

    def get_serializable_models(self):
        return {
            "user": ParameterServerModelDeployDescriptor(
                features_order=self.user_features,
                model=self.user
            ),
            "document": ParameterServerModelDeployDescriptor(
                features_order=self.document_features,
                model=self.document
            )
        }


class TorchTensorDumper(IValidationDataSerializer):
    def serialize(self, inputs: Dict[str, Union[torch.Tensor, Dict[str, torch.Tensor], List[torch.Tensor]]],
                  outputs: List[torch.Tensor], embedding_model: BaseEmbeddingModel) -> bytes:
        return b'12345'

    def get_meta(self) -> Dict[str, Any]:
        return {
            "type": "ahaha"
        }


async def main():
    parsed_model_conf = json.load(open("config.json", "r"))
    print(parsed_model_conf)
    embedding_model = build_embedding_model(features_config=parsed_model_conf["model"]["features"])
    user_model = build_network(
        cfg=parsed_model_conf["model"]["user"],
        embedding_descriptors=embedding_model.embedding_descriptors,
        feature_order_holder=embedding_model.get_feature_order_holder()
    )
    document_model = build_network(
        cfg=parsed_model_conf["model"]["document"],
        embedding_descriptors=embedding_model.embedding_descriptors,
        feature_order_holder=embedding_model.get_feature_order_holder()
    )
    model = SimpleDSSM(
        user=user_model, document=document_model, embeddings=embedding_model
    )

    progress_logger = ProgressLogger([StdoutModelSaverProgressLogger()], frequency=30)
    time_tracker = TimeTracker()
    progress_logger.register(time_tracker)

    async with DiskSavePool() as save_pool:
        await save_ideployable_model(
            model=model,
            path="./model_dump",
            fs_adapter=DiskFileSystemAdapter(),
            save_pool=save_pool,
            progress_logger=progress_logger,
            validation_inputs=None,
            validation_tensors_dumper=TorchTensorDumper()
        )


if __name__ == '__main__':
    asyncio.run(main())
