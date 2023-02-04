import vh
import pytest

from yql_api import yql_api
from mongo_runner import mongo
from yql_utils import tmpdir_module

from ads.libs.yql.testlib import local_yt_client, local_yt_proxy, local_yql_params

from ads.factor_check.mutial_information.yql.lib import yql_digitize_columns, yql_calculate_mi, yql_calculate_mi_with_digitize
from math import sqrt, fabs
from random import randint, random


@pytest.fixture
def test_data(local_yt_client):
    data = [
        {"F1": x, "F2": x ** 2, "Weight": 1, "Predict": random(), "IsClick": int((x % 4) == 0)} for x in xrange(100)
    ]
    table_path = "//test_table_for_digitize"
    local_yt_client.write_table(table_path, data, format="json")
    return table_path


def mi_test_data_1(local_yt_client):
    data = [
        {"F1": int((x % 2) == 0), "Weight": 1, "Predict": randint(0, 1), "IsClick": int((x % 4) == 0)} for x in xrange(1000)
    ]
    table_path = "//test_table_for_mi_calc_1"
    local_yt_client.write_table(table_path, data, format="json")
    return table_path


def mi_test_data_2(local_yt_client):
    data = [
        {"F1": randint(0, 10), "Weight": 1, "Predict": int((x % 2) == 0), "IsClick": int((x % 4) == 0)} for x in xrange(1000)
    ]
    table_path = "//test_table_for_mi_calc_2"
    local_yt_client.write_table(table_path, data, format="json")
    return table_path


def do_check_1(stats):
    assert stats['F1']['train']['MI_bin'] < 2e-2
    assert stats['F1']['train']['MI_bin_c_permuted'] < 2e-2
    assert stats['F1']['train']['MI_bin_c'] > 0.2


def do_check_2(stats):
    assert stats['F1']['train']['MI_bin'] > 0.2
    assert stats['F1']['train']['MI_bin_c_permuted'] > 0.2
    assert fabs(stats['F1']['train']['MI_bin_c'] - stats['F1']['train']['MI_bin_c_permuted']) < 2e-2


def test_digitize(local_yt_client, local_yql_params, local_yt_proxy, test_data):
    with vh.Graph() as graph:
        res = yql_digitize_columns(
            src_table=test_data,
            yql_token="test_secret_name",
            yql_params=local_yql_params,
            columns_desc=[
                {"column": "F1", "weight": "Weight", "bins": 5},
                {"column": "F2", "weight": "Weight", "bins": 10}
            ]
        ).dest_table

    keeper = vh.run(
        graph,
        secrets={'test_secret_name': 'test_secret_value'},
        yt_token='test_yt_token',
        yt_proxy=local_yt_proxy,
        backend=vh.LocalBackend()
    )

    res_table_path = keeper.download(res)
    rows = local_yt_client.read_table(res_table_path)
    for row in rows:
        assert row["F1"] / 20 == row["F1_digitized_5_bins"]
        assert int(sqrt(row["F2"])) / 10 == row["F2_digitized_10_bins"]


@pytest.mark.parametrize("data,check", [(mi_test_data_1, do_check_1), (mi_test_data_2, do_check_2)])
def test_mi_calculation(local_yt_client, local_yql_params, local_yt_proxy, data, check):
    with vh.Graph() as graph:
        res = yql_calculate_mi(
            src_table=data(local_yt_client),
            factors_desc=[{"name": "F1", "hash_size": int(1e6), "f_hash_size": int(1e6)}],
            weight="Weight",
            target="IsClick",
            predict="Predict",
            base_factor="",
            qid="",
            split_field="",
            yql_token="test_secret_name",
            yql_params=local_yql_params
        )

    keeper = vh.run(
        graph,
        secrets={"test_secret_name": "test_secret_value"},
        yt_token="test_yt_token",
        yt_proxy=local_yt_proxy,
        backend=vh.LocalBackend()
    )
    stats = keeper.download(res)
    check(stats)


def test_mi_calculation_with_digitize(local_yt_client, local_yql_params, local_yt_proxy, test_data):
    with vh.Graph() as graph:
        res = yql_calculate_mi_with_digitize(
            src_table=test_data,
            factors_desc=[{"name": "F1", "hash_size": int(1e6), "f_hash_size": int(1e6), "bins": 10}],
            weight="Weight",
            target="IsClick",
            predict="Predict",
            base_factor="",
            qid="",
            split_field="",
            yql_token="test_secret_name",
            yql_params=local_yql_params
        )

    keeper = vh.run(
        graph,
        secrets={"test_secret_name": "test_secret_value"},
        yt_token="test_yt_token",
        yt_proxy=local_yt_proxy,
        backend=vh.LocalBackend()
    )
    stats = keeper.download(res)
    print stats
