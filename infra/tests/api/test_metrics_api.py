import http.client

import pytest

from infra.walle.server.tests.lib.util import TestCase


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request, healthdb=True)


def test_automation_metrics(test):
    test.mock_project({"id": "test"})
    test.automation_plot.mock()

    result_yasm = test.api_client.get("/metrics/v1/automation?format=yasm")
    assert result_yasm.status_code == http.client.OK
    assert result_yasm.json

    result_no_args = test.api_client.get("/metrics/v1/automation")
    assert result_no_args.status_code == http.client.OK
    assert result_no_args.json == result_yasm.json
