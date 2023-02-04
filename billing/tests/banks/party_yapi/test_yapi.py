from bcl.banks.registry import Yapi
from bcl.core.models import Currency


def test_payment_creator(build_payment_bundle, time_freeze, get_assoc_acc_curr):

    associate = Yapi
    _, acc, _ = get_assoc_acc_curr(associate, account='12345678')

    attrs = {
        'f_acc': acc.number,
        't_name': 'recip',
        't_iban': 'TR930006700010000001234567',
        'currency_id': Currency.TRY,
        'ground': 'mypay',
    }

    with time_freeze('2022-02-05'):
        compiled = build_payment_bundle(associate, payment_dicts=[attrs]).tst_compiled

    assert compiled == (
        'H05022022\r\n'
        'D0502202212345678TRYrecip                                             '
        'TR930006700010000001234567000000000000152.00mypay                                             '
        '1                          99\r\n'
        'T00001'
    )
