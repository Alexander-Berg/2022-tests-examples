# -*- coding: utf-8 -*-
import sys
import json
import logging
import requests

from infra.cores.ui_tests import site_checker as sc
from library.python import selenium_ui_test
from library.python import coredump_filter


class ClusterType:
    PRODUCTION = "production"
    TESTING = "testing"
    DEV = "dev"
    LOCALHOST = "localhost"  # selenium tests will not work


class Cluster:
    def __init__(self, url, timeout):
        self.cluster_type = None
        self.url = url
        self.timeout = timeout


CLUSTERS = {
    ClusterType.PRODUCTION: Cluster(
        url="https://coredumps.yandex-team.ru",
        timeout=10,
    ),
    ClusterType.TESTING: Cluster(
        url="https://coredumps-testing.yandex-team.ru",
        timeout=10,
    ),
    ClusterType.DEV: Cluster(
        url="http://qyp-seek-6.vla.yp-c.yandex.net:20500",
        timeout=10,
    ),
    ClusterType.LOCALHOST: Cluster(
        url="http://localhost:20500",
        timeout=10,
    ),
}

for cluster_type in CLUSTERS:
    CLUSTERS[cluster_type].cluster_type = cluster_type


def test_main_screen(site_checker):
    site_checker.check_text(url_path="/", text="Cores")
    site_checker.check_text(url_path="/", text="Clear all filters")


def test_main_screen_with_bad_filter(site_checker):
    site_checker.check_text(url_path="/?itype=testenv1", text="Корок с такими параметрами запроса сейчас в базе нет")


def test_parse_core():
    core_text = open('../test2.txt').read()
    logging.error("core_text len: %s", len(core_text))
    core_parsed, raw_core, signal = coredump_filter.filter_stack_dump(core_text=core_text)
    core_json_dump = coredump_filter.serialize_stacks(core_parsed)
    logging.error("SIGNAL %s", signal)
    open('core.dump.json', 'w').write(core_json_dump)


def test_submit_core(site_checker):
    logging.error("BEFORE")
    sys.stderr.write(site_checker.cluster.url)
    submit_core_url = site_checker.cluster.url + "/submit_core"
    logging.error("Submitting core to %s", submit_core_url)
    request = requests.post(
        submit_core_url,
        json={
            "itype": "cores_test",
            "prj": "test1",
            "ctype": "qqq",
            "dump_json": {},
            "parsed_traces": open("../test2.txt").read(),
        },
        timeout=60,
    )
    with open('r.json', 'w') as f:
        f.write(json.dumps({
            "itype": "cores_test",
            "prj": "test1",
            "ctype": "qqq",
            "dump_json": {},
            "parsed_traces": open("../test2.txt").read(),
        }))

    logging.error("AFTER")
    assert request
    response = request.json()
    core_hash = response["core_hash"]
    print("core_hash: ", core_hash)

    print(json.dumps(response.json(), indent=4))

    if site_checker.cluster.cluster_type == ClusterType.LOCALHOST:
        return

    site_checker.check_text(url_path="/?itype=cores_test&prj_list=test1", text="THttpFetcher::TAuxRequestTask")
    # FIXME(mvel): dumb method used
    site_checker.navigator.driver.find_element_by_xpath("//a[text()=\"Details\"]").click()
    # print(site_checker.navigator.driver.page_source)


def test_all(results_folder=None, cluster_type=ClusterType.TESTING):
    test_parse_core()

    for driver in selenium_ui_test.Drivers().drivers:
        with driver:
            site_checker = sc.SiteChecker(driver, results_folder, CLUSTERS[cluster_type])

            # test_parse_core()
            test_submit_core(site_checker)

            if cluster_type != ClusterType.LOCALHOST:
                # these tests use Selenuim and it cannot access localhost
                test_main_screen(site_checker)
                test_main_screen_with_bad_filter(site_checker)

    logging.info("Tests OK")
