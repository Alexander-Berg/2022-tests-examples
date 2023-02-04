import copy
import json
import functools
import shutil
import tempfile
import torch
import torch.nn
from collections import OrderedDict
from typing import Dict, List, Set, Union, Tuple, Optional, Any

from ads_pytorch import ModelFactory, BaseParameterServerModule, ParameterServerOptimizer, EmbeddingAdamOptimizer
from ads_pytorch.nn.module.base_embedding_model import FeatureOrderHolder, BaseEmbeddingModel
from ads_pytorch.nn.module.standard_scaler import STANDARD_SCALER_MARK

from djtorch.interface import EmbeddingModelInfo, EmbeddingModel, DeepModel
from djtorch.losses import filter_loss, fps_loss, log_loss, pair_log_loss
from djtorch.optimizers import DeepAdamOptimizer, StandardScalerOptimizer
from djtorch.util import wrap_model_with_concat_wrapper, get_wrapped_model, get_wrapped_model_parameters, mark_module, mark_parameter, get_mark

from djtorch.projects.multi_trainer.document_network import (
    RecurseDescriptor,
    DeepDescriptor,
    LpNormAggregatorDescriptor,
    TransformerAggregatorDescriptor,
    TextDescriptor,
    HistoryDescriptor,
    DocumentNetwork
)


