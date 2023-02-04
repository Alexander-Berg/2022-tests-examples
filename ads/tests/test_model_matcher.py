import pytest
import datetime
from yabs.conf.utils import AttrDict
from ads.libs.py_test_mapreduce import write_mr_table  # noqa
from yabs.tabutils import read_ts_table
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource import TaskIdInfoError
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.model_matcher import DEFAULT_MR_TABLE_FIELDS, \
    get_model_matcher_from_flowkeeper_conf
from yabs.logger import error
from yabs.tabutils import dropTableWithMeta
from ads.nirvana.online_learning.datetime_utils import DATETIME_FORMAT

MAPREDUCE_HEADER = "# " + "\t".join(["TaskID:str"] + [x + ":str" for x in DEFAULT_MR_TABLE_FIELDS])
MAPREDUCE_VAL1 = "task_2016120700\tstrange_prevous_model\tsomepath/task_2016100100_full_state\t01\t02\t03"
MAPREDUCE_VAL2 = "task_2016120701\tsomepath/task_2016090100_full_state\tsomepath/task_2016100101_full_state\t01\t02\t03"


@pytest.yield_fixture()  # noqa
def prev_model_table(local_yt):
    header = MAPREDUCE_HEADER
    write_mr_table(
        "\n".join([header, MAPREDUCE_VAL1, MAPREDUCE_VAL2]),
        "//home/bs/prev_models_table/task/model_matcher_table")
    yield "//home/bs/prev_models_table/task/model_matcher_table"
    dropTableWithMeta("//home/bs/prev_models_table/task/model_matcher_table")


@pytest.fixture()
def flowkeeper_conf():
    return AttrDict({
        "online_flowkeeper": {
            "db": {
                "database": "online_learning",
                "tasks_table": "Tasks"
            },
            "recovery_policy": {
                "max_fail_count": 10,
                "wait_after_fail_min": 60,
                "max_run_time": 432000
            },
            "nirvana": {
                "tags": ['online'],
                "oauth_token": "blablablatoken",
                "url": "https://nirvana.yandex-team.ru",
                "request_retries": 30,
                "request_delay": 300,
                "flow": "76f1d00e-a4e3-11e6-98ff-0025909427cc",
                "start_flow": "8fb8ca56-a020-11e6-98ff-0025909427cc"
            },
            "sandbox": {
                "state_file": "run_nirvana_online_learning_state.json",
                "new_graphs_file": "run_nirvana_online_learning_new_graphs_queue.json"
            },
            "flow": {
                "SandboxedUserPipeline"
            },
            "model_storage": {
                "full_state_models": "ahaha_full_state_models1",
                "ml_task_id": "task"
            },
            "model_matcher": {
                "prev_model_table": "//home/bs/prev_models_table",
                "ml_task_id": "task"
            }
        }
    })


def sort_recs(recs):
    return sorted(recs, key=lambda x: x.TaskID)


def assert_records_equal(mr_rec, string):
    task_id, prev_model, cur_model, start_time, finish_time, guid = string.split('\t')
    assert mr_rec.TaskID == task_id
    assert mr_rec.PrevModelPath == prev_model
    assert mr_rec.ResultModelPath == cur_model
    assert mr_rec.FinishTime == finish_time
    assert mr_rec.WorkflowID == guid


def assert_cur_datetime_right(mr_rec):
    assert (datetime.datetime.strptime(mr_rec.StartTime,
                                       DATETIME_FORMAT) - datetime.datetime.now()).total_seconds() < 120


