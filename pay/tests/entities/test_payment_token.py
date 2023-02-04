import pytest

from pay.lib.entities.payment_token import MITInfo


class TestMITInfo:
    @pytest.mark.parametrize(
        'mit_info, expected_emptiness',
        [
            (MITInfo(), True),
            (MITInfo(recurring=True), False),
            (MITInfo(deferred=True), False),
        ],
    )
    def test_empty(self, mit_info, expected_emptiness):
        assert mit_info.is_empty() == expected_emptiness