class DSSMModel(DeepModel):
    def __init__(
        self,
        left_vector_embeddings: Dict[str, int],
        left_text_embeddings: Dict[Union[str, Tuple[str, ...]], TextDescriptor],
        left_text_cf_embeddings: Dict[str, TextDescriptor],
        left_history_embeddings: Dict[str, HistoryDescriptor],
        left_recurse_descriptor: Optional[RecurseDescriptor],
        left_deep_descriptor: DeepDescriptor,
        right_vector_embeddings: Dict[str, int],
        right_text_embeddings: Dict[Union[str, Tuple[str, ...]], TextDescriptor],
        right_text_cf_embeddings: Dict[str, TextDescriptor],
        right_history_embeddings: Dict[str, HistoryDescriptor],
        right_recurse_descriptor: Optional[RecurseDescriptor],
        right_deep_descriptor: DeepDescriptor,
        cross_embeddings: Union[Optional[str], List[Optional[str]]],
        cross_deep_descriptor: Union[Optional[DeepDescriptor], List[Optional[DeepDescriptor]]],
        apply_descriptor: Optional[Dict[str, Any]],
        embedding_model: Optional[Union[EmbeddingModelInfo, BaseEmbeddingModel]] = None,
        feature_holder: Optional[FeatureOrderHolder] = None
    ):
        super(DSSMModel, self).__init__(embedding_model=embedding_model, feature_holder=feature_holder)

        self.left = DocumentNetwork(
            vector_embeddings=left_vector_embeddings,
            text_embeddings=left_text_embeddings,
            text_cf_embeddings=left_text_cf_embeddings,
            history_embeddings=left_history_embeddings,
            recurse_descriptor=left_recurse_descriptor,
            deep_descriptor=left_deep_descriptor,
            embedding_model=self.descriptor.embedding_model
        )

        self.right = DocumentNetwork(
            vector_embeddings=right_vector_embeddings,
            text_embeddings=right_text_embeddings,
            text_cf_embeddings=right_text_cf_embeddings,
            history_embeddings=right_history_embeddings,
            recurse_descriptor=right_recurse_descriptor,
            deep_descriptor=right_deep_descriptor,
            embedding_model=self.descriptor.embedding_model
        )

        self.features = self.left.out_features
        if self.features != self.right.out_features:
            raise Exception("Left and right submodels are not consistnet in dssm model")

        self.bias = self.left.out_bias
        if self.bias != self.right.out_bias:
            raise Exception("Left and right submodels are not consistnet in dssm model")

        self.norm = self.left.out_norm
        if self.norm != self.right.out_norm:
            raise Exception("Left and right submodels are not consistnet in dssm model")

        self.linear = torch.nn.Parameter(torch.stack([torch.ones(len(self.features)), torch.zeros(len(self.features))]))

        self.cross_embeddings = {}
        self.cross = torch.nn.ModuleDict()
        for features_index, (features, bias) in enumerate(zip(self.features, self.bias)):
            embeddings = cross_embeddings[features_index] if isinstance(cross_embeddings, list) else cross_embeddings
            deep_descriptor = cross_deep_descriptor[features_index] if isinstance(cross_deep_descriptor, list) else cross_deep_descriptor
            if deep_descriptor is not None:
                cross_features = 2 * (features + bias)
                if embeddings:
                    if embeddings == "mul":
                        cross_features += features
                    elif embeddings == "sqr":
                        cross_features += 3 * features
                    else:
                        raise Exception("Invalid cross_embeddings value: {}".format(embeddings))
                    self.cross_embeddings[str(features_index)] = embeddings
                self.cross[str(features_index)] = DocumentNetwork.make_deep_network(
                    deep_descriptor=deep_descriptor,
                    features=cross_features
                )
                mark_module(self.cross[str(features_index)], f"output:{features_index}")
            self.left.mark(f"output:{features_index}", features_index)
            self.right.mark(f"output:{features_index}", features_index)
            mark_parameter(self.linear, f"output:{features_index}")

        self.apply_descriptor = apply_descriptor

    def cross_forward(self, features_index: int, left_features: torch.Tensor, right_features: torch.Tensor):
        left_bias = None
        right_bias = None
        if self.bias[features_index]:
            left_features, left_bias = left_features[:, :-1], left_features[:, -1]
            right_features, right_bias = right_features[:, :-1], right_features[:, -1]

        cross = None
        if str(features_index) in self.cross:
            if not self.applying:
                cross_left_features = left_features.repeat_interleave(right_features.size(0), dim=0)
                cross_right_features = right_features.repeat([left_features.size(0), 1])
                cross_left_bias = left_bias.repeat_interleave(right_features.size(0), dim=0) if left_bias is not None else None
                cross_right_bias = right_bias.repeat([left_features.size(0)]) if right_bias is not None else None
            else:
                cross_left_features = left_features
                cross_right_features = right_features
                cross_left_bias = left_bias
                cross_right_bias = right_bias
            cross_features = [cross_left_features, cross_right_features]
            if cross_left_bias is not None:
                cross_features.append(cross_left_bias.unsqueeze(1))
            if cross_right_bias is not None:
                cross_features.append(cross_right_bias.unsqueeze(1))
            embeddings = self.cross_embeddings.get(str(features_index))
            if embeddings in {"mul", "sqr"}:
                cross_features.append(cross_left_features * cross_right_features)
            if embeddings == "sqr":
                cross_features.append(torch.square(cross_left_features))
                cross_features.append(torch.square(cross_right_features))
            cross = torch.cat(self.cross[str(features_index)](torch.cat(cross_features, dim=1)), dim=1)
        if not self.applying:
            score = left_features @ right_features.t()
            if left_bias is not None:
                score += left_bias.unsqueeze(1)
            if right_bias is not None:
                score += right_bias.unsqueeze(0)
            yield score.mul_(self.linear[0, features_index]).add_(self.linear[1, features_index])
            if cross is not None:
                for index in range(cross.size(1)):
                    yield cross[:, index].reshape(left_features.size(0), right_features.size(0))
        else:
            dot = (left_features * right_features).sum(dim=1)
            left_norm = torch.sqrt(torch.square(left_features).sum(dim=1))
            right_norm = torch.sqrt(torch.square(right_features).sum(dim=1))

            def sum_result(*terms):
                terms = [term for term in terms if term is not None]
                return functools.reduce(torch.add, terms) if terms else torch.zeros_like(dot)

            for column in self.apply_descriptor["columns"][features_index]["columns"]:
                if column == "result":
                    yield sum_result(dot, left_bias, right_bias) * self.linear[0, features_index] + self.linear[1, features_index]
                elif column == "score":
                    yield sum_result(dot, left_bias, right_bias)
                elif column == "left_norm":
                    yield left_norm
                elif column == "right_norm":
                    yield right_norm
                elif column == "cos":
                    yield dot / (left_norm * right_norm).clamp(min=1e-30)
                elif column == "left_dot":
                    yield dot / left_norm.clamp(min=1e-30)
                elif column == "right_dot":
                    yield dot / right_norm.clamp(min=1e-30)
                elif column == "dot":
                    yield dot
                elif column == "left_bias":
                    yield sum_result(left_bias)
                elif column == "right_bias":
                    yield sum_result(right_bias)
                elif column == "bias":
                    yield sum_result(left_bias, right_bias)
                elif column == "net":
                    if cross is None or cross.size(1) != 1:
                        raise Exception("Invalid column net usage")
                    yield cross[:, 0]
                elif column.startswith("net") and column[len("net"):].isdigit():
                    index = int(column[len("net"):])
                    if cross is None or index >= cross.size(1):
                        raise Exception("Invalid column net index: {}".format(index))
                    yield cross[:, index]
                else:
                    raise Exception("Invalid column name: {}".format(column))

    def forward(self, inputs: List[torch.Tensor]) -> torch.Tensor:
        left = self.left(inputs)
        right = self.right(inputs)
        result = []
        for features_index, (left_features, right_features) in enumerate(zip(left, right)):
            result.extend(self.cross_forward(features_index=features_index,
                                             left_features=left_features,
                                             right_features=right_features))
        return result[0] if len(result) == 1 else (torch.stack(result, dim=1) if self.applying else result)

    @property
    def out_features(self):
        dot_features = sum(1 + 5 * bool(norm) + 4 * bias for bias, norm in zip(self.bias, self.norm))
        deep_features = sum(sum(deep.out_features) for deep in self.cross.values())
        return dot_features + deep_features

    @property
    def apply_descriptor(self):
        return self._apply_descriptor

    @apply_descriptor.setter
    def apply_descriptor(self, apply_descriptor):
        self._apply_descriptor = copy.deepcopy(apply_descriptor)
        if self._apply_descriptor is None:
            return

        if self._apply_descriptor.get("columns") is None:
            self._apply_descriptor["columns"] = [None] * len(self.features)
        if len(self._apply_descriptor.get("columns")) != len(self.features):
            raise Exception("Invalid apply columns length")

        for features_index, (bias, norm) in enumerate(zip(self.bias, self.norm)):
            if self._apply_descriptor["columns"][features_index] is None:
                self._apply_descriptor["columns"][features_index] = {}
            features_descriptor = self._apply_descriptor["columns"][features_index]
            if features_descriptor.get("prefix") is None:
                features_descriptor["prefix"] = ""
            if features_descriptor["prefix"] == "":
                if len(self.features) > 1:
                    features_descriptor["prefix"] = "head{}".format(features_index)
            elif not features_descriptor["prefix"].endswith("_"):
                features_descriptor["prefix"] += "_"
            if features_descriptor.get("columns") is None:
                features_descriptor["columns"] = ["score"]
                if norm:
                    features_descriptor["columns"] += ["left_norm", "right_norm", "cos", "left_dot", "right_dot"]
                if bias:
                    features_descriptor["columns"] += ["dot", "left_bias", "right_bias", "bias"]
                if str(features_index) in self.cross:
                    if sum(self.cross[str(features_index)].out_features) == 1:
                        features_descriptor["columns"].append("net")
                    else:
                        for index in range(sum(self.cross[str(features_index)].out_features)):
                            features_descriptor["columns"].append("net{}".format(index))

    def deploy_descriptor(self):
        descriptor = self.descriptor.copy()
        descriptor.deep_model.apply_descriptor = {}
        return descriptor

    def export(self):
        return {
            self.MODULE_NAME_SERIALIZE_KEY: "dssm_rec_dssm",
            self.VERSION_SERIALIZE_KEY: 1,
            "features": self.features,
            "bias": self.bias,
            "norm": self.norm,
            "cross_embeddings": self.cross_embeddings
        }


