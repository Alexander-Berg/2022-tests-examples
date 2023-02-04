from unittest import mock
from typing import Optional

import pytest

from billing.dcsaap.backend.project import utils


class TestGetCPUGuarantee:
    box_id = 'test-pod-1'
    cpu_count = 8

    @pytest.fixture(autouse=True)
    def setup(self, requests_mock):
        self.requests_mock = requests_mock

    @pytest.fixture(autouse=True)
    def mock_box_id(self):
        with mock.patch('os.getenv', return_value=self.box_id):
            yield

    @pytest.fixture(autouse=True)
    def mock_cpu_count(self):
        with mock.patch('os.cpu_count', return_value=self.cpu_count):
            yield

    def mock_guarantees(self, box_millicores: Optional[float] = None, pod_millicores: Optional[float] = None):
        assert (box_millicores or pod_millicores) is not None, 'you should specify cpu guarantees'

        response = {}
        if box_millicores:
            cpu = response.setdefault('box_resource_requirements', {}).setdefault(self.box_id, {}).setdefault('cpu', {})
            cpu['cpu_guarantee_millicores'] = float(box_millicores)

        if pod_millicores:
            cpu = response.setdefault('resource_requirements', {}).setdefault('cpu', {})
            cpu['cpu_guarantee_millicores'] = float(pod_millicores)

        url = 'http://localhost:1/pod_attributes'
        self.requests_mock.register_uri('GET', url, json=response)

    @pytest.mark.parametrize(
        'box,pod,expected',
        [
            (-1, -1, 1),
            (0, 0, 1),
            (2048, 0, 2),
            (0, 2048, 2),
            (1024, 2048, 1),
            (10240, 10240, 8),
        ],
    )
    def test_get_cpu_guarantee(self, box, pod, expected):
        self.mock_guarantees(box, pod)
        value = utils.get_cpu_guarantee()
        assert value == expected
