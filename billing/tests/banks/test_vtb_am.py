from datetime import datetime

import pytest

from bcl.banks.registry import VtbAm
from bcl.core.models import Currency
from bcl.exceptions import ValidationError


def test_payment_creator(get_payment_bundle, get_source_payment):

    attrs = {
        'f_acc': '321',
        't_acc': '123',
        'number': '444',
        'date': datetime(2017, 5, 26),
        'summ': '5125.52',
        'currency_id': Currency.AMD,
        'ground': 'Ground',
    }

    payments = [get_source_payment(attrs)]

    with pytest.raises(ValidationError):
        VtbAm.payment_dispatcher.get_creator(get_payment_bundle(payments)).create_bundle()

    attrs['summ'] = '5125.50'

    payments = [get_source_payment(attrs)]
    compiled = VtbAm.payment_dispatcher.get_creator(get_payment_bundle(payments)).create_bundle()

    assert compiled == (
        "<?xml version='1.0' encoding='utf-8' standalone='yes'?>\n"
        '<As_Import-Export_File><PayOrd CAPTION="Documents (Payment Inside of RA)">'
        '<PayOrd DOCNUM="444" DOCDATE="26/05/17" PAYERACC="321" PAYER="OOO Яндекс" BENACC="123" '
        'BENEFICIARY="ООО &quot;Кинопортал&quot;" '
        'AMOUNT="5125.50" CURRENCY="AMD" DETAILS="Ground"/></PayOrd></As_Import-Export_File>')