class DSSMLoss(torch.nn.modules.loss._Loss):
    def __init__(
        self,
        loss_type: Union[str, List[str]] = "fps",
        max_len: Union[int, List[int]] = 200,
        implicit: Union[float, List[float]] = 0.0,
        filter: Union[str, List[str]] = "",
        weight_power: Union[float, List[float]] = 1.0,
        group_weight_power: Union[float, List[float]] = 1.0,
        threshold: Union[float, List[float]] = 0.0,
        reduction: str = "mean"
    ):
        super(DSSMLoss, self).__init__(reduction=reduction)
        for loss in (loss_type if isinstance(loss_type, list) else [loss_type]):
            if loss not in {"fps", "logit", "pair_logit"}:
                raise Exception("Invalid loss type: {}".format(loss))
        self.loss_type = loss_type
        self.max_len = max_len
        self.implicit = implicit
        self.filter = filter
        self.weight_power = weight_power
        self.group_weight_power = group_weight_power
        self.threshold = threshold

    def forward(self,
                prediction: Tuple[torch.Tensor, List[torch.Tensor]],
                targets: Tuple[torch.Tensor, List[torch.Tensor], Dict[str, torch.Tensor]]):
        if targets is None:
            targets, weights, group_weights, groups = None, None, None, None
        elif isinstance(targets, torch.Tensor):
            targets, weights, group_weights, groups = targets, None, None, None
        elif isinstance(targets, dict):
            targets, weights, group_weights, groups = targets.get("target"), targets.get("weight"), targets.get("group_weight"), targets.get("group_id")
        else:
            raise TypeError(f"Invalid type of targets in DSSMLoss: {type(targets)}")
        prediction_list = prediction if isinstance(prediction, list) else [prediction]
        results = []
        for prediction_index, prediction in enumerate(prediction_list):
            loss_type = self.loss_type[prediction_index] if isinstance(self.loss_type, list) else self.loss_type
            max_len = self.max_len[prediction_index] if isinstance(self.max_len, list) else self.max_len
            implicit = self.implicit[prediction_index] if isinstance(self.implicit, list) else self.implicit
            filter = self.filter[prediction_index] if isinstance(self.filter, list) else self.filter
            weight_power = self.weight_power[prediction_index] if isinstance(self.weight_power, list) else self.weight_power
            group_weight_power = self.group_weight_power[prediction_index] if isinstance(self.group_weight_power, list) else self.group_weight_power
            threshold = self.threshold[prediction_index] if isinstance(self.threshold, list) else self.threshold
            cur_prediction, cur_targets, cur_weights, cur_group_weights, cur_groups = filter_loss(prediction, targets, weights, group_weights, groups, filter, threshold)
            if cur_weights is None and cur_targets is not None and loss_type == "fps":
                cur_weights = cur_targets
            if cur_weights is not None and weight_power != 1.0:
                cur_weights = torch.pow(cur_weights, weight_power) if weight_power != 0.0 else None
            if cur_group_weights is not None and group_weight_power != 1.0:
                cur_group_weights = torch.pow(cur_group_weights, group_weight_power) if group_weight_power != 0.0 else None
            if loss_type == "fps":
                results.append(fps_loss(
                    prediction=cur_prediction,
                    weights=cur_weights,
                    group_weights=cur_group_weights,
                    groups=cur_groups,
                    reduction=self.reduction,
                    max_len=max_len
                ))
            elif loss_type == "logit":
                results.append(log_loss(
                    prediction=cur_prediction,
                    targets=cur_targets,
                    weights=cur_weights,
                    group_weights=cur_group_weights,
                    groups=cur_groups,
                    reduction=self.reduction,
                    max_len=max_len,
                    implicit=implicit,
                    threshold=threshold
                ))
            elif loss_type == "pair_logit":
                results.append(pair_log_loss(
                    prediction=cur_prediction,
                    targets=cur_targets,
                    weights=cur_weights,
                    group_weights=cur_group_weights,
                    groups=cur_groups,
                    reduction=self.reduction
                ))
            else:
                raise Exception("Invalid loss type: {}".format(self.loss_type))
        return functools.reduce(torch.add, results)


