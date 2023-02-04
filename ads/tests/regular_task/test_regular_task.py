import os.path
import sys

import pytest
import yatest.common

from vh.core.frontend.globals import Graph as vhGraph
from vh.core.frontend.targets import File as vhFile

import ads.libs.py_yaml_loader as py_yaml_loader
import ads.ml_engine.tool.lib.entry_point


ARC_TASKS_BASE = 'ads/ml_engine/tests/regular_task/tasks'
TASK_YAMLS = {
    'matrixnet':
        'nbounce/akornet/BidCorrectionRSYA/mxnet_rsya.yml',
    'autobudget':
        'autobudget/mx_apc_8.yml',
    'matrixnet_gpu':
        'network/F16_w03_h02_alpha095_with_select_type_linear_model_on_gpu_ml_engine.yml',
    'mx_catboost':
        'network/mx_catboost_F16_w03_h02_alpha095_with_select_type_linear_model_on_gpu_ml_engine.yml',
    'vw':
        'pushkin/go_prod_2/go_prod_select_type_oriented_bm_category_title_drop_target_domain_engine_vw.yml',
    'dmlc':
        'basyl/vw_for_guar/vw_search_stable_light_60_dmlc.yml',
    'dssm': 'dssm/chatbot.yml',
    'no_split': 'minimal.yml',
    'calc_redefines': 'calc_redefines.yml'
}


class DummyVh(object):
    class Graph(vhGraph):
        pass

    class File(vhFile):
        pass

    @staticmethod
    def get_yt_token_secret():
        return "test_token"

    @staticmethod
    def get_yt_proxy():
        return "test_proxy"

    @staticmethod
    def run(*args, **kwargs):

        class DummyKeeper(object):
            def get_workflow_info(self):
                return DummyWorkflowInfo()

        class DummyWorkflowInfo(object):
            ID = 'dummy_workflow_id'

            @property
            def workflow_id(self):
                return self.ID

        return DummyKeeper()


@pytest.fixture
def monkey_vh(monkeypatch):
    monkeypatch.setattr(ads.ml_engine.lib, 'vh', DummyVh)
    monkeypatch.setattr(ads.ml_engine.tool.lib.entry_point, 'vh', DummyVh)
    monkeypatch.setattr(ads.ml_engine.tool.lib.entry_point, 'LoadTasks', task_root_load_task)
    monkeypatch.setattr(ads.ml_engine.lib, 'list_dumps', lambda task_id: [])


def task_root_load_task(filepaths, recursive):
    return py_yaml_loader.LoadTasks(
        filepaths=filepaths,
        tasks_root=yatest.common.source_path(ARC_TASKS_BASE),
        recursive=recursive,
    )


@pytest.fixture(
    params=TASK_YAMLS.keys(),
)
def call_arguments(request, monkeypatch):
    yaml_name = TASK_YAMLS[request.param]
    arguments = [
        '',
        '--nirvana-secret', 'dummy-token-name',
        '--token', 'dummy_file_token',
        '--sandbox-owner', 'SANDBOX_OWNER',
        '--timestamp', '1490821200',
        '--yt-pool', 'dummy-yt-pool',
        '--no-validation',
        yatest.common.source_path(os.path.join(ARC_TASKS_BASE, yaml_name)),
    ]
    monkeypatch.setattr(sys, 'argv', arguments)


@pytest.mark.usefixtures("monkey_vh", "call_arguments")
def test_run_combined_nirvana_regular():
    ads.ml_engine.tool.lib.entry_point.run_combined_nirvana_regular()
