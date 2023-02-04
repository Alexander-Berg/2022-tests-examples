import pytest
import yatest.common


def test_diff_canonized__data_file_absent(diff_canonized):
    if yatest.common.get_param('canonize-output', False):
        # don't allow to canonize this test -- file should absent
        return

    with pytest.raises(FileNotFoundError, match='Results should be canonized first'):
        diff_canonized('{"json": "encoded"}')


def test_diff_canonized__diff_absent(diff_canonized):
    diff_canonized('{"json": "encoded"}')


def test_diff_canonized__diff_present(diff_canonized):
    if yatest.common.get_param('canonize-output', False):
        # don't allow to canonize this test -- testing diff presence
        return

    with pytest.raises(pytest.fail.Exception, match='different from canonical'):
        diff_canonized('{"data": "new"}')
