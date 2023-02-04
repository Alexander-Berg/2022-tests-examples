import json
import torch.nn
import torch.nn.functional

from ads_pytorch import (
    ModelFactory,
    ParameterServerOptimizer,
    BaseParameterServerModule,
    wrap_model_with_concat_wrapper,
    get_mark
)
from densenet_tsar_query_attention_v2.network_factory import BuildNetworkOutput

from ads_pytorch.nn.optim.radam import RAdam
from ads_pytorch.nn.module.standard_scaler import (
    StandardScalerParameterServerOptimizer,
    is_standard_scaler_param
)
from ads_pytorch.nn.module.tzar_tensor_convolution import TzarDoubleTopLevelModel
from densenet_tsar_query_attention_v2 import (
    build_network,
    build_embedding_model,
    read_embedding_descriptors,
)
from ads_pytorch.nn.module.base_embedding_model import (
    BaseEmbeddingModel,
    build_embedding_optimizers
)
from ads_pytorch.deploy.deployable_model import IDeployableModel, TorchModuleDeployDescriptor, \
    ParameterServerModelDeployDescriptor

import torch
from typing import List, Optional, Any, Dict, Union

from ads_pytorch.nn.module.base_embedding_model import embedding_descriptors_to_dim_dict


class SimpleDSSM(IDeployableModel):
    def __init__(
        self,
        user: BuildNetworkOutput,
        banner: BuildNetworkOutput,
        embeddings: BaseEmbeddingModel
    ):
        super(SimpleDSSM, self).__init__(embedding_model=embeddings)
        self.user = user.net
        self.banner = banner.net

        self.user_features = list(user.features)
        self.banner_features = list(banner.features)

        self.top_level = TzarDoubleTopLevelModel()
        dim_dict = embedding_descriptors_to_dim_dict(self.embedding_model.embedding_descriptors)
        self._embedding_names = list(dim_dict.keys())

    def deployable_model_forward(self, embedded_inputs: List[torch.Tensor]) -> Any:
        user = self.user(embedded_inputs)
        banner = self.banner(embedded_inputs)

        holder = self.embedding_model.get_feature_order_holder()
        return {
            "IsClick": embedded_inputs[holder.get_ids(["IsClick"])[0]].detach(),
            "prediction": self.top_level(user, banner)
        }

    def get_serializable_models(self) -> Dict[str, Union[TorchModuleDeployDescriptor, ParameterServerModelDeployDescriptor]]:
        return {
            "banner": ParameterServerModelDeployDescriptor(
                features_order=self.banner_features,
                model=self.banner,
            ),
            "user": ParameterServerModelDeployDescriptor(
                features_order=self.user_features,
                model=self.user
            )
        }


class BCEWithDictInput(torch.nn.Module):
    def forward(self, inputs, targets):
        predictions = inputs["prediction"]
        return torch.nn.functional.binary_cross_entropy_with_logits(
            input=predictions,
            target=targets,
            reduction="mean"
        )


class TrainDescription(ModelFactory):
    def __model_factory_post_init__(self):
        with open(self.model_config_path, 'rt') as f:
            self.parsed_model_conf = json.load(f)

    def create_model(self) -> BaseParameterServerModule:
        embedding_model = build_embedding_model(features_config=self.parsed_model_conf["model"]["features"])
        user_model = build_network(
            cfg=self.parsed_model_conf["model"]["user"],
            embedding_descriptors=embedding_model.embedding_descriptors,
            feature_order_holder=embedding_model.get_feature_order_holder()
        )
        banner_model = build_network(
            cfg=self.parsed_model_conf["model"]["banner"],
            embedding_descriptors=embedding_model.embedding_descriptors,
            feature_order_holder=embedding_model.get_feature_order_holder()
        )
        model = SimpleDSSM(user=user_model, banner=banner_model, embeddings=embedding_model)
        return wrap_model_with_concat_wrapper(model)

    def create_optimizer(self, model) -> ParameterServerOptimizer:
        model_conf = self.parsed_model_conf["model"]
        deep_params = list(model.buffer_parameters())
        scaler_params = [p for p in deep_params if is_standard_scaler_param(p)]
        deep_params = [p for p in deep_params if not is_standard_scaler_param(p)]

        top_level_params = [p for p in deep_params if "top_level_tzar_mark" in get_mark(p)]
        usual_deep_params = [p for p in deep_params if "top_level_tzar_mark" not in get_mark(p)]

        optimizers = [
            RAdam(
                [
                    {"params": usual_deep_params, "zero_lr_steps": 10000},
                    {"params": top_level_params, "zero_lr_steps": 0},
                ],
                lr=model_conf["nn"]["learning_rate"]
            )
        ]

        net: SimpleDSSM = model.net

        optimizers += build_embedding_optimizers(
            embedding_model=net.embedding_model,
            features_config=model_conf["features"]
        )

        if len(scaler_params):
            optimizers.append(StandardScalerParameterServerOptimizer(scaler_params))

        return ParameterServerOptimizer(*optimizers)

    def create_loss(self) -> torch.nn.modules.loss._Loss:
        return BCEWithDictInput()

    def get_required_features_list(self) -> Optional[List[str]]:
        feature_cfg = self.parsed_model_conf["model"]["features"]

        def _cast(feature: str):
            return feature if isinstance(feature, str) else tuple(feature)

        embedding_features = sum([
            [_cast(x) for x in descr.get("features", [descr["name"]])]
            for descr in feature_cfg["embeddings"]["features"]
        ], [])

        print(embedding_features)
        external_features = feature_cfg.get("external", [])
        return embedding_features + external_features