class TrainDescription(ModelFactory):
    def __model_factory_post_init__(self):
        with open(self.model_config_path, 'rt') as stream:
            self.parsed_model_conf = json.load(stream)
        if self.all_files and "applier_config" in self.all_files:
            with open(self.all_files["applier_config"], "rt") as stream:
                self.parsed_apply_conf = json.load(stream)
        else:
            self.parsed_apply_conf = None

    def _get_vector_embeddings(self, config: Any) -> Dict[str, int]:
        return OrderedDict((x["name"], x["dim"]) for x in config.get("vector", []))

    def _get_text_embeddings(self, config: Any) -> Dict[Union[str, Tuple[str, ...]], TextDescriptor]:
        return OrderedDict(
            (x["name"], TextDescriptor(dim=x["dim"],
                                       algo_type=x.get("algo_type", "adam"),
                                       compute_mode=x.get("compute_mode", "mean"),
                                       layer_name=x.get("layer_name")))
            for x in config.get("text", [])
        )

    def _get_text_cf_embeddings(self, config: Any) -> Dict[str, TextDescriptor]:
        return OrderedDict(
            (x["name"], TextDescriptor(dim=None,
                                       algo_type=x.get("algo_type", "adam"),
                                       compute_mode=x.get("compute_mode", "mean"),
                                       layer_name=x.get("layer_name")))
            for x in config.get("text_cf", [])
        )

    def _get_history_embeddings(self, config: Any) -> Dict[str, HistoryDescriptor]:
        aggregators = []
        for aggregator_conf in config.get("aggregators", [{"type": "lp_norm", "p": ["inf", "fro", "4", "1"]}]):
            if aggregator_conf["type"] == "lp_norm":
                args = dict(relu=aggregator_conf.get("relu", False),
                            norm=aggregator_conf.get("norm", "*"),
                            bias=aggregator_conf.get("bias", False),
                            mark=aggregator_conf.get("mark"))
                if isinstance(aggregator_conf["p"], list):
                    aggregators.extend(LpNormAggregatorDescriptor(p=p, **args) for p in aggregator_conf["p"])
                else:
                    aggregators.append(LpNormAggregatorDescriptor(p=aggregator_conf["p"], **args))
            elif aggregator_conf["type"] == "transformer":
                aggregators.append(TransformerAggregatorDescriptor(depth=aggregator_conf["depth"],
                                                                   max_history=aggregator_conf["max_history"],
                                                                   position_dim=aggregator_conf.get("position_dim"),
                                                                   linear_dim=aggregator_conf.get("linear_dim"),
                                                                   num_heads=aggregator_conf.get("num_heads", 4),
                                                                   cls_tokens=aggregator_conf.get("cls_tokens", 1),
                                                                   out_features=aggregator_conf.get("out_features"),
                                                                   mark=aggregator_conf.get("mark")))
            else:
                raise Exception("Invalid aggregator type: {}".format(aggregator_conf["type"]))
        result = OrderedDict()
        for x in config.get("history", []):
            deep_dim = x.get("deep_dim") or None
            deep_norm = x.get("deep_norm", "*")
            if deep_dim and not isinstance(deep_dim, list):
                deep_dim = [deep_dim] * len(aggregators)
            result[x["name"]] = HistoryDescriptor(type=x.get("type", "cat"),
                                                  vector_embeddings=self._get_vector_embeddings(x),
                                                  text_embeddings=self._get_text_embeddings(x),
                                                  additional_text_embeddings=self._get_text_embeddings(x.get("additional_embeddings", {})),
                                                  deep=self._get_deep_descriptor("history", final_dim=deep_dim, final_norm=deep_norm),
                                                  aggregators=aggregators)
        return result

    def _get_recurse_descriptor(self, config: Any) -> Optional[RecurseDescriptor]:
        if "recurse" not in config:
            return None
        batch_norm_multiplier = config["recurse"].get("batch_norm_multiplier", 0.0)
        return RecurseDescriptor(
            width=config["recurse"]["width"],
            depth=config["recurse"]["depth"],
            blocks=config["recurse"].get("blocks", 1),
            orthogonal=config["recurse"].get("orthogonal", True),
            rescale=config["recurse"].get("rescale", True),
            method=config["recurse"].get("method", "cholesky"),
            type=config["recurse"].get("type", "periodic"),
            activation=config["recurse"].get("activation", "relu"),
            input_batch_norm_multiplier=config["recurse"].get("input_batch_norm_multiplier", batch_norm_multiplier),
            dropout=config["recurse"].get("dropout_probability", 0.0),
            mark=config["recurse"].get("mark")
        )

    def _get_deep_descriptor(self, name: str, **args) -> Union[Optional[DeepDescriptor], List[Optional[DeepDescriptor]]]:
        def submodel_func(submodel_conf, final_dim, final_norm, final_bias):
            batch_norm_multiplier = submodel_conf.get("batch_norm_multiplier", 0.0)
            return DeepDescriptor(
                width=submodel_conf["width"],
                depth=submodel_conf["depth"],
                split_depth=submodel_conf.get("split_depth", 0),
                out_features=final_dim,
                out_norm=final_norm,
                out_bias=final_bias,
                blocks=submodel_conf.get("blocks", 1),
                orthogonal=submodel_conf.get("orthogonal", True),
                rescale=submodel_conf.get("rescale", True),
                method=submodel_conf.get("method", "cholesky"),
                type=submodel_conf.get("type", "periodic"),
                final_orthogonal=submodel_conf.get("final_orthogonal", True),
                final_rescale=submodel_conf.get("final_rescale", True),
                activation=submodel_conf.get("activation", "relu"),
                input_batch_norm_multiplier=submodel_conf.get("input_batch_norm_multiplier", batch_norm_multiplier),
                dropout=submodel_conf.get("dropout_probability", 0.0),
                sample_in_features=submodel_conf.get("sample_in_features", None),
                architecture=submodel_conf.get("architecture", "dense_net"),
                mark=submodel_conf.get("mark")
            )
        return self._apply_final_output(submodel_func, name, **args)

    def _get_cross_embeddings(self, name: str) -> Union[Optional[str], List[Optional[str]]]:
        def submodel_func(submodel_conf, final_dim, final_norm, final_bias):
            return submodel_conf.get("additional_input")
        return self._apply_final_output(submodel_func, name)

    def _get_final_output(self, field: str, default: Optional[Any] = None) -> Union[Any, List[Any]]:
        model_conf = self.parsed_model_conf["model"]
        if field in model_conf:
            if "cross" in model_conf:
                if isinstance(model_conf["cross"], list):
                    if any(field in cross_conf for cross_conf in model_conf["cross"]):
                        raise Exception("{} could be defined in global or cross, not both".format(field))
                    if not isinstance(model_conf[field], list):
                        return [model_conf[field]] * len(model_conf["cross"])
                elif field in model_conf["cross"]:
                    raise Exception("{} could be defined in global or cross, not both".format(field))
            return model_conf[field]
        elif "cross" in model_conf:
            if isinstance(model_conf["cross"], list):
                if default is None and any(field not in cross_conf for cross_conf in model_conf["cross"]):
                    raise Exception("{} should be defined in global or cross".format(field))
                return [cross_conf.get(field, default) for cross_conf in model_conf["cross"]]
            else:
                if default is None and field not in model_conf["cross"]:
                    raise Exception("{} should be defined in global or cross".format(field))
                return model_conf["cross"].get(field, default)
        elif default is None:
            raise Exception("{} should be defined in global or cross".format(field))
        else:
            return default

    def _apply_final_output(self, submodel_func, name: str, **args):
        model_conf = self.parsed_model_conf["model"]
        submodel_conf = model_conf.get(name)
        if name == "cross":
            index = args.get("index")
            final_dim = self._get_final_output("final_dim")
            if index is None and (isinstance(submodel_conf, list) or isinstance(final_dim, list)):
                length = len(submodel_conf) if isinstance(submodel_conf, list) else len(final_dim)
                if isinstance(final_dim, list) and length != len(final_dim):
                    raise Exception("final_dim and cross sizes are inconsistent")
                return [self._apply_final_output(submodel_func, name, index=index) for index in range(length)]
            if index is not None:
                submodel_conf = submodel_conf[index] if isinstance(submodel_conf, list) else submodel_conf
            if submodel_conf is None:
                return None
            final_dim = submodel_conf.get("result_dim", 1)
            final_norm = args.get("final_norm", "*")
            final_bias = False
        elif name == "history":
            if not args.get("final_dim"):
                return None
            final_dim = args.get("final_dim")
            final_norm = args.get("final_norm", "*")
            final_bias = False
        else:
            final_dim = self._get_final_output("final_dim")
            final_norm = self._get_final_output("final_norm", default="")
            final_bias = self._get_final_output("final_bias", default=False)
        return submodel_func(submodel_conf=submodel_conf, final_dim=final_dim, final_norm=final_norm, final_bias=final_bias)

    def _get_optimizer_conf(self, name: str, subnames: Set[str]) -> Dict[str, Any]:
        result = {}
        model_conf = self.parsed_model_conf["model"]
        if isinstance(model_conf[name], list):
            for subnames_set in ({None}, set(subnames)):
                if embedding_names_set:
                    for item in model_conf[name]:
                        if item.get("name") in subnames_set:
                            result.update(item)
        else:
            result.update(model_conf[name])
        result.pop("name", None)
        return result

    def _get_dataset_table(self) -> str:
        if self.parsed_apply_conf is not None:
            return self.parsed_apply_conf["prediction_table"]["path"]
        else:
            return self.parsed_model_conf["train_data"]["table"]

    def _get_apply_descriptor(self) -> Optional[Dict[str, Any]]:
        applier_config = self.parsed_apply_conf if self.parsed_apply_conf is not None else self.parsed_model_conf.get("apply")
        return {"columns": applier_config.get("columns")} if applier_config is not None else {}

    def _ensure_deployable(self, model):
        if self.parsed_model_conf.get("save_convertible_model"):
            model.ensure_deployable()

    def _verify_reweight_factors(self, model):
        if not self.all_files or not self.all_files.get("factors_config.json"):
            return
        with open(self.all_files["factors_config.json"], 'rt') as stream:
            content = json.load(stream)
        if not isinstance(content, dict) or not isinstance(content.get('reweight_factors'), list):
            return
        factor_dict = {}
        for index, factor_item in enumerate(content['reweight_factors']):
            for factor in factor_item['factors']:
                if str(factor) in factor_dict:
                    raise Exception("Duplicated factor in reweight_factors: {}".format(factor))
                factor_dict[str(factor)] = index
        indexes_dict = {}
        for feature in model.embedding_model.embedding_descriptors:
            for subfeature in feature.features:
                indexes_dict.setdefault(feature.string_name, {}).setdefault(factor_dict.get(subfeature.string_name, -1), subfeature.string_name)
        for single_dict in indexes_dict.values():
            if len(single_dict) >= 2:
                raise Exception("An embedding layer has factors with different reweighting: {}".format(", ".join(single_dict.values())))

    def _save_column_description(self, model):
        applier_config = self.parsed_apply_conf if self.parsed_apply_conf is not None else self.parsed_model_conf.get("apply")
        if applier_config is None or model.deep_model.apply_descriptor is None:
            return
        if not applier_config.get("output_column_description"):
            return
        feature_prefix = applier_config.get("feature_prefix") or "DSSM"
        if feature_prefix and not feature_prefix.endswith("_"):
            feature_prefix += "_"
        with tempfile.NamedTemporaryFile(delete=False) as tmp_column_description:
            with open(tmp_column_description.name, "wt") as stream:
                index = 0
                stream.write("{}\tGroupId\tgroup_id\n".format(index))
                for columns in model.deep_model.apply_descriptor["columns"]:
                    for column in columns["columns"]:
                        index += 1
                        stream.write("{}\tNum\t{}{}{}\n".format(index, feature_prefix, columns["prefix"], column))
            shutil.move(tmp_column_description.name, applier_config.get("output_column_description"))

    def create_model(self) -> BaseParameterServerModule:
        left_config = self.parsed_model_conf["model"]["left"]
        right_config = self.parsed_model_conf["model"]["right"]
        cross_config = self.parsed_model_conf["model"].get("cross")
        model = DSSMModel(
            left_vector_embeddings=self._get_vector_embeddings(left_config),
            left_text_embeddings=self._get_text_embeddings(left_config),
            left_text_cf_embeddings=self._get_text_cf_embeddings(left_config),
            left_history_embeddings=self._get_history_embeddings(left_config),
            left_recurse_descriptor=self._get_recurse_descriptor(left_config),
            left_deep_descriptor=self._get_deep_descriptor("left"),
            right_vector_embeddings=self._get_vector_embeddings(right_config),
            right_text_embeddings=self._get_text_embeddings(right_config),
            right_text_cf_embeddings=self._get_text_cf_embeddings(right_config),
            right_history_embeddings=self._get_history_embeddings(right_config),
            right_recurse_descriptor=self._get_recurse_descriptor(right_config),
            right_deep_descriptor=self._get_deep_descriptor("right"),
            cross_embeddings=self._get_cross_embeddings("cross"),
            cross_deep_descriptor=self._get_deep_descriptor("cross"),
            apply_descriptor=self._get_apply_descriptor(),
            embedding_model=EmbeddingModel()
        ).descriptor.build()
        self._ensure_deployable(model)
        self._verify_reweight_factors(model)
        self._save_column_description(model)
        model.deep_model.apply_mode(self.parsed_apply_conf is not None or bool(self.parsed_model_conf.get("evaluation", {}).get("eval_confs", [])))
        return wrap_model_with_concat_wrapper(model)

    def create_optimizer(self, model) -> ParameterServerOptimizer:
        model_conf = self.parsed_model_conf["model"]
        optimizers = []
        scaler_optimizer_dict = {}
        adam_optimizer_dict = {}
        for nn_parameter in get_wrapped_model_parameters(model, ordered=True):
            optimizer_names = frozenset(get_mark(nn_parameter))
            if STANDARD_SCALER_MARK in optimizer_names:
                scaler_optimizer_dict.setdefault(optimizer_names, []).append(nn_parameter)
            else:
                adam_optimizer_dict.setdefault(optimizer_names, []).append(nn_parameter)
        for params in scaler_optimizer_dict.values():
            optimizers.append(StandardScalerOptimizer(params))
        for optimizer_names, params in adam_optimizer_dict.items():
            optimizer_conf = self._get_optimizer_conf("nn", optimizer_names)
            optimizers.append(
                DeepAdamOptimizer(
                    params,
                    lr=optimizer_conf["learning_rate"],
                    amsgrad=optimizer_conf.get("amsgrad", True),
                    fit_constraints_period=optimizer_conf.get("fit_constraints_period", 0),
                    gradient_accumulation=optimizer_conf.get("gradient_accumulation", 1)
                )
            )
        for embedding_parameter in model.hash_embedding_parameters():
            optimizer_names = set()
            for embedding in get_wrapped_model(model).embedding_model.embedding_descriptors:
                if get_wrapped_model(model).embedding_model.embeddings[embedding.string_name].parameter_with_hash_table is embedding_parameter:
                    optimizer_names.add(embedding.string_name)
                    optimizer_names.update(feature.string_name for feature in embedding.features)
            optimizer_conf = self._get_optimizer_conf("embedding", optimizer_names)
            optimizers.append(
                EmbeddingAdamOptimizer(
                    [embedding_parameter],
                    lr=optimizer_conf["learning_rate"],
                    ttl=optimizer_conf["expiration_ttl"],
                    l2=optimizer_conf.get("l2", 0.0)
                )
            )
        return ParameterServerOptimizer(*optimizers)

    def create_loss(self) -> torch.nn.modules.loss._Loss:
        model_conf = self.parsed_model_conf["model"]
        return DSSMLoss(
            reduction="mean",
            loss_type=model_conf["loss"]["type"],
            max_len=model_conf["loss"]["max_len"],
            implicit=model_conf["loss"].get("implicit", 0.0),
            filter=model_conf["loss"].get("filter", ""),
            weight_power=model_conf["loss"].get("weight_power", 1.0),
            group_weight_power=model_conf["loss"].get("group_weight_power", 1.0),
            threshold=model_conf["loss"].get("threshold", 0.0)
        )
