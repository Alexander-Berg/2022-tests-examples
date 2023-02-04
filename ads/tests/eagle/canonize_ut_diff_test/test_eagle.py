import collections.abc
import json
import logging
import os
import pickle
from urllib.parse import quote

import pytest
import requests
import yatest.common
from ads.bsyeti.tests.test_lib.data_collector.config import (
    COMMON_REQUESTS,
    REQUESTS_DIR,
    TIMESTAMP_PATH,
)
from ads.bsyeti.tests.test_lib.eagle_daemon import get_eagle_protos
from library.python.testing.deprecated import setup_environment

logging.getLogger("urllib3").setLevel(logging.CRITICAL)

GT_NOGLUE = "noGlue"
GT_VULTCRYPTA = "VultureCrypta"
GT_IS2 = "IS2.0"

PROTO_OUTPUT = "output/proto"


def ensure_output_dir(output_dir):
    if not os.path.exists(output_dir):
        try:
            os.makedirs(output_dir)
        except os.error as e:
            logging.info("cannot create directory %s, %s", output_dir, e)


def get_glue_types():
    return [GT_NOGLUE, GT_VULTCRYPTA]


def get_accesslog_clients_for_test():
    return [
        "yabs",
        "search",
        "ssp",
        "ks_1",
        "server_count",
        "distribution_main_page",
        "yabs-market",
        "laas",
        "adfox",
        "yabs-meta-bannerset",
        "ssp-international",
        "metrika",
        "appmetrica-cam",
    ]


def update_dict_recursive(dict_o, dict_n):
    for key, val in dict_n.items():
        if isinstance(val, collections.abc.Mapping):
            dict_o[key] = update_dict_recursive(dict_o.get(key, {}), val)
        else:
            dict_o[key] = val
    return dict_o


@pytest.fixture(scope="module")
def testing_data_time():
    with open(TIMESTAMP_PATH, "r") as file_pointer:
        return {"TEST_TIME": int(file_pointer.readlines()[0])}


@pytest.fixture(scope="module")
def real_requests():
    result = {}
    avaliable_clients = os.listdir(REQUESTS_DIR)
    for client in avaliable_clients:
        with open(REQUESTS_DIR + "/" + client, "rb") as f_p:
            result[client] = pickle.load(f_p)
    logging.info("total clients: %d", len(result))
    return result


@pytest.fixture(scope="module")
def common_requests():
    with open(COMMON_REQUESTS, "rb") as f_p:
        requests = pickle.load(f_p)
    logging.info("total common requests: %d", len(requests))
    return requests


def eagle_tester_proto(
    test_time,
    eagle_port,
    all_requests,
    test_name,
    glue_param="",
    experiment_parameters="",
    client_param="",
    test_file_name="answers",
):
    uids = yatest.common.get_param("uids")
    assert not uids

    assert all_requests, "zero requests"

    experiment_parameter_base = {
        "EagleSettings": {
            "LoadSettings": {
                "UseVultureCrypta": False,
            }
        }
    }

    if experiment_parameters not in ({}, ""):
        update_dict_recursive(experiment_parameter_base, experiment_parameters)

    exp_json = "&exp-json=" + quote(json.dumps(experiment_parameter_base))

    if glue_param != "":
        glue_param = "&glue=" + str(int(glue_param))

    if client_param != "":
        client_param = "&" + client_param

    url_body_suffix = (
        "&format=protobuf"
        "&strict=1"
        "&seed=100"
        "&time={time}"
        "{glue_param}"
        "&timeout=10000"
        "{client_param}"
        "{exp_param}"
    ).format(time=test_time, glue_param=glue_param, client_param=client_param, exp_param=exp_json)
    prepared_requests = [request + url_body_suffix for request in all_requests]

    url_template = "http://localhost:{port}".format(port=eagle_port)

    checkdata = get_eagle_protos(prepared_requests, url_template, test_file_name)

    full_path = yatest.common.output_path(os.path.join(PROTO_OUTPUT, test_name))
    ensure_output_dir(os.path.dirname(full_path))
    with open(full_path, "wb") as f_p:
        f_p.write(checkdata)

    resp = requests.get("http://localhost:{port}/sensors".format(port=eagle_port))
    assert resp.ok, resp.ok

    logging.info("All %d requests done", len(prepared_requests))


@pytest.fixture(scope="module")
def ready_testenv(
    eagle_module_with_only_rsya_tsar,
    all_yt_filled_tables,
    testing_data_time,
    common_requests,
    real_requests,
):
    return {
        "TEST_TIME": testing_data_time["TEST_TIME"],
        "EAGLE_PORT": eagle_module_with_only_rsya_tsar["EAGLE_PORT"],
        "common_requests": common_requests,
        "real_requests": real_requests,
    }


@pytest.mark.parametrize("glue_type", get_glue_types())
@pytest.mark.parametrize("client", get_accesslog_clients_for_test())
def test_real_clients_requests(ready_testenv, request, glue_type, client):
    setup_environment.setup_bin_dir()

    experiment_parameters = {}
    if glue_type == GT_VULTCRYPTA:
        experiment_parameters = {"EagleSettings": {"LoadSettings": {"UseVultureCrypta": True}}}

    if client in ready_testenv:
        return eagle_tester_proto(
            ready_testenv["TEST_TIME"],
            ready_testenv["EAGLE_PORT"],
            ready_testenv["real_requests"][client],
            test_name=request.node.name,
            glue_param=(glue_type != GT_NOGLUE),
            experiment_parameters=experiment_parameters,
            test_file_name="accesslog_answers",
        )
    else:
        client_arg = "client=" + client if not client.startswith("ks_") else "keyword-set=" + client.strip("ks_")
        return eagle_tester_proto(
            ready_testenv["TEST_TIME"],
            ready_testenv["EAGLE_PORT"],
            ready_testenv["common_requests"],
            test_name=request.node.name,
            glue_param=(glue_type != GT_NOGLUE),
            experiment_parameters=experiment_parameters,
            client_param=client_arg,
            test_file_name="common_answers",
        )
