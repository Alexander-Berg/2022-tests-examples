# coding=utf-8

from infra.yp.monitoring.account_overuse_monitoring.lib import fetchers
from infra.yp.monitoring.account_overuse_monitoring.lib import main
from infra.yp.monitoring.account_overuse_monitoring.lib import reporters

import yatest.common
import json


class FakeYpAccountFetcher(fetchers.IYPFetcher):
    def __init__(self, accounts_data, segments_data):
        self.accounts_data = accounts_data
        self.segments_data = segments_data

    def query_accounts(self):
        for item in self.accounts_data:
            yield item

    def query_segments(self):
        return self.segments_data


def setup_fake_yp_fetcher(accounts_dataset, segments_dataset):
    accounts_data_path = yatest.common.test_source_path(accounts_dataset)
    with open(accounts_data_path) as f:
        accounts_data = json.load(f)
    segments_data_path = yatest.common.test_source_path(segments_dataset)
    with open(segments_data_path) as f:
        segments_data = json.load(f)

    return FakeYpAccountFetcher(accounts_data, segments_data)


class FakeAbcAccountFetcher(fetchers.IABCServiceFetcher):
    def __init__(self, mock_data):
        self.mock_data = mock_data

    def fetch_all_service_ids(self):
        return set(iter(self.mock_data))


class CapturingSolomonReporter(reporters.ISolomonReporter):

    def __init__(self):
        self._captured = []

    def push_sensors(self, project, service, cluster, common_labels, sensors):
        common_labels = common_labels.copy()
        common_labels.update(dict(project=project, service=service, cluster=cluster))
        solomon_packet = dict(
            commonLabels=common_labels,
            sensors=sensors,
        )

        self._captured.append(solomon_packet)

    def get_pushed_sensor_count(self):
        return sum(len(x["sensors"]) for x in self._captured)

    def get_captured(self):
        return self._captured


class CapturingJugglerReporter(reporters.JugglerReporterBase):
    def __init__(self):
        self._captured = []

    def _do_notify(self, payload):
        self._captured.append(payload)

    def get_captured(self):
        return self._captured


def test_integral():
    bootstrap = main.Bootstrap(
        abc=FakeAbcAccountFetcher([5787, 1, 2, 3]),
        juggler_reporter=CapturingJugglerReporter(),
        cluster="fake_cluster",
        yp_fetcher=setup_fake_yp_fetcher("data/yp_accounts.json", "data/node_segments.json"),
        solomon_reporter=CapturingSolomonReporter(),
    )

    main.process_cluster(bootstrap)

    result = {
        "juggler_reports": bootstrap.juggler_reporter.get_captured(),
        "solomon_reports": bootstrap.solomon_reporter.get_captured()
    }
    for item in result["solomon_reports"]:
        item["sensors"].sort(
            key=lambda x: (x["labels"]["kind"], x["labels"]["flavor"], x["labels"]["metric"], x["labels"]["sensor"])
        )
    return result
