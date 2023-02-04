import os
import pytest
import mock
from yatest.common import source_path as yatest_source_path


@pytest.fixture
def get_image_size_mock():
    with mock.patch('infra.qyp.vmagent.src.helpers.get_image_size', mock.Mock()) as _mock:
        yield _mock


@pytest.fixture
def source_path():
    def _source_path(path):
        try:
            return yatest_source_path(path)
        except AttributeError:
            # only for local pycharm tests
            print "local pycharm tests"
            return os.path.join(os.environ["PWD"], path)

    return _source_path
