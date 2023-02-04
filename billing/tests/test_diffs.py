"""
Тестируем логику работы модуля `diffs`
"""

import pytest

from billing.library.python.dcsaap_utils import diffs


class TestExtractKeys(object):
    """
    Тестируем логику функции `extract_keys`
    """

    @pytest.fixture
    def diff(self):
        return {
            # Ключ
            'product_id': 123456,
            'service_id': 777,

            # Информация о расхождении
            'column_name': 'amount',
            't1_value': '12.3456',
            't2_value': '12.3457',
            'diff_type': 1,
        }

    @pytest.fixture
    def expected_diff(self):
        return {
            'key1_name': 'product_id',
            'key1_value': 123456,
            'key2_name': 'service_id',
            'key2_value': 777,

            'column_name': 'amount',
            'column_value1': '12.3456',
            'column_value2': '12.3457',
            'type': 1,
        }

    @pytest.mark.parametrize('keys', [
        ['product_id', 'service_id'],
        'product_id service_id'
    ], ids=['list', 'string'])
    def test_different_keys_types(self, diff, expected_diff, keys):
        """
        Тестируем логику с разными типами ключей
        """
        result = diffs.extract_keys(keys, diff)
        assert result == expected_diff
