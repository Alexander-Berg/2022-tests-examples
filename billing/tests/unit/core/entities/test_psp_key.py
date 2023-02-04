import pytest

from billing.yandex_pay.yandex_pay.core.entities.psp_key import make_kid, unpack_kid
from billing.yandex_pay.yandex_pay.core.exceptions import CorePSPKidMalformedError


def test_make_kid():
    assert make_kid(psp_key_id=1, psp_external_id='gwid') == '1-gwid'


def test_unpack_kid():
    assert unpack_kid(make_kid(psp_key_id=1, psp_external_id='gwid')) == (1, 'gwid')


@pytest.mark.parametrize('wrong_kid', (
    'x-gwid', 'word'
))
def test_unpack_kid_errors(wrong_kid):
    with pytest.raises(CorePSPKidMalformedError):
        unpack_kid(wrong_kid)
