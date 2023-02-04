from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.workflow.workflow import process_flow
from ads.nirvana.online_learning.run_flowkeeper.workflow_tests.lib.resource_mocks import resource_factory  # noqa
from ads.nirvana.online_learning.path_resolver.path_resolver import PathResolver
from ads.nirvana.online_learning.datetime_utils import convert_datetime
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.model_matcher import DEFAULT_MR_TABLE_FIELDS
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.sandbox import SandboxStateHandler

# imports for monkey patching
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.user.pipelines import *  # noqa
from yabs.tabutils import dropTableWithMeta, uploadTSFile, read_ts_table
from yabs.logger import error
from mapreducelib import MapReduce
import yabs.conf
import tempfile

import pytest
import yatest
import os
import shutil
import simplejson as sj
from pprint import pformat
from freezegun import freeze_time


@pytest.yield_fixture()
def yt_base_dir():
    yield "online_learning_unittest_dir"


@pytest.yield_fixture()
def ml_task_id():
    yield "workflow_tests_taskid"


@pytest.yield_fixture()
def flowkeeper_conf(yt_base_dir, ml_task_id):
    yield yabs.conf.utils.AttrDict({
        "online_flowkeeper": {
            "recovery_policy": {
                "max_fail_count": 10,
                "max_leader_run_time": 3600,
                "future_trail_time_length": 5
            },
            "nirvana": {
                "tags": ['online'],
                "oauth_token": "blablablatoken",
                "url": "nirvana_url",
                "request_retries": 30,
                "request_delay": 300,
            },
            "sandbox": {
                "state_file": "run_nirvana_online_learning_state.json",
                "new_graphs_file": "run_nirvana_online_learning_new_graphs_queue.json"
            },
            "flow": {
                "pipeline_type": "SandboxedUserPipeline"
            },
            "model_storage": {
                "full_state_models": yt_base_dir,
                "ml_task_id": ml_task_id
            },
            "model_matcher": {
                "prev_model_table": yt_base_dir,
                "ml_task_id": ml_task_id
            },
            "non_leader_filters": {
                "max_non_leader_delay": "max_non_leader_delay"
            },
            "graphite": {
                "graphite_prefix": "one_hour.online_learning.graph_runtime_info",
                "attempts": 2,
                "delay": 10,
                "backoff": 1.0
            }
        }
    })


TEST_NAMES = [
    "initial_tasks_flow_without_leader",
    "tasks_flow_with_dublicates",
    "many_failed_graphs_with_big_trails"
]

WORK_DIRS = [yatest.common.source_path(os.path.join("ads/nirvana/online_learning/run_flowkeeper/workflow_tests/fixture",
                                                    x)) for x in TEST_NAMES]


@pytest.yield_fixture(params=WORK_DIRS, ids=TEST_NAMES)  # noqa
def configure_test_launch(request, ml_task_id, yt_base_dir, local_yt, monkeypatch):
    # copy files to '.' dir for current test
    cur_work_dir = request.param
    copied_files = os.listdir(cur_work_dir)
    assert set(copied_files) == {'model_matcher_table', 'run_nirvana_online_learning_new_graphs_queue.json',
                                 'run_nirvana_online_learning_state.json',
                                 'datetime_now'}, "You does not have all necessary files for this particular test"

    # Copy state and new graphs files to local directory like in Sandbox
    for x in copied_files:
        shutil.copy(os.path.join(cur_work_dir, x), os.path.join('.', x))

    # Create model matcher table on local YT
    path_resolver = PathResolver(ml_task_id, yt_base_dir)
    with open('./model_matcher_table') as f:
        uploadTSFile(f, path_resolver.get_model_matcher_table_path())

    with tempfile.NamedTemporaryFile() as tmp:
        # Patch path for update_sandbox_state function (in test-env, we are not permitted to write files to '.')
        def _testenv_update_sandbox_state(task, tasks_flow, *args):
            state_file_handler = SandboxStateHandler(tmp.name)
            dump_tasks = tasks_flow.get_tasks_to_state_dump()
            state_file_handler.write_tasks(dump_tasks)

        idx = SandboxedUserPipeline.FINALIZE.index(update_sandbox_state)
        SandboxedUserPipeline.FINALIZE[idx] = _testenv_update_sandbox_state

        # Freeze datetime.now() as if we have launcher flowkeeper at some date
        with open("./datetime_now") as f:
            datetime_now = convert_datetime(f.readlines()[0].strip())

        with freeze_time(datetime_now):
            yield tmp.name

    # reverse patch
    idx = SandboxedUserPipeline.FINALIZE.index(_testenv_update_sandbox_state)
    SandboxedUserPipeline.FINALIZE[idx] = update_sandbox_state

    # Drop model matcher table to avoid collisions between tests (local_yt does not drop tables between tests and
    # if tables have equal names, we can propagate dependencies in tests)
    yt_client = MapReduce.get_yt_client()
    if yt_client.exists(path_resolver.get_model_matcher_table_path()):
        dropTableWithMeta(path_resolver.get_model_matcher_table_path())

    # remove all files from '.'
    for x in copied_files:
        os.remove(x)


def test__process_flow(configure_test_launch, resource_factory, flowkeeper_conf, local_yt):  # noqa
    state_file_path = configure_test_launch
    process_flow(flowkeeper_conf.online_flowkeeper)

    with open(state_file_path) as f:
        result = sj.load(f)

    # we will filter out restart_time field because by now it is used only for information and never used in the code
    # But keeping this field can cause unstability in test-env (like every feature connected with datetime)
    for x in result:
        x.pop('restart_time')
    error("\n\nPrinting resulting state.json\n\n")
    error(pformat(result))
    error("\n\n#############################\n\n")

    # Read the model matcher table
    path_resolver = PathResolver(flowkeeper_conf.online_flowkeeper.model_matcher.ml_task_id,
                                 flowkeeper_conf.online_flowkeeper.model_matcher.prev_model_table)
    recs = [r for r in read_ts_table(path_resolver.get_model_matcher_table_path())]
    recs = sorted([{f: getattr(r, f) for f in DEFAULT_MR_TABLE_FIELDS + ["TaskID"]} for r in recs])
    error("\n\nPrinting result ts table\n\n")
    error(pformat(recs))
    error("\n\n#########################\n\n")

    # Compare flowkeeper state and model matcher table with canonicals
    return sorted(result), recs
