import json
import logging

import pytest

import attr
from deepdiff import DeepDiff

from logos.libs.logging import configure_lib_loggers

from ads.emily.viewer.backend.lib.converter.ml_engine import MlEngineConverter

logger = logging.getLogger(__name__)
configure_lib_loggers({"ads.emily": None}, debug=True)


@attr.s
class ModelTestCase(object):
    type = attr.ib(type=str)
    path = attr.ib(type=str)


CASES = [
    # TODO: @maksimkulis make this test canonical https://st.yandex-team.ru/EMILY-217
    # ModelTestCase(
    #     type="lm",
    #     path="LClick_positional_model_weight_newFeatures_rsyafactors_filtersearch_novideo_user_banner_qpf/20201128"
    # ),
    # ModelTestCase(
    #     type="mx",
    #     path="search_prod_fix_new_err_ignore_tsar_log_2_8000_0.24_mx/20201122"
    # ),
    ModelTestCase(
        type="dmlc",
        path="rsya_ab_conv_nirvana_dmlc_static_v0_f_tIsGoalReached_rwm1_eta0.01_passes50_l1_6500_s1_28d/2020-11-26"
    ),
    ModelTestCase(
        type="tlm",
        path="tlm_lm_production_quad_efh_big_10_100_no_crr/20201103"
    ),
    ModelTestCase(
        type="vw",
        path="blv1mk_APC2_rev_kub/20210313"
    )
]


@pytest.mark.parametrize("model_case", CASES)
def test_models(
        model_case  # type: ModelTestCase
):  # type: (...) -> None

    logger.debug("Testing converter of ML_ENIGNE_DUMP to models_v2 for {}".format(model_case.type))

    converter = MlEngineConverter(model_case.path)
    meta_path = "{}/model_v2.json".format(model_case.path)

    with open(meta_path, "r") as test_file:
        correct_json = json.loads(json.dumps(json.load(test_file), ensure_ascii=False))
    current_json = json.loads(json.dumps(converter.model_to_json(model_case.path), ensure_ascii=False))
    diff = DeepDiff(current_json, correct_json, ignore_order=True, significant_digits=8)
    if diff:
        jdiff = json.loads(diff.json)
        assert False, "{}\n{}".format(model_case.type, json.dumps(jdiff, indent=3, ensure_ascii=False))
