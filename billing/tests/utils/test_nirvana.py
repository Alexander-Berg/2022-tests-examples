import json
import pathlib
from unittest import mock

import pytest

from . import project_utils as pu


class TestCPUGuarantee:
    context_file_name = 'job_context.json'

    @pytest.fixture(autouse=True)
    def setup(self, tmp_path: pathlib.Path, monkeypatch):
        root = tmp_path / 'TestCPUGuarantee'
        root.mkdir()
        monkeypatch.chdir(root)
        yield

    @pytest.fixture(autouse=True)
    def os_cpu_count_mock(self):
        self.os_cpu_count = 666
        with mock.patch('os.cpu_count', return_value=self.os_cpu_count) as m:
            yield m

    @pytest.fixture
    def change_guarantee(self, percent: int, job_context_json: dict):
        job_context_json['parameters']['cpu-guarantee'] = percent
        with open(self.context_file_name, 'w') as f:
            json.dump(job_context_json, f)

    def test_local_run(self):
        expected = self.os_cpu_count
        result = pu.CPUGuarantee.get()
        assert result == expected

    @pytest.mark.parametrize('percent, expected', ((0, 1), (25, 1), (150, 1), (210, 2), (77700, 666)))
    def test_job_environment(self, change_guarantee, expected):
        result = pu.CPUGuarantee.get()
        assert result == expected