def test__configure_model_matcher_from_flowkeeper_conf(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    assert model_matcher.mapping_table == "//home/bs/prev_models_table/task/model_matcher_table"


def test__model_matcher_find_existing_task(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    prev_model_table = prev_model_table
    from mapreducelib import MapReduce
    assert MapReduce.table_exists(prev_model_table)
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    error(model_matcher.get_records_count())
    res = model_matcher.find_task_id_model_info("task_2016120701")
    assert res == {"PrevModelPath": "somepath/task_2016090100_full_state",
                   "ResultModelPath": "somepath/task_2016100101_full_state",
                   "StartTime": "01", "FinishTime": "02", "WorkflowID": "03"}
    assert model_matcher.get_records_count() == 2


def test__model_matcher_find_not_existing_task(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    prev_model_table = prev_model_table
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    with pytest.raises(TaskIdInfoError):
        model_matcher.find_task_id_model_info("svkjebrihjdfbj")


def test__model_matched_update_not_existing_empty(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120712")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], MAPREDUCE_VAL2)
    assert_records_equal(recs[2], "task_2016120712\t\t\t\t\t")
    assert model_matcher.get_records_count() == 3


def test__model_matched_update_not_existing_prev_model(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120712", PrevModelPath="ahahapath")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], MAPREDUCE_VAL2)
    assert_records_equal(recs[2], "task_2016120712\tahahapath\t\t\t\t")


def test__model_matched_update_not_existing_result_model(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120712", ResultModelPath="ahahapath2")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], MAPREDUCE_VAL2)
    assert_records_equal(recs[2], "task_2016120712\t\tahahapath2\t\t\t")


def test__model_matched_update_not_existing_all(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120712",
                                            ResultModelPath="ahahapath2",
                                            PrevModelPath="ahahapath")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], MAPREDUCE_VAL2)
    assert_records_equal(recs[2], "task_2016120712\tahahapath\tahahapath2\t\t\t")


# TESTS FOR EDITING EXISTING RECORD

def test__model_matched_update_existing_empty(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120701")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], MAPREDUCE_VAL2)


def test__model_matched_update_existing_prev_model(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120701", PrevModelPath="newpath1")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1],
                         '\t'.join(["task_2016120701", "newpath1", MAPREDUCE_VAL2.split('\t')[2]] + ["01", "02", "03"]))


def test__model_matched_update_existing_result_model(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120701", ResultModelPath="newpath2")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1],
                         '\t'.join(["task_2016120701", MAPREDUCE_VAL2.split('\t')[1], "newpath2"] + ["01", "02", "03"]))


def test__model_matched_update_existing_all(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.update_task_id_model_info("task_2016120701",
                                            ResultModelPath="newpath2",
                                            PrevModelPath="newpath1")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], '\t'.join(["task_2016120701", "newpath1", "newpath2"] + ["01", "02", "03"]))


# TEST CONNECT MODELS


def test__model_matched_connect_existing_tasks(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.connect_tasks("task_2016120700", "task_2016120701")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_cur_datetime_right(recs[1])
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], '\t'.join(["task_2016120701",
                                             MAPREDUCE_VAL1.split('\t')[2],
                                             MAPREDUCE_VAL2.split('\t')[2],
                                             MAPREDUCE_VAL1.split('\t')[4],
                                             "02",
                                             "03"]))


def test__model_matched_connect_tasks_add_new(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    model_matcher.connect_tasks("task_2016120701", "task_2016120702")
    recs = [r for r in read_ts_table(prev_model_table)]
    assert_records_equal(recs[0], MAPREDUCE_VAL1)
    assert_records_equal(recs[1], MAPREDUCE_VAL2)
    assert_records_equal(recs[2], '\t'.join(
        ["task_2016120702", MAPREDUCE_VAL2.split('\t')[2], "", MAPREDUCE_VAL2.split('\t')[4], "", ""]))
    assert_cur_datetime_right(recs[2])


def test__model_matched_connect_tasks_with_not_existing(prev_model_table, flowkeeper_conf, local_yt):  # noqa
    model_matcher = get_model_matcher_from_flowkeeper_conf(flowkeeper_conf.online_flowkeeper.model_matcher)
    with pytest.raises(TaskIdInfoError):
        model_matcher.connect_tasks("task_2016139493", "task_2016120701")
